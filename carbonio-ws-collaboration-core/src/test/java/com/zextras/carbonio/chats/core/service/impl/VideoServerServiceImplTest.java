// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.consul.ConsulService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerDataInfo;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeDataInfo;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgePluginData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomDataInfo;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomPluginData;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceImpl;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.meeting.model.MediaStreamDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto.TypeEnum;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
public class VideoServerServiceImplTest {

  private final VideoServerClient videoServerClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final VideoServerService videoServerService;
  private final ConsulService consulServiceMock;
  private final Clock clock;

  public VideoServerServiceImplTest() {
    AppConfig appConfig = mock(AppConfig.class);
    this.videoServerClient = mock(VideoServerClient.class);
    this.videoServerMeetingRepository = mock(VideoServerMeetingRepository.class);
    this.videoServerSessionRepository = mock(VideoServerSessionRepository.class);
    this.consulServiceMock = mock(ConsulService.class);
    this.clock = mock(Clock.class);
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST))
        .thenReturn(Optional.of("127.78.0.4"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT))
        .thenReturn(Optional.of("20006"));
    when(appConfig.get(String.class, ConfigName.VIDEO_RECORDER_HOST))
        .thenReturn(Optional.of("127.78.0.4"));
    when(appConfig.get(String.class, ConfigName.VIDEO_RECORDER_PORT))
        .thenReturn(Optional.of("20007"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN))
        .thenReturn(Optional.of("token"));

    this.videoServerService =
        new VideoServerServiceImpl(
            appConfig,
            videoServerClient,
            videoServerMeetingRepository,
            videoServerSessionRepository,
            consulServiceMock,
            clock);
  }

  private UUID meeting1Id;
  private UUID meeting1SessionId;
  private UUID meeting1AudioHandleId;
  private UUID meeting1VideoHandleId;
  private UUID meeting1AudioRoomId;
  private UUID meeting1VideoRoomId;
  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID queue1Id;
  private UUID queue2Id;
  private UUID queue3Id;
  private UUID user1SessionId;
  private UUID user2SessionId;
  private UUID user3SessionId;
  private UUID user1AudioHandleId;
  private UUID user1VideoOutHandleId;
  private UUID user1VideoInHandleId;
  private UUID user2VideoInHandleId;
  private UUID user3VideoInHandleId;
  private UUID user1ScreenHandleId;
  private String recordingPath;
  private String videoServerURL;
  private String videoRecorderURL;
  private String janusEndpoint;
  private String janusInfoEndpoint;
  private String postProcessorEndpoint;

  @BeforeEach
  public void init() {
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T11:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.of("UTC+01:00"));
    meeting1Id = UUID.randomUUID();
    meeting1SessionId = UUID.randomUUID();
    meeting1AudioHandleId = UUID.randomUUID();
    meeting1VideoHandleId = UUID.randomUUID();
    meeting1AudioRoomId = UUID.randomUUID();
    meeting1VideoRoomId = UUID.randomUUID();
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    queue1Id = UUID.randomUUID();
    queue2Id = UUID.randomUUID();
    queue3Id = UUID.randomUUID();
    user1SessionId = UUID.randomUUID();
    user2SessionId = UUID.randomUUID();
    user3SessionId = UUID.randomUUID();
    user1AudioHandleId = UUID.randomUUID();
    user1VideoOutHandleId = UUID.randomUUID();
    user1VideoInHandleId = UUID.randomUUID();
    user2VideoInHandleId = UUID.randomUUID();
    user3VideoInHandleId = UUID.randomUUID();
    user1ScreenHandleId = UUID.randomUUID();
    videoServerURL = "http://127.78.0.4:20006";
    videoRecorderURL = "http://127.78.0.4:20007";
    janusEndpoint = "/janus";
    janusInfoEndpoint = "/info";
    postProcessorEndpoint = "/PostProcessor/meeting";
    recordingPath = "/var/lib/videoserver/recordings/meeting";
  }

  @AfterEach
  public void cleanup() {
    verifyNoMoreInteractions(videoServerClient);
    verifyNoMoreInteractions(videoServerMeetingRepository);
    verifyNoMoreInteractions(videoServerSessionRepository);
    reset(videoServerClient, videoServerMeetingRepository, videoServerSessionRepository);
  }

  private VideoServerMeeting createVideoServerMeeting(UUID serverId, UUID meetingId) {
    VideoServerMeeting videoServerMeeting =
        VideoServerMeeting.create()
            .serverId(serverId.toString())
            .meetingId(meetingId.toString())
            .connectionId(meeting1SessionId.toString())
            .audioHandleId(meeting1AudioHandleId.toString())
            .videoHandleId(meeting1VideoHandleId.toString())
            .audioRoomId(meeting1AudioRoomId.toString())
            .videoRoomId(meeting1VideoRoomId.toString());
    when(videoServerMeetingRepository.getById(meetingId.toString()))
        .thenReturn(Optional.of(videoServerMeeting));
    return videoServerMeeting;
  }

  private static String getServerIdQueryParam(UUID serverId) {
    return "?service_id=" + serverId;
  }

  @Nested
  @DisplayName("Start Meeting tests")
  class StartMeetingTests {

    @Test
    @DisplayName("Start a new meeting on a room")
    void startMeeting_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      when(consulServiceMock.getHealthyServices("carbonio-videoserver", "service_id"))
          .thenReturn(List.of(serverId));
      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(meeting1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm), any(VideoServerMessageRequest.class)))
          .thenReturn(sessionResponse);

      VideoServerResponse audioHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(meeting1AudioHandleId.toString()));

      VideoServerResponse videoHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(meeting1VideoHandleId.toString()));

      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioHandleResponse, videoHandleResponse);

      AudioBridgeResponse audioRoomResponse =
          AudioBridgeResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1AudioHandleId.toString())
              .pluginData(
                  AudioBridgePluginData.create()
                      .plugin("janus.plugin.audiobridge")
                      .dataInfo(
                          AudioBridgeDataInfo.create()
                              .audioBridge("created")
                              .room(meeting1AudioRoomId.toString())
                              .permanent(false)));
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioRoomResponse);

      VideoRoomResponse videoRoomResponse =
          VideoRoomResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1VideoHandleId.toString())
              .pluginData(
                  VideoRoomPluginData.create()
                      .plugin("janus.plugin.videoroom")
                      .dataInfo(
                          VideoRoomDataInfo.create()
                              .videoRoom("created")
                              .room(meeting1VideoRoomId.toString())
                              .permanent(false)));
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoRoomResponse);

      videoServerService.startMeeting(meeting1Id.toString());

      ArgumentCaptor<VideoServerMessageRequest> createConnectionRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> createHandleRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> createAudioRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> createVideoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm),
              createConnectionRequestCaptor.capture());
      verify(videoServerClient, times(2))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString() + serverQPm),
              createHandleRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              createAudioRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              createVideoRoomRequestCaptor.capture());
      verify(videoServerMeetingRepository, times(1))
          .insert(
              VideoServerMeeting.create()
                  .serverId(serverId.toString())
                  .meetingId(meeting1Id.toString())
                  .connectionId(meeting1SessionId.toString())
                  .audioHandleId(meeting1AudioHandleId.toString())
                  .videoHandleId(meeting1VideoHandleId.toString())
                  .audioRoomId(meeting1AudioRoomId.toString())
                  .videoRoomId(meeting1VideoRoomId.toString()));

      assertEquals(1, createConnectionRequestCaptor.getAllValues().size());
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("create").apiSecret("token"),
          createConnectionRequestCaptor.getValue());

      assertEquals(2, createHandleRequestCaptor.getAllValues().size());
      VideoServerMessageRequest createAudioHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.audiobridge")
              .apiSecret("token"),
          createAudioHandleMessageRequest);
      VideoServerMessageRequest createVideoHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(1);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          createVideoHandleMessageRequest);

      assertEquals(1, createAudioRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest createAudioRoomMessageRequest =
          createAudioRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  AudioBridgeCreateRequest.create()
                      .request("create")
                      .room("audio_" + meeting1Id)
                      .permanent(false)
                      .description("audio_room_" + meeting1Id)
                      .isPrivate(false)
                      .record(false)
                      .samplingRate(16000L)
                      .audioActivePackets(10L)
                      .audioLevelAverage(65)
                      .audioLevelEvent(true)),
          createAudioRoomMessageRequest);

      assertEquals(1, createVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest createVideoRoomMessageRequest =
          createVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomCreateRequest.create()
                      .request("create")
                      .room("video_" + meeting1Id)
                      .permanent(false)
                      .description("video_room_" + meeting1Id)
                      .isPrivate(false)
                      .record(false)
                      .publishers(100)
                      .bitrate(600L * 1024)
                      .bitrateCap(true)
                      .videoCodec("vp8,h264,vp9,h265,av1")),
          createVideoRoomMessageRequest);
    }

    @Test
    @DisplayName("Try to start a meeting that is already active")
    void startMeeting_testErrorAlreadyActive() {
      createVideoServerMeeting(UUID.randomUUID(), meeting1Id);
      String idMeeting = meeting1Id.toString();

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.startMeeting(idMeeting),
          "Videoserver meeting " + meeting1Id + " is already active");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Stop Meeting tests")
  class StopMeetingTests {

    @Test
    @DisplayName("Stop an existing meeting")
    void stopMeeting_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      VideoRoomResponse destroyVideoRoomResponse =
          VideoRoomResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1VideoHandleId.toString())
              .pluginData(
                  VideoRoomPluginData.create()
                      .plugin("janus.plugin.videoroom")
                      .dataInfo(VideoRoomDataInfo.create().videoRoom("destroyed")));
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoRoomResponse);

      VideoServerResponse destroyVideoRoomPluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoRoomPluginResponse);

      AudioBridgeResponse destroyAudioRoomResponse =
          AudioBridgeResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1AudioHandleId.toString())
              .pluginData(
                  AudioBridgePluginData.create()
                      .plugin("janus.plugin.audiobridge")
                      .dataInfo(AudioBridgeDataInfo.create().audioBridge("destroyed")));
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyAudioRoomResponse);

      VideoServerResponse destroyAudioBridgePluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyAudioBridgePluginResponse);

      VideoServerResponse destroyConnectionResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyConnectionResponse);

      videoServerService.stopMeeting(meeting1Id.toString());

      ArgumentCaptor<VideoServerMessageRequest> destroyVideoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyVideoRoomPluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyAudioRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyAudioBridgePluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyConnectionRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              destroyVideoRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              destroyVideoRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              destroyAudioRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              destroyAudioBridgePluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString() + serverQPm),
              destroyConnectionRequestCaptor.capture());
      verify(videoServerMeetingRepository, times(1)).deleteById(meeting1Id.toString());

      assertEquals(1, destroyVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyVideoRoomRequest = destroyVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomDestroyRequest.create()
                      .request("destroy")
                      .room(meeting1VideoRoomId.toString())
                      .permanent(false)),
          destroyVideoRoomRequest);

      assertEquals(1, destroyVideoRoomPluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyVideoRoomPluginRequest =
          destroyVideoRoomPluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyVideoRoomPluginRequest);

      assertEquals(1, destroyAudioRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyAudioRoomRequest = destroyAudioRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  AudioBridgeDestroyRequest.create()
                      .request("destroy")
                      .room(meeting1AudioRoomId.toString())
                      .permanent(false)),
          destroyAudioRoomRequest);

      assertEquals(1, destroyAudioBridgePluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyAudioBridgePluginRequest =
          destroyAudioBridgePluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyAudioBridgePluginRequest);

      assertEquals(1, destroyConnectionRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyConnectionRequest =
          destroyConnectionRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("destroy").apiSecret("token"),
          destroyConnectionRequest);
    }

    @Test
    @DisplayName("Try to stop a meeting that does not exist, it ignores it silently")
    void stopMeeting_testErrorMeetingNotExists() {
      videoServerService.stopMeeting(meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Add Meeting Participant tests")
  class AddMeetingParticipantTests {

    @Test
    @DisplayName("add a participant in an existing meeting")
    void addMeetingParticipant_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);

      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm), any(VideoServerMessageRequest.class)))
          .thenReturn(sessionResponse);

      VideoServerResponse audioHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1AudioHandleId.toString()));

      VideoServerResponse videoOutHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoOutHandleId.toString()));

      VideoServerResponse videoInHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoInHandleId.toString()));

      VideoServerResponse screenHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1ScreenHandleId.toString()));

      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(
              audioHandleResponse,
              videoOutHandleResponse,
              videoInHandleResponse,
              screenHandleResponse);

      VideoRoomResponse joinPublisherVideoResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoOutHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinPublisherVideoResponse);

      VideoRoomResponse joinPublisherScreenResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1ScreenHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinPublisherScreenResponse);

      videoServerService.addMeetingParticipant(
          user1Id.toString(), queue1Id.toString(), meeting1Id.toString(), false, true);

      ArgumentCaptor<VideoServerMessageRequest> createConnectionRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> createHandleRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> joinPublisherVideoRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> joinPublisherScreenRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm),
              createConnectionRequestCaptor.capture());
      verify(videoServerClient, times(4))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              createHandleRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              joinPublisherVideoRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              joinPublisherScreenRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1))
          .insert(
              VideoServerSession.create(user1Id.toString(), queue1Id.toString(), videoServerMeeting)
                  .connectionId(user1SessionId.toString())
                  .audioHandleId(user1AudioHandleId.toString())
                  .videoOutHandleId(user1VideoOutHandleId.toString())
                  .videoInHandleId(user1VideoInHandleId.toString())
                  .screenHandleId(user1ScreenHandleId.toString()));

      assertEquals(1, createConnectionRequestCaptor.getAllValues().size());
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("create").apiSecret("token"),
          createConnectionRequestCaptor.getValue());

      assertEquals(4, createHandleRequestCaptor.getAllValues().size());
      VideoServerMessageRequest audioHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.audiobridge")
              .apiSecret("token"),
          audioHandleMessageRequest);
      VideoServerMessageRequest videoOutHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(1);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          videoOutHandleMessageRequest);
      VideoServerMessageRequest videoInHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(2);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          videoInHandleMessageRequest);
      VideoServerMessageRequest screenHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(3);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          screenHandleMessageRequest);

      assertEquals(1, joinPublisherVideoRequestCaptor.getAllValues().size());
      VideoServerMessageRequest joinPublisherVideoRequest =
          joinPublisherVideoRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomJoinRequest.create()
                      .request("join")
                      .ptype("publisher")
                      .room(meeting1VideoRoomId.toString())
                      .id(user1Id.toString() + "/video")),
          joinPublisherVideoRequest);

      assertEquals(1, joinPublisherScreenRequestCaptor.getAllValues().size());
      VideoServerMessageRequest joinPublisherScreenRequest =
          joinPublisherScreenRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomJoinRequest.create()
                      .request("join")
                      .ptype("publisher")
                      .room(meeting1VideoRoomId.toString())
                      .id(user1Id.toString() + "/screen")),
          joinPublisherScreenRequest);
    }

    @Test
    @DisplayName("Try to add a participant in a meeting that does not exist")
    void addMeetingParticipant_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idQueue = queue1Id.toString();
      String idMeeting = meeting1Id.toString();

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.addMeetingParticipant(idUser, idQueue, idMeeting, false, true),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to add a participant in a meeting but video server returns error joining as publisher")
    void addMeetingParticipant_testErrorJoiningAsPublisher() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm), any(VideoServerMessageRequest.class)))
          .thenReturn(sessionResponse);

      VideoServerResponse audioHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1AudioHandleId.toString()));

      VideoServerResponse videoOutHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoOutHandleId.toString()));

      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioHandleResponse, videoOutHandleResponse);

      VideoRoomResponse joinPublisherVideoResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinPublisherVideoResponse);

      String idUser = user1Id.toString();
      String idQueue = queue1Id.toString();
      String idMeeting = meeting1Id.toString();

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.addMeetingParticipant(idUser, idQueue, idMeeting, false, true),
          "An error occured while user "
              + user1Id
              + " with connection id "
              + queue1Id
              + " is joining video room as publisher");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + serverQPm), any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(2))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to add a participant in a meeting when it's already in")
    void addMeetingParticipant_testErrorAlreadyPresent() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      String idUser = user1Id.toString();
      String idQueue = queue1Id.toString();
      String idMeeting = meeting1Id.toString();

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.addMeetingParticipant(idUser, idQueue, idMeeting, false, true),
          "Videoserver session user with user  "
              + user1Id
              + "is already present in the videoserver meeting "
              + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Destroy Meeting Participant tests")
  class DestroyMeetingParticipantTests {

    @Test
    @DisplayName("destroy a participant in a meeting when it's in")
    void destroyMeetingParticipant_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .screenHandleId(user1ScreenHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoServerResponse destroyAudioBridgePluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyAudioBridgePluginResponse);

      VideoServerResponse destroyVideoOutRoomPluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoOutRoomPluginResponse);

      VideoServerResponse destroyVideoInRoomPluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoInRoomPluginResponse);

      VideoServerResponse destroyVideoScreenPluginResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoScreenPluginResponse);

      VideoServerResponse destroyConnectionResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyConnectionResponse);

      videoServerService.destroyMeetingParticipant(user1Id.toString(), meeting1Id.toString());

      ArgumentCaptor<VideoServerMessageRequest> destroyAudioBridgePluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyVideoInRoomPluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyVideoOutRoomPluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyVideoScreenPluginRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerMessageRequest> destroyConnectionRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              destroyAudioBridgePluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              destroyVideoOutRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              destroyVideoInRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              destroyVideoScreenPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString() + serverQPm),
              destroyConnectionRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).remove(videoServerSession);

      assertEquals(1, destroyAudioBridgePluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyAudioBridgePluginRequest =
          destroyAudioBridgePluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyAudioBridgePluginRequest);

      assertEquals(1, destroyVideoOutRoomPluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyVideoOutRoomPluginRequest =
          destroyVideoOutRoomPluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyVideoOutRoomPluginRequest);

      assertEquals(1, destroyVideoInRoomPluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyVideoInRoomPluginRequest =
          destroyVideoInRoomPluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyVideoInRoomPluginRequest);

      assertEquals(1, destroyVideoScreenPluginRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyVideoScreenPluginRequest =
          destroyVideoScreenPluginRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("detach").apiSecret("token"),
          destroyVideoScreenPluginRequest);

      assertEquals(1, destroyConnectionRequestCaptor.getAllValues().size());
      VideoServerMessageRequest destroyConnectionRequest =
          destroyConnectionRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("destroy").apiSecret("token"),
          destroyConnectionRequest);
    }

    @Test
    @DisplayName(
        "Try to destroy a participant in a meeting that does not exist, it ignores it silently")
    void destroyMeetingParticipant_testErrorMeetingNotExists() {
      videoServerService.destroyMeetingParticipant(user1Id.toString(), meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to destroy a participant in a meeting when it's not in, it ignores it silently")
    void destroyMeetingParticipant_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      videoServerService.destroyMeetingParticipant(user1Id.toString(), meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Update media stream tests")
  class UpdateMediaStreamTests {

    @Test
    @DisplayName("enable video stream in a meeting")
    void updateMediaStream_testEnableVideoOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoRoomResponse publishStreamVideoRoomResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoOutHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(publishStreamVideoRoomResponse);

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(true)
              .sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerMessageRequest> publishStreamVideoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              publishStreamVideoRoomRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest =
          publishStreamVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomPublishRequest.create()
                      .request("publish")
                      .filename("video" + "_" + user1Id + "_" + "20220101T120000"))
              .rtcSessionDescription(
                  RtcSessionDescription.create()
                      .type(RtcType.OFFER)
                      .sdp("session-description-protocol")),
          publishStreamVideoRoomRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(true),
          videoServerSessionUpdated);
    }

    @Test
    @DisplayName("enable screen stream in a meeting")
    void updateMediaStream_testEnableScreenOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoRoomResponse publishStreamVideoRoomResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1ScreenHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(publishStreamVideoRoomResponse);

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.SCREEN)
              .enabled(true)
              .sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerMessageRequest> publishStreamVideoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()
                      + serverQPm),
              publishStreamVideoRoomRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest =
          publishStreamVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomPublishRequest.create()
                      .request("publish")
                      .filename("screen" + "_" + user1Id + "_" + "20220101T120000"))
              .rtcSessionDescription(
                  RtcSessionDescription.create()
                      .type(RtcType.OFFER)
                      .sdp("session-description-protocol")),
          publishStreamVideoRoomRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(true),
          videoServerSessionUpdated);
    }

    @Test
    @DisplayName("disable video stream in a meeting")
    void updateMediaStream_testDisableVideoOk() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(false)
              .sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(false),
          videoServerSessionUpdated);
    }

    @Test
    @DisplayName("disable screen stream in a meeting")
    void updateMediaStream_testDisableScreenOk() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.SCREEN)
              .enabled(false)
              .sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(false),
          videoServerSessionUpdated);
    }

    @Test
    @DisplayName("Try to update media stream on a meeting that does not exist")
    void updateMediaStream_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      MediaStreamSettingsDto mediaStreamSettingsDto =
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(true)
              .sdp("session-description-protocol");

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateMediaStream(idUser, idMeeting, mediaStreamSettingsDto),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to update media stream on a meeting of a participant that is not in")
    void updateMediaStream_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      MediaStreamSettingsDto mediaStreamSettingsDto =
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(true)
              .sdp("session-description-protocol");

      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateMediaStream(idUser, idMeeting, mediaStreamSettingsDto),
          "No Videoserver session found for user " + user1Id + " for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to update media stream on a meeting but video server returns error publishing video"
            + " stream")
    void updateMediaStream_testErrorPublishingVideoStream() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoRoomResponse publishStreamVideoRoomResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(publishStreamVideoRoomResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      MediaStreamSettingsDto mediaStreamSettingsDto =
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(true)
              .sdp("session-description-protocol");
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateMediaStream(idUser, idMeeting, mediaStreamSettingsDto),
          "An error occured while connection id "
              + queue1Id.toString()
              + " is publishing video stream");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to enable video stream on a meeting when it is already enabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyEnabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(true)
              .sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to enable screen stream on a meeting when it is already enabled")
    void updateMediaStream_testIgnoreScreenStreamAlreadyEnabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.SCREEN)
              .enabled(true)
              .sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable video stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyDisabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .videoOutHandleId(user1VideoOutHandleId.toString())
              .videoOutStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.VIDEO)
              .enabled(false)
              .sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable screen stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreScreenStreamAlreadyDisabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .screenHandleId(user1ScreenHandleId.toString())
              .screenStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          MediaStreamSettingsDto.create()
              .type(TypeEnum.SCREEN)
              .enabled(false)
              .sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("enable audio stream in a meeting")
  class UpdateAudioStreamTests {

    @Test
    @DisplayName("enable audio stream test")
    void updateAudioStream_testEnableOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString())
              .audioStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      AudioBridgeResponse updateAudioStreamResponse =
          AudioBridgeResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1AudioHandleId.toString())
              .pluginData(
                  AudioBridgePluginData.create()
                      .plugin("janus.plugin.audiobridge")
                      .dataInfo(AudioBridgeDataInfo.create().audioBridge("success")));
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateAudioStreamResponse);

      videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true);

      ArgumentCaptor<VideoServerMessageRequest> updateAudioStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              updateAudioStreamRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, updateAudioStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest updateAudioStreamRequest =
          updateAudioStreamRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  AudioBridgeMuteRequest.create()
                      .request("unmute")
                      .room(meeting1AudioRoomId.toString())
                      .id(user1Id.toString())),
          updateAudioStreamRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString())
              .audioStreamOn(true),
          videoServerSessionUpdated);
    }

    @Test
    @DisplayName("Try to update audio stream on a meeting that does not exist")
    void updateAudioStream_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateAudioStream(idUser, idMeeting, true),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to update audio stream on a meeting of a participant that is not in")
    void updateAudioStream_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateAudioStream(idUser, idMeeting, true),
          "No Videoserver session found for user " + user1Id + " for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to update audio stream on a meeting but video server returns error enabling audio"
            + " stream")
    void updateAudioStream_testErrorEnablingAudioStream() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString())
              .audioStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      AudioBridgeResponse updateAudioStreamResponse =
          AudioBridgeResponse.create()
              .status("error")
              .pluginData(
                  AudioBridgePluginData.create()
                      .plugin("janus.plugin.audiobridge")
                      .dataInfo(AudioBridgeDataInfo.create().audioBridge("error")));
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateAudioStreamResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateAudioStream(idUser, idMeeting, true),
          "An error occured while setting audio stream status for "
              + user1Id
              + " with connection id "
              + queue1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to enable audio stream on a meeting when it is already enabled")
    void updateAudioStream_testIgnoreAudioStreamAlreadyEnabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .audioHandleId(user1AudioHandleId.toString())
              .audioStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable audio stream on a meeting when it is already disabled")
    void updateAudioStream_testIgnoreAudioStreamAlreadyDisabled() {
      UUID serverId = UUID.randomUUID();
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .audioHandleId(user1AudioHandleId.toString())
              .audioStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), false);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Answer rtc media stream tests")
  class AnswerRtcMediaStreamTests {

    @Test
    @DisplayName("send answer for video stream")
    void answerRtcMediaStream_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoRoomResponse answerRtcMediaStreamResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoInHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(answerRtcMediaStreamResponse);

      videoServerService.answerRtcMediaStream(
          user1Id.toString(), meeting1Id.toString(), "session-description-protocol");

      ArgumentCaptor<VideoServerMessageRequest> answerRtcMediaStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              answerRtcMediaStreamRequestCaptor.capture());

      assertEquals(1, answerRtcMediaStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest answerRtcMediaStreamRequest =
          answerRtcMediaStreamRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(VideoRoomStartVideoInRequest.create().request("start"))
              .rtcSessionDescription(
                  RtcSessionDescription.create()
                      .type(RtcType.ANSWER)
                      .sdp("session-description-protocol")),
          answerRtcMediaStreamRequest);
    }

    @Test
    @DisplayName("Try to send answer for media stream on a meeting that does not exist")
    void answerRtcMediaStream_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.answerRtcMediaStream(
                  idUser, idMeeting, "session-description-protocol"),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to send answer for media stream on a meeting of a participant that is not in")
    void answerRtcMediaStream_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.answerRtcMediaStream(
                  idUser, idMeeting, "session-description-protocol"),
          "No Videoserver session found for user " + user1Id + " for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to send answer for media stream on a meeting but video server returns error starting"
            + " receiving video streams")
    void answerRtcMediaStream_testErrorStartingReceivingVideoStreams() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      VideoRoomResponse answerRtcMediaStreamResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(answerRtcMediaStreamResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.answerRtcMediaStream(
                  idUser, idMeeting, "session-description-protocol"),
          "An error occured while session with connection id "
              + queue1Id
              + " is starting receiving video streams available in the video room");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Update subscriptions media stream tests")
  class UpdateSubscriptionsMediaStreamTests {

    @Test
    @DisplayName("subscribe to another participant stream video for first time")
    void updateSubscriptionsMediaStream_testJoinSubscriberOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString());
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user2VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoRoomResponse joinSubscriberResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoInHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinSubscriberResponse);

      videoServerService.updateSubscriptionsMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString()))));

      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);
      ArgumentCaptor<VideoServerMessageRequest> joinSubscriberRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              joinSubscriberRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, joinSubscriberRequestCaptor.getAllValues().size());
      VideoServerMessageRequest joinSubscriberRequest = joinSubscriberRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomJoinRequest.create()
                      .request("join")
                      .ptype("subscriber")
                      .useMsid(true)
                      .room(meeting1VideoRoomId.toString())
                      .streams(
                          List.of(
                              Stream.create()
                                  .feed(
                                      Feed.create()
                                          .type(MediaType.VIDEO)
                                          .userId(user2Id.toString())
                                          .toString())))),
          joinSubscriberRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSession = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true),
          videoServerSession);
    }

    @Test
    @DisplayName("subscribe to another participant stream video")
    void updateSubscriptionsMediaStream_testSubscribeOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true);
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user2VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoRoomResponse updateSubscriptionsMediaStreamResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoInHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString()))));

      ArgumentCaptor<VideoServerMessageRequest> updateSubscriptionsMediaStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              updateSubscriptionsMediaStreamRequestCaptor.capture());

      assertEquals(1, updateSubscriptionsMediaStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest =
          updateSubscriptionsMediaStreamRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomUpdateSubscriptionsRequest.create()
                      .request("update")
                      .subscriptions(
                          List.of(
                              Stream.create()
                                  .feed(
                                      Feed.create()
                                          .type(MediaType.VIDEO)
                                          .userId(user2Id.toString())
                                          .toString())))
                      .unsubscriptions(List.of())),
          updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("unsubscribe from another participant stream video")
    void updateSubscriptionsMediaStream_testUnSubscribeOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true);
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user2VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoRoomResponse updateSubscriptionsMediaStreamResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoInHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          SubscriptionUpdatesDto.create()
              .unsubscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString()))));

      ArgumentCaptor<VideoServerMessageRequest> updateSubscriptionsMediaStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              updateSubscriptionsMediaStreamRequestCaptor.capture());

      assertEquals(1, updateSubscriptionsMediaStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest =
          updateSubscriptionsMediaStreamRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomUpdateSubscriptionsRequest.create()
                      .request("update")
                      .subscriptions(List.of())
                      .unsubscriptions(
                          List.of(
                              Stream.create()
                                  .feed(
                                      Feed.create()
                                          .type(MediaType.VIDEO)
                                          .userId(user2Id.toString())
                                          .toString())))),
          updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("subscribe and unsubscribe to and from other participants' stream video")
    void updateSubscriptionsMediaStream_testSubscribeAndUnSubscribeOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true);
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user2VideoInHandleId.toString());
      VideoServerSession videoServerSession3 =
          VideoServerSession.create()
              .userId(user3Id.toString())
              .queueId(queue3Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user3SessionId.toString())
              .videoInHandleId(user3VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(
          List.of(videoServerSession1, videoServerSession2, videoServerSession3));

      VideoRoomResponse updateSubscriptionsMediaStreamResponse =
          VideoRoomResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1VideoInHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString())))
              .unsubscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user3Id.toString()))));

      ArgumentCaptor<VideoServerMessageRequest> updateSubscriptionsMediaStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              updateSubscriptionsMediaStreamRequestCaptor.capture());

      assertEquals(1, updateSubscriptionsMediaStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest =
          updateSubscriptionsMediaStreamRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(
                  VideoRoomUpdateSubscriptionsRequest.create()
                      .request("update")
                      .subscriptions(
                          List.of(
                              Stream.create()
                                  .feed(
                                      Feed.create()
                                          .type(MediaType.VIDEO)
                                          .userId(user2Id.toString())
                                          .toString())))
                      .unsubscriptions(
                          List.of(
                              Stream.create()
                                  .feed(
                                      Feed.create()
                                          .type(MediaType.VIDEO)
                                          .userId(user3Id.toString())
                                          .toString())))),
          updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("Try to update subscriptions for media stream on a meeting that does not exist")
    void updateSubscriptionsMediaStream_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      SubscriptionUpdatesDto subscriptionUpdatesDto =
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString())));
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.updateSubscriptionsMediaStream(
                  idUser, idMeeting, subscriptionUpdatesDto),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to update subscriptions for media stream on a meeting of a participant that is not in")
    void updateSubscriptionsMediaStream_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      SubscriptionUpdatesDto subscriptionUpdatesDto =
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString())));
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.updateSubscriptionsMediaStream(
                  idUser, idMeeting, subscriptionUpdatesDto),
          "No Videoserver session found for user " + user1Id + " for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to update subscriptions for media stream on a meeting but video server returns error"
            + " joining as subscriber")
    void updateSubscriptionsMediaStream_testErrorJoiningAsSubscriber() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString());
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoRoomResponse joinSubscriberResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinSubscriberResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      SubscriptionUpdatesDto subscriptionUpdatesDto =
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString())));
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.updateSubscriptionsMediaStream(
                  idUser, idMeeting, subscriptionUpdatesDto),
          "An error occured while user "
              + user1Id
              + " with connection id "
              + queue1Id
              + " is joining video room as subscriber"
              + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName(
        "Try to update subscriptions for media stream on a meeting but video server returns error"
            + " updating subscriptions")
    void updateSubscriptionsMediaStream_testErrorUpdatingSubscriptions() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true);
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString())
              .videoInHandleId(user2VideoInHandleId.toString())
              .videoInStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoRoomResponse updateSubscriptionsMediaStreamResponse =
          VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(updateSubscriptionsMediaStreamResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      SubscriptionUpdatesDto subscriptionUpdatesDto =
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString())));
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.updateSubscriptionsMediaStream(
                  idUser, idMeeting, subscriptionUpdatesDto),
          "An error occured while user "
              + user1Id
              + " with connection id "
              + queue1Id
              + " is updating media subscriptions in the video room"
              + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Offer rtc audio stream tests")
  class OfferRtcAudioStreamTests {

    @Test
    @DisplayName("send offer for audio stream")
    void offerRtcAudioStream_testOk() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      AudioBridgeResponse offerRtcAudioStreamResponse =
          AudioBridgeResponse.create()
              .status("ack")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(user1AudioHandleId.toString());
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(offerRtcAudioStreamResponse);

      videoServerService.offerRtcAudioStream(
          user1Id.toString(), meeting1Id.toString(), "session-description-protocol");

      ArgumentCaptor<VideoServerMessageRequest> offerRtcAudioStreamRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              offerRtcAudioStreamRequestCaptor.capture());

      assertEquals(1, offerRtcAudioStreamRequestCaptor.getAllValues().size());
      VideoServerMessageRequest offerRtcAudioStreamRequest =
          offerRtcAudioStreamRequestCaptor.getValue();
      assertEquals("message", offerRtcAudioStreamRequest.getMessageRequest());
      assertEquals("token", offerRtcAudioStreamRequest.getApiSecret());
      AudioBridgeJoinRequest audioBridgeJoinRequest =
          (AudioBridgeJoinRequest) offerRtcAudioStreamRequest.getVideoServerPluginRequest();
      assertEquals("join", audioBridgeJoinRequest.getRequest());
      assertEquals(meeting1AudioRoomId.toString(), audioBridgeJoinRequest.getRoom());
      assertTrue(audioBridgeJoinRequest.getMuted());
      assertEquals(
          "audio" + "_" + user1Id + "_" + "20220101T120000", audioBridgeJoinRequest.getFilename());
    }

    @Test
    @DisplayName("Try to send offer for audio stream on a meeting that does not exist")
    void offerRtcAudioStream_testErrorMeetingNotExists() {
      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.offerRtcAudioStream(
                  idUser, idMeeting, "session-description-protocol"),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to send offer for audio stream on a meeting of a participant that is not in")
    void offerRtcAudioStream_testErrorParticipantNotExists() {
      UUID serverId = UUID.randomUUID();
      createVideoServerMeeting(serverId, meeting1Id);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.offerRtcAudioStream(
                  idUser, idMeeting, "session-description-protocol"),
          "No Videoserver session found for user " + user1Id + " for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to send offer for audio stream on a meeting but video server returns error joining"
            + " audio room")
    void offerRtcAudioStream_testErrorJoiningAudioRoom() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(serverId, meeting1Id);
      VideoServerSession videoServerSession =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .audioHandleId(user1AudioHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      AudioBridgeResponse offerRtcAudioStreamResponse =
          AudioBridgeResponse.create().status("error");
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(offerRtcAudioStreamResponse);

      String idUser = user1Id.toString();
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () ->
              videoServerService.offerRtcAudioStream(
                  idUser, idMeeting, "session-description-protocol"),
          "An error occured while user "
              + user1Id
              + " with connection id "
              + queue1Id
              + " is joining the audio room");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Start recording tests")
  class StartRecordingTests {

    @Test
    @DisplayName("Start recording on a meeting")
    void startRecording_testOK() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      AudioBridgeResponse audioBridgeResponse =
          AudioBridgeResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1AudioHandleId.toString());
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioBridgeResponse);
      VideoRoomResponse videoRoomResponse =
          VideoRoomResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1VideoHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoRoomResponse);

      videoServerService.updateRecording(meeting1Id.toString(), true);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());

      ArgumentCaptor<VideoServerMessageRequest> audioBridgeRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              audioBridgeRequestCaptor.capture());
      assertEquals(1, audioBridgeRequestCaptor.getAllValues().size());
      VideoServerMessageRequest audioBridgeEnableMjrsRecordRequest =
          audioBridgeRequestCaptor.getValue();
      assertEquals("message", audioBridgeEnableMjrsRecordRequest.getMessageRequest());
      assertEquals("token", audioBridgeEnableMjrsRecordRequest.getApiSecret());
      AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
          (AudioBridgeEnableMjrsRequest)
              audioBridgeEnableMjrsRecordRequest.getVideoServerPluginRequest();
      assertEquals("enable_mjrs", audioBridgeEnableMjrsRequest.getRequest());
      assertEquals(meeting1AudioRoomId.toString(), audioBridgeEnableMjrsRequest.getRoom());
      assertTrue(audioBridgeEnableMjrsRequest.getMjrs());

      ArgumentCaptor<VideoServerMessageRequest> videoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      verify(videoServerClient, times(2))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              videoRoomRequestCaptor.capture());
      assertEquals(2, videoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest videoRoomEditRequest = videoRoomRequestCaptor.getAllValues().get(0);
      assertEquals("message", videoRoomEditRequest.getMessageRequest());
      assertEquals("token", videoRoomEditRequest.getApiSecret());
      VideoRoomEditRequest videoRoomEditRoomRequest =
          (VideoRoomEditRequest) videoRoomEditRequest.getVideoServerPluginRequest();
      assertEquals("edit", videoRoomEditRoomRequest.getRequest());
      assertEquals(meeting1VideoRoomId.toString(), videoRoomEditRoomRequest.getRoom());
      assertEquals(
          recordingPath + "_" + meeting1Id + "/" + "20220101T120000",
          videoRoomEditRoomRequest.getNewRecDir());
      VideoServerMessageRequest videoRoomEnableRecordRequest =
          videoRoomRequestCaptor.getAllValues().get(1);
      assertEquals("message", videoRoomEnableRecordRequest.getMessageRequest());
      assertEquals("token", videoRoomEnableRecordRequest.getApiSecret());
      VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
          (VideoRoomEnableRecordingRequest)
              videoRoomEnableRecordRequest.getVideoServerPluginRequest();
      assertEquals("enable_recording", videoRoomEnableRecordingRequest.getRequest());
      assertEquals(meeting1VideoRoomId.toString(), videoRoomEnableRecordingRequest.getRoom());
      assertTrue(videoRoomEnableRecordingRequest.getRecord());
    }

    @Test
    @DisplayName("Try to start recording on a meeting that does not exist")
    void startRecording_testErrorMeetingNotExists() {
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateRecording(idMeeting, true),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName(
        "Try to start recording on a meeting but the video server returns error enabling audio room"
            + " recording")
    void startRecording_testErrorEnablingAudioRoomRecording() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      VideoRoomResponse videoRoomEditResponse = VideoRoomResponse.create().status("success");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoRoomEditResponse);

      AudioBridgeResponse audioBridgeResponse = AudioBridgeResponse.create().status("error");
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioBridgeResponse);

      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateRecording(idMeeting, true),
          "An error occurred when recording the audiobridge room for the connection "
              + meeting1SessionId
              + " with plugin "
              + meeting1AudioHandleId
              + " for the meeting "
              + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName(
        "Try to start recording on a meeting but the video server returns error enabling video room"
            + " recording")
    void startRecording_testErrorEnablingVideoRoomRecording() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      AudioBridgeResponse audioBridgeResponse = AudioBridgeResponse.create().status("success");
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioBridgeResponse);
      VideoRoomResponse videoRoomEditResponse = VideoRoomResponse.create().status("success");
      VideoRoomResponse videoRoomResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoRoomEditResponse, videoRoomResponse);

      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateRecording(idMeeting, true),
          "An error occurred when recording the audiobridge room for the connection "
              + meeting1SessionId
              + " with plugin "
              + meeting1AudioHandleId
              + " for the meeting "
              + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(2))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Stop recording tests")
  class StopRecordingTests {

    @Test
    @DisplayName("Stop recording on a meeting")
    void stopRecording_testOK() {
      UUID serverId = UUID.randomUUID();
      String serverQPm = getServerIdQueryParam(serverId);
      createVideoServerMeeting(serverId, meeting1Id);

      AudioBridgeResponse audioBridgeResponse =
          AudioBridgeResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1AudioHandleId.toString());
      when(videoServerClient.sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(audioBridgeResponse);
      VideoRoomResponse videoRoomResponse =
          VideoRoomResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id")
              .handleId(meeting1VideoHandleId.toString());
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoRoomResponse);

      videoServerService.updateRecording(meeting1Id.toString(), false);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());

      ArgumentCaptor<VideoServerMessageRequest> audioBridgeRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()
                      + serverQPm),
              audioBridgeRequestCaptor.capture());
      assertEquals(1, audioBridgeRequestCaptor.getAllValues().size());
      VideoServerMessageRequest audioBridgeEnableMjrsRecordRequest =
          audioBridgeRequestCaptor.getAllValues().get(0);
      assertEquals("message", audioBridgeEnableMjrsRecordRequest.getMessageRequest());
      assertEquals("token", audioBridgeEnableMjrsRecordRequest.getApiSecret());
      AudioBridgeEnableMjrsRequest audioBridgeEnableMjrsRequest =
          (AudioBridgeEnableMjrsRequest)
              audioBridgeEnableMjrsRecordRequest.getVideoServerPluginRequest();
      assertEquals("enable_mjrs", audioBridgeEnableMjrsRequest.getRequest());
      assertEquals(meeting1AudioRoomId.toString(), audioBridgeEnableMjrsRequest.getRoom());
      assertFalse(audioBridgeEnableMjrsRequest.getMjrs());

      ArgumentCaptor<VideoServerMessageRequest> videoRoomRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()
                      + serverQPm),
              videoRoomRequestCaptor.capture());
      assertEquals(1, videoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest videoRoomEnableRecordRequest =
          videoRoomRequestCaptor.getAllValues().get(0);
      assertEquals("message", videoRoomEnableRecordRequest.getMessageRequest());
      assertEquals("token", videoRoomEnableRecordRequest.getApiSecret());
      VideoRoomEnableRecordingRequest videoRoomEnableRecordingRequest =
          (VideoRoomEnableRecordingRequest)
              videoRoomEnableRecordRequest.getVideoServerPluginRequest();
      assertEquals("enable_recording", videoRoomEnableRecordingRequest.getRequest());
      assertEquals(meeting1VideoRoomId.toString(), videoRoomEnableRecordingRequest.getRoom());
      assertFalse(videoRoomEnableRecordingRequest.getRecord());
    }

    @Test
    @DisplayName("Try to stop recording on a meeting that does not exist")
    void stopRecording_testErrorMeetingNotExists() {
      String idMeeting = meeting1Id.toString();
      assertThrows(
          VideoServerException.class,
          () -> videoServerService.updateRecording(idMeeting, false),
          "No videoserver meeting found for the meeting " + meeting1Id);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Health check tests")
  class HealthCheckTests {

    @Test
    @DisplayName("Send is alive request to video server")
    void isAlive_testOK() {
      VideoServerResponse sessionResponse =
          VideoServerResponse.create().status("server_info").transactionId("transaction-id");
      when(videoServerClient.sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint))
          .thenReturn(sessionResponse);

      assertTrue(videoServerService.isAlive());

      verify(videoServerClient, times(1))
          .sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint);
    }

    @Test
    @DisplayName("Send is alive request to video server and it returns error")
    void isAlive_testErrorResponse() {
      VideoServerResponse sessionResponse =
          VideoServerResponse.create().status("error").transactionId("transaction-id");
      when(videoServerClient.sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint))
          .thenReturn(sessionResponse);

      assertFalse(videoServerService.isAlive());

      verify(videoServerClient, times(1))
          .sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint);
    }

    @Test
    @DisplayName("Send is alive request to video server and it throws exception")
    void isAlive_testExceptionError() {
      when(videoServerClient.sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint))
          .thenThrow(RuntimeException.class);

      assertFalse(videoServerService.isAlive());

      verify(videoServerClient, times(1))
          .sendGetInfoRequest(videoServerURL + janusEndpoint + janusInfoEndpoint);
    }
  }

  @Nested
  @DisplayName("Start recording post processing tests")
  class StartRecordingPostProcessingTests {

    @Test
    @DisplayName("Send request to video recorder for post processing")
    void startRecordingPostProcessing_testOk() {
      videoServerService.startRecordingPostProcessing(
          meeting1Id.toString(), "meeting-name", "rec-dir-id", "rec-name", "fake-token");

      verify(videoServerClient, times(1))
          .sendVideoRecorderRequest(
              videoRecorderURL + postProcessorEndpoint + "_" + meeting1Id.toString(),
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName("meeting-name")
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .folderId("rec-dir-id")
                  .recordingName("rec-name")
                  .authToken("ZM_AUTH_TOKEN=fake-token"));
    }

    @Test
    @DisplayName("Send request to video recorder for post processing without auth token")
    void startRecordingPostProcessing_testOkWithoutAuhToken() {
      videoServerService.startRecordingPostProcessing(
          meeting1Id.toString(), "meeting-name", "rec-dir-id", "rec-name", null);

      verify(videoServerClient, times(1))
          .sendVideoRecorderRequest(
              videoRecorderURL + postProcessorEndpoint + "_" + meeting1Id.toString(),
              VideoRecorderRequest.create()
                  .meetingId(meeting1Id.toString())
                  .meetingName("meeting-name")
                  .audioActivePackets(10L)
                  .audioLevelAverage(65)
                  .folderId("rec-dir-id")
                  .recordingName("rec-name"));
    }
  }
}
