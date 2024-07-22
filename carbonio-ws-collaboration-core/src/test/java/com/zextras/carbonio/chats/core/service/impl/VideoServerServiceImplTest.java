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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcSessionDescription;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.RtcType;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
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

  public VideoServerServiceImplTest() {
    AppConfig appConfig = mock(AppConfig.class);
    this.videoServerClient = mock(VideoServerClient.class);
    this.videoServerMeetingRepository = mock(VideoServerMeetingRepository.class);
    this.videoServerSessionRepository = mock(VideoServerSessionRepository.class);

    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST))
        .thenReturn(Optional.of("127.78.0.4"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT))
        .thenReturn(Optional.of("20006"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN))
        .thenReturn(Optional.of("token"));

    this.videoServerService =
        new VideoServerServiceImpl(
            appConfig,
            videoServerClient,
            videoServerMeetingRepository,
            videoServerSessionRepository);
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
  private String videoServerURL;
  private String janusEndpoint;
  private String janusInfoEndpoint;

  @BeforeEach
  public void init() {
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
    janusEndpoint = "/janus";
    janusInfoEndpoint = "/info";
  }

  @AfterEach
  public void cleanup() {
    verifyNoMoreInteractions(videoServerClient);
    verifyNoMoreInteractions(videoServerMeetingRepository);
    verifyNoMoreInteractions(videoServerSessionRepository);
    reset(videoServerClient, videoServerMeetingRepository, videoServerSessionRepository);
  }

  private VideoServerMeeting createVideoServerMeeting(UUID meetingId) {
    VideoServerMeeting videoServerMeeting =
        VideoServerMeeting.create()
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

  @Nested
  @DisplayName("Start Meeting tests")
  class StartMeetingTests {

    @Test
    @DisplayName("Start a new meeting on a room")
    void startMeeting_testOk() {
      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(meeting1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint), any(VideoServerMessageRequest.class)))
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
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
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
                      + meeting1AudioHandleId.toString()),
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
                      + meeting1VideoHandleId.toString()),
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
              eq(videoServerURL + janusEndpoint), createConnectionRequestCaptor.capture());
      verify(videoServerClient, times(2))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
              createHandleRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()),
              createAudioRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()),
              createVideoRoomRequestCaptor.capture());
      verify(videoServerMeetingRepository, times(1))
          .insert(
              VideoServerMeeting.create()
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
      createVideoServerMeeting(meeting1Id);
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
      createVideoServerMeeting(meeting1Id);

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
                      + meeting1VideoHandleId.toString()),
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
                      + meeting1VideoHandleId.toString()),
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
                      + meeting1AudioHandleId.toString()),
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
                      + meeting1AudioHandleId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyAudioBridgePluginResponse);

      VideoServerResponse destroyConnectionResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(meeting1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
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
                      + meeting1VideoHandleId.toString()),
              destroyVideoRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1VideoHandleId.toString()),
              destroyVideoRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendAudioBridgeRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()),
              destroyAudioRoomRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + meeting1SessionId.toString()
                      + "/"
                      + meeting1AudioHandleId.toString()),
              destroyAudioBridgePluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);

      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint), any(VideoServerMessageRequest.class)))
          .thenReturn(sessionResponse);

      VideoServerResponse videoOutHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoOutHandleId.toString()));

      VideoServerResponse screenHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1ScreenHandleId.toString()));

      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoOutHandleResponse, screenHandleResponse);

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
                      + user1VideoOutHandleId.toString()),
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
                      + user1ScreenHandleId.toString()),
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
              eq(videoServerURL + janusEndpoint), createConnectionRequestCaptor.capture());
      verify(videoServerClient, times(2))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              createHandleRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()),
              joinPublisherVideoRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()),
              joinPublisherScreenRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1))
          .insert(
              VideoServerSession.create(user1Id.toString(), queue1Id.toString(), videoServerMeeting)
                  .connectionId(user1SessionId.toString())
                  .videoOutHandleId(user1VideoOutHandleId.toString())
                  .screenHandleId(user1ScreenHandleId.toString()));

      assertEquals(1, createConnectionRequestCaptor.getAllValues().size());
      assertEquals(
          VideoServerMessageRequest.create().messageRequest("create").apiSecret("token"),
          createConnectionRequestCaptor.getValue());

      assertEquals(2, createHandleRequestCaptor.getAllValues().size());
      VideoServerMessageRequest videoOutHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          videoOutHandleMessageRequest);
      VideoServerMessageRequest screenHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(1);
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
      createVideoServerMeeting(meeting1Id);

      VideoServerResponse sessionResponse =
          VideoServerResponse.create()
              .status("success")
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1SessionId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint), any(VideoServerMessageRequest.class)))
          .thenReturn(sessionResponse);

      VideoServerResponse videoOutHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoOutHandleId.toString()));

      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoOutHandleResponse);

      VideoRoomResponse joinPublisherVideoResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()),
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
              eq(videoServerURL + janusEndpoint), any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class));
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to add a participant in a meeting when it's already in")
    void addMeetingParticipant_testErrorAlreadyPresent() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1AudioHandleId.toString()),
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
                      + user1VideoOutHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
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
                      + user1ScreenHandleId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(destroyVideoScreenPluginResponse);

      VideoServerResponse destroyConnectionResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id");
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
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
                      + user1AudioHandleId.toString()),
              destroyAudioBridgePluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()),
              destroyVideoOutRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()),
              destroyVideoInRoomPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1ScreenHandleId.toString()),
              destroyVideoScreenPluginRequestCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
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
      createVideoServerMeeting(meeting1Id);

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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoOutHandleId.toString()),
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
                      + user1VideoOutHandleId.toString()),
              publishStreamVideoRoomRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest =
          publishStreamVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(VideoRoomPublishRequest.create().request("publish"))
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1ScreenHandleId.toString()),
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
                      + user1ScreenHandleId.toString()),
              publishStreamVideoRoomRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomRequestCaptor.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest =
          publishStreamVideoRoomRequestCaptor.getValue();
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("message")
              .apiSecret("token")
              .videoServerPluginRequest(VideoRoomPublishRequest.create().request("publish"))
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoOutHandleId.toString()),
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
          "An error occured while connection id " + queue1Id + " is publishing video stream");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoOutHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to enable video stream on a meeting when it is already enabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyEnabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + meeting1AudioHandleId.toString()),
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
                      + meeting1AudioHandleId.toString()),
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
      createVideoServerMeeting(meeting1Id);

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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + meeting1AudioHandleId.toString()),
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
                      + meeting1AudioHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName("Try to enable audio stream on a meeting when it is already enabled")
    void updateAudioStream_testIgnoreAudioStreamAlreadyEnabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoInHandleId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(answerRtcMediaStreamResponse);

      videoServerService.answerRtcMediaStream(
          user1Id.toString(), meeting1Id.toString(), "session-description-protocol");

      ArgumentCaptor<VideoServerMessageRequest> answerRtcMediaStreamRequestCaptor =
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
                      + user1VideoInHandleId.toString()),
              answerRtcMediaStreamRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

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

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getValue();
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString())
              .videoInStreamOn(true),
          videoServerSessionUpdated);
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
      createVideoServerMeeting(meeting1Id);

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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoInHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Update subscriptions media stream tests")
  class UpdateSubscriptionsMediaStreamTests {

    @Test
    @DisplayName("subscribe to another participant stream video for first time")
    void updateSubscriptionsMediaStream_testJoinSubscriberOk() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString());
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoServerResponse videoInHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoInHandleId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoInHandleResponse);

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
                      + user1VideoInHandleId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(joinSubscriberResponse);

      VideoServerSession videoServerSession1Updated = mock(VideoServerSession.class);
      when(videoServerSession1Updated.getConnectionId()).thenReturn(user1SessionId.toString());
      when(videoServerSession1Updated.getVideoInHandleId())
          .thenReturn(user1VideoInHandleId.toString());
      when(videoServerSessionRepository.update(any(VideoServerSession.class)))
          .thenReturn(videoServerSession1Updated);

      videoServerService.updateSubscriptionsMediaStream(
          user1Id.toString(),
          meeting1Id.toString(),
          SubscriptionUpdatesDto.create()
              .subscribe(
                  List.of(
                      MediaStreamDto.create()
                          .type(MediaStreamDto.TypeEnum.VIDEO)
                          .userId(user2Id.toString()))));

      ArgumentCaptor<VideoServerMessageRequest> createHandleRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor =
          ArgumentCaptor.forClass(VideoServerSession.class);
      ArgumentCaptor<VideoServerMessageRequest> joinSubscriberRequestCaptor =
          ArgumentCaptor.forClass(VideoServerMessageRequest.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerClient, times(1))
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              createHandleRequestCaptor.capture());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()),
              joinSubscriberRequestCaptor.capture());

      assertEquals(1, createHandleRequestCaptor.getAllValues().size());
      VideoServerMessageRequest videoInHandleMessageRequest =
          createHandleRequestCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerMessageRequest.create()
              .messageRequest("attach")
              .pluginName("janus.plugin.videoroom")
              .apiSecret("token"),
          videoInHandleMessageRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSession = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString())
              .videoInHandleId(user1VideoInHandleId.toString()),
          videoServerSession);

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
    }

    @Test
    @DisplayName("subscribe to another participant stream video")
    void updateSubscriptionsMediaStream_testSubscribeOk() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoInHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoInHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1VideoInHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
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
      createVideoServerMeeting(meeting1Id);
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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession1 =
          VideoServerSession.create()
              .userId(user1Id.toString())
              .queueId(queue1Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user1SessionId.toString());
      VideoServerSession videoServerSession2 =
          VideoServerSession.create()
              .userId(user2Id.toString())
              .queueId(queue2Id.toString())
              .videoServerMeeting(videoServerMeeting)
              .connectionId(user2SessionId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      VideoServerResponse videoInHandleResponse =
          VideoServerResponse.create()
              .status("success")
              .connectionId(user1SessionId.toString())
              .transactionId("transaction-id")
              .data(VideoServerDataInfo.create().id(user1VideoInHandleId.toString()));
      when(videoServerClient.sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class)))
          .thenReturn(videoInHandleResponse);

      VideoServerSession videoServerSession1Updated = mock(VideoServerSession.class);
      when(videoServerSession1Updated.getConnectionId()).thenReturn(user1SessionId.toString());
      when(videoServerSession1Updated.getVideoInHandleId())
          .thenReturn(user1VideoInHandleId.toString());
      when(videoServerSessionRepository.update(any(VideoServerSession.class)))
          .thenReturn(videoServerSession1Updated);

      VideoRoomResponse joinSubscriberResponse = VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()),
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
          .sendVideoServerRequest(
              eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
              any(VideoServerMessageRequest.class));
      verify(videoServerSessionRepository, times(1)).update(any(VideoServerSession.class));
      verify(videoServerClient, times(1))
          .sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }

    @Test
    @DisplayName(
        "Try to update subscriptions for media stream on a meeting but video server returns error"
            + " updating subscriptions")
    void updateSubscriptionsMediaStream_testErrorUpdatingSubscriptions() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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

      VideoRoomResponse updateSubscriptionsMediaStreamResponse =
          VideoRoomResponse.create().status("error");
      when(videoServerClient.sendVideoRoomRequest(
              eq(
                  videoServerURL
                      + janusEndpoint
                      + "/"
                      + user1SessionId.toString()
                      + "/"
                      + user1VideoInHandleId.toString()),
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
                      + user1VideoInHandleId.toString()),
              any(VideoServerMessageRequest.class));
    }
  }

  @Nested
  @DisplayName("Offer rtc audio stream tests")
  class OfferRtcAudioStreamTests {

    @Test
    @DisplayName("send offer for audio stream")
    void offerRtcAudioStream_testOk() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1AudioHandleId.toString()),
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
                      + user1AudioHandleId.toString()),
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
      assertFalse(audioBridgeJoinRequest.getFilename().isEmpty());
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
      createVideoServerMeeting(meeting1Id);

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
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
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
                      + user1AudioHandleId.toString()),
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
                      + user1AudioHandleId.toString()),
              any(VideoServerMessageRequest.class));
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
}
