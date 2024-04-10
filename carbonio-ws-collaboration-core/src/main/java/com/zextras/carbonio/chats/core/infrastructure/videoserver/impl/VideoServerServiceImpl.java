// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.consul.ConsulService;
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
import io.ebean.annotation.Transactional;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class VideoServerServiceImpl implements VideoServerService {

  private static final String JANUS_ENDPOINT = "/janus";
  private static final String JANUS_INFO_ENDPOINT = "/info";
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

  private static final String VIDEOSERVER_ROUTING_QUERYPARAM = "?service_id=";
  private final String videoServerURL;
  private final String apiSecret;
  private final VideoServerClient videoServerClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;

  private final ConsulService consulService;

  private final Random random = new Random();

  @Inject
  public VideoServerServiceImpl(
      AppConfig appConfig,
      VideoServerClient videoServerClient,
      VideoServerMeetingRepository videoServerMeetingRepository,
      VideoServerSessionRepository videoServerSessionRepository,
      ConsulService consulService) {
    this.videoServerURL =
        String.format(
            "http://%s:%s",
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow());
    this.apiSecret = appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElse(null);
    this.videoServerClient = videoServerClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
    this.consulService = consulService;
  }

  @Override
  @Transactional
  public void startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already active");
    }
    List<UUID> healthyVideoservers =
        consulService.getHealthyServices(VIDEOSERVER_SERVICE_NAME, VIDEOSERVER_SERVICE_METADATA);

    UUID chosenServer = healthyVideoservers.get(random.nextInt(healthyVideoservers.size()));

    VideoServerResponse videoServerResponse = createNewConnection(chosenServer, meetingId);
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse =
        attachToPlugin(chosenServer, connectionId, JANUS_AUDIOBRIDGE_PLUGIN, meetingId);
    String audioHandleId = videoServerResponse.getDataId();
    AudioBridgeResponse audioBridgeResponse =
        createAudioBridgeRoom(chosenServer, meetingId, connectionId, audioHandleId);
    videoServerResponse =
        attachToPlugin(chosenServer, connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoHandleId = videoServerResponse.getDataId();
    VideoRoomResponse videoRoomResponse =
        createVideoRoom(chosenServer, meetingId, connectionId, videoHandleId);

    videoServerMeetingRepository.insert(
        chosenServer,
        meetingId,
        connectionId,
        audioHandleId,
        videoHandleId,
        audioBridgeResponse.getRoom(),
        videoRoomResponse.getRoom());
  }

  private VideoServerResponse createNewConnection(UUID serverId, String meetingId) {
    VideoServerResponse videoServerResponse = createConnection(serverId);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred when creating a videoserver connection for the meeting " + meetingId);
    }
    return videoServerResponse;
  }

  private VideoServerResponse attachToPlugin(
      UUID serverId, String connectionId, String pluginType, String meetingId) {
    VideoServerResponse videoServerResponse =
        interactWithConnection(serverId, connectionId, JANUS_ATTACH, pluginType);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred when attaching to the plugin for the connection "
              + connectionId
              + " for the meeting "
              + meetingId);
    }
    return videoServerResponse;
  }

  private AudioBridgeResponse createAudioBridgeRoom(
      UUID serverId, String meetingId, String connectionId, String audioHandleId) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse =
        sendAudioBridgePluginMessage(
            serverId,
            connectionId,
            audioHandleId,
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
                .audioLevelEvent(true),
            null);
    if (!AudioBridgeResponse.CREATED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
          "An error occurred when creating an audiobridge room for the connection "
              + connectionId
              + " with plugin "
              + audioHandleId
              + " for the meeting "
              + meetingId);
    }
    return audioBridgeResponse;
  }

  private VideoRoomResponse createVideoRoom(
      UUID serverId, String meetingId, String connectionId, String videoHandleId) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
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
                        .collect(Collectors.joining(","))),
            null);
    if (!VideoRoomResponse.CREATED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
          "An error occurred when creating a videoroom room for the connection "
              + connectionId
              + " with plugin "
              + videoHandleId
              + " for the meeting "
              + meetingId);
    }
    return videoRoomResponse;
  }

  @Override
  @Transactional
  public void stopMeeting(String meetingId) {
    videoServerMeetingRepository
        .getById(meetingId)
        .ifPresent(
            videoServerMeeting -> {
              UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
              destroyVideoRoom(
                  serverId,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  videoServerMeeting.getVideoRoomId());
              destroyAudioBridgeRoom(
                  serverId,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  videoServerMeeting.getAudioRoomId());
              destroyPluginHandle(
                  serverId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  meetingId);
              destroyPluginHandle(
                  serverId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  meetingId);
              destroyConnection(serverId, videoServerMeeting.getConnectionId(), meetingId);
              videoServerMeetingRepository.deleteById(meetingId);
            });
  }

  private void destroyPluginHandle(
      UUID serverId, String connectionId, String handleId, String meetingId) {
    VideoServerResponse videoServerResponse = destroyPluginHandle(serverId, connectionId, handleId);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred when destroying the plugin handle for the connection "
              + connectionId
              + " with plugin "
              + handleId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyConnection(UUID serverId, String connectionId, String meetingId) {
    VideoServerResponse videoServerResponse = destroyConnection(serverId, connectionId);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred when destroying the videoserver connection "
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
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
            VideoRoomDestroyRequest.create()
                .request(VideoRoomDestroyRequest.DESTROY)
                .room(videoRoomId)
                .permanent(false),
            null);
    if (!VideoRoomResponse.DESTROYED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
          "An error occurred when destroying the videoroom for the connection "
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
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse =
        sendAudioBridgePluginMessage(
            serverId,
            connectionId,
            audioHandleId,
            AudioBridgeDestroyRequest.create()
                .request(AudioBridgeDestroyRequest.DESTROY)
                .room(audioRoomId)
                .permanent(false),
            null);
    if (!AudioBridgeResponse.DESTROYED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
          "An error occurred when destroying the audiobridge room for the connection "
              + connectionId
              + " with plugin "
              + audioHandleId
              + " for the meeting "
              + meetingId);
    }
  }

  @Override
  @Transactional
  public void addMeetingParticipant(
      String userId,
      String queueId,
      String meetingId,
      boolean videoStreamOn,
      boolean audioStreamOn) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    if (videoServerMeeting.getVideoServerSessions().stream()
        .anyMatch(videoServerSessionUser -> videoServerSessionUser.getQueueId().equals(queueId))) {
      throw new VideoServerException(
          "Videoserver session user with user  "
              + userId
              + "is already present in the videoserver meeting "
              + meetingId);
    }
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    VideoServerResponse videoServerResponse = createNewConnection(serverId, meetingId);
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = attachToPlugin(serverId, connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoOutHandleId = videoServerResponse.getDataId();
    joinVideoRoomAsPublisher(
        serverId,
        connectionId,
        userId,
        videoOutHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.VIDEO);
    videoServerResponse = attachToPlugin(serverId, connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String screenHandleId = videoServerResponse.getDataId();
    joinVideoRoomAsPublisher(
        serverId,
        connectionId,
        userId,
        screenHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.SCREEN);
    videoServerSessionRepository.insert(
        videoServerMeeting, userId, queueId, connectionId, videoOutHandleId, screenHandleId);
  }

  private void joinVideoRoomAsPublisher(
      UUID serverId,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      MediaType mediaType) {
    VideoRoomResponse videoRoomResponse;
    VideoRoomJoinRequest videoRoomJoinRequest =
        VideoRoomJoinRequest.create()
            .request(VideoRoomJoinRequest.JOIN)
            .ptype(Ptype.PUBLISHER.toString().toLowerCase())
            .room(videoRoomId)
            .id(Feed.create().type(mediaType).userId(userId).toString());
    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId, connectionId, videoHandleId, videoRoomJoinRequest, null);
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while user "
              + userId
              + " with connection id "
              + connectionId
              + " is joining video room as publisher");
    }
  }

  @Override
  @Transactional
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
                        Optional.ofNullable(videoServerSession.getAudioHandleId())
                            .ifPresent(
                                audioHandleId ->
                                    destroyPluginHandle(
                                        serverId,
                                        videoServerSession.getConnectionId(),
                                        audioHandleId,
                                        meetingId));
                        Optional.ofNullable(videoServerSession.getVideoOutHandleId())
                            .ifPresent(
                                videoOutHandleId ->
                                    destroyPluginHandle(
                                        serverId,
                                        videoServerSession.getConnectionId(),
                                        videoOutHandleId,
                                        meetingId));
                        Optional.ofNullable(videoServerSession.getVideoInHandleId())
                            .ifPresent(
                                videoInHandleId ->
                                    destroyPluginHandle(
                                        serverId,
                                        videoServerSession.getConnectionId(),
                                        videoInHandleId,
                                        meetingId));
                        Optional.ofNullable(videoServerSession.getScreenHandleId())
                            .ifPresent(
                                screenHandleId ->
                                    destroyPluginHandle(
                                        serverId,
                                        videoServerSession.getConnectionId(),
                                        screenHandleId,
                                        meetingId));
                        destroyConnection(
                            serverId, videoServerSession.getConnectionId(), meetingId);
                        videoServerSessionRepository.remove(videoServerSession);
                      });
            });
  }

  @Override
  @Transactional
  public void updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No Videoserver session found for user "
                            + userId
                            + " for the meeting "
                            + meetingId));
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    switch (mediaStreamSettingsDto.getType()) {
      case VIDEO:
        updateVideoStream(
            serverId,
            userId,
            meetingId,
            videoServerSession,
            mediaStreamSettingsDto.isEnabled(),
            mediaStreamSettingsDto.getSdp());
        break;
      case SCREEN:
        updateScreenStream(
            serverId,
            userId,
            meetingId,
            videoServerSession,
            mediaStreamSettingsDto.isEnabled(),
            mediaStreamSettingsDto.getSdp());
        break;
      default:
        break;
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
    } else {
      if (enabled) {
        publishStreamOnVideoRoom(
            serverId,
            videoServerSession.getConnectionId(),
            videoServerSession.getVideoOutHandleId(),
            sdp);
      }
      videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled));
    }
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
    } else {
      if (enabled) {
        publishStreamOnVideoRoom(
            serverId,
            videoServerSession.getConnectionId(),
            videoServerSession.getScreenHandleId(),
            sdp);
      }
      videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled));
    }
  }

  private void publishStreamOnVideoRoom(
      UUID serverId, String connectionId, String videoHandleId, String sessionDescriptionProtocol) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoHandleId,
            VideoRoomPublishRequest.create().request(VideoRoomPublishRequest.PUBLISH),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sessionDescriptionProtocol));
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while connection id " + connectionId + " is publishing video stream");
    }
  }

  @Override
  @Transactional
  public void updateAudioStream(String userId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No Videoserver session found for user "
                            + userId
                            + " for the meeting "
                            + meetingId));
    if (enabled == videoServerSession.hasAudioStreamOn()) {
      ChatsLogger.debug(
          "Audio stream status is already updated for user "
              + userId
              + " for the meeting "
              + meetingId);
    } else {
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
  }

  private void muteAudioStream(
      UUID serverId,
      String meetingConnectionId,
      String connectionId,
      String userId,
      String meetingAudioHandleId,
      String audioRoomId,
      boolean enabled) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse =
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
          "An error occured while setting audio stream status for "
              + userId
              + " with connection id "
              + connectionId);
    }
  }

  @Override
  @Transactional
  public void answerRtcMediaStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No Videoserver session found for user "
                            + userId
                            + " for the meeting "
                            + meetingId));
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    Optional.ofNullable(videoServerSession.getVideoInHandleId())
        .ifPresentOrElse(
            handleId -> {
              startVideoIn(serverId, videoServerSession.getConnectionId(), handleId, sdp);
              videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
            },
            () -> {
              VideoServerResponse videoServerResponse =
                  attachToPlugin(
                      serverId,
                      videoServerSession.getConnectionId(),
                      JANUS_VIDEOROOM_PLUGIN,
                      meetingId);
              VideoServerSession videoServerSessionUpdated =
                  videoServerSessionRepository.update(
                      videoServerSession.videoInHandleId(videoServerResponse.getDataId()));
              startVideoIn(
                  serverId,
                  videoServerSessionUpdated.getConnectionId(),
                  videoServerSessionUpdated.getVideoInHandleId(),
                  sdp);
              videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
            });
  }

  private void startVideoIn(
      UUID serverId, String connectionId, String videoInHandleId, String sdp) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId,
            connectionId,
            videoInHandleId,
            VideoRoomStartVideoInRequest.create().request(VideoRoomStartVideoInRequest.START),
            RtcSessionDescription.create().type(RtcType.ANSWER).sdp(sdp));
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while session with connection id "
              + connectionId
              + " is starting receiving video streams available in the video room");
    }
  }

  @Override
  @Transactional
  public void updateSubscriptionsMediaStream(
      String userId, String meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No Videoserver session found for user "
                            + userId
                            + " for the meeting "
                            + meetingId));
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    Optional.ofNullable(videoServerSession.getVideoInHandleId())
        .ifPresentOrElse(
            handleId ->
                updateSubscriptions(
                    serverId,
                    videoServerSession.getConnectionId(),
                    userId,
                    handleId,
                    subscriptionUpdatesDto),
            () -> {
              VideoServerResponse videoServerResponse =
                  attachToPlugin(
                      serverId,
                      videoServerSession.getConnectionId(),
                      JANUS_VIDEOROOM_PLUGIN,
                      meetingId);
              VideoServerSession videoServerSessionUpdated =
                  videoServerSessionRepository.update(
                      videoServerSession.videoInHandleId(videoServerResponse.getDataId()));
              joinVideoRoomAsSubscriber(
                  serverId,
                  videoServerSessionUpdated.getConnectionId(),
                  userId,
                  videoServerSessionUpdated.getVideoInHandleId(),
                  videoServerMeeting.getVideoRoomId(),
                  subscriptionUpdatesDto.getSubscribe());
            });
  }

  private void joinVideoRoomAsSubscriber(
      UUID serverId,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      List<MediaStreamDto> mediaStreamDtos) {
    VideoRoomResponse videoRoomResponse;
    VideoRoomJoinRequest videoRoomJoinRequest =
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
                                                mediaStreamDto.getType().toString().toUpperCase()))
                                        .userId(mediaStreamDto.getUserId())
                                        .toString()))
                    .collect(Collectors.toList()));

    videoRoomResponse =
        sendVideoRoomPluginMessage(
            serverId, connectionId, videoHandleId, videoRoomJoinRequest, null);
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while user "
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
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse =
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
                        .collect(Collectors.toList()))
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
                        .collect(Collectors.toList())),
            null);
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while user "
              + userId
              + " with connection id "
              + connectionId
              + " is updating media subscriptions in the video room");
    }
  }

  @Override
  @Transactional
  public void offerRtcAudioStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting =
        videoServerMeetingRepository
            .getById(meetingId)
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession =
        videoServerMeeting.getVideoServerSessions().stream()
            .filter(sessionUser -> sessionUser.getUserId().equals(userId))
            .findFirst()
            .orElseThrow(
                () ->
                    new VideoServerException(
                        "No Videoserver session found for user "
                            + userId
                            + " for the meeting "
                            + meetingId));
    UUID serverId = UUID.fromString(videoServerMeeting.getServerId());
    Optional.ofNullable(videoServerSession.getAudioHandleId())
        .ifPresentOrElse(
            handleId ->
                joinAudioBridgeRoom(
                    serverId,
                    userId,
                    videoServerSession.getConnectionId(),
                    handleId,
                    videoServerMeeting.getAudioRoomId(),
                    sdp),
            () -> {
              VideoServerResponse videoServerResponse =
                  attachToPlugin(
                      serverId,
                      videoServerSession.getConnectionId(),
                      JANUS_AUDIOBRIDGE_PLUGIN,
                      meetingId);
              VideoServerSession videoServerSessionUpdated =
                  videoServerSessionRepository.update(
                      videoServerSession.audioHandleId(videoServerResponse.getDataId()));
              joinAudioBridgeRoom(
                  serverId,
                  userId,
                  videoServerSessionUpdated.getConnectionId(),
                  videoServerSessionUpdated.getAudioHandleId(),
                  videoServerMeeting.getAudioRoomId(),
                  sdp);
            });
  }

  private void joinAudioBridgeRoom(
      UUID serverId,
      String userId,
      String connectionId,
      String audioHandleId,
      String audioRoomId,
      String sdp) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse =
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
                    AudioBridgeJoinRequest.FILENAME_DEFAULT
                        + "_"
                        + userId
                        + "_"
                        + OffsetDateTime.now()),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
          "An error occured while user "
              + userId
              + " with connection id "
              + connectionId
              + " is joining the audio room");
    }
  }

  @Override
  public boolean isAlive() {
    try {
      return JANUS_SERVER_INFO.equals(
          videoServerClient
              .sendGetInfoRequest(videoServerURL + JANUS_ENDPOINT + JANUS_INFO_ENDPOINT)
              .getStatus());
    } catch (Exception e) {
      ChatsLogger.warn("Can't communicate with Video server due to: " + e);
      return false;
    }
  }

  /**
   * This method creates a 'connection' (session) on the VideoServer
   *
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse createConnection(UUID serverId) {
    return videoServerClient.sendVideoServerRequest(
        videoServerURL + JANUS_ENDPOINT + VIDEOSERVER_ROUTING_QUERYPARAM + serverId,
        VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret));
  }

  /**
   * This method destroys a specified connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerResponse destroyConnection(UUID serverId, String connectionId) {
    return interactWithConnection(serverId, connectionId, JANUS_DESTROY, null);
  }

  /**
   * This method allows you to interact with the connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action the action you want to perform on this 'connection' (session)
   * @param pluginName the plugin name you want to use to perform the action (optional)
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse interactWithConnection(
      UUID serverId, String connectionId, String action, @Nullable String pluginName) {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create()
            .messageRequest(action)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    Optional.ofNullable(pluginName)
        .ifPresent(v -> videoServerMessageRequest.pluginName(pluginName));
    return videoServerClient.sendVideoServerRequest(
        videoServerURL
            + JANUS_ENDPOINT
            + "/"
            + connectionId
            + VIDEOSERVER_ROUTING_QUERYPARAM
            + serverId,
        videoServerMessageRequest);
  }

  /**
   * This method destroys the previously attached plugin handle
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the plugin handle id
   * @return {@link AudioBridgeResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse destroyPluginHandle(
      UUID serverId, String connectionId, String handleId) {
    return sendDetachPluginMessage(serverId, connectionId, handleId);
  }

  /**
   * This method allows you to send a message to detach audio bridge plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached plugin handle id
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse sendDetachPluginMessage(
      UUID serverId, String connectionId, String handleId) {
    return videoServerClient.sendVideoServerRequest(
        videoServerURL
            + JANUS_ENDPOINT
            + "/"
            + connectionId
            + "/"
            + handleId
            + VIDEOSERVER_ROUTING_QUERYPARAM
            + serverId,
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret));
  }

  /**
   * This method allows you to send a message on audio bridge plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached audio bridge plugin handle id
   * @param videoServerPluginRequest the video server plugin request sent as body
   * @param rtcSessionDescription the rtc session description needed for WebRTC negotiation
   *     (optional)
   * @return {@link AudioBridgeResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private AudioBridgeResponse sendAudioBridgePluginMessage(
      UUID serverId,
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription)
        .ifPresent(videoServerMessageRequest::rtcSessionDescription);
    return videoServerClient.sendAudioBridgeRequest(
        videoServerURL
            + JANUS_ENDPOINT
            + "/"
            + connectionId
            + "/"
            + handleId
            + VIDEOSERVER_ROUTING_QUERYPARAM
            + serverId,
        videoServerMessageRequest);
  }

  /**
   * This method allows you to send a message on a video room plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached video room plugin handle id
   * @param videoServerPluginRequest the video server plugin request sent as body
   * @param rtcSessionDescription the rtc session description needed for WebRTC negotiation
   *     (optional)
   * @return {@link VideoRoomResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoRoomResponse sendVideoRoomPluginMessage(
      UUID serverId,
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {
    VideoServerMessageRequest videoServerMessageRequest =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription)
        .ifPresent(videoServerMessageRequest::rtcSessionDescription);
    return videoServerClient.sendVideoRoomRequest(
        videoServerURL
            + JANUS_ENDPOINT
            + "/"
            + connectionId
            + "/"
            + handleId
            + VIDEOSERVER_ROUTING_QUERYPARAM
            + serverId,
        videoServerMessageRequest);
  }
}
