// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Feed.Type;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Jsep;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.Stream;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.VideoCodec;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeAllowedRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeChangeRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeConfigureRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeEditRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeEnableMjrsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeEnableRecordingRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeExistsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeKickRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeLeaveRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeListParticipantsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeListRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeMuteRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgePlayFileRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomAllowedRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomConfigurePublisherRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomConfigureSubscriberRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomEditRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomEnableRecordingRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomExistsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomJoinPublisherRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomJoinSubscriberRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomKickRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomLeavePublisherRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomLeaveSubscriberRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomListParticipantsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomListRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomModerateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomPauseVideoInRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomPublishRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomStartVideoInRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomSwitchRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomUnpublishRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomUpdateSubscriptionsRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PongResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoRoomResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.meeting.model.RtcSessionDescriptionDto;
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
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
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
    this.apiSecret = appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN).orElseThrow();
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  private String writeValueAsAString(Object value) throws JsonProcessingException {
    return objectMapper.writeValueAsString(value);
  }

  @Override
  @Transactional
  public void createMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already present");
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
      VideoServerMeeting.create()
        .meetingId(meetingId)
        .connectionId(connectionId)
        .audioHandleId(audioHandleId)
        .videoHandleId(videoHandleId)
        .audioRoomId(audioBridgeResponse.getRoom())
        .videoRoomId(videoRoomResponse.getRoom())
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
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        connectionId,
        audioHandleId,
        writeValueAsAString(
          AudioBridgeCreateRequest.create()
            .request(AudioBridgeCreateRequest.CREATE)
            .room(AudioBridgeCreateRequest.ROOM_DEFAULT + UUID.randomUUID())
            .permanent(false)
            .description(AudioBridgeCreateRequest.DESCRIPTION_DEFAULT + UUID.randomUUID())
            .isPrivate(false)
            .samplingRate(AudioBridgeCreateRequest.SAMPLING_RATE_DEFAULT)
            .audioActivePackets(AudioBridgeCreateRequest.AUDIO_ACTIVE_PACKETS_DEFAULT)
            .audioLevelAverage(AudioBridgeCreateRequest.AUDIO_LEVEL_AVERAGE_DEFAULT)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.CREATED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occurred when creating an audiobridge room for the connection " + connectionId + " with plugin "
          + audioHandleId + " for the meeting " + meetingId);
    }
    return audioBridgeResponse;
  }

  private VideoRoomResponse createVideoRoom(String meetingId, String connectionId, String videoHandleId) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoHandleId,
        writeValueAsAString(
          VideoRoomCreateRequest.create()
            .request(VideoRoomCreateRequest.CREATE)
            .room(VideoRoomCreateRequest.ROOM_DEFAULT + UUID.randomUUID())
            .permanent(false)
            .description(VideoRoomCreateRequest.DESCRIPTION_DEFAULT + UUID.randomUUID())
            .isPrivate(false)
            .publishers(VideoRoomCreateRequest.MAX_PUBLISHERS_DEFAULT)
            .bitrate(VideoRoomCreateRequest.BITRATE_DEFAULT)
            .videoCodec(Arrays.stream(VideoCodec.values()).map(videoCodec -> videoCodec.toString().toLowerCase())
              .collect(Collectors.joining(",")))
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.CREATED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occurred when creating a videoroom room for the connection " + connectionId + " with plugin "
          + videoHandleId + " for the meeting " + meetingId);
    }
    return videoRoomResponse;
  }

  @Override
  @Transactional
  public void deleteMeeting(String meetingId) {
    VideoServerMeeting videoServerMeetingToRemove = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    destroyVideoRoom(meetingId, videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getVideoHandleId(), videoServerMeetingToRemove.getVideoRoomId());
    destroyAudioBridgeRoom(meetingId, videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getAudioHandleId(), videoServerMeetingToRemove.getAudioRoomId());
    destroyPluginHandle(videoServerMeetingToRemove.getConnectionId(), videoServerMeetingToRemove.getAudioHandleId(),
      meetingId);
    destroyPluginHandle(videoServerMeetingToRemove.getConnectionId(), videoServerMeetingToRemove.getVideoHandleId(),
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
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoHandleId,
        writeValueAsAString(
          VideoRoomDestroyRequest.create()
            .request(VideoRoomDestroyRequest.DESTROY)
            .room(videoRoomId)
            .permanent(false)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.DESTROYED.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException("An error occurred when destroying the videoroom for the connection "
        + connectionId + " with plugin "
        + videoHandleId + " for the meeting " + meetingId);
    }
  }

  private void destroyAudioBridgeRoom(String meetingId, String connectionId, String audioHandleId, String audioRoomId) {
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        connectionId,
        audioHandleId,
        writeValueAsAString(
          AudioBridgeDestroyRequest.create()
            .request(AudioBridgeDestroyRequest.DESTROY)
            .room(audioRoomId)
            .permanent(false)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.DESTROYED.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException("An error occurred when destroying the audiobridge room for the connection "
        + connectionId + " with plugin "
        + audioHandleId + " for the meeting " + meetingId);
    }
  }

  @Override
  @Transactional
  public void joinMeeting(String sessionId, String meetingId, boolean videoStreamOn, boolean audioStreamOn) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    if (videoServerMeeting.getVideoServerSessions().stream()
      .anyMatch(videoServerSessionUser -> videoServerSessionUser.getSessionId().equals(sessionId))) {
      throw new VideoServerException(
        "Videoserver session user with sessionId " + sessionId + "is already present in the videoserver meeting "
          + meetingId);
    }
    VideoServerResponse videoServerResponse = createNewConnection(meetingId);
    String connectionId = videoServerResponse.getDataId();
    videoServerSessionRepository.insert(
      VideoServerSession.create(sessionId, videoServerMeeting)
        .connectionId(connectionId)
        .videoOutStreamOn(false)
        .audioStreamOn(false)
    );
  }

  @Override
  @Transactional
  public void leaveMeeting(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    leaveAudioBridgeRoom(videoServerSession.getConnectionId(), videoServerSession.getAudioHandleId(),
      videoServerSession.getSessionId());
    if (videoServerSession.getVideoInHandleId() != null) {
      leaveVideoRoomAsSubscriber(videoServerSession.getConnectionId(), videoServerSession.getSessionId(),
        videoServerSession.getVideoInHandleId());
      destroyPluginHandle(videoServerSession.getConnectionId(), videoServerSession.getVideoInHandleId());
    }
    if (videoServerSession.getVideoOutHandleId() != null) {
      leaveVideoRoomAsPublisher(videoServerSession.getConnectionId(), videoServerSession.getSessionId(),
        videoServerSession.getVideoOutHandleId());
      destroyPluginHandle(videoServerSession.getConnectionId(), videoServerSession.getVideoOutHandleId());
    }
    if (videoServerSession.getScreenHandleId() != null) {
      leaveVideoRoomAsPublisher(videoServerSession.getConnectionId(), videoServerSession.getSessionId(),
        videoServerSession.getScreenHandleId());
      destroyPluginHandle(videoServerSession.getConnectionId(), videoServerSession.getScreenHandleId());
    }
    destroyPluginHandle(videoServerSession.getConnectionId(), videoServerSession.getAudioHandleId());
    destroyConnection(videoServerSession.getConnectionId(), meetingId);
    videoServerSessionRepository.remove(videoServerSession);
  }

  private void leaveAudioBridgeRoom(String connectionId, String audioHandleId, String sessionId) {
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        connectionId,
        audioHandleId,
        writeValueAsAString(
          AudioBridgeLeaveRequest.create()
            .request(AudioBridgeLeaveRequest.LEAVE)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is leaving the audio room");
    }
  }

  private void leaveVideoRoomAsSubscriber(String connectionId, String sessionId, String videoInHandleId) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoInHandleId,
        writeValueAsAString(
          VideoRoomLeaveSubscriberRequest.create()
            .request(VideoRoomLeaveSubscriberRequest.LEAVE)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is leaving the video room as subscriber");
    }
  }

  private void leaveVideoRoomAsPublisher(String connectionId, String sessionId, String videoOutHandleId) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoOutHandleId,
        writeValueAsAString(
          VideoRoomLeavePublisherRequest.create()
            .request(VideoRoomLeavePublisherRequest.LEAVE)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is leaving the video room as publisher");
    }
  }

  @Override
  public void updateVideoStream(String sessionId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    if (enabled == videoServerSession.hasVideoOutStreamOn()) {
      throw new VideoServerException(
        "Audio stream status is already updated for session " + sessionId + " for the meeting " + meetingId);
    }
    videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled));
  }

  @Override
  public void updateAudioStream(String sessionId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    if (enabled == videoServerSession.hasAudioStreamOn()) {
      throw new VideoServerException(
        "Audio stream status is already updated for session " + sessionId + " for the meeting " + meetingId);
    }
    muteAudioStream(videoServerMeeting.getConnectionId(), videoServerSession.getConnectionId(), sessionId,
      videoServerMeeting.getAudioHandleId(), videoServerMeeting.getAudioRoomId(), enabled);
    videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled));
  }

  private void muteAudioStream(String meetingConnectionId, String connectionId, String sessionId, String audioHandleId,
    String audioRoomId, boolean enabled) {
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        meetingConnectionId,
        audioHandleId,
        writeValueAsAString(
          AudioBridgeMuteRequest.create()
            .request(enabled ? AudioBridgeMuteRequest.UNMUTE : AudioBridgeMuteRequest.MUTE)
            .room(audioRoomId)
            .id(connectionId)
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while setting audio stream status for " + sessionId + " with connection id " + connectionId);
    }
  }

  @Override
  public void updateScreenStream(String sessionId, String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    if (enabled == videoServerSession.hasScreenStreamOn()) {
      throw new VideoServerException(
        "Audio stream status is already updated for session " + sessionId + " for the meeting " + meetingId);
    }
    videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled));
  }

  @Override
  public void offerRtcVideoStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
      JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoOutHandleId = videoServerResponse.getDataId();
    joinVideoRoomAsPublisher(videoServerSession.getConnectionId(), sessionId, videoOutHandleId,
      videoServerMeeting.getVideoRoomId(), Type.VIDEO, rtcSessionDescriptionDto);
    videoServerSessionRepository.update(videoServerSession.videoOutHandleId(videoOutHandleId).videoOutStreamOn(true));
  }

  private void joinVideoRoomAsPublisher(String connectionId, String sessionId, String videoOutHandleId,
    String videoRoomId, Type feedType, RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoOutHandleId,
        writeValueAsAString(
          VideoRoomJoinPublisherRequest.create()
            .request(VideoRoomJoinPublisherRequest.JOIN_AND_CONFIGURE)
            .ptype(VideoRoomJoinPublisherRequest.PUBLISHER)
            .room(videoRoomId)
            .id(Feed.create().type(feedType).sessionId(sessionId).toString())
        ),
        rtcSessionDescriptionDto
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is joining video room as publisher");
    }
  }

  @Override
  public void answerRtcMediaStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
      JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoInHandleId = videoServerResponse.getDataId();
    startVideoIn(videoServerSession.getConnectionId(), sessionId, videoInHandleId, rtcSessionDescriptionDto);
    videoServerSessionRepository.update(videoServerSession.videoInHandleId(videoInHandleId).videoInStreamOn(true));
  }

  private void startVideoIn(String connectionId, String sessionId, String videoInHandleId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoInHandleId,
        writeValueAsAString(
          VideoRoomStartVideoInRequest.create()
            .request(VideoRoomStartVideoInRequest.START)
        ),
        rtcSessionDescriptionDto
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is starting receiving video streams available in the video room");
    }
  }

  @Override
  public void updateSubscriptionsMediaStream(String sessionId, String meetingId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
      JANUS_VIDEOROOM_PLUGIN, meetingId);
    String videoInHandleId = videoServerResponse.getDataId();
    updateSubscriptions(videoServerSession.getConnectionId(), sessionId, videoInHandleId, subscriptionUpdatesDto);
    videoServerSessionRepository.update(videoServerSession.videoInHandleId(videoInHandleId));
  }

  private void updateSubscriptions(String connectionId, String sessionId, String videoInHandleId,
    SubscriptionUpdatesDto subscriptionUpdatesDto) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoInHandleId,
        writeValueAsAString(
          VideoRoomUpdateSubscriptionsRequest.create()
            .request(VideoRoomUpdateSubscriptionsRequest.UPDATE)
            .subscriptions(subscriptionUpdatesDto.getSubscribe().stream().map(mediaStreamDto -> Stream.create()
              .feed(Feed.create().type(Type.valueOf(mediaStreamDto.getType().toString().toUpperCase()))
                .sessionId(mediaStreamDto.getSessionId()))).collect(Collectors.toList()))
            .unsubscriptions(subscriptionUpdatesDto.getUnsubscribe().stream().map(mediaStreamDto -> Stream.create()
              .feed(Feed.create().type(Type.valueOf(mediaStreamDto.getType().toString().toUpperCase()))
                .sessionId(mediaStreamDto.getSessionId()))).collect(Collectors.toList()))
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is updating media subscriptions in the video room");
    }
  }

  @Override
  public void offerRtcAudioStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
      JANUS_AUDIOBRIDGE_PLUGIN, meetingId);
    String audioHandleId = videoServerResponse.getDataId();
    joinAudioBridgeRoom(sessionId, videoServerSession.getConnectionId(), audioHandleId,
      videoServerMeeting.getAudioRoomId(), rtcSessionDescriptionDto);
    videoServerSessionRepository.update(videoServerSession.audioHandleId(audioHandleId).audioStreamOn(true));
  }

  private void joinAudioBridgeRoom(String sessionId, String connectionId, String audioHandleId, String audioRoomId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        connectionId,
        audioHandleId,
        writeValueAsAString(
          AudioBridgeJoinRequest.create()
            .request(AudioBridgeJoinRequest.JOIN)
            .room(audioRoomId)
            .id(sessionId)
            .muted(false)
            .filename(AudioBridgeJoinRequest.FILENAME_DEFAULT + "_" + sessionId + "_" + OffsetDateTime.now())
        ),
        rtcSessionDescriptionDto
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is joining the audio room");
    }
  }

  @Override
  public void offerRtcScreenStream(String sessionId, String meetingId,
    RtcSessionDescriptionDto rtcSessionDescriptionDto) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = attachToPlugin(videoServerSession.getConnectionId(),
      JANUS_VIDEOROOM_PLUGIN, meetingId);
    String screenHandleId = videoServerResponse.getDataId();
    joinVideoRoomAsPublisher(videoServerSession.getConnectionId(), sessionId, screenHandleId,
      videoServerMeeting.getVideoRoomId(), Type.SCREEN, rtcSessionDescriptionDto);
    videoServerSessionRepository.update(videoServerSession.screenHandleId(screenHandleId).screenStreamOn(true));
  }

  @Override
  public boolean isAlive() {
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ADMIN_ENDPOINT,
        Map.of("content-type", "application/json"),
        writeValueAsAString(
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
        writeValueAsAString(
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
        writeValueAsAString(
          videoServerMessageRequest
        )
      );
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
        writeValueAsAString(
          VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceJanus.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret)
        )
      );
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
   * @param connectionId          the 'connection' (session) id
   * @param handleId              the previously attached audio bridge plugin handle id
   * @param body                  the body you want to send with the message
   * @param rtcSessionDescription the rtc session description needed for WebRTC negotiation (optional)
   * @return {@link AudioBridgeResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private AudioBridgeResponse sendAudioBridgePluginMessage(String connectionId, String handleId, String body,
    @Nullable RtcSessionDescriptionDto rtcSessionDescription) {
    try {
      VideoServerMessageRequest videoServerMessageRequest = VideoServerMessageRequest.create()
        .messageRequest(VideoServerServiceJanus.JANUS_MESSAGE)
        .transactionId(UUID.randomUUID().toString())
        .body(body)
        .apiSecret(apiSecret);
      Optional.ofNullable(rtcSessionDescription)
        .ifPresent(v -> videoServerMessageRequest.jsep(
          Jsep.create().type(rtcSessionDescription.getType().toString()).sdp(rtcSessionDescription.getSdp())));
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
        Map.of("content-type", "application/json"),
        writeValueAsAString(
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
   * @param connectionId          the 'connection' (session) id
   * @param handleId              the previously attached video room plugin handle id
   * @param body                  the body you want to send with the message
   * @param rtcSessionDescription the rtc session description needed for WebRTC negotiation (optional)
   * @return {@link VideoRoomResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoRoomResponse sendVideoRoomPluginMessage(String connectionId, String handleId, String body,
    @Nullable RtcSessionDescriptionDto rtcSessionDescription) {
    VideoServerMessageRequest videoServerMessageRequest = VideoServerMessageRequest.create()
      .messageRequest(VideoServerServiceJanus.JANUS_MESSAGE)
      .transactionId(UUID.randomUUID().toString())
      .body(body)
      .apiSecret(apiSecret);
    Optional.ofNullable(rtcSessionDescription)
      .ifPresent(v -> videoServerMessageRequest.jsep(
        Jsep.create().type(rtcSessionDescription.getType().toString()).sdp(rtcSessionDescription.getSdp())));
    try {
      CloseableHttpResponse response = httpClient.sendPost(
        videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
        Map.of("content-type", "application/json"),
        writeValueAsAString(
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

  //Methods which will be used by future implementations

  private AudioBridgeResponse editAudioBridgeRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeEditRequest.create()
            .request(AudioBridgeEditRequest.EDIT)
            .room(videoServerMeeting.getAudioRoomId()) //TODO remove or update me
            .newDescription("new description")
        ),
        null
      );
      //TODO check audiobridge = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse existsAudioBridgeRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeExistsRequest.create()
            .request(AudioBridgeExistsRequest.EXISTS)
            .room(videoServerMeeting.getAudioRoomId())
        ),
        null
      );
      //TODO check exists = true in response
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse changeACLAudioBridgeRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeAllowedRequest.create()
            .request(AudioBridgeAllowedRequest.ALLOWED)
            .action(AudioBridgeAllowedRequest.ADD) //TODO update me
            .room(videoServerMeeting.getAudioRoomId())
            .allowed(List.of()) //TODO update me
        ),
        null
      );
      //TODO check audiobridge = success
      //TODO info: these ACL can be used as token in join request
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse enableAudioBridgeRoomRecording(String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeEnableRecordingRequest.create()
            .request(AudioBridgeEnableRecordingRequest.ENABLE_RECORDING)
            .room(videoServerMeeting.getAudioRoomId())
            .record(enabled)
            .recordFile("file") //TODO update me
            .recordDir("dir") //TODO update me
        ),
        null
      );
      //TODO check audiobridge = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse enableMjrsAudioBridgeRoomRecording(String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeEnableMjrsRequest.create()
            .request(AudioBridgeEnableMjrsRequest.ENABLE_MJRS)
            .room(videoServerMeeting.getAudioRoomId())
            .mjrs(enabled)
            .mjrsDir("dir")
        ),
        null
      );
      //TODO check audiobridge = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse listAudioBridgeRooms() {
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection while listing audio bridge rooms");
    }
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the audiobridge plugin for the connection " + connectionId
          + " while listing rooms");
    }
    try {
      return sendAudioBridgePluginMessage(
        connectionId,
        videoServerResponse.getDataId(),
        writeValueAsAString(
          AudioBridgeListRequest.create()
            .request(AudioBridgeListRequest.LIST)
        ),
        null
      );
      //TODO check audiobridge = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse listParticipantsAudioBridgeRoom(String audioRoomId) {
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection while listing audio bridge rooms participants");
    }
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the audiobridge plugin for the connection " + connectionId
          + " while listing rooms participants");
    }
    try {
      return sendAudioBridgePluginMessage(
        connectionId,
        videoServerResponse.getDataId(),
        writeValueAsAString(
          AudioBridgeListParticipantsRequest.create()
            .request(AudioBridgeListParticipantsRequest.LIST_PARTICIPANTS)
            .room(audioRoomId)
        ),
        null
      );
      //TODO check audiobridge = participants
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private AudioBridgeResponse playFileAudioBridgeRoom(String meetingId, boolean played) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      AudioBridgePlayFileRequest audioBridgePlayFileRequest = AudioBridgePlayFileRequest.create();
      if (played) {
        audioBridgePlayFileRequest
          .request(AudioBridgePlayFileRequest.PLAY_FILE)
          .room(videoServerMeeting.getAudioRoomId())
          .fileId("file id") //TODO update me
          .filename("file name") //TODO update me
          .loop(true);
      } else {
        audioBridgePlayFileRequest
          .request(AudioBridgePlayFileRequest.STOP_FILE)
          .room(videoServerMeeting.getAudioRoomId())
          .fileId("file id"); //TODO update me
      }
      return sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          audioBridgePlayFileRequest
        ),
        null
      );
      //TODO check audiobridge = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private void kickParticipantFromAudioBridgeRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeKickRequest.create()
            .request(AudioBridgeKickRequest.KICK)
            .room(videoServerMeeting.getAudioRoomId())
            .id(videoServerSession.getConnectionId())
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + videoServerSession.getConnectionId()
          + " is being kicked from the audio room");
    }
    videoServerSessionRepository.update(
      videoServerSession
        .audioStreamOn(false)
        .audioHandleId(null)
    );
    //TODO nullable audiohandle id
  }

  private void kickAllParticipantsFromAudioBridgeRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeKickRequest.create()
            .request(AudioBridgeKickRequest.KICK_ALL)
            .room(videoServerMeeting.getAudioRoomId())
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while kicking all sessions from audio room in the videoserver meeting " + meetingId);
    }
    videoServerMeeting.getVideoServerSessions().forEach(videoServerSession -> {
      videoServerSessionRepository.update(
        videoServerSession
          .audioStreamOn(false)
          .audioHandleId(null)
      );
    });
    //TODO nullable audiohandle id
  }

  private void configureAudioBridgeRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeConfigureRequest.create()
            .request(AudioBridgeConfigureRequest.CONFIGURE)
            .muted(false) //TODO update me
            .record(true) //TODO update me
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.SUCCESS.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + videoServerSession.getConnectionId()
          + " is being kicked from the audio room");
    }
  }

  private void changeAudioBridgeRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    AudioBridgeResponse audioBridgeResponse;
    try {
      audioBridgeResponse = sendAudioBridgePluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        writeValueAsAString(
          AudioBridgeKickRequest.create()
            .request(AudioBridgeChangeRoomRequest.CHANGE_ROOM)
            .room(videoServerMeeting.getAudioRoomId()) //TODO update me
            .id(videoServerSession.getConnectionId())
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getAudioBridge())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + videoServerSession.getConnectionId()
          + " is being kicked from the audio room");
    }
    videoServerMeetingRepository.insert(
      VideoServerMeeting.create()
        .meetingId(meetingId)
        .connectionId(videoServerMeeting.getConnectionId())
        .audioHandleId(videoServerMeeting.getVideoHandleId())
        .videoHandleId(videoServerMeeting.getVideoHandleId())
        .audioRoomId(videoServerMeeting.getVideoRoomId()) //TODO update me
    );
    //TODO id meeting id + video room id + audio room id
    //TODO meeting id + video room id + audio room id exposed to clients
  }

  private VideoRoomResponse editVideoRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomEditRequest.create()
            .request(VideoRoomEditRequest.EDIT)
            .room(videoServerMeeting.getVideoRoomId()) //TODO remove or update me
            .newDescription("new description")
        ),
        null
      );
      //TODO check videoroom = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse existsVideoRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomExistsRequest.create()
            .request(VideoRoomExistsRequest.EXISTS)
            .room(videoServerMeeting.getVideoRoomId())
        ),
        null
      );
      //TODO check exists = true in response
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse changeACLVideoRoom(String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomAllowedRequest.create()
            .request(VideoRoomAllowedRequest.ALLOWED)
            .action(VideoRoomAllowedRequest.ADD) //TODO update me
            .room(videoServerMeeting.getVideoRoomId())
            .allowed(List.of()) //TODO update me
        ),
        null
      );
      //TODO check audiobridge = success
      //TODO info: these ACL can be used as token in join request
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private void kickParticipantFromVideoRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomKickRequest.create()
            .request(VideoRoomKickRequest.KICK)
            .room(videoServerMeeting.getVideoRoomId())
            .id(videoServerSession.getConnectionId())
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!AudioBridgeResponse.SUCCESS.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + videoServerSession.getConnectionId()
          + " is being kicked from the video room");
    }
    videoServerSessionRepository.update(
      videoServerSession
        .videoOutStreamOn(false)
        .videoOutHandleId(null)
    );
    //TODO nullable videohandle id
  }

  private VideoRoomResponse listVideoRooms() {
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection while listing video rooms");
    }
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId
          + " while listing rooms");
    }
    try {
      return sendVideoRoomPluginMessage(
        connectionId,
        videoServerResponse.getDataId(),
        writeValueAsAString(
          VideoRoomListRequest.create()
            .request(VideoRoomListRequest.LIST)
        ),
        null
      );
      //TODO check videoroom = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse listParticipantsVideoRoom(String videoRoomId) {
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection while listing video rooms participants");
    }
    String connectionId = videoServerResponse.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId
          + " while listing rooms participants");
    }
    try {
      return sendVideoRoomPluginMessage(
        connectionId,
        videoServerResponse.getDataId(),
        writeValueAsAString(
          VideoRoomListParticipantsRequest.create()
            .request(VideoRoomListParticipantsRequest.LIST_PARTICIPANTS)
            .room(videoRoomId)
        ),
        null
      );
      //TODO check videoroom = participants
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse moderateVideoRoom(String sessionId, String meetingId, String mediaId, boolean enabled) {
    //TODO unmute a stream does not unmute it only removes the block and user can unmute itself
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomModerateRequest.create()
            .request(VideoRoomModerateRequest.MODERATE)
            .room(videoServerMeeting.getVideoRoomId())
            .id(videoServerSession.getConnectionId())
            .mid(mediaId)
            .mute(enabled)
        ),
        null
      );
      //TODO check videoroom = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse publishStreamOnVideoRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomPublishRequest.create()
            .request(VideoRoomPublishRequest.PUBLISH)
            .display(videoServerSession.getSessionId()) //TODO update me
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse unpublishStreamOnVideoRoom(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomUnpublishRequest.create()
            .request(VideoRoomUnpublishRequest.UNPUBLISH)
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse configureVideoRoomAsPublisher(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomConfigurePublisherRequest.create()
            .request(VideoRoomConfigurePublisherRequest.CONFIGURE)
            .record(true) //TODO
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse enableVideoRoomRecording(String meetingId, boolean enabled) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerMeeting.getConnectionId(),
        videoServerMeeting.getVideoHandleId(),
        writeValueAsAString(
          VideoRoomEnableRecordingRequest.create()
            .request(VideoRoomEnableRecordingRequest.ENABLE_RECORDING)
            .room(videoServerMeeting.getVideoRoomId())
            .record(enabled)
        ),
        null
      );
      //TODO check videoroom = success
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse joinVideoRoomAsSubscriber(String connectionId, String sessionId, String videoInHandleId,
    String videoRoomId) {
    VideoRoomResponse videoRoomResponse;
    try {
      videoRoomResponse = sendVideoRoomPluginMessage(
        connectionId,
        videoInHandleId,
        writeValueAsAString(
          VideoRoomJoinSubscriberRequest.create()
            .request(VideoRoomJoinSubscriberRequest.JOIN)
            .ptype(VideoRoomJoinSubscriberRequest.SUBSCRIBER)
            .room(videoRoomId)
            .useMsid(true)
            .autoupdate(true)
            .streams(List.of())
        ),
        null
      );
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getVideoRoom())) {
      throw new VideoServerException(
        "An error occured while session " + sessionId + " with connection id " + connectionId
          + " is joining the video room as subscriber");
    }
    return videoRoomResponse;
  }

  private VideoRoomResponse pauseVideoIn(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomPauseVideoInRequest.create()
            .request(VideoRoomPauseVideoInRequest.PAUSE)
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse configureVideoRoomAsSubscriber(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomConfigureSubscriberRequest.create()
            .request(VideoRoomConfigureSubscriberRequest.CONFIGURE)
            .streams(List.of())
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }

  private VideoRoomResponse switchStreams(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    try {
      return sendVideoRoomPluginMessage(
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        writeValueAsAString(
          VideoRoomSwitchRequest.create()
            .request(VideoRoomSwitchRequest.SWITCH)
            .streams(List.of())
        ),
        null
      );
      //TODO check videoroom = ack
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    }
  }
}
