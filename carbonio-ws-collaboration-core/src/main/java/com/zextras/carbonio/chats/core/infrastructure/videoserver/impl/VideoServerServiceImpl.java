// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.data.model.RecordingInfo;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.consul.ConsulService;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderConfig;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderService;
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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeEnableMjrsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeMuteRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomEditRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomEnableRecordingRequest;
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
import java.util.Random;
import java.util.UUID;
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

  private static final String VIDEOSERVER_SERVICE_NAME = "carbonio-videoserver";
  private static final String VIDEOSERVER_SERVICE_METADATA = "service_id";

  private static final String MEETING_PATTERN_NAME_WITH_TIMESTAMP = "meeting_%s/%s";
  private static final String AUDIO_VIDEO_PATTERN_NAME_WITH_TIMESTAMP = "%s_%s_%s";
  private static final String DATE_TIME_DEFAULT_FORMAT = "yyyyMMdd'T'HHmmss";

  private static final String MEETING_AUDIO_OPAQUE_ID_PATTERN = "meeting/a/%s";
  private static final String MEETING_VIDEO_OPAQUE_ID_PATTERN = "meeting/v/%s";
  private static final String USER_AUDIO_OPAQUE_ID_PATTERN = "a/%s/%s";
  private static final String USER_VIDEO_OUT_OPAQUE_ID_PATTERN = "vo/%s/%s";
  private static final String USER_VIDEO_IN_OPAQUE_ID_PATTERN = "vi/%s/%s";
  private static final String USER_SCREEN_OPAQUE_ID_PATTERN = "s/%s/%s";

  private final VideoServerClient videoServerClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final ConsulService consulService;
  private final VideoRecorderService videoRecorderService;
  private final Clock clock;
  private final Random random;

  private final String apiSecret;
  private final String recordingPath;

  @Inject
  public VideoServerServiceImpl(
      VideoServerConfig videoServerConfig,
      VideoRecorderConfig videoRecorderConfig,
      VideoServerClient videoServerClient,
      VideoServerMeetingRepository videoServerMeetingRepository,
      VideoServerSessionRepository videoServerSessionRepository,
      ConsulService consulService,
      VideoRecorderService videoRecorderService,
      Clock clock) {
    this.videoServerClient = videoServerClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
    this.consulService = consulService;
    this.videoRecorderService = videoRecorderService;
    this.clock = clock;
    this.random = new Random();
    this.apiSecret = videoServerConfig.getApiSecret();
    this.recordingPath = videoRecorderConfig.getRecordingPath();
  }

  @Override
  public void startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      ChatsLogger.debug("Videoserver meeting " + meetingId + " is already active");
      return;
    }

    List<UUID> healthyVideoServers =
        consulService.getHealthyServices(VIDEOSERVER_SERVICE_NAME, VIDEOSERVER_SERVICE_METADATA);
    UUID serverId = healthyVideoServers.get(random.nextInt(healthyVideoServers.size()));

    VideoServerResponse connectionResponse = createMeetingConnection(serverId);
    String connectionId = connectionResponse.getDataId();

    VideoServerResponse audioPluginResponse =
        attachToPlugin(
            serverId,
            connectionId,
            JANUS_AUDIOBRIDGE_PLUGIN,
            String.format(MEETING_AUDIO_OPAQUE_ID_PATTERN, meetingId));
    VideoServerResponse videoPluginResponse =
        attachToPlugin(
            serverId,
            connectionId,
            JANUS_VIDEOROOM_PLUGIN,
            String.format(MEETING_VIDEO_OPAQUE_ID_PATTERN, meetingId));

    String audioHandleId = audioPluginResponse.getDataId();
    String videoHandleId = videoPluginResponse.getDataId();

    AudioBridgeResponse audioRoomResponse =
        createAudioBridgeRoom(serverId, meetingId, connectionId, audioHandleId);
    VideoRoomResponse videoRoomResponse =
        createVideoRoom(serverId, meetingId, connectionId, videoHandleId);

    String audioRoomId = audioRoomResponse.getRoom();
    String videoRoomId = videoRoomResponse.getRoom();

    videoServerMeetingRepository.insert(
        VideoServerMeeting.create()
            .serverId(serverId.toString())
            .meetingId(meetingId)
            .connectionId(connectionId)
            .audioHandleId(audioHandleId)
            .videoHandleId(videoHandleId)
            .audioRoomId(audioRoomId)
            .videoRoomId(videoRoomId));
  }

  private VideoServerResponse createMeetingConnection(UUID serverId) {
    VideoServerResponse response = createConnection(serverId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error creating video server connection");
    }
    return response;
  }

  private VideoServerResponse attachToPlugin(
      UUID serverId, String connectionId, String pluginType, String opaqueId) {
    VideoServerResponse response =
        interactWithConnection(serverId, connectionId, JANUS_ATTACH, pluginType, opaqueId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error attaching to plugin " + pluginType);
    }
    return response;
  }

  private AudioBridgeResponse createAudioBridgeRoom(
      UUID serverId, String meetingId, String connectionId, String audioHandleId) {
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

    AudioBridgeResponse response =
        sendAudioBridgePluginMessage(serverId, connectionId, audioHandleId, audioRequest, null);
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
  }

  private VideoRoomResponse createVideoRoom(
      UUID serverId, String meetingId, String connectionId, String videoHandleId) {
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

    VideoRoomResponse response =
        sendVideoRoomPluginMessage(serverId, connectionId, videoHandleId, videoRequest, null);
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
  }

  @Override
  public void stopMeeting(String meetingId) {
    videoServerMeetingRepository
        .getById(meetingId)
        .ifPresent(
            videoServerMeeting -> {
              UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

              destroyAudioBridgeRoom(
                  serverId,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  videoServerMeeting.getAudioRoomId());

              destroyVideoRoom(
                  serverId,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  videoServerMeeting.getVideoRoomId());

              destroyPluginHandle(
                  serverId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  meetingId);

              destroyPluginHandle(
                  serverId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  meetingId);

              destroyConnection(serverId, videoServerMeeting.getConnectionId(), meetingId);
              videoServerMeetingRepository.deleteById(meetingId);
            });
  }

  private void destroyPluginHandle(
      UUID serverId, String connectionId, String handleId, String meetingId) {
    VideoServerResponse response = destroyPluginHandle(serverId, connectionId, handleId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      ChatsLogger.debug(
          "An error occurred when destroying the plugin handle for the connection "
              + connectionId
              + " with plugin "
              + handleId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyConnection(UUID serverId, String connectionId, String meetingId) {
    VideoServerResponse response = destroyConnection(serverId, connectionId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      ChatsLogger.debug(
          "An error occurred when destroying the video server connection "
              + connectionId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyVideoRoom(
      UUID serverId,
      String meetingId,
      String connectionId,
      String videoHandleId,
      String videoRoomId) {
    VideoRoomDestroyRequest destroyRequest =
        VideoRoomDestroyRequest.create()
            .request(VideoRoomDestroyRequest.DESTROY)
            .room(videoRoomId)
            .permanent(false);

    VideoRoomResponse response =
        sendVideoRoomPluginMessage(serverId, connectionId, videoHandleId, destroyRequest, null);
    if (!VideoRoomResponse.DESTROYED.equals(response.getVideoRoom())) {
      ChatsLogger.debug(
          "An error occurred when destroying the video room for the connection "
              + connectionId
              + " with plugin "
              + videoHandleId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyAudioBridgeRoom(
      UUID serverId,
      String meetingId,
      String connectionId,
      String audioHandleId,
      String audioRoomId) {
    AudioBridgeDestroyRequest destroyRequest =
        AudioBridgeDestroyRequest.create()
            .request(AudioBridgeDestroyRequest.DESTROY)
            .room(audioRoomId)
            .permanent(false);

    AudioBridgeResponse response =
        sendAudioBridgePluginMessage(serverId, connectionId, audioHandleId, destroyRequest, null);
    if (!AudioBridgeResponse.DESTROYED.equals(response.getAudioBridge())) {
      ChatsLogger.debug(
          "An error occurred when destroying the audio bridge room for the connection "
              + connectionId
              + " with plugin "
              + audioHandleId
              + " for the meeting "
              + meetingId);
    }
  }

  @Override
  public void addMeetingParticipant(
      String userId,
      String queueId,
      String meetingId,
      boolean videoStreamOn,
      boolean audioStreamOn) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);

    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

    Optional<VideoServerSession> videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst();

    if (videoServerSession.isPresent()) {
      ChatsLogger.debug(
          "Videoserver session with user  "
              + userId
              + " is already present in the videoserver meeting "
              + meetingId);
      return;
    }

    String connectionId = createConnection(serverId).getDataId();

    String audioHandleId =
        attachToPlugin(
                serverId,
                connectionId,
                JANUS_AUDIOBRIDGE_PLUGIN,
                String.format(USER_AUDIO_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String videoOutHandleId =
        attachToPlugin(
                serverId,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_VIDEO_OUT_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String videoInHandleId =
        attachToPlugin(
                serverId,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_VIDEO_IN_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String screenHandleId =
        attachToPlugin(
                serverId,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_SCREEN_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();

    joinVideoRoomAsPublisher(
        serverId,
        connectionId,
        userId,
        videoOutHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.VIDEO);
    joinVideoRoomAsPublisher(
        serverId,
        connectionId,
        userId,
        screenHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.SCREEN);

    videoServerSession.ifPresentOrElse(
        session ->
            videoServerSessionRepository.update(
                session
                    .userId(userId)
                    .queueId(queueId)
                    .videoServerMeeting(videoServerMeeting)
                    .connectionId(connectionId)
                    .audioHandleId(audioHandleId)
                    .videoOutHandleId(videoOutHandleId)
                    .videoInHandleId(videoInHandleId)
                    .screenHandleId(screenHandleId)),
        () ->
            videoServerSessionRepository.insert(
                VideoServerSession.create(userId, queueId, videoServerMeeting)
                    .connectionId(connectionId)
                    .audioHandleId(audioHandleId)
                    .videoOutHandleId(videoOutHandleId)
                    .videoInHandleId(videoInHandleId)
                    .screenHandleId(screenHandleId)));
  }

  private void joinVideoRoomAsPublisher(
      UUID serverId,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      MediaType mediaType) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
            VideoRoomJoinRequest.create()
                .request(VideoRoomJoinRequest.JOIN)
                .ptype(Ptype.PUBLISHER.toString().toLowerCase())
                .room(videoRoomId)
                .id(Feed.create().type(mediaType).userId(userId).toString()),
            null);

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user "
              + userId
              + " with connection id "
              + connectionId
              + " is joining video room as publisher");
    }
  }

  @Override
  public void destroyMeetingParticipant(String userId, String meetingId) {
    videoServerMeetingRepository
        .getById(meetingId)
        .ifPresent(
            videoServerMeeting -> {
              UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

              videoServerMeeting.getVideoServerSessions().stream()
                  .filter(sessionUser -> sessionUser.getUserId().equals(userId))
                  .findFirst()
                  .ifPresent(
                      videoServerSession -> {
                        destroyParticipantSession(meetingId, videoServerSession, serverId);
                        videoServerSessionRepository.remove(videoServerSession);
                      });
            });
  }

  private void destroyParticipantSession(
      String meetingId, VideoServerSession videoServerSession, UUID serverId) {
    destroyPluginHandle(
        serverId,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        meetingId);
    destroyPluginHandle(
        serverId,
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        meetingId);
    destroyPluginHandle(
        serverId,
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoInHandleId(),
        meetingId);
    destroyPluginHandle(
        serverId,
        videoServerSession.getConnectionId(),
        videoServerSession.getScreenHandleId(),
        meetingId);

    destroyConnection(serverId, videoServerSession.getConnectionId(), meetingId);
  }

  @Override
  public List<VideoServerSession> getSessions(String meetingId) {
    return videoServerSessionRepository.getByMeetingId(meetingId);
  }

  @Override
  public void updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto) {

    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

    try {
      switch (mediaStreamSettingsDto.getType()) {
        case VIDEO ->
            updateVideoStream(
                serverId,
                userId,
                meetingId,
                videoServerSession,
                mediaStreamSettingsDto.isEnabled(),
                mediaStreamSettingsDto.getSdp());
        case SCREEN ->
            updateScreenStream(
                serverId,
                userId,
                meetingId,
                videoServerSession,
                mediaStreamSettingsDto.isEnabled(),
                mediaStreamSettingsDto.getSdp());
      }
    } catch (Exception ex) {
      throw new VideoServerException(
          "Failed to update media stream for user " + userId + " in meeting " + meetingId, ex);
    }
  }

  private void updateVideoStream(
      UUID serverId,
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
      return;
    }

    if (enabled) {
      publishStreamOnVideoRoom(
          serverId,
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getVideoOutHandleId(),
          sdp,
          MediaType.VIDEO.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled));
  }

  private void updateScreenStream(
      UUID serverId,
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
      return;
    }

    if (enabled) {
      publishStreamOnVideoRoom(
          serverId,
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getScreenHandleId(),
          sdp,
          MediaType.SCREEN.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled));
  }

  private void publishStreamOnVideoRoom(
      UUID serverId,
      String userId,
      String connectionId,
      String handleId,
      String sessionDescriptionProtocol,
      String mediaType) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
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
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sessionDescriptionProtocol));

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while connection id "
              + connectionId
              + " is publishing "
              + mediaType
              + " stream");
    }
  }

  @Override
  public void updateAudioStream(String userId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    if (videoServerSession.hasAudioStreamOn() == enabled) {
      ChatsLogger.debug(
          String.format(
              "Audio stream status is already %s for user %s in meeting %s",
              enabled ? "enabled" : "disabled", userId, meetingId));
      return;
    }

    muteAudioStream(
        UUID.fromString(videoServerMeeting.getServerId()),
        videoServerMeeting.getConnectionId(),
        videoServerSession.getConnectionId(),
        userId,
        videoServerMeeting.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        enabled);

    videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled));
  }

  private void muteAudioStream(
      UUID serverId,
      String meetingConnectionId,
      String connectionId,
      String userId,
      String meetingAudioHandleId,
      String audioRoomId,
      boolean enabled) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
            serverId,
            meetingConnectionId,
            meetingAudioHandleId,
            AudioBridgeMuteRequest.create()
                .request(enabled ? AudioBridgeMuteRequest.UNMUTE : AudioBridgeMuteRequest.MUTE)
                .room(audioRoomId)
                .id(userId),
            null);

    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
          String.format(
              "An error occurred while setting audio stream status for user %s with connection id"
                  + " %s",
              userId, connectionId));
    }
  }

  @Override
  public void answerRtcMediaStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

    startVideoIn(
        serverId,
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoInHandleId(),
        sdp);
  }

  private void startVideoIn(
      UUID serverId, String connectionId, String videoInHandleId, String sdp) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoInHandleId,
            VideoRoomStartVideoInRequest.create().request(VideoRoomStartVideoInRequest.START),
            RtcSessionDescription.create().type(RtcType.ANSWER).sdp(sdp));

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      ChatsLogger.debug(
          String.format(
              "An error occurred while session with connection id %s is starting receiving video"
                  + " streams",
              connectionId));
    }
  }

  @Override
  public void updateSubscriptionsMediaStream(
      String userId, String meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

    if (!videoServerSession.hasVideoInStreamOn()) {
      joinVideoRoomAsSubscriber(
          serverId,
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          videoServerMeeting.getVideoRoomId(),
          subscriptionUpdatesDto.getSubscribe());
      videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
    } else {
      updateSubscriptions(
          serverId,
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          subscriptionUpdatesDto);
    }
  }

  private void joinVideoRoomAsSubscriber(
      UUID serverId,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      List<MediaStreamDto> mediaStreamDtos) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
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
            null);

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user "
              + userId
              + " with connection id "
              + connectionId
              + " is joining video room as subscriber");
    }
  }

  private void updateSubscriptions(
      UUID serverId,
      String connectionId,
      String userId,
      String videoInHandleId,
      SubscriptionUpdatesDto subscriptionUpdatesDto) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
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
            null);

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      ChatsLogger.debug(
          "An error occurred while user "
              + userId
              + " with connection id "
              + connectionId
              + " is updating media subscriptions in the video room");
    }
  }

  @Override
  public void offerRtcAudioStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());

    joinAudioBridgeRoom(
        serverId,
        userId,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        sdp);
  }

  private void joinAudioBridgeRoom(
      UUID serverId,
      String userId,
      String connectionId,
      String audioHandleId,
      String audioRoomId,
      String sdp) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
            serverId,
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
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));

    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user "
              + userId
              + " with connection id "
              + connectionId
              + " is joining the audio room");
    }
  }

  @Override
  public void startRecording(String meetingId) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    String filePath =
        recordingPath
            + String.format(
                MEETING_PATTERN_NAME_WITH_TIMESTAMP,
                meetingId,
                OffsetDateTime.now(clock)
                    .format(DateTimeFormatter.ofPattern(DATE_TIME_DEFAULT_FORMAT)));

    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    String connectionId = videoServerMeeting.getConnectionId();
    String videoHandleId = videoServerMeeting.getVideoHandleId();
    String videoRoomId = videoServerMeeting.getVideoRoomId();
    String audioHandleId = videoServerMeeting.getAudioHandleId();
    String audioRoomId = videoServerMeeting.getAudioRoomId();

    editVideoRoom(serverId, connectionId, videoHandleId, videoRoomId, filePath, meetingId);

    updateAudioBridgeRoomRecordingStatus(
        serverId, connectionId, audioHandleId, audioRoomId, true, meetingId, filePath);
    updateVideoRoomRecordingStatus(
        serverId, connectionId, videoHandleId, videoRoomId, true, meetingId);
  }

  @Override
  public void stopRecording(String meetingId, RecordingInfo recordingInfo) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);

    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    String connectionId = videoServerMeeting.getConnectionId();
    String videoHandleId = videoServerMeeting.getVideoHandleId();
    String videoRoomId = videoServerMeeting.getVideoRoomId();
    String audioHandleId = videoServerMeeting.getAudioHandleId();
    String audioRoomId = videoServerMeeting.getAudioRoomId();

    updateAudioBridgeRoomRecordingStatus(
        serverId,
        connectionId,
        audioHandleId,
        audioRoomId,
        false,
        videoServerMeeting.getMeetingId(),
        null);
    updateVideoRoomRecordingStatus(
        serverId,
        connectionId,
        videoHandleId,
        videoRoomId,
        false,
        videoServerMeeting.getMeetingId());

    videoRecorderService.startRecordingPostProcessing(recordingInfo.serverId(serverId.toString()));
  }

  private void updateAudioBridgeRoomRecordingStatus(
      UUID serverId,
      String connectionId,
      String audioHandleId,
      String audioRoomId,
      boolean enabled,
      String meetingId,
      @Nullable String recordingPath) {

    AudioBridgeEnableMjrsRequest request =
        AudioBridgeEnableMjrsRequest.create()
            .request(AudioBridgeEnableMjrsRequest.ENABLE_MJRS)
            .room(audioRoomId)
            .mjrs(enabled);

    if (recordingPath != null) {
      request.mjrsDir(recordingPath);
    }

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(serverId, connectionId, audioHandleId, request, null);
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while recording the audiobridge room for connection "
              + connectionId
              + " and meeting "
              + meetingId);
    }
  }

  private void editVideoRoom(
      UUID serverId,
      String connectionId,
      String videoHandleId,
      String videoRoomId,
      String filePath,
      String meetingId) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
            VideoRoomEditRequest.create()
                .request(VideoRoomEditRequest.EDIT)
                .room(videoRoomId)
                .newRecDir(filePath),
            null);

    if (!VideoRoomResponse.SUCCESS.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while setting rec dir on videoroom for connection "
              + connectionId
              + " and meeting "
              + meetingId);
    }
  }

  private void updateVideoRoomRecordingStatus(
      UUID serverId,
      String connectionId,
      String videoHandleId,
      String videoRoomId,
      boolean enabled,
      String meetingId) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
            VideoRoomEnableRecordingRequest.create()
                .request(VideoRoomEnableRecordingRequest.ENABLE_RECORDING)
                .room(videoRoomId)
                .record(enabled),
            null);

    if (!VideoRoomResponse.SUCCESS.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while recording the videoroom for connection "
              + connectionId
              + " and meeting "
              + meetingId);
    }
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
   * This method creates a 'connection' (session) on the VideoServer.
   *
   * @param serverId {@link UUID} of the destination server
   * @return VideoServerResponse
   */
  private VideoServerResponse createConnection(UUID serverId) {
    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    return videoServerClient.sendVideoServerRequest(serverId.toString(), request);
  }

  /**
   * This method destroys a specified connection on the VideoServer.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyConnection(UUID serverId, String connectionId) {
    return interactWithConnection(serverId, connectionId, JANUS_DESTROY, null, null);
  }

  /**
   * This method allows interaction with a connection on the VideoServer.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action the action to perform on this 'connection' (session)
   * @param opaqueId the user id or meeting id associated to this handle-session on the VideoServer
   * @param pluginName the plugin name to perform the action with (optional)
   * @return VideoServerResponse
   */
  private VideoServerResponse interactWithConnection(
      UUID serverId,
      String connectionId,
      String action,
      @Nullable String pluginName,
      @Nullable String opaqueId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(action)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    Optional.ofNullable(pluginName).ifPresent(request::pluginName);
    Optional.ofNullable(opaqueId).ifPresent(request::opaqueId);

    return videoServerClient.sendConnectionVideoServerRequest(
        connectionId, serverId.toString(), request);
  }

  /**
   * This method destroys the previously attached plugin handle.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id
   * @param handleId the plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyPluginHandle(
      UUID serverId, String connectionId, String handleId) {
    return sendDetachPluginMessage(serverId, connectionId, handleId);
  }

  /**
   * This method detaches the audio bridge plugin handle.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse sendDetachPluginMessage(
      UUID serverId, String connectionId, String handleId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);

    return videoServerClient.sendHandleVideoServerRequest(
        connectionId, handleId, serverId.toString(), request);
  }

  /**
   * This method sends a message to an audio bridge plugin.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id
   * @param handleId the audio bridge plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return AudioBridgeResponse
   */
  private AudioBridgeResponse sendAudioBridgePluginMessage(
      UUID serverId,
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

    return videoServerClient.sendAudioBridgeRequest(
        connectionId, handleId, serverId.toString(), request);
  }

  /**
   * This method sends a message to a video room plugin.
   *
   * @param serverId {@link UUID} of the destination server
   * @param connectionId the 'connection' (session) id
   * @param handleId the video room plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return VideoRoomResponse
   */
  private VideoRoomResponse sendVideoRoomPluginMessage(
      UUID serverId,
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

    return videoServerClient.sendVideoRoomRequest(
        connectionId, handleId, serverId.toString(), request);
  }
}
