// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeLeaveRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeMuteRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomDestroyRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomJoinRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomLeaveRequest;
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
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceJanus;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import com.zextras.carbonio.meeting.model.MediaStreamDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto.TypeEnum;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
public class VideoServerServiceImplTest {

  private final ObjectMapper                 objectMapper;
  private final HttpClient                   httpClient;
  private final VideoServerMeetingRepository videoServerMeetingRepository;
  private final VideoServerSessionRepository videoServerSessionRepository;
  private final VideoServerService           videoServerService;

  public VideoServerServiceImplTest() {
    AppConfig appConfig = mock(AppConfig.class);
    this.objectMapper = new ObjectMapper();
    this.httpClient = mock(HttpClient.class);
    this.videoServerMeetingRepository = mock(VideoServerMeetingRepository.class);
    this.videoServerSessionRepository = mock(VideoServerSessionRepository.class);

    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST)).thenReturn(Optional.of("127.0.0.1"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT)).thenReturn(Optional.of("8088"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_ADMIN_PORT)).thenReturn(Optional.of("7088"));
    when(appConfig.get(String.class, ConfigName.VIDEO_SERVER_TOKEN)).thenReturn(Optional.of("token"));

    this.videoServerService = new VideoServerServiceJanus(
      appConfig,
      objectMapper,
      httpClient,
      videoServerMeetingRepository,
      videoServerSessionRepository
    );
  }

  private UUID   meeting1Id;
  private UUID   meeting1SessionId;
  private UUID   meeting1AudioHandleId;
  private UUID   meeting1VideoHandleId;
  private UUID   meeting1AudioRoomId;
  private UUID   meeting1VideoRoomId;
  private UUID   user1Id;
  private UUID   user2Id;
  private UUID   user3Id;
  private UUID   queue1Id;
  private UUID   queue2Id;
  private UUID   queue3Id;
  private UUID   user1SessionId;
  private UUID   user2SessionId;
  private UUID   user3SessionId;
  private UUID   user1AudioHandleId;
  private UUID   user2AudioHandleId;
  private UUID   user3AudioHandleId;
  private UUID   user1VideoOutHandleId;
  private UUID   user2VideoOutHandleId;
  private UUID   user3VideoOutHandleId;
  private UUID   user1VideoInHandleId;
  private UUID   user2VideoInHandleId;
  private UUID   user3VideoInHandleId;
  private UUID   user1ScreenHandleId;
  private UUID   user2ScreenHandleId;
  private UUID   user3ScreenHandleId;
  private String videoServerURL;
  private String janusEndpoint;

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
    user2AudioHandleId = UUID.randomUUID();
    user3AudioHandleId = UUID.randomUUID();
    user1VideoOutHandleId = UUID.randomUUID();
    user2VideoOutHandleId = UUID.randomUUID();
    user3VideoOutHandleId = UUID.randomUUID();
    user1VideoInHandleId = UUID.randomUUID();
    user2VideoInHandleId = UUID.randomUUID();
    user3VideoInHandleId = UUID.randomUUID();
    user1ScreenHandleId = UUID.randomUUID();
    user2ScreenHandleId = UUID.randomUUID();
    user3ScreenHandleId = UUID.randomUUID();
    videoServerURL = "http://127.0.0.1:8088";
    janusEndpoint = "/janus";
  }

  @AfterEach
  public void cleanup() {
    verifyNoMoreInteractions(httpClient);
    verifyNoMoreInteractions(videoServerMeetingRepository);
    verifyNoMoreInteractions(videoServerSessionRepository);
    reset(
      httpClient,
      videoServerMeetingRepository,
      videoServerSessionRepository
    );
  }

  private CloseableHttpResponse mockResponse(Object bodyResponse) throws IOException {
    CloseableHttpResponse sessionResponse = mock(CloseableHttpResponse.class);
    StatusLine sessionStatusLine = mock(StatusLine.class);
    HttpEntity sessionHttpEntity = mock(HttpEntity.class);
    when(sessionResponse.getStatusLine()).thenReturn(sessionStatusLine);
    when(sessionResponse.getEntity()).thenReturn(sessionHttpEntity);
    when(sessionStatusLine.getStatusCode()).thenReturn(200);
    when(sessionHttpEntity.getContent()).thenReturn(
      new ByteArrayInputStream(objectMapper.writeValueAsString(bodyResponse).getBytes(StandardCharsets.UTF_8)));
    return sessionResponse;
  }

  private VideoServerMeeting createVideoServerMeeting(UUID meetingId) {
    VideoServerMeeting videoServerMeeting = VideoServerMeeting.create()
      .meetingId(meetingId.toString())
      .connectionId(meeting1SessionId.toString())
      .audioHandleId(meeting1AudioHandleId.toString())
      .videoHandleId(meeting1VideoHandleId.toString())
      .audioRoomId(meeting1AudioRoomId.toString())
      .videoRoomId(meeting1VideoRoomId.toString());
    when(videoServerMeetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(videoServerMeeting));
    return videoServerMeeting;
  }

  @Nested
  @DisplayName("Start Meeting tests")
  class StartMeetingTests {

    @Test
    @DisplayName("Start a new meeting on a room")
    void startMeeting_testOk() throws IOException {
      CloseableHttpResponse sessionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(meeting1SessionId.toString())));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(sessionResponse);

      CloseableHttpResponse audioHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(meeting1AudioHandleId.toString())));

      CloseableHttpResponse videoHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(meeting1VideoHandleId.toString())));

      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioHandleResponse, videoHandleResponse);

      CloseableHttpResponse audioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1AudioHandleId.toString())
        .pluginData(
          AudioBridgePluginData.create()
            .plugin("janus.plugin.audiobridge")
            .dataInfo(AudioBridgeDataInfo.create().audioBridge("created").room(meeting1AudioRoomId.toString())
              .permanent(false))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioRoomResponse);

      CloseableHttpResponse videoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1VideoHandleId.toString())
        .pluginData(
          VideoRoomPluginData.create()
            .plugin("janus.plugin.videoroom")
            .dataInfo(
              VideoRoomDataInfo.create().videoRoom("created").room(meeting1VideoRoomId.toString()).permanent(false))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1VideoHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(videoRoomResponse);

      videoServerService.startMeeting(meeting1Id.toString());

      ArgumentCaptor<String> createConnectionJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> createHandleJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> createAudioRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> createVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        createConnectionJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        createHandleJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        createAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1VideoHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        createVideoRoomJsonBody.capture()
      );
      verify(videoServerMeetingRepository, times(1)).insert(
        meeting1Id.toString(),
        meeting1SessionId.toString(),
        meeting1AudioHandleId.toString(),
        meeting1VideoHandleId.toString(),
        meeting1AudioRoomId.toString(),
        meeting1VideoRoomId.toString()
      );

      assertEquals(1, createConnectionJsonBody.getAllValues().size());
      assertEquals(VideoServerMessageRequest.create().messageRequest("create").apiSecret("token"),
        objectMapper.readValue(createConnectionJsonBody.getValue(), VideoServerMessageRequest.class));

      assertEquals(2, createHandleJsonBody.getAllValues().size());
      VideoServerMessageRequest audioHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("attach")
        .pluginName("janus.plugin.audiobridge")
        .apiSecret("token"), audioHandleMessageRequest);
      VideoServerMessageRequest videoHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("attach")
        .pluginName("janus.plugin.videoroom")
        .apiSecret("token"), videoHandleMessageRequest);

      assertEquals(1, createAudioRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest audioRoomMessageRequest = objectMapper.readValue(createAudioRoomJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
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
            .audioLevelAverage(55)
            .audioLevelEvent(true)), audioRoomMessageRequest);

      assertEquals(1, createVideoRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest videoRoomMessageRequest = objectMapper.readValue(createVideoRoomJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
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
            .bitrate(200L)
            .bitrateCap(true)
            .videoCodec("vp8,h264,vp9,h265,av1")), videoRoomMessageRequest);
    }

    @Test
    @DisplayName("Try to start a meeting that is already active")
    void startMeeting_testErrorAlreadyActive() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class, () -> videoServerService.startMeeting(meeting1Id.toString()),
        "Videoserver meeting " + meeting1Id.toString() + " is already active");

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Stop Meeting tests")
  class StopMeetingTests {

    @Test
    @DisplayName("Stop an existing meeting")
    void stopMeeting_testOk() throws IOException {
      createVideoServerMeeting(meeting1Id);

      CloseableHttpResponse destroyVideoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1VideoHandleId.toString())
        .pluginData(VideoRoomPluginData.create().plugin("janus.plugin.videoroom")
          .dataInfo(VideoRoomDataInfo.create().videoRoom("destroyed"))));

      CloseableHttpResponse destroyVideoRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1VideoHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1VideoHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyVideoRoomResponse, destroyVideoRoomPluginResponse);

      CloseableHttpResponse destroyAudioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1AudioHandleId.toString())
        .pluginData(AudioBridgePluginData.create().plugin("janus.plugin.audiobridge")
          .dataInfo(AudioBridgeDataInfo.create().audioBridge("destroyed"))));

      CloseableHttpResponse destroyAudioBridgePluginResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(meeting1AudioHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyAudioRoomResponse, destroyAudioBridgePluginResponse);

      CloseableHttpResponse destroyConnectionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(meeting1SessionId.toString())
        .transactionId("transaction-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyConnectionResponse);

      videoServerService.stopMeeting(meeting1Id.toString());

      ArgumentCaptor<String> destroyVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destroyAudioRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destroyConnectionJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1VideoHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        destroyVideoRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        destroyAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        destroyConnectionJsonBody.capture()
      );
      verify(videoServerMeetingRepository, times(1)).deleteById(meeting1Id.toString());

      assertEquals(2, destroyVideoRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest destroyVideoRoomRequest = objectMapper.readValue(
        destroyVideoRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(VideoRoomDestroyRequest.create()
          .request("destroy")
          .room(meeting1VideoRoomId.toString())
          .permanent(false)), destroyVideoRoomRequest);
      VideoServerMessageRequest destroyVideoRoomPluginRequest = objectMapper.readValue(
        destroyVideoRoomJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyVideoRoomPluginRequest);

      assertEquals(2, destroyAudioRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest destroyAudioRoomRequest = objectMapper.readValue(
        destroyAudioRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(AudioBridgeDestroyRequest.create()
          .request("destroy")
          .room(meeting1AudioRoomId.toString())
          .permanent(false)), destroyAudioRoomRequest);
      VideoServerMessageRequest destroyAudioBridgePluginRequest = objectMapper.readValue(
        destroyAudioRoomJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyAudioBridgePluginRequest);

      assertEquals(1, destroyConnectionJsonBody.getAllValues().size());
      VideoServerMessageRequest destroyConnectionRequest = objectMapper.readValue(
        destroyConnectionJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("destroy")
        .apiSecret("token"), destroyConnectionRequest);
    }

    @Test
    @DisplayName("Try to stop a meeting that does not exist")
    void stopMeeting_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class, () -> videoServerService.stopMeeting(meeting1Id.toString()),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Join Meeting tests")
  class JoinMeetingTests {

    @Test
    @DisplayName("join an existing meeting")
    void joinMeeting_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);

      CloseableHttpResponse sessionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(user1SessionId.toString())));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(sessionResponse);

      CloseableHttpResponse videoOutHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(user1VideoOutHandleId.toString())));

      CloseableHttpResponse screenHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id(user1ScreenHandleId.toString())));

      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(videoOutHandleResponse, screenHandleResponse);

      CloseableHttpResponse joinPublisherVideoResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoOutHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(joinPublisherVideoResponse);

      CloseableHttpResponse joinPublisherScreenResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1ScreenHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(joinPublisherScreenResponse);

      videoServerService.joinMeeting(user1Id.toString(), queue1Id.toString(), meeting1Id.toString(), false, true);

      ArgumentCaptor<String> createConnectionJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> createHandleJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> joinPublisherVideoJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> joinPublisherScreenJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        createConnectionJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        createHandleJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        joinPublisherVideoJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        joinPublisherScreenJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).insert(
        videoServerMeeting,
        user1Id.toString(),
        queue1Id.toString(),
        user1SessionId.toString(),
        user1VideoOutHandleId.toString(),
        user1ScreenHandleId.toString()
      );

      assertEquals(1, createConnectionJsonBody.getAllValues().size());
      assertEquals(VideoServerMessageRequest.create().messageRequest("create").apiSecret("token"),
        objectMapper.readValue(createConnectionJsonBody.getValue(), VideoServerMessageRequest.class));

      assertEquals(2, createHandleJsonBody.getAllValues().size());
      VideoServerMessageRequest videoOutHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("attach")
        .pluginName("janus.plugin.videoroom")
        .apiSecret("token"), videoOutHandleMessageRequest);
      VideoServerMessageRequest screenHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("attach")
        .pluginName("janus.plugin.videoroom")
        .apiSecret("token"), screenHandleMessageRequest);

      assertEquals(1, joinPublisherVideoJsonBody.getAllValues().size());
      VideoServerMessageRequest joinPublisherVideoRequest = objectMapper.readValue(
        joinPublisherVideoJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomJoinRequest.create()
            .request("join")
            .ptype("publisher")
            .room(meeting1VideoRoomId.toString())
            .id(user1Id.toString() + "/video")), joinPublisherVideoRequest);

      assertEquals(1, joinPublisherScreenJsonBody.getAllValues().size());
      VideoServerMessageRequest joinPublisherScreenRequest = objectMapper.readValue(
        joinPublisherScreenJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomJoinRequest.create()
            .request("join")
            .ptype("publisher")
            .room(meeting1VideoRoomId.toString())
            .id(user1Id.toString() + "/screen")), joinPublisherScreenRequest);
    }

    @Test
    @DisplayName("Try to join a meeting that does not exist")
    void joinMeeting_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.joinMeeting(user1Id.toString(), queue1Id.toString(), meeting1Id.toString(),
          false, true),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to join a meeting already joined")
    void joinMeeting_testErrorAlreadyJoined() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      assertThrows(VideoServerException.class,
        () -> videoServerService.joinMeeting(user1Id.toString(), queue1Id.toString(), meeting1Id.toString(),
          false, true),
        "Videoserver session user with user  " + user1Id.toString() +
          "is already present in the videoserver meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Leave Meeting tests")
  class LeaveMeetingTests {

    @Test
    @DisplayName("leave a meeting previously joined")
    void leaveMeeting_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .audioHandleId(user1AudioHandleId.toString())
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoInHandleId(user1VideoInHandleId.toString())
        .screenHandleId(user1ScreenHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse leaveAudioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1AudioHandleId.toString()));
      CloseableHttpResponse destroyAudioBridgePluginResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1AudioHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveAudioRoomResponse, destroyAudioBridgePluginResponse);

      CloseableHttpResponse leaveVideoOutRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoOutHandleId.toString()));
      CloseableHttpResponse destroyVideoOutRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoOutHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoOutRoomResponse, destroyVideoOutRoomPluginResponse);

      CloseableHttpResponse leaveVideoInRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      CloseableHttpResponse destroyVideoInRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoInRoomResponse, destroyVideoInRoomPluginResponse);

      CloseableHttpResponse leaveVideoScreenResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1ScreenHandleId.toString()));
      CloseableHttpResponse destroyVideoScreenPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1ScreenHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoScreenResponse, destroyVideoScreenPluginResponse);

      CloseableHttpResponse destroyConnectionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyConnectionResponse);

      videoServerService.leaveMeeting(user1Id.toString(), meeting1Id.toString());

      ArgumentCaptor<String> leaveAudioRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> leaveVideoInRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> leaveVideoOutRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> leaveVideoScreenJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destroyConnectionJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        leaveAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        leaveVideoOutRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        leaveVideoInRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        leaveVideoScreenJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()),
        eq(Map.of("content-type", "application/json")),
        destroyConnectionJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).remove(videoServerSession);

      assertEquals(2, leaveAudioRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest leaveAudioRoomRequest = objectMapper.readValue(
        leaveAudioRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          AudioBridgeLeaveRequest.create().request("leave")), leaveAudioRoomRequest);
      VideoServerMessageRequest destroyAudioBridgePluginRequest = objectMapper.readValue(
        leaveAudioRoomJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyAudioBridgePluginRequest);

      assertEquals(2, leaveVideoOutRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest leaveVideoOutRoomRequest = objectMapper.readValue(
        leaveVideoOutRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomLeaveRequest.create().request("leave")), leaveVideoOutRoomRequest);
      VideoServerMessageRequest destroyVideoOutRoomPluginRequest = objectMapper.readValue(
        leaveVideoOutRoomJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyVideoOutRoomPluginRequest);

      assertEquals(2, leaveVideoInRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest leaveVideoInRoomRequest = objectMapper.readValue(
        leaveVideoInRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomLeaveRequest.create().request("leave")), leaveVideoInRoomRequest);
      VideoServerMessageRequest destroyVideoInRoomPluginRequest = objectMapper.readValue(
        leaveVideoInRoomJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyVideoInRoomPluginRequest);

      assertEquals(2, leaveVideoScreenJsonBody.getAllValues().size());
      VideoServerMessageRequest leaveVideoScreenRequest = objectMapper.readValue(
        leaveVideoScreenJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomLeaveRequest.create().request("leave")), leaveVideoScreenRequest);
      VideoServerMessageRequest destroyVideoScreenPluginRequest = objectMapper.readValue(
        leaveVideoScreenJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("detach")
        .apiSecret("token"), destroyVideoScreenPluginRequest);

      assertEquals(1, destroyConnectionJsonBody.getAllValues().size());
      VideoServerMessageRequest destroyConnectionRequest = objectMapper.readValue(
        destroyConnectionJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("destroy")
        .apiSecret("token"), destroyConnectionRequest);
    }

    @Test
    @DisplayName("Try to leave a meeting that does not exist")
    void leaveMeeting_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.leaveMeeting(user1Id.toString(), meeting1Id.toString()),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to leave a meeting that is not joined previously")
    void leaveMeeting_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.leaveMeeting(user1Id.toString(), meeting1Id.toString()),
        "No Videoserver session user found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Update media stream tests")
  class UpdateMediaStreamTests {

    @Test
    @DisplayName("enable video stream in a meeting")
    void updateMediaStream_testEnableVideoOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse publishStreamVideoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoOutHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(publishStreamVideoRoomResponse);

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol"));

      ArgumentCaptor<String> publishStreamVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoOutHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        publishStreamVideoRoomJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest = objectMapper.readValue(
        publishStreamVideoRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomPublishRequest.create().request("publish")).rtcSessionDescription(RtcSessionDescription.create()
          .type(RtcType.OFFER).sdp("session-description-protocol")), publishStreamVideoRoomRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(true), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("enable screen stream in a meeting")
    void updateMediaStream_testEnableScreenOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .screenHandleId(user1ScreenHandleId.toString())
        .screenStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse publishStreamVideoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1ScreenHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(publishStreamVideoRoomResponse);

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("session-description-protocol"));

      ArgumentCaptor<String> publishStreamVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1ScreenHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        publishStreamVideoRoomJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, publishStreamVideoRoomJsonBody.getAllValues().size());
      VideoServerMessageRequest publishStreamVideoRoomRequest = objectMapper.readValue(
        publishStreamVideoRoomJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomPublishRequest.create().request("publish")).rtcSessionDescription(RtcSessionDescription.create()
          .type(RtcType.OFFER).sdp("session-description-protocol")), publishStreamVideoRoomRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .screenHandleId(user1ScreenHandleId.toString())
        .screenStreamOn(true), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("disable video stream in a meeting")
    void updateMediaStream_testDisableVideoOk() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false).sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(false), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("disable screen stream in a meeting")
    void updateMediaStream_testDisableScreenOk() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .screenHandleId(user1ScreenHandleId.toString())
        .screenStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false).sdp("session-description-protocol"));

      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .screenHandleId(user1ScreenHandleId.toString())
        .screenStreamOn(false), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("Try to update media stream on a meeting that does not exist")
    void updateMediaStream_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol")),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to update media stream on a meeting that is not joined previously")
    void updateMediaStream_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol")),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to enable video stream on a meeting when it is already enabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyEnabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to enable screen stream on a meeting when it is already enabled")
    void updateMediaStream_testIgnoreScreenStreamAlreadyEnabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .screenHandleId(user1ScreenHandleId.toString())
        .screenStreamOn(true);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(true).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable video stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyDisabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .videoOutHandleId(user1VideoOutHandleId.toString())
        .videoOutStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable screen stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreScreenStreamAlreadyDisabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .screenHandleId(user1VideoOutHandleId.toString())
        .screenStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("enable audio stream in a meeting")
  class UpdateAudioStreamTests {

    @Test
    @DisplayName("enable audio stream test")
    void updateAudioStream_testEnableOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .audioHandleId(user1AudioHandleId.toString())
        .audioStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse updateAudioStreamResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1AudioHandleId.toString())
        .pluginData(
          AudioBridgePluginData.create()
            .plugin("janus.plugin.audiobridge")
            .dataInfo(AudioBridgeDataInfo.create().audioBridge("success"))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateAudioStreamResponse);

      videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true);

      ArgumentCaptor<String> updateAudioStreamJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + meeting1SessionId.toString()
          + "/" + meeting1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        updateAudioStreamJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, updateAudioStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest updateAudioStreamRequest = objectMapper.readValue(
        updateAudioStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          AudioBridgeMuteRequest.create()
            .request("unmute")
            .room(meeting1AudioRoomId.toString())
            .id(user1Id.toString())), updateAudioStreamRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .audioHandleId(user1AudioHandleId.toString())
        .audioStreamOn(true), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("Try to update audio stream on a meeting that does not exist")
    void updateAudioStream_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to update audio stream on a meeting that is not joined previously")
    void updateAudioStream_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true),
        "No Videoserver session found for user " + user1Id.toString() + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to enable audio stream on a meeting when it is already enabled")
    void updateAudioStream_testIgnoreAudioStreamAlreadyEnabled() {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
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
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
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
    void answerRtcMediaStream_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoInHandleId(user1VideoInHandleId.toString())
        .videoInStreamOn(false);
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse answerRtcMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(answerRtcMediaStreamResponse);

      videoServerService.answerRtcMediaStream(user1Id.toString(), meeting1Id.toString(),
        "session-description-protocol");

      ArgumentCaptor<String> answerRtcMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        answerRtcMediaStreamJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).update(videoServerSessionCaptor.capture());

      assertEquals(1, answerRtcMediaStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest answerRtcMediaStreamRequest = objectMapper.readValue(
        answerRtcMediaStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
        .messageRequest("message")
        .apiSecret("token")
        .videoServerPluginRequest(
          VideoRoomStartVideoInRequest.create()
            .request("start"))
        .rtcSessionDescription(RtcSessionDescription.create()
          .type(RtcType.ANSWER).sdp("session-description-protocol")), answerRtcMediaStreamRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSessionUpdated = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoInHandleId(user1VideoInHandleId.toString())
        .videoInStreamOn(true), videoServerSessionUpdated);
    }

    @Test
    @DisplayName("Try to send answer for media stream on a meeting that does not exist")
    void answerRtcMediaStream_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.answerRtcMediaStream(user1Id.toString(), meeting1Id.toString(),
          "session-description-protocol"),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to send answer for media stream on a meeting that is not joined previously")
    void answerRtcMediaStream_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.answerRtcMediaStream(user1Id.toString(), meeting1Id.toString(),
          "session-description-protocol"),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Update subscriptions media stream tests")
  class UpdateSubscriptionsMediaStreamTests {

    @Test
    @DisplayName("subscribe to another participant stream video")
    void updateSubscriptionsMediaStream_testSubscribeOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession1 = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoInHandleId(user1VideoInHandleId.toString());
      VideoServerSession videoServerSession2 = VideoServerSession.create().userId(user2Id.toString())
        .queueId(queue2Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user2SessionId.toString())
        .videoOutHandleId(user2VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      CloseableHttpResponse updateSubscriptionsMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
        SubscriptionUpdatesDto.create()
          .subscribe(List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString()))));

      ArgumentCaptor<String> updateSubscriptionsMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        updateSubscriptionsMediaStreamJsonBody.capture()
      );

      assertEquals(1, updateSubscriptionsMediaStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest = objectMapper.readValue(
        updateSubscriptionsMediaStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
          .messageRequest("message")
          .apiSecret("token")
          .videoServerPluginRequest(
            VideoRoomUpdateSubscriptionsRequest.create()
              .request("update").subscriptions(List.of(Stream.create()
                .feed(Feed.create().type(MediaType.VIDEO).userId(user2Id.toString()).toString())))),
        updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("unsubscribe from another participant stream video")
    void updateSubscriptionsMediaStream_testUnSubscribeOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession1 = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoInHandleId(user1VideoInHandleId.toString());
      VideoServerSession videoServerSession2 = VideoServerSession.create().userId(user2Id.toString())
        .queueId(queue2Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user2SessionId.toString())
        .videoOutHandleId(user2VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2));

      CloseableHttpResponse updateSubscriptionsMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
        SubscriptionUpdatesDto.create()
          .unsubscribe(
            List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString()))));

      ArgumentCaptor<String> updateSubscriptionsMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        updateSubscriptionsMediaStreamJsonBody.capture()
      );

      assertEquals(1, updateSubscriptionsMediaStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest = objectMapper.readValue(
        updateSubscriptionsMediaStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
          .messageRequest("message")
          .apiSecret("token")
          .videoServerPluginRequest(
            VideoRoomUpdateSubscriptionsRequest.create()
              .request("update").unsubscriptions(List.of(Stream.create()
                .feed(Feed.create().type(MediaType.VIDEO).userId(user2Id.toString()).toString())))),
        updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("subscribe and unsubscribe to and from other participants' stream video")
    void updateSubscriptionsMediaStream_testSubscribeAndUnSubscribeOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession1 = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .videoInHandleId(user1VideoInHandleId.toString());
      VideoServerSession videoServerSession2 = VideoServerSession.create().userId(user2Id.toString())
        .queueId(queue2Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user2SessionId.toString())
        .videoOutHandleId(user2VideoInHandleId.toString());
      VideoServerSession videoServerSession3 = VideoServerSession.create().userId(user3Id.toString())
        .queueId(queue3Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user3SessionId.toString())
        .videoOutHandleId(user3VideoInHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession1, videoServerSession2, videoServerSession3));

      CloseableHttpResponse updateSubscriptionsMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1VideoInHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
        SubscriptionUpdatesDto.create()
          .subscribe(
            List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString())))
          .unsubscribe(
            List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user3Id.toString()))));

      ArgumentCaptor<String> updateSubscriptionsMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1VideoInHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        updateSubscriptionsMediaStreamJsonBody.capture()
      );

      assertEquals(1, updateSubscriptionsMediaStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest updateSubscriptionMediaStreamRequest = objectMapper.readValue(
        updateSubscriptionsMediaStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals(VideoServerMessageRequest.create()
          .messageRequest("message")
          .apiSecret("token")
          .videoServerPluginRequest(
            VideoRoomUpdateSubscriptionsRequest.create()
              .request("update")
              .subscriptions(List.of(Stream.create()
                .feed(Feed.create().type(MediaType.VIDEO).userId(user2Id.toString()).toString())))
              .unsubscriptions(List.of(Stream.create()
                .feed(Feed.create().type(MediaType.VIDEO).userId(user3Id.toString()).toString())))),
        updateSubscriptionMediaStreamRequest);
    }

    @Test
    @DisplayName("Try to update subscriptions for media stream on a meeting that does not exist")
    void updateSubscriptionsMediaStream_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
          SubscriptionUpdatesDto.create().subscribe(
            List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString())))),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to update subscriptions for media stream on a meeting that is not joined previously")
    void updateSubscriptionsMediaStream_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
          SubscriptionUpdatesDto.create().subscribe(
            List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString())))),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("Offer rtc audio stream tests")
  class OfferRtcAudioStreamTests {

    @Test
    @DisplayName("send offer for audio stream")
    void offerRtcAudioStream_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = createVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = VideoServerSession.create().userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId(user1SessionId.toString())
        .audioHandleId(user1AudioHandleId.toString());
      videoServerMeeting.videoServerSessions(List.of(videoServerSession));

      CloseableHttpResponse offerRtcAudioStreamResponse = mockResponse(AudioBridgeResponse.create()
        .status("ack")
        .connectionId(user1SessionId.toString())
        .transactionId("transaction-id")
        .handleId(user1AudioHandleId.toString()));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(offerRtcAudioStreamResponse);

      videoServerService.offerRtcAudioStream(user1Id.toString(), meeting1Id.toString(), "session-description-protocol");

      ArgumentCaptor<String> offerRtcAudioStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/" + user1SessionId.toString()
          + "/" + user1AudioHandleId.toString()),
        eq(Map.of("content-type", "application/json")),
        offerRtcAudioStreamJsonBody.capture()
      );

      assertEquals(1, offerRtcAudioStreamJsonBody.getAllValues().size());
      VideoServerMessageRequest offerRtcAudioStreamRequest = objectMapper.readValue(
        offerRtcAudioStreamJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals("message", offerRtcAudioStreamRequest.getMessageRequest());
      assertEquals("token", offerRtcAudioStreamRequest.getApiSecret());
      AudioBridgeJoinRequest audioBridgeJoinRequest = (AudioBridgeJoinRequest) offerRtcAudioStreamRequest.getVideoServerPluginRequest();
      assertEquals("join", audioBridgeJoinRequest.getRequest());
      assertEquals(meeting1AudioRoomId.toString(), audioBridgeJoinRequest.getRoom());
      assertTrue(audioBridgeJoinRequest.getMuted());
      assertFalse(audioBridgeJoinRequest.getFilename().isEmpty());
    }

    @Test
    @DisplayName("Try to send offer for audio stream on a meeting that does not exist")
    void offerRtcAudioStream_testErrorMeetingNotExists() {
      assertThrows(VideoServerException.class,
        () -> videoServerService.offerRtcAudioStream(user1Id.toString(), meeting1Id.toString(),
          "session-description-protocol"),
        "No videoserver meeting found for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to send offer for audio stream on a meeting that is not joined previously")
    void offerRtcAudioStream_testErrorMeetingNotJoined() {
      createVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.offerRtcAudioStream(user1Id.toString(), meeting1Id.toString(),
          "session-description-protocol"),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }
}
