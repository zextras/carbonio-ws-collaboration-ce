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
  public void startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      ChatsLogger.debug("Videoserver meeting " + meetingId + " is already active");
      return;
    }

    VideoServerResponse connectionResponse = createMeetingConnection();
    String connectionId = connectionResponse.getDataId();

    VideoServerResponse audioPluginResponse =
        attachToPlugin(connectionId, JANUS_AUDIOBRIDGE_PLUGIN);
    VideoServerResponse videoPluginResponse = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN);

    String audioHandleId = audioPluginResponse.getDataId();
    String videoHandleId = videoPluginResponse.getDataId();

    AudioBridgeResponse audioRoomResponse =
        createAudioBridgeRoom(meetingId, connectionId, audioHandleId);
    VideoRoomResponse videoRoomResponse = createVideoRoom(meetingId, connectionId, videoHandleId);

    String audioRoomId = audioRoomResponse.getRoom();
    String videoRoomId = videoRoomResponse.getRoom();

    videoServerMeetingRepository.insert(
        VideoServerMeeting.create()
            .meetingId(meetingId)
            .connectionId(connectionId)
            .audioHandleId(audioHandleId)
            .videoHandleId(videoHandleId)
            .audioRoomId(audioRoomId)
            .videoRoomId(videoRoomId));
  }

  private VideoServerResponse createMeetingConnection() {
    VideoServerResponse response = createConnection();
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error creating video server connection");
    }
    return response;
  }

  private VideoServerResponse attachToPlugin(String connectionId, String pluginType) {
    VideoServerResponse response = interactWithConnection(connectionId, JANUS_ATTACH, pluginType);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error attaching to plugin " + pluginType);
    }
    return response;
  }

  private AudioBridgeResponse createAudioBridgeRoom(
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

    AudioBridgeResponse response =
        sendAudioBridgePluginMessage(connectionId, audioHandleId, audioRequest, null);
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

    VideoRoomResponse response =
        sendVideoRoomPluginMessage(connectionId, videoHandleId, videoRequest, null);
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
              destroyAudioBridgeRoom(
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  videoServerMeeting.getAudioRoomId());
              destroyVideoRoom(
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  videoServerMeeting.getVideoRoomId());

              destroyPluginHandle(
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  meetingId);
              destroyPluginHandle(
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  meetingId);

              destroyConnection(videoServerMeeting.getConnectionId(), meetingId);
              videoServerMeetingRepository.deleteById(meetingId);
            });
  }

  private void destroyPluginHandle(String connectionId, String handleId, String meetingId) {
    VideoServerResponse response = destroyPluginHandle(connectionId, handleId);
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

  private void destroyConnection(String connectionId, String meetingId) {
    VideoServerResponse response = destroyConnection(connectionId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      ChatsLogger.debug(
          "An error occurred when destroying the video server connection "
              + connectionId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyVideoRoom(
      String meetingId, String connectionId, String videoHandleId, String videoRoomId) {
    VideoRoomDestroyRequest destroyRequest =
        VideoRoomDestroyRequest.create()
            .request(VideoRoomDestroyRequest.DESTROY)
            .room(videoRoomId)
            .permanent(false);

    VideoRoomResponse response =
        sendVideoRoomPluginMessage(connectionId, videoHandleId, destroyRequest, null);
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
      String meetingId, String connectionId, String audioHandleId, String audioRoomId) {
    AudioBridgeDestroyRequest destroyRequest =
        AudioBridgeDestroyRequest.create()
            .request(AudioBridgeDestroyRequest.DESTROY)
            .room(audioRoomId)
            .permanent(false);

    AudioBridgeResponse response =
        sendAudioBridgePluginMessage(connectionId, audioHandleId, destroyRequest, null);
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

    String connectionId = createConnection().getDataId();

    String audioHandleId = attachToPlugin(connectionId, JANUS_AUDIOBRIDGE_PLUGIN).getDataId();
    String videoOutHandleId = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN).getDataId();
    String videoInHandleId = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN).getDataId();
    String screenHandleId = attachToPlugin(connectionId, JANUS_VIDEOROOM_PLUGIN).getDataId();

    joinVideoRoomAsPublisher(
        connectionId,
        userId,
        videoOutHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.VIDEO);

    joinVideoRoomAsPublisher(
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
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      MediaType mediaType) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
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
        .flatMap(
            videoServerMeeting ->
                videoServerMeeting.getVideoServerSessions().stream()
                    .filter(sessionUser -> sessionUser.getUserId().equals(userId))
                    .findFirst())
        .ifPresent(
            videoServerSession -> {
              destroyParticipantSession(meetingId, videoServerSession);
              videoServerSessionRepository.remove(videoServerSession);
            });
  }

  private void destroyParticipantSession(String meetingId, VideoServerSession videoServerSession) {
    destroyPluginHandle(
        videoServerSession.getConnectionId(), videoServerSession.getAudioHandleId(), meetingId);
    destroyPluginHandle(
        videoServerSession.getConnectionId(), videoServerSession.getVideoOutHandleId(), meetingId);
    destroyPluginHandle(
        videoServerSession.getConnectionId(), videoServerSession.getVideoInHandleId(), meetingId);
    destroyPluginHandle(
        videoServerSession.getConnectionId(), videoServerSession.getScreenHandleId(), meetingId);

    destroyConnection(videoServerSession.getConnectionId(), meetingId);
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
  public void updateMediaStream(
      String userId, String meetingId, MediaStreamSettingsDto mediaStreamSettingsDto) {

    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    try {
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
      }
    } catch (Exception ex) {
      throw new VideoServerException(
          "Failed to update media stream for user " + userId + " in meeting " + meetingId, ex);
    }
  }

  private void updateVideoStream(
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
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getVideoOutHandleId(),
          sdp,
          MediaType.VIDEO.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled));
  }

  private void updateScreenStream(
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
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getScreenHandleId(),
          sdp,
          MediaType.SCREEN.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled));
  }

  private void publishStreamOnVideoRoom(
      String userId,
      String connectionId,
      String handleId,
      String sessionDescriptionProtocol,
      String mediaType) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
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
        videoServerMeeting.getConnectionId(),
        videoServerSession.getConnectionId(),
        userId,
        videoServerMeeting.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        enabled);

    videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled));
  }

  private void muteAudioStream(
      String meetingConnectionId,
      String connectionId,
      String userId,
      String meetingAudioHandleId,
      String audioRoomId,
      boolean enabled) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
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

    startVideoIn(
        videoServerSession.getConnectionId(), videoServerSession.getVideoInHandleId(), sdp);
  }

  private void startVideoIn(String connectionId, String videoInHandleId, String sdp) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
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

    if (!videoServerSession.hasVideoInStreamOn()) {
      joinVideoRoomAsSubscriber(
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          videoServerMeeting.getVideoRoomId(),
          subscriptionUpdatesDto.getSubscribe());
      videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
    } else {
      updateSubscriptions(
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          subscriptionUpdatesDto);
    }
  }

  private void joinVideoRoomAsSubscriber(
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      List<MediaStreamDto> mediaStreamDtos) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
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
      String connectionId,
      String userId,
      String videoInHandleId,
      SubscriptionUpdatesDto subscriptionUpdatesDto) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
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

    joinAudioBridgeRoom(
        userId,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        sdp);
  }

  private void joinAudioBridgeRoom(
      String userId, String connectionId, String audioHandleId, String audioRoomId, String sdp) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
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
   * @return VideoServerResponse
   */
  private VideoServerResponse createConnection() {
    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);
    return videoServerClient.sendVideoServerRequest(request);
  }

  /**
   * This method destroys a specified connection on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyConnection(String connectionId) {
    return interactWithConnection(connectionId, JANUS_DESTROY, null);
  }

  /**
   * This method allows interaction with a connection on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action the action to perform on this 'connection' (session)
   * @param pluginName the plugin name to perform the action with (optional)
   * @return VideoServerResponse
   */
  private VideoServerResponse interactWithConnection(
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
   * This method destroys the previously attached plugin handle.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendDetachPluginMessage(connectionId, handleId);
  }

  /**
   * This method detaches the audio bridge plugin handle.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse sendDetachPluginMessage(String connectionId, String handleId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(apiSecret);

    return videoServerClient.sendHandleVideoServerRequest(connectionId, handleId, request);
  }

  /**
   * This method sends a message to an audio bridge plugin.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the audio bridge plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return AudioBridgeResponse
   */
  private AudioBridgeResponse sendAudioBridgePluginMessage(
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
   * This method sends a message to a video room plugin.
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId the video room plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return VideoRoomResponse
   */
  private VideoRoomResponse sendVideoRoomPluginMessage(
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
