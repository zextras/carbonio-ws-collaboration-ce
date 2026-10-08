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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.*;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.*;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.model.MediaStreamDto;
import com.zextras.carbonio.chats.model.MediaStreamSettingsDto;
import com.zextras.carbonio.chats.model.SubscriptionUpdatesDto;
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

  private static final String MEETING_AUDIO_OPAQUE_ID_PATTERN = "meeting/a/%s";
  private static final String MEETING_VIDEO_OPAQUE_ID_PATTERN = "meeting/v/%s";
  private static final String USER_AUDIO_OPAQUE_ID_PATTERN = "a/%s/%s";
  private static final String USER_VIDEO_OUT_OPAQUE_ID_PATTERN = "vo/%s/%s";
  private static final String USER_VIDEO_IN_OPAQUE_ID_PATTERN = "vi/%s/%s";
  private static final String USER_SCREEN_OPAQUE_ID_PATTERN = "s/%s/%s";

  private final VideoServerClient videoServerClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final Clock clock;

  private final VideoServerConfig videoServerConfig;

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
    this.videoServerConfig = videoServerConfig;
  }

  @Override
  public void startMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      ChatsLogger.debug("Videoserver meeting " + meetingId + " is already active");
      return;
    }

    VideoServerClient client = clientForNewMeeting(meetingId);

    VideoServerResponse connectionResponse = createMeetingConnection(client);
    String connectionId = connectionResponse.getDataId();

    VideoServerResponse audioPluginResponse =
        attachToPlugin(
            client,
            connectionId,
            JANUS_AUDIOBRIDGE_PLUGIN,
            String.format(MEETING_AUDIO_OPAQUE_ID_PATTERN, meetingId));
    VideoServerResponse videoPluginResponse =
        attachToPlugin(
            client,
            connectionId,
            JANUS_VIDEOROOM_PLUGIN,
            String.format(MEETING_VIDEO_OPAQUE_ID_PATTERN, meetingId));

    String audioHandleId = audioPluginResponse.getDataId();
    String videoHandleId = videoPluginResponse.getDataId();

    AudioBridgeResponse audioRoomResponse =
        createAudioBridgeRoom(client, meetingId, connectionId, audioHandleId);
    VideoRoomResponse videoRoomResponse =
        createVideoRoom(client, meetingId, connectionId, videoHandleId);

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
    afterMeetingStarted(meetingId, client);
  }

  protected VideoServerClient clientForNewMeeting(String meetingId) {
    return videoServerClient;
  }

  protected VideoServerClient clientFor(VideoServerMeeting videoServerMeeting) {
    return videoServerClient;
  }

  protected void afterMeetingStarted(String meetingId, VideoServerClient client) {}

  private VideoServerResponse createMeetingConnection(VideoServerClient client) {
    VideoServerResponse response = createConnection(client);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error creating video server connection");
    }
    return response;
  }

  private VideoServerResponse attachToPlugin(
      VideoServerClient client, String connectionId, String pluginType, String opaqueId) {
    VideoServerResponse response =
        interactWithConnection(client, connectionId, JANUS_ATTACH, pluginType, opaqueId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      throw new VideoServerException("Error attaching to plugin " + pluginType);
    }
    return response;
  }

  private AudioBridgeResponse createAudioBridgeRoom(
      VideoServerClient client, String meetingId, String connectionId, String audioHandleId) {
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
        sendAudioBridgePluginMessage(client, connectionId, audioHandleId, audioRequest, null);
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
      VideoServerClient client, String meetingId, String connectionId, String videoHandleId) {
    VideoRoomCreateRequest videoRequest =
        VideoRoomCreateRequest.create()
            .request(VideoRoomCreateRequest.CREATE)
            .room(VideoRoomCreateRequest.ROOM_DEFAULT + meetingId)
            .permanent(false)
            .description(VideoRoomCreateRequest.DESCRIPTION_DEFAULT + meetingId)
            .isPrivate(false)
            .record(false)
            .videoorientExt(false)
            .publishers(VideoRoomCreateRequest.MAX_PUBLISHERS_DEFAULT)
            .bitrate(videoServerConfig.getBitrate())
            .bitrateCap(videoServerConfig.getBitrateCap())
            .videoCodec(
                Arrays.stream(VideoCodec.values())
                    .map(videoCodec -> videoCodec.toString().toLowerCase())
                    .collect(Collectors.joining(",")));

    VideoRoomResponse response =
        sendVideoRoomPluginMessage(client, connectionId, videoHandleId, videoRequest, null);
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
              VideoServerClient client = clientFor(videoServerMeeting);

              destroyAudioBridgeRoom(
                  client,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  videoServerMeeting.getAudioRoomId());
              destroyVideoRoom(
                  client,
                  meetingId,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  videoServerMeeting.getVideoRoomId());

              destroyPluginHandle(
                  client,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getAudioHandleId(),
                  meetingId);
              destroyPluginHandle(
                  client,
                  videoServerMeeting.getConnectionId(),
                  videoServerMeeting.getVideoHandleId(),
                  meetingId);

              destroyConnection(client, videoServerMeeting.getConnectionId(), meetingId);
              videoServerMeetingRepository.deleteById(meetingId);
            });
  }

  private void destroyPluginHandle(
      VideoServerClient client, String connectionId, String handleId, String meetingId) {
    VideoServerResponse response = destroyPluginHandle(client, connectionId, handleId);
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

  private void destroyConnection(VideoServerClient client, String connectionId, String meetingId) {
    VideoServerResponse response = destroyConnection(client, connectionId);
    if (!JANUS_SUCCESS.equals(response.getStatus())) {
      ChatsLogger.debug(
          "An error occurred when destroying the video server connection "
              + connectionId
              + " for the meeting "
              + meetingId);
    }
  }

  private void destroyVideoRoom(
      VideoServerClient client,
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
        sendVideoRoomPluginMessage(client, connectionId, videoHandleId, destroyRequest, null);
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
      VideoServerClient client,
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
        sendAudioBridgePluginMessage(client, connectionId, audioHandleId, destroyRequest, null);
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

    VideoServerClient client = clientFor(videoServerMeeting);

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

    String connectionId = createConnection(client).getDataId();

    String audioHandleId =
        attachToPlugin(
                client,
                connectionId,
                JANUS_AUDIOBRIDGE_PLUGIN,
                String.format(USER_AUDIO_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String videoOutHandleId =
        attachToPlugin(
                client,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_VIDEO_OUT_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String videoInHandleId =
        attachToPlugin(
                client,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_VIDEO_IN_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();
    String screenHandleId =
        attachToPlugin(
                client,
                connectionId,
                JANUS_VIDEOROOM_PLUGIN,
                String.format(USER_SCREEN_OPAQUE_ID_PATTERN, userId, meetingId))
            .getDataId();

    joinVideoRoomAsPublisher(
        client,
        connectionId,
        userId,
        videoOutHandleId,
        videoServerMeeting.getVideoRoomId(),
        MediaType.VIDEO);

    joinVideoRoomAsPublisher(
        client,
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
      VideoServerClient client,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      MediaType mediaType) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
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
              VideoServerClient client = clientFor(videoServerMeeting);
              videoServerMeeting.getVideoServerSessions().stream()
                  .filter(sessionUser -> sessionUser.getUserId().equals(userId))
                  .findFirst()
                  .ifPresent(
                      videoServerSession -> {
                        destroyParticipantSession(client, meetingId, videoServerSession);
                        videoServerSessionRepository.remove(videoServerSession);
                      });
            });
  }

  private void destroyParticipantSession(
      VideoServerClient client, String meetingId, VideoServerSession videoServerSession) {
    destroyPluginHandle(
        client,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        meetingId);
    destroyPluginHandle(
        client,
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoOutHandleId(),
        meetingId);
    destroyPluginHandle(
        client,
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoInHandleId(),
        meetingId);
    destroyPluginHandle(
        client,
        videoServerSession.getConnectionId(),
        videoServerSession.getScreenHandleId(),
        meetingId);

    destroyConnection(client, videoServerSession.getConnectionId(), meetingId);
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
    VideoServerClient client = clientFor(videoServerMeeting);

    try {
      switch (mediaStreamSettingsDto.getType()) {
        case VIDEO ->
            updateVideoStream(
                client,
                userId,
                meetingId,
                videoServerSession,
                mediaStreamSettingsDto.isEnabled(),
                mediaStreamSettingsDto.getSdp());
        case SCREEN ->
            updateScreenStream(
                client,
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
      VideoServerClient client,
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
          client,
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getVideoOutHandleId(),
          sdp,
          MediaType.VIDEO.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.videoOutStreamOn(enabled));
  }

  private void updateScreenStream(
      VideoServerClient client,
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
          client,
          userId,
          videoServerSession.getConnectionId(),
          videoServerSession.getScreenHandleId(),
          sdp,
          MediaType.SCREEN.toString().toLowerCase());
    }

    videoServerSessionRepository.update(videoServerSession.screenStreamOn(enabled));
  }

  private void publishStreamOnVideoRoom(
      VideoServerClient client,
      String userId,
      String connectionId,
      String handleId,
      String sdp,
      String mediaType) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
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
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));

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
        clientFor(videoServerMeeting),
        videoServerMeeting.getConnectionId(),
        videoServerSession.getConnectionId(),
        userId,
        videoServerMeeting.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        enabled);

    videoServerSessionRepository.update(videoServerSession.audioStreamOn(enabled));
  }

  private void muteAudioStream(
      VideoServerClient client,
      String meetingConnectionId,
      String connectionId,
      String userId,
      String meetingAudioHandleId,
      String audioRoomId,
      boolean enabled) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
            client,
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
        clientFor(videoServerMeeting),
        videoServerSession.getConnectionId(),
        videoServerSession.getVideoInHandleId(),
        sdp);
  }

  private void startVideoIn(
      VideoServerClient client, String connectionId, String videoInHandleId, String sdp) {
    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
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
    VideoServerClient client = clientFor(videoServerMeeting);

    if (!videoServerSession.hasVideoInStreamOn()) {
      joinVideoRoomAsSubscriber(
          client,
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          videoServerMeeting.getVideoRoomId(),
          subscriptionUpdatesDto.getSubscribe());
      videoServerSessionRepository.update(videoServerSession.videoInStreamOn(true));
    } else {
      updateSubscriptions(
          client,
          videoServerSession.getConnectionId(),
          userId,
          videoServerSession.getVideoInHandleId(),
          subscriptionUpdatesDto);
    }
  }

  private void joinVideoRoomAsSubscriber(
      VideoServerClient client,
      String connectionId,
      String userId,
      String videoHandleId,
      String videoRoomId,
      List<MediaStreamDto> mediaStreamDtos) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
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
      VideoServerClient client,
      String connectionId,
      String userId,
      String videoInHandleId,
      SubscriptionUpdatesDto subscriptionUpdatesDto) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
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
        clientFor(videoServerMeeting),
        userId,
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        videoServerMeeting.getAudioRoomId(),
        sdp);
  }

  private void joinAudioBridgeRoom(
      VideoServerClient client,
      String userId,
      String connectionId,
      String audioHandleId,
      String audioRoomId,
      String sdp) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
            client,
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
  public void iceRestartAudio(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    iceRestartAudioWithSdp(
        clientFor(videoServerMeeting),
        videoServerSession.getConnectionId(),
        videoServerSession.getAudioHandleId(),
        sdp);
  }

  private void iceRestartAudioWithSdp(
      VideoServerClient client, String connectionId, String audioHandleId, String sdp) {

    AudioBridgeResponse audioBridgeResponse =
        sendAudioBridgePluginMessage(
            client,
            connectionId,
            audioHandleId,
            AudioBridgeConfigureRequest.create().request(AudioBridgeConfigureRequest.CONFIGURE),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));

    if (!AudioBridgeResponse.ACK.equals(audioBridgeResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user with connection id "
              + connectionId
              + " is triggering audio ice restart");
    }
  }

  @Override
  public void iceRestartVideo(String userId, String meetingId, @Nullable String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);
    VideoServerClient client = clientFor(videoServerMeeting);

    if (sdp == null) {
      iceRestartVideoIn(
          client, videoServerSession.getConnectionId(), videoServerSession.getVideoInHandleId());
    } else {
      iceRestartVideoWithSdp(
          client,
          videoServerSession.getConnectionId(),
          videoServerSession.getVideoOutHandleId(),
          sdp);
    }
  }

  private void iceRestartVideoIn(
      VideoServerClient client, String connectionId, String videoInHandleId) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
            connectionId,
            videoInHandleId,
            VideoRoomConfigureRequest.create()
                .request(VideoRoomConfigureRequest.CONFIGURE)
                .restart(true),
            null);

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user with connection id "
              + connectionId
              + " is triggering video in ice restart");
    }
  }

  private void iceRestartVideoWithSdp(
      VideoServerClient client, String connectionId, String videoOutHandleId, String sdp) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
            connectionId,
            videoOutHandleId,
            VideoRoomConfigureRequest.create().request(VideoRoomConfigureRequest.CONFIGURE),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user with connection id "
              + connectionId
              + " is triggering video out ice restart");
    }
  }

  @Override
  public void iceRestartScreen(String userId, String meetingId, String sdp) {
    VideoServerMeeting videoServerMeeting = getVideoServerMeeting(meetingId);
    VideoServerSession videoServerSession = getVideoServerSession(userId, videoServerMeeting);

    iceRestartScreenWithSdp(
        clientFor(videoServerMeeting),
        videoServerSession.getConnectionId(),
        videoServerSession.getScreenHandleId(),
        sdp);
  }

  private void iceRestartScreenWithSdp(
      VideoServerClient client, String connectionId, String screenHandleId, String sdp) {

    VideoRoomResponse videoRoomResponse =
        sendVideoRoomPluginMessage(
            client,
            connectionId,
            screenHandleId,
            VideoRoomConfigureRequest.create().request(VideoRoomConfigureRequest.CONFIGURE),
            RtcSessionDescription.create().type(RtcType.OFFER).sdp(sdp));

    if (!VideoRoomResponse.ACK.equals(videoRoomResponse.getStatus())) {
      throw new VideoServerException(
          "An error occurred while user with connection id "
              + connectionId
              + " is triggering screen ice restart");
    }
  }

  protected VideoServerMeeting getVideoServerMeeting(String meetingId) {
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
   * @see <a href= "https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
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
   * @param client the video server client the request is sent with
   * @return VideoServerResponse
   */
  private VideoServerResponse createConnection(VideoServerClient client) {
    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(JANUS_CREATE)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(videoServerConfig.getApiSecret());
    return client.sendVideoServerRequest(request);
  }

  /**
   * This method destroys a specified connection on the VideoServer.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyConnection(VideoServerClient client, String connectionId) {
    return interactWithConnection(client, connectionId, JANUS_DESTROY, null, null);
  }

  /**
   * This method allows interaction with a connection on the VideoServer.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action the action to perform on this 'connection' (session)
   * @param opaqueId the user id or meeting id associated to this handle-session on the VideoServer
   * @param pluginName the plugin name to perform the action with (optional)
   * @return VideoServerResponse
   */
  private VideoServerResponse interactWithConnection(
      VideoServerClient client,
      String connectionId,
      String action,
      @Nullable String pluginName,
      @Nullable String opaqueId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(action)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(videoServerConfig.getApiSecret());
    Optional.ofNullable(pluginName).ifPresent(request::pluginName);
    Optional.ofNullable(opaqueId).ifPresent(request::opaqueId);

    return client.sendConnectionVideoServerRequest(connectionId, request);
  }

  /**
   * This method destroys the previously attached plugin handle.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id
   * @param handleId the plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse destroyPluginHandle(
      VideoServerClient client, String connectionId, String handleId) {
    return sendDetachPluginMessage(client, connectionId, handleId);
  }

  /**
   * This method detaches the audio bridge plugin handle.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id
   * @param handleId the previously attached plugin handle id
   * @return VideoServerResponse
   */
  private VideoServerResponse sendDetachPluginMessage(
      VideoServerClient client, String connectionId, String handleId) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_DETACH)
            .transactionId(UUID.randomUUID().toString())
            .apiSecret(videoServerConfig.getApiSecret());

    return client.sendHandleVideoServerRequest(connectionId, handleId, request);
  }

  /**
   * This method sends a message to an audio bridge plugin.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id
   * @param handleId the audio bridge plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return AudioBridgeResponse
   */
  protected AudioBridgeResponse sendAudioBridgePluginMessage(
      VideoServerClient client,
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(videoServerConfig.getApiSecret());
    Optional.ofNullable(rtcSessionDescription).ifPresent(request::rtcSessionDescription);

    return client.sendAudioBridgeRequest(connectionId, handleId, request);
  }

  /**
   * This method sends a message to a video room plugin.
   *
   * @param client the video server client the request is sent with
   * @param connectionId the 'connection' (session) id
   * @param handleId the video room plugin handle id
   * @param videoServerPluginRequest the plugin request body
   * @param rtcSessionDescription the WebRTC negotiation session description (optional)
   * @return VideoRoomResponse
   */
  protected VideoRoomResponse sendVideoRoomPluginMessage(
      VideoServerClient client,
      String connectionId,
      String handleId,
      VideoServerPluginRequest videoServerPluginRequest,
      @Nullable RtcSessionDescription rtcSessionDescription) {

    VideoServerMessageRequest request =
        VideoServerMessageRequest.create()
            .messageRequest(VideoServerServiceImpl.JANUS_MESSAGE)
            .transactionId(UUID.randomUUID().toString())
            .videoServerPluginRequest(videoServerPluginRequest)
            .apiSecret(videoServerConfig.getApiSecret());
    Optional.ofNullable(rtcSessionDescription).ifPresent(request::rtcSessionDescription);

    return client.sendVideoRoomRequest(connectionId, handleId, request);
  }
}
