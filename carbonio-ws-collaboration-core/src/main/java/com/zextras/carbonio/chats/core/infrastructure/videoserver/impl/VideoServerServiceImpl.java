// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerConfig;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.codec.VideoCodec;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Ptype;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerPluginRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeMuteRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomPublishRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomStartVideoInRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomUpdateSubscriptionsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.meeting.model.MediaStreamDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import jakarta.annotation.Nullable;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Singleton
public class VideoServerServiceImpl implements VideoServerService {

  private static final String JANUS_CREATE = "create";
  private static final String JANUS_MESSAGE = "message";
  private static final String JANUS_ATTACH = "attach";
  private static final String JANUS_DETACH = "detach";
  private static final String JANUS_DESTROY = "destroy";
  private static final String JANUS_SUCCESS = "success";
  private static final String JANUS_SERVER_INFO = "server_info";
  private static final String JANUS_VIDEOROOM_PLUGIN = "janus.plugin.videoroom";
  private static final String JANUS_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";

  private static final String AUDIO_VIDEO_PATTERN_NAME_WITH_TIMESTAMP = "%s_%s_%s";
  private static final String DATE_TIME_DEFAULT_FORMAT = "yyyyMMdd'T'HHmmss";

  private final VideoServerClient videoServerClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final Clock clock;

  private final String apiSecret;

  @Inject
  public VideoServerServiceImpl(
      VideoServerConfig videoServerConfig,
      VideoServerClient videoServerClient,
      VideoServerMeetingRepository videoServerMeetingRepository,
      VideoServerSessionRepository videoServerSessionRepository,
      Clock clock) {
    this.videoServerClient = videoServerClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
    this.clock = clock;
    this.apiSecret = videoServerConfig.getApiSecret();
  }

