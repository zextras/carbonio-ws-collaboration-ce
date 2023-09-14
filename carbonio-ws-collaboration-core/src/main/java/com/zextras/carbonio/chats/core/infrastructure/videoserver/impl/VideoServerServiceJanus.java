// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeLeaveRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeMuteRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomLeaveRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomPublishRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomStartVideoInRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomUpdateSubscriptionsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PongResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.meeting.model.MediaStreamDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import io.ebean.annotation.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

@Singleton
public class VideoServerServiceJanus implements VideoServerService {

  private static final String                       JANUS_ENDPOINT           = "/janus";
  private static final String                       JANUS_ADMIN_ENDPOINT     = "/admin";
  private static final String                       JANUS_PING               = "ping";
  private static final String                       JANUS_CREATE             = "create";
  private static final String                       JANUS_MESSAGE            = "message";
  private static final String                       JANUS_ATTACH             = "attach";
  private static final String                       JANUS_DETACH             = "detach";
  private static final String                       JANUS_DESTROY            = "destroy";
  private static final String                       JANUS_SUCCESS            = "success";
  private static final String                       JANUS_VIDEOROOM_PLUGIN   = "janus.plugin.videoroom";
  private static final String                       JANUS_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";
  private final        ObjectMapper                 objectMapper;
  private final        String                       videoServerURL;
  private final        String                       videoServerAdminURL;
  private final        String                       apiSecret;
  private final        HttpClient                   httpClient;
  private final        VideoServerMeetingRepository videoServerMeetingRepository;
  private final        VideoServerSessionRepository videoServerSessionRepository;

