// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import com.fasterxml.jackson.databind.JsonNode;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingRecordingStarted;
import com.zextras.carbonio.async.model.MeetingRecordingStopped;
import com.zextras.carbonio.ws.plugin.api.EventPublisher;
import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RecordingService {

  private static final Logger LOG = Logger.getLogger(RecordingService.class);
  static final DateTimeFormatter DIR_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

  private final MeetingAccess meetings;
  private final VideoServerGateway videoServer;
  private final RecordingRepository recordings;
  private final VideoRecorderClient videoRecorder;
  private final EventPublisher events;
  private final RecordingConfig config;
  private final Executor executor;
  private final Clock clock;

  @Inject
  public RecordingService(
      MeetingAccess meetings,
      VideoServerGateway videoServer,
      RecordingRepository recordings,
      VideoRecorderClient videoRecorder,
      EventPublisher events,
      RecordingConfig config) {
    // A plain pool, not ManagedExecutor: ManagedExecutor propagates the JTA transaction to the async
    // task, which then completes the request's transaction from a second thread ("Commit invoked
    // while multiple threads active"). The post-processing call must not be transaction-bound.
    this(meetings, videoServer, recordings, videoRecorder, events, config,
        Infrastructure.getDefaultWorkerPool(), Clock.systemUTC());
  }

  RecordingService(
      MeetingAccess meetings,
      VideoServerGateway videoServer,
      RecordingRepository recordings,
      VideoRecorderClient videoRecorder,
      EventPublisher events,
      RecordingConfig config,
      Executor executor,
      Clock clock) {
    this.meetings = meetings;
    this.videoServer = videoServer;
    this.recordings = recordings;
    this.videoRecorder = videoRecorder;
    this.events = events;
    this.config = config;
    this.executor = executor;
    this.clock = clock;
  }

  @Transactional
  public void start(UUID meetingId, String folderId, UserPrincipal user) {
    MeetingView meeting = meetings.requireActiveMeetingModerator(meetingId, user);
    if (recordings.findStarted(meetingId.toString()).isPresent()) {
      return;
    }
    VideoServerRooms rooms = rooms(meetingId);
    String path = config.recordingsPath() + "meeting_" + meetingId + "/"
        + OffsetDateTime.now(clock).format(DIR_TIMESTAMP);
    require(videoServer.sendVideoRoomMessage(meetingId,
        Map.of("request", "edit", "room", rooms.videoRoomId(), "new_rec_dir", path)),
        "setting rec dir on videoroom", meetingId);
    require(videoServer.sendAudioBridgeMessage(meetingId,
        Map.of("request", "enable_mjrs", "room", rooms.audioRoomId(), "mjrs", true, "mjrs_dir", path)),
        "recording the audiobridge room", meetingId);
    require(videoServer.sendVideoRoomMessage(meetingId,
        Map.of("request", "enable_recording", "room", rooms.videoRoomId(), "record", true)),
        "recording the videoroom", meetingId);

    RecordingEntity recording = new RecordingEntity();
    recording.id = UUID.randomUUID().toString();
    recording.meetingId = meetingId.toString();
    recording.starterId = user.id().toString();
    recording.folderId = folderId;
    recording.startedAt = OffsetDateTime.now(clock);
    recording.status = RecordingStatus.STARTED;
    recordings.persist(recording);

    events.sendToUsers(meeting.participantUserIds(),
        MeetingRecordingStarted.create()
            .meetingId(meetingId)
            .userId(user.id())
            .type(EventType.MEETING_RECORDING_STARTED)
            .sentDate(OffsetDateTime.now(clock)));
  }

  @Transactional
  public void stop(UUID meetingId, UserPrincipal user) {
    stopActive(meetings.requireActiveMeetingModerator(meetingId, user), user.id());
  }

  /** Replaces ADV's stopActiveRecording seam: runs inside core's transaction, rooms still alive. */
  @Transactional
  void onMeetingStopping(@Observes MeetingStopping event) {
    meetings.find(event.meetingId()).ifPresent(meeting -> stopActive(meeting, event.userId()));
  }

  private void stopActive(MeetingView meeting, UUID userId) {
    recordings.findStarted(meeting.id().toString()).ifPresent(recording -> {
      VideoServerRooms rooms = rooms(meeting.id());
      require(videoServer.sendAudioBridgeMessage(meeting.id(),
          Map.of("request", "enable_mjrs", "room", rooms.audioRoomId(), "mjrs", false)),
          "stopping the audiobridge recording", meeting.id());
      require(videoServer.sendVideoRoomMessage(meeting.id(),
          Map.of("request", "enable_recording", "room", rooms.videoRoomId(), "record", false)),
          "stopping the videoroom recording", meeting.id());
      recording.status = RecordingStatus.STOPPED;

      RecordingInfo info = new RecordingInfo(
          rooms.serverId(), meeting.id().toString(), meeting.name(), recording.starterId, recording.folderId);
      CompletableFuture.runAsync(() -> videoRecorder.process(info), executor)
          .exceptionally(e -> {
            LOG.errorf(e, "Recording post-processing failed for meeting %s", info.meetingId());
            return null;
          });

      events.sendToUsers(meeting.participantUserIds(),
          MeetingRecordingStopped.create()
              .meetingId(meeting.id())
              .userId(userId)
              .type(EventType.MEETING_RECORDING_STOPPED)
              .sentDate(OffsetDateTime.now(clock)));
    });
  }

  private VideoServerRooms rooms(UUID meetingId) {
    return videoServer.roomsOf(meetingId)
        .orElseThrow(() -> new IllegalStateException("No videoserver meeting found for the meeting " + meetingId));
  }

  private static void require(JsonNode response, String what, UUID meetingId) {
    if (!"success".equals(response.path("janus").asText())) {
      throw new IllegalStateException("An error occurred while " + what + " for meeting " + meetingId);
    }
  }
}
