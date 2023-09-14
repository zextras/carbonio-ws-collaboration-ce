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
  private UUID   user1Id;
  private UUID   user2Id;
  private UUID   queue1Id;
  private UUID   queue2Id;
  private String videoServerURL;
  private String janusEndpoint;

  @BeforeEach
  public void init() {
    meeting1Id = UUID.randomUUID();
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    queue1Id = UUID.randomUUID();
    queue2Id = UUID.randomUUID();
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

  private VideoServerMeeting mockVideoServerMeeting(UUID meetingId) {
    VideoServerMeeting videoServerMeeting = VideoServerMeeting.create()
      .meetingId(meetingId.toString())
      .connectionId("session-id")
      .videoHandleId("video-handle-id")
      .audioHandleId("audio-handle-id")
      .videoRoomId("video-room-id")
      .audioRoomId("audio-room-id");
    when(videoServerMeetingRepository.getById(meetingId.toString())).thenReturn(Optional.of(videoServerMeeting));
    return videoServerMeeting;
  }

  private VideoServerSession mockVideoServerSession(VideoServerMeeting videoServerMeeting, UUID userId, UUID queueId,
    int userIndex) {
    VideoServerSession videoServerSession = VideoServerSession.create()
      .userId(userId.toString())
      .queueId(queueId.toString())
      .videoServerMeeting(videoServerMeeting)
      .connectionId(String.format("user%d-session-id", userIndex))
      .audioHandleId(String.format("user%d-audio-handle-id", userIndex))
      .videoInHandleId(String.format("user%d-video-in-handle-id", userIndex))
      .videoOutHandleId(String.format("user%d-video-out-handle-id", userIndex))
      .screenHandleId(String.format("user%d-screen-handle-id", userIndex));
    videoServerMeeting.getVideoServerSessions().add(videoServerSession);
    return videoServerSession;
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
        .data(VideoServerDataInfo.create().id("session-id")));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(sessionResponse);

      CloseableHttpResponse audioHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id("audio-handle-id")));

      CloseableHttpResponse videoHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id("video-handle-id")));

      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioHandleResponse, videoHandleResponse);

      CloseableHttpResponse audioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("audio-handle-id")
        .pluginData(
          AudioBridgePluginData.create()
            .plugin("janus.plugin.audiobridge")
            .dataInfo(AudioBridgeDataInfo.create().audioBridge("created").room("audio-room-id").permanent(false))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioRoomResponse);

      CloseableHttpResponse videoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("video-handle-id")
        .pluginData(
          VideoRoomPluginData.create()
            .plugin("janus.plugin.videoroom")
            .dataInfo(VideoRoomDataInfo.create().videoRoom("created").room("video-room-id").permanent(false))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/video-handle-id"),
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
        eq(videoServerURL + janusEndpoint + "/session-id"),
        eq(Map.of("content-type", "application/json")),
        createHandleJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        createAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/video-handle-id"),
        eq(Map.of("content-type", "application/json")),
        createVideoRoomJsonBody.capture()
      );
      verify(videoServerMeetingRepository, times(1)).insert(
        meeting1Id.toString(),
        "session-id",
        "audio-handle-id",
        "video-handle-id",
        "audio-room-id",
        "video-room-id"
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
      mockVideoServerMeeting(meeting1Id);

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
      mockVideoServerMeeting(meeting1Id);

      CloseableHttpResponse destroyVideoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("video-handle-id")
        .pluginData(VideoRoomPluginData.create().plugin("janus.plugin.videoroom")
          .dataInfo(VideoRoomDataInfo.create().videoRoom("destroyed"))));

      CloseableHttpResponse destroyVideoRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("video-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/video-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyVideoRoomResponse, destroyVideoRoomPluginResponse);

      CloseableHttpResponse destroyAudioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("audio-handle-id")
        .pluginData(AudioBridgePluginData.create().plugin("janus.plugin.audiobridge")
          .dataInfo(AudioBridgeDataInfo.create().audioBridge("destroyed"))));

      CloseableHttpResponse destroyAudioBridgePluginResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("audio-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyAudioRoomResponse, destroyAudioBridgePluginResponse);

      CloseableHttpResponse destroyConnectionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(destroyConnectionResponse);

      videoServerService.stopMeeting(meeting1Id.toString());

      ArgumentCaptor<String> destroyVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destroyAudioRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> destroyConnectionJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/video-handle-id"),
        eq(Map.of("content-type", "application/json")),
        destroyVideoRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        destroyAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id"),
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
          .room("video-room-id")
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
          .room("audio-room-id")
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
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);

      CloseableHttpResponse sessionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id("user-session-id")));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(sessionResponse);

      CloseableHttpResponse videoOutHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("user-session-id")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id("user-video-out-handle-id")));

      CloseableHttpResponse screenHandleResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("user-session-id")
        .transactionId("transaction-id")
        .data(VideoServerDataInfo.create().id("user-screen-handle-id")));

      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user-session-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(videoOutHandleResponse, screenHandleResponse);

      CloseableHttpResponse joinPublisherVideoResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user-session-id")
        .transactionId("transaction-id")
        .handleId("user-video-out-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user-session-id" + "/user-video-out-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(joinPublisherVideoResponse);

      CloseableHttpResponse joinPublisherScreenResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user-session-id")
        .transactionId("transaction-id")
        .handleId("user-screen-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user-session-id" + "/user-screen-handle-id"),
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
        eq(videoServerURL + janusEndpoint + "/user-session-id"),
        eq(Map.of("content-type", "application/json")),
        createHandleJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user-session-id" + "/user-video-out-handle-id"),
        eq(Map.of("content-type", "application/json")),
        joinPublisherVideoJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user-session-id" + "/user-screen-handle-id"),
        eq(Map.of("content-type", "application/json")),
        joinPublisherScreenJsonBody.capture()
      );
      verify(videoServerSessionRepository, times(1)).insert(
        videoServerMeeting,
        user1Id.toString(),
        queue1Id.toString(),
        "user-session-id",
        "user-video-out-handle-id",
        "user-screen-handle-id"
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
            .room("video-room-id")
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
            .room("video-room-id")
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
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

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
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      VideoServerSession videoServerSession = mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      CloseableHttpResponse leaveAudioRoomResponse = mockResponse(AudioBridgeResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-audio-handle-id"));
      CloseableHttpResponse destroyAudioBridgePluginResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-audio-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveAudioRoomResponse, destroyAudioBridgePluginResponse);

      CloseableHttpResponse leaveVideoOutRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-out-handle-id"));
      CloseableHttpResponse destroyVideoOutRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-out-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-out-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoOutRoomResponse, destroyVideoOutRoomPluginResponse);

      CloseableHttpResponse leaveVideoInRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-in-handle-id"));
      CloseableHttpResponse destroyVideoInRoomPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-in-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoInRoomResponse, destroyVideoInRoomPluginResponse);

      CloseableHttpResponse leaveVideoScreenResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-screen-handle-id"));
      CloseableHttpResponse destroyVideoScreenPluginResponse = mockResponse(VideoRoomResponse.create()
        .status("success")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-screen-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-screen-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(leaveVideoScreenResponse, destroyVideoScreenPluginResponse);

      CloseableHttpResponse destroyConnectionResponse = mockResponse(VideoServerResponse.create()
        .status("success")
        .connectionId("user1-session-id")
        .transactionId("transaction-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id"),
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
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        leaveAudioRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-out-handle-id"),
        eq(Map.of("content-type", "application/json")),
        leaveVideoOutRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
        eq(Map.of("content-type", "application/json")),
        leaveVideoInRoomJsonBody.capture()
      );
      verify(httpClient, times(2)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-screen-handle-id"),
        eq(Map.of("content-type", "application/json")),
        leaveVideoScreenJsonBody.capture()
      );
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id"),
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
      mockVideoServerMeeting(meeting1Id);

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
    void updateMediaStream_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      CloseableHttpResponse publishStreamVideoRoomResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-out-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-out-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(publishStreamVideoRoomResponse);

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol"));

      ArgumentCaptor<String> publishStreamVideoRoomJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-out-handle-id"),
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
      VideoServerSession videoServerSession = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId("user1-session-id")
        .audioHandleId("user1-audio-handle-id")
        .videoInHandleId("user1-video-in-handle-id")
        .videoOutHandleId("user1-video-out-handle-id")
        .screenHandleId("user1-screen-handle-id")
        .videoOutStreamOn(true), videoServerSession);
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
      mockVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
          MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(true).sdp("session-description-protocol")),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable video stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreVideoStreamAlreadyDisabled() {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.VIDEO).enabled(false).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable screen stream on a meeting when it is already disabled")
    void updateMediaStream_testIgnoreScreenStreamAlreadyDisabled() {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      videoServerService.updateMediaStream(user1Id.toString(), meeting1Id.toString(),
        MediaStreamSettingsDto.create().type(TypeEnum.SCREEN).enabled(false).sdp("session-description-protocol"));

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }

  @Nested
  @DisplayName("enable audio stream in a meeting")
  class UpdateAudioStreamTests {

    @Test
    @DisplayName("update audio stream test")
    void updateAudioStream_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      CloseableHttpResponse updateAudioStreamResponse = mockResponse(AudioBridgeResponse.create()
        .status("success")
        .connectionId("session-id")
        .transactionId("transaction-id")
        .handleId("audio-handle-id")
        .pluginData(
          AudioBridgePluginData.create()
            .plugin("janus.plugin.audiobridge")
            .dataInfo(AudioBridgeDataInfo.create().audioBridge("success"))));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateAudioStreamResponse);

      videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true);

      ArgumentCaptor<String> updateAudioStreamJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
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
            .room("audio-room-id")
            .id(user1Id.toString())), updateAudioStreamRequest);

      assertEquals(1, videoServerSessionCaptor.getAllValues().size());
      VideoServerSession videoServerSession = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId("user1-session-id")
        .audioHandleId("user1-audio-handle-id")
        .videoInHandleId("user1-video-in-handle-id")
        .videoOutHandleId("user1-video-out-handle-id")
        .screenHandleId("user1-screen-handle-id")
        .audioStreamOn(true), videoServerSession);
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
      mockVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.updateAudioStream(user1Id.toString(), meeting1Id.toString(), true),
        "No Videoserver session found for user " + user1Id.toString() + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }

    @Test
    @DisplayName("Try to disable audio stream on a meeting when it is already disabled")
    void updateAudioStream_testIgnoreAudioStreamAlreadyDisabled() {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

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
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      CloseableHttpResponse answerRtcMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-in-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(answerRtcMediaStreamResponse);

      videoServerService.answerRtcMediaStream(user1Id.toString(), meeting1Id.toString(),
        "session-description-protocol");

      ArgumentCaptor<String> answerRtcMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<VideoServerSession> videoServerSessionCaptor = ArgumentCaptor.forClass(VideoServerSession.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
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
      VideoServerSession videoServerSession = videoServerSessionCaptor.getAllValues().get(0);
      assertEquals(VideoServerSession.create()
        .userId(user1Id.toString())
        .queueId(queue1Id.toString())
        .videoServerMeeting(videoServerMeeting)
        .connectionId("user1-session-id")
        .audioHandleId("user1-audio-handle-id")
        .videoInHandleId("user1-video-in-handle-id")
        .videoOutHandleId("user1-video-out-handle-id")
        .screenHandleId("user1-screen-handle-id")
        .videoInStreamOn(true), videoServerSession);
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
      mockVideoServerMeeting(meeting1Id);

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
    void updateSubscriptionsMediaStream_testOk() throws IOException {
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);
      mockVideoServerSession(videoServerMeeting, user2Id, queue2Id, 2);

      CloseableHttpResponse updateSubscriptionsMediaStreamResponse = mockResponse(VideoRoomResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-video-in-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(updateSubscriptionsMediaStreamResponse);

      videoServerService.updateSubscriptionsMediaStream(user1Id.toString(), meeting1Id.toString(),
        SubscriptionUpdatesDto.create()
          .subscribe(List.of(MediaStreamDto.create().type(MediaStreamDto.TypeEnum.VIDEO).userId(user2Id.toString()))));

      ArgumentCaptor<String> updateSubscriptionsMediaStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-video-in-handle-id"),
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
      mockVideoServerMeeting(meeting1Id);

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
      VideoServerMeeting videoServerMeeting = mockVideoServerMeeting(meeting1Id);
      mockVideoServerSession(videoServerMeeting, user1Id, queue1Id, 1);

      CloseableHttpResponse offerRtcAudioStreamResponse = mockResponse(AudioBridgeResponse.create()
        .status("ack")
        .connectionId("user1-session-id")
        .transactionId("transaction-id")
        .handleId("user1-audio-handle-id"));
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(offerRtcAudioStreamResponse);

      videoServerService.offerRtcAudioStream(user1Id.toString(), meeting1Id.toString(), "session-description-protocol");

      ArgumentCaptor<String> offerRtcAudioStreamJsonBody = ArgumentCaptor.forClass(String.class);

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
      verify(httpClient, times(1)).sendPost(
        eq(videoServerURL + janusEndpoint + "/user1-session-id" + "/user1-audio-handle-id"),
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
      assertEquals("audio-room-id", audioBridgeJoinRequest.getRoom());
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
      mockVideoServerMeeting(meeting1Id);

      assertThrows(VideoServerException.class,
        () -> videoServerService.offerRtcAudioStream(user1Id.toString(), meeting1Id.toString(),
          "session-description-protocol"),
        "No Videoserver session found for user " + user1Id.toString()
          + " for the meeting " + meeting1Id.toString());

      verify(videoServerMeetingRepository, times(1)).getById(meeting1Id.toString());
    }
  }
}
