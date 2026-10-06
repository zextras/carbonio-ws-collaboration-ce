// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.MeetingRecordingStarted;
import com.zextras.carbonio.async.model.MeetingRecordingStopped;
import com.zextras.carbonio.ws.plugin.api.EventPublisher;
import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.MeetingView;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.UserType;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecordingServiceTest {

  static final JsonNode OK = new ObjectMapper().createObjectNode().put("janus", "success");
  static final JsonNode KO = new ObjectMapper().createObjectNode().put("janus", "error");

  MeetingAccess meetings = mock(MeetingAccess.class);
  VideoServerGateway videoServer = mock(VideoServerGateway.class);
  RecordingRepository recordings = mock(RecordingRepository.class);
  VideoRecorderClient videoRecorder = mock(VideoRecorderClient.class);
  EventPublisher events = mock(EventPublisher.class);
  RecordingConfig config = mock(RecordingConfig.class);
  AfterCommit afterCommit = Runnable::run;
  Clock clock = Clock.fixed(Instant.parse("2026-10-06T10:15:30Z"), ZoneOffset.UTC);
  RecordingService service =
      new RecordingService(meetings, videoServer, recordings, videoRecorder, events, config, Runnable::run, clock, afterCommit);

  UUID meetingId = UUID.randomUUID();
  UUID owner = UUID.randomUUID();
  UUID other = UUID.randomUUID();
  UserPrincipal user = new UserPrincipal(owner, UserType.INTERNAL, "e", "n", "t", null, Map.of());
  MeetingView view = new MeetingView(meetingId, "Weekly", UUID.randomUUID(), true, List.of(owner, other));

  @BeforeEach
  void setUp() {
    when(config.recordingsPath()).thenReturn("/rec/");
    when(meetings.requireActiveMeetingModerator(meetingId, user)).thenReturn(view);
    when(meetings.find(meetingId)).thenReturn(Optional.of(view));
    when(videoServer.roomsOf(meetingId)).thenReturn(Optional.of(new VideoServerRooms("vs-1", "audio-room", "video-room")));
    when(videoServer.sendVideoRoomMessage(eq(meetingId), any())).thenReturn(OK);
    when(videoServer.sendAudioBridgeMessage(eq(meetingId), any())).thenReturn(OK);
  }

  RecordingEntity started() {
    RecordingEntity r = new RecordingEntity();
    r.id = "r1";
    r.meetingId = meetingId.toString();
    r.starterId = owner.toString();
    r.folderId = "f1";
    r.status = RecordingStatus.STARTED;
    return r;
  }

  @Test
  void startSendsTheJanusSequenceAndPersists() {
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.empty());

    service.start(meetingId, "f1", user);

    String path = "/rec/meeting_" + meetingId + "/20261006T101530";
    var order = inOrder(videoServer);
    order.verify(videoServer).sendVideoRoomMessage(meetingId,
        Map.of("request", "edit", "room", "video-room", "new_rec_dir", path));
    order.verify(videoServer).sendAudioBridgeMessage(meetingId,
        Map.of("request", "enable_mjrs", "room", "audio-room", "mjrs", true, "mjrs_dir", path));
    order.verify(videoServer).sendVideoRoomMessage(meetingId,
        Map.of("request", "enable_recording", "room", "video-room", "record", true));
    ArgumentCaptor<RecordingEntity> saved = ArgumentCaptor.forClass(RecordingEntity.class);
    verify(recordings).persist(saved.capture());
    assertThat(saved.getValue().status).isEqualTo(RecordingStatus.STARTED);
    assertThat(saved.getValue().starterId).isEqualTo(owner.toString());
    assertThat(saved.getValue().folderId).isEqualTo("f1");
    verify(events).sendToUsers(eq(List.of(owner, other)), argThat(e -> e instanceof MeetingRecordingStarted));
  }

  @Test
  void startIsIdempotent() {
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(started()));

    service.start(meetingId, "f1", user);

    verify(videoServer, never()).sendVideoRoomMessage(any(), any());
    verify(recordings, never()).persist(any(RecordingEntity.class));
    verifyNoInteractions(events);
  }

  @Test
  void startFailsWhenJanusRefuses() {
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.empty());
    when(videoServer.sendVideoRoomMessage(eq(meetingId), any())).thenReturn(KO);

    assertThatThrownBy(() -> service.start(meetingId, "f1", user)).isInstanceOf(IllegalStateException.class);
    verify(recordings, never()).persist(any(RecordingEntity.class));
  }

  @Test
  void meetingStoppingStopsTheActiveRecording() {
    RecordingEntity recording = started();
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(recording));

    service.onMeetingStopping(new MeetingStopping(meetingId, other));

    verify(videoServer).sendAudioBridgeMessage(meetingId,
        Map.of("request", "enable_mjrs", "room", "audio-room", "mjrs", false));
    verify(videoServer).sendVideoRoomMessage(meetingId,
        Map.of("request", "enable_recording", "room", "video-room", "record", false));
    assertThat(recording.status).isEqualTo(RecordingStatus.STOPPED);
    verify(videoRecorder).process(new RecordingInfo("vs-1", meetingId.toString(), "Weekly", owner.toString(), "f1"));
    verify(events).sendToUsers(eq(List.of(owner, other)), argThat(e ->
        e instanceof MeetingRecordingStopped s && other.equals(s.getUserId())));
  }

  @Test
  void meetingStoppingWithoutRecordingDoesNothing() {
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.empty());
    service.onMeetingStopping(new MeetingStopping(meetingId, other));
    verifyNoInteractions(videoRecorder, events);
  }

  @Test
  void postProcessingFailureDoesNotBreakTheStop() {
    RecordingEntity recording = started();
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(recording));
    doThrow(new RuntimeException("videorecorder down")).when(videoRecorder).process(any());

    service.stop(meetingId, user);

    assertThat(recording.status).isEqualTo(RecordingStatus.STOPPED);
  }

  @Test
  void postProcessingIsNotSentWhenTheTransactionRollsBack() {
    RecordingEntity recording = started();
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(recording));
    AfterCommit rolledBack = task -> { };
    RecordingService rollingBack = new RecordingService(
        meetings, videoServer, recordings, videoRecorder, events, config, Runnable::run, clock, rolledBack);

    rollingBack.stop(meetingId, user);

    verifyNoInteractions(videoRecorder);
  }

  @Test
  void meetingStoppingSurvivesAJanusRefusalAndStillClosesTheRecording() {
    RecordingEntity recording = started();
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(recording));
    when(videoServer.sendAudioBridgeMessage(eq(meetingId), any())).thenReturn(KO);

    service.onMeetingStopping(new MeetingStopping(meetingId, other));

    assertThat(recording.status).isEqualTo(RecordingStatus.STOPPED);
    verify(videoRecorder).process(any());
  }

  @Test
  void meetingStoppingSurvivesMissingRooms() {
    RecordingEntity recording = started();
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(recording));
    when(videoServer.roomsOf(meetingId)).thenReturn(Optional.empty());

    service.onMeetingStopping(new MeetingStopping(meetingId, other));

    assertThat(recording.status).isEqualTo(RecordingStatus.STOPPED);
  }

  @Test
  void explicitStopStillPropagatesJanusFailures() {
    when(recordings.findStarted(meetingId.toString())).thenReturn(Optional.of(started()));
    when(videoServer.sendAudioBridgeMessage(eq(meetingId), any())).thenReturn(KO);

    assertThatThrownBy(() -> service.stop(meetingId, user)).isInstanceOf(IllegalStateException.class);
  }
}