  @Override
  public CompletableFuture<Void> startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already active");
    }

    // Step 1: Create new connection for meeting
    return createMeetingConnection()
        .thenCompose(
            connectionResponse -> {
              String connectionId = connectionResponse.getDataId();

              // Step 2: Attach to both plugins in parallel
              CompletableFuture<VideoServerResponse> audioPluginFuture =
                  attachToPlugin(connectionId, JANUS_AUDIOBRIDGE_PLUGIN);

              CompletableFuture<VideoServerResponse> videoPluginFuture =
                  attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN);

              return CompletableFuture.allOf(audioPluginFuture, videoPluginFuture)
                  .thenCompose(
                      v -> {
                        String audioHandleId = audioPluginFuture.join().getDataId();
                        String videoHandleId = videoPluginFuture.join().getDataId();

                        // Step 3: Create rooms for both audio and video plugins in
                        // parallel
                        CompletableFuture<AudioBridgeResponse> audioRoomFuture =
                            createAudioBridgeRoom(meetingId, connectionId, audioHandleId);

                        CompletableFuture<VideoRoomResponse> videoRoomFuture =
                            createVideoRoom(meetingId, connectionId, videoHandleId);

                        return CompletableFuture.allOf(audioRoomFuture, videoRoomFuture)
                            .thenRun(
                                () -> {
                                  String audioRoomId = audioRoomFuture.join().getRoom();
                                  String videoRoomId = videoRoomFuture.join().getRoom();

                                  videoServerMeetingRepository.insert(
                                      VideoServerMeeting.create()
                                          .meetingId(meetingId)
                                          .connectionId(connectionId)
                                          .audioHandleId(audioHandleId)
                                          .videoHandleId(videoHandleId)
                                          .audioRoomId(audioRoomId)
                                          .videoRoomId(videoRoomId));
                                });
                      });
            })
        .exceptionally(
            ex -> {
              throw new VideoServerException(
                  String.format("Failed to start meeting: %s", meetingId), ex);
            });
  }

  private CompletableFuture<VideoServerResponse> createMeetingConnection() {
    return createConnection()
        .thenApply(
            response -> {
              if (!JANUS_SUCCESS.equals(response.getStatus())) {
                throw new VideoServerException("Error creating video server connection");
              }
              return response;
            });
  }

  private CompletableFuture<VideoServerResponse> attachToPlugin(
      String connectionId, String pluginType) {
    return interactWithConnection(connectionId, JANUS_ATTACH, pluginType)
        .thenApply(
            response -> {
              if (!JANUS_SUCCESS.equals(response.getStatus())) {
                throw new VideoServerException("Error attaching to plugin " + pluginType);
              }
              return response;
            });
  }

  private CompletableFuture<AudioBridgeResponse> createAudioBridgeRoom(
      String meetingId, String connectionId, String audioHandleId) {

    AudioBridgeCreateRequest audioRequest =
        AudioBridgeCreateRequest.create()
            .request(AudioBridgeCreateRequest.CREATE)
            .room(AudioBridgeCreateRequest.ROOM_DEFAULT + meetingId)
            .permanent(false)
            .description(AudioBridgeCreateRequest.DESCRIPTION_DEFAULT + meetingId)
            .isPrivate(false)
            .record(false)
            .samplingRate(AudioBridgeCreateRequest.SAMPLING_RATE_DEFAULT)
            .audioActivePackets(AudioBridgeCreateRequest.AUDIO_ACTIVE_PACKETS_DEFAULT)
            .audioLevelAverage(AudioBridgeCreateRequest.AUDIO_LEVEL_AVERAGE_DEFAULT)
            .audioLevelEvent(true);

    return sendAudioBridgePluginMessage(connectionId, audioHandleId, audioRequest, null)
        .thenApply(
            response -> {
              if (!AudioBridgeResponse.CREATED.equals(response.getAudioBridge())) {
                throw new VideoServerException(
                    "An error occurred when creating an audiobridge room for the connection "
                        + connectionId
                        + " with plugin "
                        + audioHandleId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  private CompletableFuture<VideoRoomResponse> createVideoRoom(
      String meetingId, String connectionId, String videoHandleId) {

    VideoRoomCreateRequest videoRequest =
        VideoRoomCreateRequest.create()
            .request(VideoRoomCreateRequest.CREATE)
            .room(VideoRoomCreateRequest.ROOM_DEFAULT + meetingId)
            .permanent(false)
            .description(VideoRoomCreateRequest.DESCRIPTION_DEFAULT + meetingId)
            .isPrivate(false)
            .record(false)
            .publishers(VideoRoomCreateRequest.MAX_PUBLISHERS_DEFAULT)
            .bitrate(VideoRoomCreateRequest.BITRATE_DEFAULT)
            .bitrateCap(true)
            .videoCodec(
                Arrays.stream(VideoCodec.values())
                    .map(videoCodec -> videoCodec.toString().toLowerCase())
                    .collect(Collectors.joining(",")));

    return sendVideoRoomPluginMessage(connectionId, videoHandleId, videoRequest, null)
        .thenApply(
            response -> {
              if (!VideoRoomResponse.CREATED.equals(response.getVideoRoom())) {
                throw new VideoServerException(
                    "An error occurred when creating a videoroom room for the connection "
                        + connectionId
                        + " with plugin "
                        + videoHandleId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  @Override
  public CompletableFuture<Void> stopMeeting(String meetingId) {
    return videoServerMeetingRepository
        .getById(meetingId)
        .map(
            videoServerMeeting -> {

              // Destroy rooms first
              CompletableFuture<Void> destroyRooms =
                  CompletableFuture.allOf(
                      destroyAudioBridgeRoom(
                          meetingId,
                          videoServerMeeting.getConnectionId(),
                          videoServerMeeting.getAudioHandleId(),
                          videoServerMeeting.getAudioRoomId()),
                      destroyVideoRoom(
                          meetingId,
                          videoServerMeeting.getConnectionId(),
                          videoServerMeeting.getVideoHandleId(),
                          videoServerMeeting.getVideoRoomId()));

              // Then destroy plugins
              CompletableFuture<Void> destroyPlugins =
                  destroyRooms.thenCompose(
                      v ->
                          CompletableFuture.allOf(
                              destroyPluginHandle(
                                  videoServerMeeting.getConnectionId(),
                                  videoServerMeeting.getAudioHandleId(),
                                  meetingId),
                              destroyPluginHandle(
                                  videoServerMeeting.getConnectionId(),
                                  videoServerMeeting.getVideoHandleId(),
                                  meetingId)));

              // Finally, destroy the connection and delete the meeting
              return destroyPlugins
                  .thenCompose(
                      v -> destroyConnection(videoServerMeeting.getConnectionId(), meetingId))
                  .thenRun(() -> videoServerMeetingRepository.deleteById(meetingId));
            })
        .orElseGet(() -> CompletableFuture.completedFuture(null))
        .exceptionally(
            ex -> {
              throw new VideoServerException("Failed to stop meeting " + meetingId, ex);
            });
  }

  private CompletableFuture<VideoServerResponse> destroyPluginHandle(
      String connectionId, String handleId, String meetingId) {
    return destroyPluginHandle(connectionId, handleId)
        .thenApply(
            response -> {
              if (!JANUS_SUCCESS.equals(response.getStatus())) {
                throw new VideoServerException(
                    "An error occurred when destroying the plugin handle for the connection "
                        + connectionId
                        + " with plugin "
                        + handleId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  private CompletableFuture<VideoServerResponse> destroyConnection(
      String connectionId, String meetingId) {
    return destroyConnection(connectionId)
        .thenApply(
            response -> {
              if (!JANUS_SUCCESS.equals(response.getStatus())) {
                throw new VideoServerException(
                    "An error occurred when destroying the video server connection "
                        + connectionId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  private CompletableFuture<VideoRoomResponse> destroyVideoRoom(
      String meetingId, String connectionId, String videoHandleId, String videoRoomId) {
    VideoRoomDestroyRequest destroyRequest =
        VideoRoomDestroyRequest.create()
            .request(VideoRoomDestroyRequest.DESTROY)
            .room(videoRoomId)
            .permanent(false);

    return sendVideoRoomPluginMessage(connectionId, videoHandleId, destroyRequest, null)
        .thenApply(
            response -> {
              if (!VideoRoomResponse.DESTROYED.equals(response.getVideoRoom())) {
                throw new VideoServerException(
                    "An error occurred when destroying the video room for the connection "
                        + connectionId
                        + " with plugin "
                        + videoHandleId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  private CompletableFuture<AudioBridgeResponse> destroyAudioBridgeRoom(
      String meetingId, String connectionId, String audioHandleId, String audioRoomId) {
    AudioBridgeDestroyRequest destroyRequest =
        AudioBridgeDestroyRequest.create()
            .request(AudioBridgeDestroyRequest.DESTROY)
            .room(audioRoomId)
            .permanent(false);

    return sendAudioBridgePluginMessage(connectionId, audioHandleId, destroyRequest, null)
        .thenApply(
            response -> {
              if (!AudioBridgeResponse.DESTROYED.equals(response.getAudioBridge())) {
                throw new VideoServerException(
                    "An error occurred when destroying the audio bridge room for the connection "
                        + connectionId
                        + " with plugin "
                        + audioHandleId
                        + " for the meeting "
                        + meetingId);
              }
              return response;
            });
  }

  @Override
  public CompletableFuture<Void> addMeetingParticipant(
      String userId,
      String queueId,
      String meetingId,
      boolean videoStreamOn,
      boolean audioStreamOn) {

    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);

    // Check if the user is already in the meeting
    if (videoServerMeeting.getVideoServerSessions().stream()
        .anyMatch(videoServerSessionUser -> videoServerSessionUser.getQueueId().equals(queueId))) {
      throw new VideoServerException(
          "Videoserver session user with user  "
              + userId
              + " is already present in the videoserver meeting "
              + meetingId);
    }

    // Step 1: Create a new connection
    return createConnection()
        .thenCompose(
            videoServerResponse -> {
              String connectionId = videoServerResponse.getDataId();

              // Step 2: Attach to plugins (Audio, Video Out, Video In, and Screen) in parallel
              CompletableFuture<VideoServerResponse> audioFuture =
                  attachToPlugin(connectionId, JANUS_AUDIOBRIDGE_PLUGIN);
              CompletableFuture<VideoServerResponse> videoOutFuture =
                  attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN);
              CompletableFuture<VideoServerResponse> videoInFuture =
                  attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN);
              CompletableFuture<VideoServerResponse> screenFuture =
                  attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN);

              return CompletableFuture.allOf(
                      audioFuture, videoOutFuture, videoInFuture, screenFuture)
                  .thenCompose(
                      aVoid -> {
                        String audioHandleId = audioFuture.join().getDataId();
                        String videoOutHandleId = videoOutFuture.join().getDataId();
                        String videoInHandleId = videoInFuture.join().getDataId();
                        String screenHandleId = screenFuture.join().getDataId();

                        // Step 3: Join video room as publisher (Video Out and Screen) in parallel
                        CompletableFuture<Void> joinVideoOutFuture =
                            joinVideoRoomAsPublisher(
                                connectionId,
                                userId,
                                videoOutHandleId,
                                videoServerMeeting.getVideoRoomId(),
                                MediaType.VIDEO);
                        CompletableFuture<Void> joinScreenFuture =
                            joinVideoRoomAsPublisher(
                                connectionId,
                                userId,
                                screenHandleId,
                                videoServerMeeting.getVideoRoomId(),
                                MediaType.SCREEN);

                        return CompletableFuture.allOf(joinVideoOutFuture, joinScreenFuture)
                            .thenRun(
                                () ->
                                    videoServerSessionRepository.insert(
                                        VideoServerSession.create(
                                                userId, queueId, videoServerMeeting)
                                            .connectionId(connectionId)
                                            .audioHandleId(audioHandleId)
                                            .videoOutHandleId(videoOutHandleId)
                                            .videoInHandleId(videoInHandleId)
                                            .screenHandleId(screenHandleId)));
                      });
            })
        .exceptionally(
            ex -> {
              throw new VideoServerException(
                  "An error occurred while adding participant "
                      + userId
                      + " to meeting "
                      + meetingId,
                  ex);
            });
  }

  private CompletableFuture<Void> joinVideoRoomAsPublisher(
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      MediaType mediaType) {

    return sendVideoRoomPluginMessage(
            connectionId,
            videoHandleId,
            VideoRoomJoinRequest.create()
                .request(VideoRoomJoinRequest.JOIN)
                .ptype(Ptype.PUBLISHER.toString().toLowerCase())
                .room(videoRoomId)
                .id(Feed.create().type(mediaType).userId(userId).toString()),
            null)
        .thenAccept(
            videoRoomResponse -> {
              if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
                throw new VideoServerException(
                    "An error occurred while user "
                        + userId
                        + " with connection id "
                        + connectionId
                        + " is joining video room as publisher");
              }
            });
  }

  @Override
  public CompletableFuture<Void> destroyMeetingParticipant(String userId, String meetingId) {
    return videoServerMeetingRepository
        .getById(meetingId)
        .map(
            videoServerMeeting -> {
              return videoServerMeeting.getVideoServerSessions().stream()
                  .filter(sessionUser -> sessionUser.getUserId().equals(userId))
                  .findFirst()
                  .map(
                      videoServerSession -> {
                        List<CompletableFuture<VideoServerResponse>> pluginFutures =
                            Arrays.asList(
                                destroyPluginHandle(
                                    videoServerSession.getConnectionId(),
                                    videoServerSession.getAudioHandleId(),
                                    meetingId),
                                destroyPluginHandle(
                                    videoServerSession.getConnectionId(),
                                    videoServerSession.getVideoOutHandleId(),
                                    meetingId),
                                destroyPluginHandle(
                                    videoServerSession.getConnectionId(),
                                    videoServerSession.getVideoInHandleId(),
                                    meetingId),
                                destroyPluginHandle(
                                    videoServerSession.getConnectionId(),
                                    videoServerSession.getScreenHandleId(),
                                    meetingId));

                        // Wait for all plugin destruction tasks to complete
                        return CompletableFuture.allOf(
                                pluginFutures.toArray(new CompletableFuture[0]))
                            .thenCompose(
                                v ->
                                    // After destroying all plugins, destroy the connection
                                    destroyConnection(
                                        videoServerSession.getConnectionId(), meetingId))
                            .thenRun(() -> videoServerSessionRepository.remove(videoServerSession));
                      })
                  .orElse(CompletableFuture.completedFuture(null));
            })
        .orElseGet(() -> CompletableFuture.completedFuture(null));
  }

  @Override
  public Optional<VideoServerSession> getSession(String connectionId) {
    return videoServerSessionRepository.getByConnectionId(connectionId);
  }

  @Override
  public List<VideoServerSession> getSessions(String meetingId) {
    return videoServerSessionRepository.getByMeetingId(meetingId);
  }

  @Override
  public CompletableFuture<Void> updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto) {

    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    CompletableFuture<Void> updateFuture =
        switch (mediaStreamSettingsDto.getType()) {
          case VIDEO ->
              updateVideoStream(
                  userId,
                  meetingId,
                  videoServerSession,
                  mediaStreamSettingsDto.isEnabled(),
                  mediaStreamSettingsDto.getSdp());
          case SCREEN ->
              updateScreenStream(
                  userId,
                  meetingId,
                  videoServerSession,
                  mediaStreamSettingsDto.isEnabled(),
                  mediaStreamSettingsDto.getSdp());
        };

    return updateFuture.exceptionally(
        ex -> {
          throw new VideoServerException(
              "Failed to update media stream for user " + userId + " in meeting " + meetingId, ex);
        });
  }

  private CompletableFuture<Void> updateVideoStream(
      String userId,
      String meetingId,
      VideoServerSession videoServerSession,
      boolean enabled,
      String sdp) {

    if (videoServerSession.hasVideoOutStreamOn() == enabled) {
      ChatsLogger.debug(
          "Video stream status is already updated for session "
              + userId
              + " for the meeting "
              + meetingId);
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> publishFuture =
        enabled
            ? publishStreamOnVideoRoom(
                userId,
                videoServerSession.getConnectionId(),
                videoServerSession.getVideoOutHandleId(),
                sdp,
                MediaType.VIDEO.toString().toLowerCase())
            : CompletableFuture.completedFuture(null);

    return publishFuture.thenRun(
        () -> videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled)));
  }

  private CompletableFuture<Void> updateScreenStream(
      String userId,
      String meetingId,
      VideoServerSession videoServerSession,
      boolean enabled,
      String sdp) {

    if (videoServerSession.hasScreenStreamOn() == enabled) {
      ChatsLogger.debug(
          "Screen stream status is already updated for session "
              + userId
              + " for the meeting "
              + meetingId);
      return CompletableFuture.completedFuture(null);
    }

    CompletableFuture<Void> publishFuture =
        enabled
            ? publishStreamOnVideoRoom(
                userId,
                videoServerSession.getConnectionId(),
                videoServerSession.getScreenHandleId(),
                sdp,
                MediaType.SCREEN.toString().toLowerCase())
            : CompletableFuture.completedFuture(null);

    return publishFuture.thenRun(
        () -> videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled)));
  }

  private CompletableFuture<Void> publishStreamOnVideoRoom(
      String userId,
      String connectionId,
      String handleId,
      String sessionDescriptionProtocol,
      String mediaType) {

    return sendVideoRoomPluginMessage(
            connectionId,
            handleId,
            VideoRoomPublishRequest.create()
                .request(VideoRoomPublishRequest.PUBLISH)
                .filename(
                    String.format(
                        AUDIO_VIDEO_PATTERN_NAME_WITH_TIMESTAMP,
                        mediaType,
                        userId,
                        OffsetDateTime.now(clock)
                            .format(DateTimeFormatter.ofPattern(DATE_TIME_DEFAULT_FORMAT)))),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sessionDescriptionProtocol))
        .thenAccept(
            videoRoomResponse -> {
              if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
                throw new VideoServerException(
                    "An error occurred while connection id "
                        + connectionId
                        + " is publishing "
                        + mediaType
                        + " stream");
              }
            });
  }

  @Override
  public CompletableFuture<Void> updateAudioStream(
      String userId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    // If the stream status is already as requested, we skip the operation
    if (videoServerSession.hasAudioStreamOn() == enabled) {
      ChatsLogger.debug(
          String.format(
              "Audio stream status is already %s for user %s in meeting %s",
              enabled ? "enabled" : "disabled", userId, meetingId));
      return CompletableFuture.completedFuture(null);
    }

    // Otherwise, mute or unmute the audio stream as requested
    return muteAudioStream(
            videoServerMeeting.getConnectionId(),
            videoServerSession.getConnectionId(),
            userId,
            videoServerMeeting.getAudioHandleId(),
            videoServerMeeting.getAudioRoomId(),
            enabled)
        .thenRun(
            () -> videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled)));
  }

  private CompletableFuture<Void> muteAudioStream(
      String meetingConnectionId,
      String connectionId,
      String userId,
      String meetingAudioHandleId,
      String audioRoomId,
      boolean enabled) {

    return sendAudioBridgePluginMessage(
            meetingConnectionId,
            meetingAudioHandleId,
            AudioBridgeMuteRequest.create()
                .request(enabled ? AudioBridgeMuteRequest.UNMUTE : AudioBridgeMuteRequest.MUTE)
                .room(audioRoomId)
                .id(userId),
            null)
        .thenAccept(
            audioBridgeResponse -> {
              if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
                throw new VideoServerException(
                    String.format(
                        "An error occurred while setting audio stream status for user %s with"
                            + " connection id %s",
                        userId, connectionId));
              }
            })
        .exceptionally(
            ex -> {
              throw new VideoServerException(
                  String.format(
                      "An error occurred while changing audio stream for user %s with connection id"
                          + " %s",
                      userId, connectionId),
                  ex);
            });
  }

  @Override
  public CompletableFuture<Void> answerRtcMediaStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    return startVideoIn(
        videoServerSession.getConnectionId(), videoServerSession.getVideoInHandleId(), sdp);
  }

  private CompletableFuture<Void> startVideoIn(
      String connectionId, String videoInHandleId, String sdp) {

    return sendVideoRoomPluginMessage(
            connectionId,
            videoInHandleId,
            VideoRoomStartVideoInRequest.create().request(VideoRoomStartVideoInRequest.START),
            RtcSessionDescription.create().type(RtcType.ANSWER).sdp(sdp))
        .thenAccept(
            videoRoomResponse -> {
              if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
                throw new VideoServerException(
                    String.format(
                        "An error occurred while session with connection id %s is starting"
                            + " receiving video streams",
                        connectionId));
              }
            })
        .exceptionally(
            ex -> {
              throw new VideoServerException(
                  String.format(
                      "An error occurred while starting video in for connection id %s",
                      connectionId),
                  ex);
            });
  }

  @Override
  public CompletableFuture<Void> updateSubscriptionsMediaStream(
      String userId, String meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    if (!videoServerSession.hasVideoInStreamOn()) {
      return joinVideoRoomAsSubscriber(
              videoServerSession.getConnectionId(),
              userId,
              videoServerSession.getVideoInHandleId(),
              videoServerMeeting.getVideoRoomId(),
              subscriptionUpdatesDto.getSubscribe())
          .thenRun(
              () -> videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true)));
    } else {
      return updateSubscriptions(
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          subscriptionUpdatesDto);
    }
  }

  private CompletableFuture<Void> joinVideoRoomAsSubscriber(
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      List<MediaStreamDto> mediaStreamDtos) {

    return sendVideoRoomPluginMessage(
            connectionId,
            videoHandleId,
            VideoRoomJoinRequest.create()
                .request(VideoRoomJoinRequest.JOIN)
                .ptype(Ptype.SUBSCRIBER.toString().toLowerCase())
                .room(videoRoomId)
                .useMsid(true)
                .streams(
                    mediaStreamDtos.stream()
                        .map(
                            mediaStreamDto ->
                                Stream.create()
                                    .feed(
                                        Feed.create()
                                            .type(
                                                MediaType.valueOf(
                                                    mediaStreamDto
                                                        .getType()
                                                        .toString()
                                                        .toUpperCase()))
                                            .userId(mediaStreamDto.getUserId())
                                            .toString()))
                        .toList()),
            null)
        .thenAccept(
            videoRoomResponse -> {
              if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
                throw new VideoServerException(
                    "An error occurred while user "
                        + userId
                        + " with connection id "
                        + connectionId
                        + " is joining video room as subscriber");
              }
            });
  }

  private CompletableFuture<Void> updateSubscriptions(
      String connectionId,
      String userId,
      String videoInHandleId,
      SubscriptionUpdatesDto subscriptionUpdatesDto) {

    return sendVideoRoomPluginMessage(
            connectionId,
            videoInHandleId,
            VideoRoomUpdateSubscriptionsRequest.create()
                .request(VideoRoomUpdateSubscriptionsRequest.UPDATE)
                .subscriptions(
                    subscriptionUpdatesDto.getSubscribe().stream()
                        .distinct()
                        .map(
                            mediaStreamDto ->
                                Stream.create()
                                    .feed(
                                        Feed.create()
                                            .type(
                                                MediaType.valueOf(
                                                    mediaStreamDto
                                                        .getType()
                                                        .toString()
                                                        .toUpperCase()))
                                            .userId(mediaStreamDto.getUserId())
                                            .toString()))
                        .toList())
                .unsubscriptions(
                    subscriptionUpdatesDto.getUnsubscribe().stream()
                        .distinct()
                        .map(
                            mediaStreamDto ->
                                Stream.create()
                                    .feed(
                                        Feed.create()
                                            .type(
                                                MediaType.valueOf(
                                                    mediaStreamDto
                                                        .getType()
                                                        .toString()
                                                        .toUpperCase()))
                                            .userId(mediaStreamDto.getUserId())
                                            .toString()))
                        .toList()),
            null)
        .thenAccept(
            videoRoomResponse -> {
              if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
                throw new VideoServerException(
                    "An error occurred while user "
                        + userId
                        + " with connection id "
                        + connectionId
                        + " is updating media subscriptions in the video room");
              }
            });
  }

  @Override
  public CompletableFuture<Void> offerRtcAudioStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    return joinAudioBridgeRoom(
        userId,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        sdp);
  }

  private CompletableFuture<Void> joinAudioBridgeRoom(
      String userId, String connectionId, String audioHandleId, String audioRoomId, String sdp) {

    return sendAudioBridgePluginMessage(
            connectionId,
            audioHandleId,
            AudioBridgeJoinRequest.create()
                .request(AudioBridgeJoinRequest.JOIN)
                .room(audioRoomId)
                .id(userId)
                .muted(true)
                .filename(
                    String.format(
                        AUDIO_VIDEO_PATTERN_NAME_WITH_TIMESTAMP,
                        AudioBridgeJoinRequest.FILENAME_DEFAULT,
                        userId,
                        OffsetDateTime.now(clock)
                            .format(DateTimeFormatter.ofPattern(DATE_TIME_DEFAULT_FORMAT)))),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp))
        .thenAccept(
            audioBridgeResponse -> {
              if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
                throw new VideoServerException(
                    "An error occurred while user "
                        + userId
                        + " with connection id "
                        + connectionId
                        + " is joining the audio room");
              }
            })
        .exceptionally(
            ex -> {
              throw new VideoServerException(
                  "Failed to join audio bridge room for user "
                      + userId
                      + " and connection "
                      + connectionId,
                  ex);
            });
  }

  private VideoServerMeeting getVideoServerMeeting(String meetingId) {
    return videoServerMeetingRepository
        .getById(meetingId)
        .orElseThrow(
            () ->
                new VideoServerException(
                    "No videoserver meeting found for the meeting " + meetingId));
  }

  private VideoServerSession getVideoServerSession(
      String userId, VideoServerMeeting videoServerMeeting) {
    return videoServerMeeting.getVideoServerSessions().stream()
        .filter(sessionUser -> sessionUser.getUserId().equals(userId))
        .findFirst()
        .orElseThrow(
            () ->
                new VideoServerException(
                    "No Videoserver session found for user "
                        + userId
                        + " for the meeting "
                        + videoServerMeeting.getMeetingId()));
  }

  /**
   * This method checks if the video server is alive.
   *
   * @return true if the video server returns the server_info status, false otherwise
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  @Override
  public boolean isAlive() {
    try {
      return JANUS_SERVER_INFO.equals(videoServerClient.sendGetInfoRequest().getStatus());
    } catch (Exception e) {
      ChatsLogger.warn("Can't communicate with Video server due to: " + e);
      return false;
    }
  }

  /**
   * This method asynchronously creates a 'connection' (session) on the VideoServer.
   *
   * @return CompletableFuture<VideoServerResponse>
   */
  private CompletableFuture<VideoServerResponse> createConnection() {
    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    return videoServerClient.sendVideoServerRequest(request);
  }

  /**
   * This method asynchronously destroys a specified connection on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id
   * @return CompletableFuture<VideoServerResponse>
   */
  private CompletableFuture<VideoServerResponse> destroyConnection(String connectionId) {
    return interactWithConnection(connectionId, JANUS_DESTROY, null);
  }

  /**
   * This method allows asynchronous interaction with a connection on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action the action to perform on this 'connection' (session)
   * @param pluginName the plugin name to perform the action with (optional)
   * @return CompletableFuture<VideoServerResponse>
   */
  private CompletableFuture<VideoServerResponse> interactWithConnection(
      String connectionId, String action, @Nullable String pluginName) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(action)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    Optional.ofNullable(pluginName).ifPresent(request::pluginName);

    return videoServerClient.sendConnectionVideoServerRequest(connectionId, request);
  }

  /**
   * This method asynchronously destroys the previously attached plugin handle.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the plugin handle id
   * @return CompletableFuture<VideoServerResponse>
   */
  private CompletableFuture<VideoServerResponse> destroyPluginHandle(
      String connectionId, String handleId) {
    return sendDetachPluginMessage(connectionId, handleId);
  }

  /**
   * This method asynchronously detaches the audio bridge plugin handle.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached plugin handle id
   * @return CompletableFuture<VideoServerResponse>
   */
  private CompletableFuture<VideoServerResponse> sendDetachPluginMessage(
      String connectionId, String handleId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);

    return videoServerClient.sendHandleVideoServerRequest(connectionId, handleId, request);
  }

  /**
   * This method asynchronously sends a message to an audio bridge plugin.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the audio bridge plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return CompletableFuture<AudioBridgeResponse>
   */
  private CompletableFuture<AudioBridgeResponse> sendAudioBridgePluginMessage(
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription).ifPresent(request::rtcSessionDescription);

    return videoServerClient.sendAudioBridgeRequest(connectionId, handleId, request);
  }

  /**
   * This method asynchronously sends a message to a video room plugin.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the video room plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return CompletableFuture<VideoRoomResponse>
   */
  private CompletableFuture<VideoRoomResponse> sendVideoRoomPluginMessage(
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription).ifPresent(request::rtcSessionDescription);

    return videoServerClient.sendVideoRoomRequest(connectionId, handleId, request);
  }
}