  @Inject
  public VideoServerServiceJanus(
    AppConfig appConfig,
    ObjectMapper objectMapper,
    HttpClient httpClient,
    VideoServerMeetingRepository videoServerMeetingRepository,
    VideoServerSessionRepository videoServerSessionRepository
  ) {
    this.videoServerURL = String.format("http://%s:%s",
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()
    );
    this.videoServerAdminURL = String.format("http://%s:%s",
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_ADMIN_PORT).orElseThrow()
    );
    this.apiSecret = appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElseThrow();
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  @Override
  @Transactional
  public void startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already active");
    }
    VideoServerResponse videoServerResponse = createNewConnection(meetingId);
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = attachToPlugin(connectionId, JANUS_AUDIOBRIDGE_PLUGIN, meetingId);
    String audioHandleId = videoServerResponse.getDataId();
    AudioBridgeResponse audioBridgeResponse = createAudioBridgeRoom(meetingId, connectionId, audioHandleId);
    videoServerResponse = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoHandleId = videoServerResponse.getDataId();
    VideoRoomResponse videoRoomResponse = createVideoRoom(meetingId, connectionId, videoHandleId);
    videoServerMeetingRepository.insert(
      meetingId,
      connectionId,
      audioHandleId,
      videoHandleId,
      audioBridgeResponse.getRoom(),
      videoRoomResponse.getRoom()
    );
  }

  private VideoServerResponse createNewConnection(String meetingId) {
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection for the meeting " + meetingId);
    }
    return videoServerResponse;
  }

  private VideoServerResponse attachToPlugin(String connectionId, String pluginType, String meetingId) {
    VideoServerResponse videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, pluginType);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the plugin for the connection " + connectionId +
          " for the meeting " + meetingId);
    }
    return videoServerResponse;
  }

  private AudioBridgeResponse createAudioBridgeRoom(String meetingId, String connectionId, String audioHandleId) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse = sendAudioBridgePluginMessage(
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
      null
    );
    if (!AudioBridgeResponse.CREATED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occurred when creating an audiobridge room for the connection " + connectionId + " with plugin "
          + audioHandleId + " for the meeting " + meetingId);
    }
    return audioBridgeResponse;
  }

  private VideoRoomResponse createVideoRoom(String meetingId, String connectionId, String videoHandleId) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
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
        .videoCodec(Arrays.stream(VideoCodec.values()).map(videoCodec -> videoCodec.toString().toLowerCase())
          .collect(Collectors.joining(","))),
      null
    );
    if (!VideoRoomResponse.CREATED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occurred when creating a videoroom room for the connection " + connectionId + " with plugin "
          + videoHandleId + " for the meeting " + meetingId);
    }
    return videoRoomResponse;
  }

  @Override
  @Transactional
  public void stopMeeting(String meetingId) {
    VideoServerMeeting videoServerMeetingToRemove = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    destroyVideoRoom(meetingId, videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getVideoHandleId(), videoServerMeetingToRemove.getVideoRoomId());
    destroyAudioBridgeRoom(meetingId, videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getAudioHandleId(), videoServerMeetingToRemove.getAudioRoomId());
    destroyPluginHandle(videoServerMeetingToRemove.getConnectionId(), videoServerMeetingToRemove.getVideoHandleId(),
      meetingId);
    destroyPluginHandle(videoServerMeetingToRemove.getConnectionId(), videoServerMeetingToRemove.getAudioHandleId(),
      meetingId);
    destroyConnection(videoServerMeetingToRemove.getConnectionId(), meetingId);
    videoServerMeetingRepository.deleteById(meetingId);
  }

  private void destroyPluginHandle(String connectionId, String handleId, String meetingId) {
    VideoServerResponse videoServerResponse = destroyPluginHandle(connectionId, handleId);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the plugin handle for the connection "
          + connectionId + " with plugin "
          + handleId + " for the meeting " + meetingId);
    }
  }

  private void destroyConnection(String connectionId, String meetingId) {
    VideoServerResponse videoServerResponse = destroyConnection(connectionId);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the videoserver connection "
          + connectionId + " for the meeting " + meetingId);
    }
  }

  private void destroyVideoRoom(String meetingId, String connectionId, String videoHandleId,
    String videoRoomId) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoHandleId,
      VideoRoomDestroyRequest.create()
        .request(VideoRoomDestroyRequest.DESTROY)
        .room(videoRoomId)
        .permanent(false),
      null
    );
    if (!VideoRoomResponse.DESTROYED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException("An error occurred when destroying the videoroom for the connection "
        + connectionId + " with plugin "
        + videoHandleId + " for the meeting " + meetingId);
    }
  }

  private void destroyAudioBridgeRoom(String meetingId, String connectionId, String audioHandleId, String audioRoomId) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse = sendAudioBridgePluginMessage(
      connectionId,
      audioHandleId,
      AudioBridgeDestroyRequest.create()
        .request(AudioBridgeDestroyRequest.DESTROY)
        .room(audioRoomId)
        .permanent(false),
      null
    );
    if (!AudioBridgeResponse.DESTROYED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException("An error occurred when destroying the audiobridge room for the connection "
        + connectionId + " with plugin "
        + audioHandleId + " for the meeting " + meetingId);
    }
  }

  @Override
  @Transactional
  public void joinMeeting(String userId, String queueId, String meetingId, boolean videoStreamOn,
    boolean audioStreamOn) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    if (videoServerMeeting.getVideoServerSessions().stream()
      .anyMatch(videoServerSessionUser -> videoServerSessionUser.getQueueId().equals(queueId))) {
      throw new VideoServerException(
        "Videoserver session user with user  " + userId + "is already present in the videoserver meeting " + meetingId);
    }
    VideoServerResponse videoServerResponse = createNewConnection(meetingId);
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoOutHandleId = videoServerResponse.getDataId();
    joinVideoRoom(connectionId, userId, videoOutHandleId, videoServerMeeting.getVideoRoomId(), Ptype.PUBLISHER,
      MediaType.VIDEO, null);
    videoServerResponse = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN, meetingId);
    String screenHandleId = videoServerResponse.getDataId();
    joinVideoRoom(connectionId, userId, screenHandleId, videoServerMeeting.getVideoRoomId(), Ptype.PUBLISHER,
      MediaType.SCREEN, null);
    videoServerSessionRepository.insert(
      videoServerMeeting, userId, queueId, connectionId, videoOutHandleId, screenHandleId
    );
  }

  private void joinVideoRoom(String connectionId, String userId, String videoHandleId, String videoRoomId,
    Ptype ptype, @Nullable MediaType mediaType, @Nullable List<MediaStreamDto> mediaStreamDtos) {
    VideoRoomResponse videoRoomResponse;
    VideoRoomJoinRequest videoRoomJoinRequest = VideoRoomJoinRequest.create()
      .request(VideoRoomJoinRequest.JOIN)
      .ptype(ptype.toString().toLowerCase())
      .room(videoRoomId);
    if (Ptype.PUBLISHER.equals(ptype) && mediaType != null) {
      videoRoomJoinRequest.id(Feed.create().type(mediaType).userId(userId).toString());
    }
    if (Ptype.SUBSCRIBER.equals(ptype) && mediaStreamDtos != null) {
      videoRoomJoinRequest.streams(mediaStreamDtos.stream().map(
        mediaStreamDto ->
          Stream.create().feed(
            Feed.create()
              .type(MediaType.valueOf(mediaStreamDto.getType().toString().toUpperCase()))
              .userId(mediaStreamDto.getUserId()).toString())
      ).collect(Collectors.toList()));
    }
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoHandleId,
      videoRoomJoinRequest,
      null
    );
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while user " + userId + " with connection id " + connectionId
          + " is joining video room as " + ptype.toString().toLowerCase());
    }
  }

  @Override
  @Transactional
  public void leaveMeeting(String userId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for user " + userId + " for the meeting " + meetingId));
    Optional.ofNullable(videoServerSession.getAudioHandleId()).ifPresent(audioHandleId -> {
      leaveAudioBridgeRoom(videoServerSession.getConnectionId(), audioHandleId);
      destroyPluginHandle(videoServerSession.getConnectionId(), audioHandleId);
    });
    Optional.ofNullable(videoServerSession.getVideoOutHandleId()).ifPresent(videoOutHandleId -> {
      leaveVideoRoom(videoServerSession.getConnectionId(), videoOutHandleId);
      destroyPluginHandle(videoServerSession.getConnectionId(), videoOutHandleId);
    });
    Optional.ofNullable(videoServerSession.getVideoInHandleId()).ifPresent(videoInHandleId -> {
      leaveVideoRoom(videoServerSession.getConnectionId(), videoInHandleId);
      destroyPluginHandle(videoServerSession.getConnectionId(), videoInHandleId);
    });
    Optional.ofNullable(videoServerSession.getScreenHandleId()).ifPresent(screenHandleId -> {
      leaveVideoRoom(videoServerSession.getConnectionId(), screenHandleId);
      destroyPluginHandle(videoServerSession.getConnectionId(), screenHandleId);
    });
    destroyConnection(videoServerSession.getConnectionId(), meetingId);
    videoServerSessionRepository.remove(videoServerSession);
  }

  private void leaveAudioBridgeRoom(String connectionId, String audioHandleId) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse = sendAudioBridgePluginMessage(
      connectionId,
      audioHandleId,
      AudioBridgeLeaveRequest.create()
        .request(AudioBridgeLeaveRequest.LEAVE),
      null
    );
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while connection id " + connectionId
          + " is leaving the audio room");
    }
  }

  private void leaveVideoRoom(String connectionId, String videoHandleId) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoHandleId,
      VideoRoomLeaveRequest.create()
        .request(VideoRoomLeaveRequest.LEAVE),
      null
    );
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while connection id " + connectionId
          + " is leaving the video room");
    }
  }

  @Override
  public void updateMediaStream(String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session found for user " + userId + " for the meeting " + meetingId));
    boolean isVideoStream = MediaType.VIDEO.toString().equalsIgnoreCase(mediaStreamSettingsDto.getType().toString());
    boolean mediaStreamEnabled =
      isVideoStream ? videoServerSession.hasVideoOutStreamOn() : videoServerSession.hasScreenStreamOn();
    if (mediaStreamSettingsDto.isEnabled() == mediaStreamEnabled) {
      ChatsLogger.debug(
        "Media stream status is already updated for session " + userId + " for the meeting " + meetingId);
    } else {
      if (mediaStreamSettingsDto.isEnabled()) {
        publishStreamOnVideoRoom(videoServerSession.getConnectionId(),
          isVideoStream ? videoServerSession.getVideoOutHandleId() : videoServerSession.getScreenHandleId(),
          mediaStreamSettingsDto.getSdp());
      }
      VideoServerSession videoServerSessionToUpdate =
        isVideoStream ? videoServerSession.videoOutStreamOn(mediaStreamSettingsDto.isEnabled())
          : videoServerSession.screenStreamOn(mediaStreamSettingsDto.isEnabled());
      videoServerSessionRepository.update(videoServerSessionToUpdate);
    }
  }

  private void publishStreamOnVideoRoom(String connectionId, String videoHandleId,
    String sessionDescriptionProtocol) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoHandleId,
      VideoRoomPublishRequest.create()
        .request(VideoRoomPublishRequest.PUBLISH),
      RtcSessionDescription.create().type(RtcType.OFFER).sdp(sessionDescriptionProtocol)
    );
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while connection id " + connectionId
          + " is joining video room as publisher");
    }
  }

  @Override
  public void updateAudioStream(String userId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session found for user " + userId + " for the meeting " + meetingId));
    if (enabled == videoServerSession.hasAudioStreamOn()) {
      ChatsLogger.debug(
        "Audio stream status is already updated for user " + userId + " for the meeting " + meetingId);
    } else {
      muteAudioStream(videoServerMeeting.getConnectionId(), videoServerSession.getConnectionId(), userId,
        videoServerMeeting.getAudioHandleId(), videoServerMeeting.getAudioRoomId(), enabled);
      videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled));
    }
  }

  private void muteAudioStream(String meetingConnectionId, String connectionId, String userId,
    String meetingAudioHandleId, String audioRoomId, boolean enabled) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse = sendAudioBridgePluginMessage(
      meetingConnectionId,
      meetingAudioHandleId,
      AudioBridgeMuteRequest.create()
        .request(enabled ? AudioBridgeMuteRequest.UNMUTE : AudioBridgeMuteRequest.MUTE)
        .room(audioRoomId)
        .id(userId),
      null
    );
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while setting audio stream status for " + userId + " with connection id " + connectionId);
    }
  }

  @Override
  public void answerRtcMediaStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session found for user " + userId + " for the meeting " + meetingId));
    AtomicReference<String> videoInHandleId = new AtomicReference<>();
    Optional.ofNullable(videoServerSession.getVideoInHandleId()).ifPresentOrElse(videoInHandleId::set, () -> {
      VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
        JANUS_VIDEOROOM_PLUGIN, meetingId);
      videoInHandleId.set(videoServerResponse.getDataId());
      videoServerSessionRepository.update(videoServerSession.videoInHandleId(videoInHandleId.get()));
    });
    startVideoIn(videoServerSession.getConnectionId(), videoInHandleId.get(), sdp);
    videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
  }

  private void startVideoIn(String connectionId, String videoInHandleId, String sdp) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoInHandleId,
      VideoRoomStartVideoInRequest.create()
        .request(VideoRoomStartVideoInRequest.START),
      RtcSessionDescription.create().type(RtcType.ANSWER).sdp(sdp)
    );
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while session with connection id " + connectionId
          + " is starting receiving video streams available in the video room");
    }
  }

  @Override
  public void updateSubscriptionsMediaStream(String userId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session found for user " + userId + " for the meeting " + meetingId));
    Optional.ofNullable(videoServerSession.getVideoInHandleId()).ifPresentOrElse(
      handleId -> updateSubscriptions(videoServerSession.getConnectionId(), userId, handleId,
        subscriptionUpdatesDto),
      () -> {
        VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
          JANUS_VIDEOROOM_PLUGIN, meetingId);
        VideoServerSession videoServerSessionUpdated = videoServerSessionRepository.update(
          videoServerSession.videoInHandleId(videoServerResponse.getDataId()));
        joinVideoRoom(videoServerSessionUpdated.getConnectionId(), userId,
          videoServerSessionUpdated.getVideoInHandleId(), videoServerMeeting.getVideoRoomId(),
          Ptype.SUBSCRIBER, null, subscriptionUpdatesDto.getSubscribe());
      });
  }

  private void updateSubscriptions(String connectionId, String userId, String videoInHandleId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoRoomResponse videoRoomResponse;
    videoRoomResponse = sendVideoRoomPluginMessage(
      connectionId,
      videoInHandleId,
      VideoRoomUpdateSubscriptionsRequest.create()
        .request(VideoRoomUpdateSubscriptionsRequest.UPDATE)
        .subscriptions(subscriptionUpdatesDto.getSubscribe().stream().map(mediaStreamDto -> Stream.create()
          .feed(Feed.create().type(MediaType.valueOf(mediaStreamDto.getType().toString().toUpperCase()))
            .userId(mediaStreamDto.getUserId()).toString())).collect(Collectors.toList()))
        .unsubscriptions(subscriptionUpdatesDto.getUnsubscribe().stream().map(mediaStreamDto -> Stream.create()
          .feed(Feed.create().type(MediaType.valueOf(mediaStreamDto.getType().toString().toUpperCase()))
            .userId(mediaStreamDto.getUserId()).toString())).collect(Collectors.toList())),
      null
    );
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while user " + userId + " with connection id " + connectionId
          + " is updating media subscriptions in the video room");
    }
  }

  @Override
  public void offerRtcAudioStream(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getUserId().equals(userId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session found for user " + userId + " for the meeting " + meetingId));
    AtomicReference<String> audioHandleId = new AtomicReference<>();
    Optional.ofNullable(videoServerSession.getAudioHandleId()).ifPresentOrElse(audioHandleId::set, () -> {
      VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
        JANUS_AUDIOBRIDGE_PLUGIN, meetingId);
      audioHandleId.set(videoServerResponse.getDataId());
      videoServerSession.audioHandleId(audioHandleId.get());
      videoServerSessionRepository.update(videoServerSession);
    });
    joinAudioBridgeRoom(userId, videoServerSession.getConnectionId(), audioHandleId.get(),
      videoServerMeeting.getAudioRoomId(), sdp);
  }

  private void joinAudioBridgeRoom(String userId, String connectionId, String audioHandleId, String audioRoomId,
    String sdp) {
    AudioBridgeResponse audioBridgeResponse;
    audioBridgeResponse = sendAudioBridgePluginMessage(
      connectionId,
      audioHandleId,
      AudioBridgeJoinRequest.create()
        .request(AudioBridgeJoinRequest.JOIN)
        .room(audioRoomId)
        .id(userId)
        .muted(true)
        .filename(AudioBridgeJoinRequest.FILENAME_DEFAULT + "_" + userId + "_" + OffsetDateTime.now()),
      RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp)
    );
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
        "An error occured while user " + userId + " with connection id " + connectionId
          + " is joining the audio room");
    }
  }

  @Override
  public boolean isAlive() {
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerAdminURL + JANUS_ADMIN_ENDPOINT,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          VideoServerMessageRequest.create()
            .messageRequest(JANUS_PING)
            .transactionId(UUID.randomUUID().toString())
        )
      );
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return PongResponse.PONG.equals(
        objectMapper.readValue(
          IOUtils.toString(
            response.getEntity().getContent(),
            StandardCharsets.UTF_8),
          PongResponse.class
        ).getStatus()
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method creates a 'connection' (session) on the VideoServer
   *
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse createConnection() {
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret)
        )
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(
          response.getEntity().getContent(),
          StandardCharsets.UTF_8),
        VideoServerResponse.class
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method destroys the previously attached plugin handle
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId     the plugin handle id
   * @return {@link AudioBridgeResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendDetachPluginMessage(connectionId, handleId);
  }

  /**
   * This method destroys a specified connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerResponse destroyConnection(String connectionId) {
    return interactWithConnection(connectionId, JANUS_DESTROY, null);
  }

  /**
   * This method allows you to interact with the connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action       the action you want to perform on this 'connection' (session)
   * @param pluginName   the plugin name you want to use to perform the action (optional)
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse interactWithConnection(String connectionId, String action, @Nullable String pluginName) {
    try {
      VideoServerMessageRequest videoServerMessageRequest = VideoServerMessageRequest.create()
        .messageRequest(action)
        .transactionId(UUID.randomUUID().toString())
        .apiSecret(apiSecret);
      Optional.ofNullable(pluginName).ifPresent(v -> videoServerMessageRequest.pluginName(pluginName));
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          videoServerMessageRequest
        )
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(
          response.getEntity().getContent(),
          StandardCharsets.UTF_8
        ),
        VideoServerResponse.class
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method allows you to send a message to detach audio bridge plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId     the previously attached plugin handle id
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse sendDetachPluginMessage(String connectionId, String handleId) {
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceJanus.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret)
        )
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(
          response.getEntity().getContent(),
          StandardCharsets.UTF_8
        ),
        VideoServerResponse.class
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method allows you to send a message on audio bridge plugin previously attached
   *
   * @param connectionId             the 'connection' (session) id
   * @param handleId                 the previously attached audio bridge plugin handle id
   * @param videoServerPluginRequest the video server plugin request sent as body
   * @param rtcSessionDescription    the rtc session description needed for WebRTC negotiation (optional)
   * @return {@link AudioBridgeResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private AudioBridgeResponse sendAudioBridgePluginMessage(String connectionId, String handleId,
    VideoServerPluginRequest videoServerPluginRequest, @Nullable RtcSessionDescription rtcSessionDescription) {
    VideoServerMessageRequest videoServerMessageRequest = VideoServerMessageRequest.create()
      .messageRequest(VideoServerServiceJanus.JANUS_MESSAGE)
      .transactionId(UUID.randomUUID().toString())
      .videoServerPluginRequest(videoServerPluginRequest)
      .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription).ifPresent(videoServerMessageRequest::rtcSessionDescription);
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          videoServerMessageRequest
        )
      );
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(
          response.getEntity().getContent(),
          StandardCharsets.UTF_8
        ),
        AudioBridgeResponse.class
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method allows you to send a message on a video room plugin previously attached
   *
   * @param connectionId             the 'connection' (session) id
   * @param handleId                 the previously attached video room plugin handle id
   * @param videoServerPluginRequest the video server plugin request sent as body
   * @param rtcSessionDescription    the rtc session description needed for WebRTC negotiation (optional)
   * @return {@link VideoRoomResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoRoomResponse sendVideoRoomPluginMessage(String connectionId, String handleId,
    VideoServerPluginRequest videoServerPluginRequest, @Nullable RtcSessionDescription rtcSessionDescription) {
    VideoServerMessageRequest videoServerMessageRequest = VideoServerMessageRequest.create()
      .messageRequest(VideoServerServiceJanus.JANUS_MESSAGE)
      .transactionId(UUID.randomUUID().toString())
      .videoServerPluginRequest(videoServerPluginRequest)
      .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription).ifPresent(videoServerMessageRequest::rtcSessionDescription);
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
        Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(
          videoServerMessageRequest
        )
      );
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(
          response.getEntity().getContent(),
          StandardCharsets.UTF_8
        ),
        VideoRoomResponse.class
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }
}
