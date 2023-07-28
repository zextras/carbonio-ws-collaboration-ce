// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static io.smallrye.common.constraint.Assert.assertFalse;
import static io.smallrye.common.constraint.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge.AudioBridgeCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom.VideoRoomCreateRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.impl.VideoServerServiceJanus;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
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

    objectMapper.registerSubtypes(new NamedType(AudioBridgeCreateRequest.class, "AudioBridgeCreateRequest"));
    objectMapper.registerSubtypes(new NamedType(VideoRoomCreateRequest.class, "VideoRoomCreateRequest"));

    this.videoServerService = new VideoServerServiceJanus(
      appConfig,
      objectMapper,
      httpClient,
      videoServerMeetingRepository,
      videoServerSessionRepository
    );
  }

  private UUID   meeting1Id;
  private String videoServerURL;
  private String janusEndpoint;

  @BeforeEach
  public void init() {
    meeting1Id = UUID.randomUUID();
    videoServerURL = "http://127.0.0.1:8088";
    janusEndpoint = "/janus";
  }

  @Nested
  @DisplayName("Start Meeting tests")
  class StartMeetingTests {

    @Test
    @DisplayName("Start meeting test")
    void startMeeting_testOk() throws IOException {
      CloseableHttpResponse sessionResponse = mock(CloseableHttpResponse.class);
      StatusLine sessionStatusLine = mock(StatusLine.class);
      HttpEntity sessionHttpEntity = mock(HttpEntity.class);
      when(sessionResponse.getStatusLine()).thenReturn(sessionStatusLine);
      when(sessionResponse.getEntity()).thenReturn(sessionHttpEntity);
      when(sessionStatusLine.getStatusCode()).thenReturn(200);
      when(sessionHttpEntity.getContent()).thenReturn(
        new ByteArrayInputStream(
          ("{\n"
            + "    \"janus\": \"success\",\n"
            + "    \"transaction\": \"transaction-id\",\n"
            + "    \"data\": {\n"
            + "        \"id\": \"session-id\"\n"
            + "    }\n"
            + "}"
          ).getBytes(StandardCharsets.UTF_8)
        )
      );
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(sessionResponse);

      CloseableHttpResponse audioHandleResponse = mock(CloseableHttpResponse.class);
      StatusLine audioHandleStatusLine = mock(StatusLine.class);
      HttpEntity audioHandleHttpEntity = mock(HttpEntity.class);
      when(audioHandleResponse.getStatusLine()).thenReturn(audioHandleStatusLine);
      when(audioHandleResponse.getEntity()).thenReturn(audioHandleHttpEntity);
      when(audioHandleStatusLine.getStatusCode()).thenReturn(200);
      when(audioHandleHttpEntity.getContent()).thenReturn(
        new ByteArrayInputStream(
          ("{\n"
            + "    \"janus\": \"success\",\n"
            + "    \"session_id\": \"session-id\",\n"
            + "    \"transaction\": \"transaction-id\",\n"
            + "    \"data\": {\n"
            + "        \"id\": \"audio-handle-id\"\n"
            + "    }\n"
            + "}"
          ).getBytes(StandardCharsets.UTF_8)
        )
      );

      CloseableHttpResponse videoHandleResponse = mock(CloseableHttpResponse.class);
      StatusLine videoHandleStatusLine = mock(StatusLine.class);
      HttpEntity videoHandleHttpEntity = mock(HttpEntity.class);
      when(videoHandleResponse.getStatusLine()).thenReturn(videoHandleStatusLine);
      when(videoHandleResponse.getEntity()).thenReturn(videoHandleHttpEntity);
      when(videoHandleStatusLine.getStatusCode()).thenReturn(200);
      when(videoHandleHttpEntity.getContent()).thenReturn(
        new ByteArrayInputStream(
          ("{\n"
            + "    \"janus\": \"success\",\n"
            + "    \"session_id\": \"session-id\",\n"
            + "    \"transaction\": \"transaction-id\",\n"
            + "    \"data\": {\n"
            + "        \"id\": \"video-handle-id\"\n"
            + "    }\n"
            + "}"
          ).getBytes(StandardCharsets.UTF_8)
        )
      );

      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioHandleResponse, videoHandleResponse);

      CloseableHttpResponse audioRoomResponse = mock(CloseableHttpResponse.class);
      StatusLine audioRoomStatusLine = mock(StatusLine.class);
      HttpEntity audioRoomHttpEntity = mock(HttpEntity.class);
      when(audioRoomResponse.getStatusLine()).thenReturn(audioRoomStatusLine);
      when(audioRoomResponse.getEntity()).thenReturn(audioRoomHttpEntity);
      when(audioRoomStatusLine.getStatusCode()).thenReturn(200);
      when(audioRoomHttpEntity.getContent()).thenReturn(
        new ByteArrayInputStream(
          ("{\n"
            + "    \"janus\": \"success\",\n"
            + "    \"session_id\": \"session-id\",\n"
            + "    \"transaction\": \"transaction-id\",\n"
            + "    \"sender\": \"audio-handle-id\",\n"
            + "    \"plugindata\": {\n"
            + "        \"plugin\": \"janus.plugin.audiobridge\",\n"
            + "        \"data\": {\n"
            + "            \"audiobridge\": \"created\",\n"
            + "            \"room\": \"audio-room-id\",\n"
            + "            \"permanent\": false\n"
            + "        }\n"
            + "    }\n"
            + "}"
          ).getBytes(StandardCharsets.UTF_8)
        )
      );
      when(httpClient.sendPost(
        eq(videoServerURL + janusEndpoint + "/session-id" + "/audio-handle-id"),
        eq(Map.of("content-type", "application/json")),
        anyString()
      )).thenReturn(audioRoomResponse);

      CloseableHttpResponse videoRoomResponse = mock(CloseableHttpResponse.class);
      StatusLine videoRoomStatusLine = mock(StatusLine.class);
      HttpEntity videoRoomHttpEntity = mock(HttpEntity.class);
      when(videoRoomResponse.getStatusLine()).thenReturn(videoRoomStatusLine);
      when(videoRoomResponse.getEntity()).thenReturn(videoRoomHttpEntity);
      when(videoRoomStatusLine.getStatusCode()).thenReturn(200);
      when(videoRoomHttpEntity.getContent()).thenReturn(
        new ByteArrayInputStream(
          ("{\n"
            + "    \"janus\": \"success\",\n"
            + "    \"session_id\": \"session-id\",\n"
            + "    \"transaction\": \"transaction-id\",\n"
            + "    \"sender\": \"video-handle-id\",\n"
            + "    \"plugindata\": {\n"
            + "        \"plugin\": \"janus.plugin.videoroom\",\n"
            + "        \"data\": {\n"
            + "            \"videoroom\": \"created\",\n"
            + "            \"room\": \"video-room-id\",\n"
            + "            \"permanent\": false\n"
            + "        }\n"
            + "    }\n"
            + "}"
          ).getBytes(StandardCharsets.UTF_8)
        )
      );
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

      VideoServerMessageRequest videoServerMessageRequest = objectMapper.readValue(
        createConnectionJsonBody.getValue(), VideoServerMessageRequest.class);
      assertEquals("create", videoServerMessageRequest.getMessageRequest());
      assertFalse(videoServerMessageRequest.getTransactionId().isEmpty());
      assertEquals("token", videoServerMessageRequest.getApiSecret());

      assertEquals(2, createHandleJsonBody.getAllValues().size());
      VideoServerMessageRequest audioHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(0), VideoServerMessageRequest.class);
      assertEquals("attach", audioHandleMessageRequest.getMessageRequest());
      assertEquals("janus.plugin.audiobridge", audioHandleMessageRequest.getPluginName());
      assertFalse(audioHandleMessageRequest.getTransactionId().isEmpty());
      assertEquals("token", audioHandleMessageRequest.getApiSecret());
      VideoServerMessageRequest videoHandleMessageRequest = objectMapper.readValue(
        createHandleJsonBody.getAllValues().get(1), VideoServerMessageRequest.class);
      assertEquals("attach", videoHandleMessageRequest.getMessageRequest());
      assertEquals("janus.plugin.videoroom", videoHandleMessageRequest.getPluginName());
      assertFalse(videoHandleMessageRequest.getTransactionId().isEmpty());
      assertEquals("token", videoHandleMessageRequest.getApiSecret());

      VideoServerMessageRequest audioRoomMessageRequest = objectMapper.readValue(createAudioRoomJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals("message", audioRoomMessageRequest.getMessageRequest());
      assertFalse(audioRoomMessageRequest.getTransactionId().isEmpty());
      assertEquals("token", audioRoomMessageRequest.getApiSecret());
      AudioBridgeCreateRequest audioBridgeCreateRequest = (AudioBridgeCreateRequest) audioRoomMessageRequest.getVideoServerPluginRequest();
      assertEquals("create", audioBridgeCreateRequest.getRequest());
      assertEquals("audio_" + meeting1Id, audioBridgeCreateRequest.getRoom());
      assertFalse(audioBridgeCreateRequest.getPermanent());
      assertEquals("audio_room_" + meeting1Id, audioBridgeCreateRequest.getDescription());
      assertFalse(audioBridgeCreateRequest.getIsPrivate());
      assertFalse(audioBridgeCreateRequest.getRecord());
      assertEquals(16000L, audioBridgeCreateRequest.getSamplingRate());
      assertEquals(10L, audioBridgeCreateRequest.getAudioActivePackets());
      assertEquals(55, audioBridgeCreateRequest.getAudioLevelAverage());
      assertTrue(audioBridgeCreateRequest.getAudioLevelEvent());

      VideoServerMessageRequest videoRoomMessageRequest = objectMapper.readValue(createVideoRoomJsonBody.getValue(),
        VideoServerMessageRequest.class);
      assertEquals("message", videoRoomMessageRequest.getMessageRequest());
      assertFalse(videoRoomMessageRequest.getTransactionId().isEmpty());
      assertEquals("token", videoRoomMessageRequest.getApiSecret());
      VideoRoomCreateRequest videoRoomCreateRequest = (VideoRoomCreateRequest) videoRoomMessageRequest.getVideoServerPluginRequest();
      assertEquals("create", videoRoomCreateRequest.getRequest());
      assertEquals("video_" + meeting1Id, videoRoomCreateRequest.getRoom());
      assertFalse(videoRoomCreateRequest.getPermanent());
      assertEquals("video_room_" + meeting1Id, videoRoomCreateRequest.getDescription());
      assertFalse(videoRoomCreateRequest.getIsPrivate());
      assertFalse(videoRoomCreateRequest.getRecord());
      assertEquals(100, videoRoomCreateRequest.getPublishers());
      assertEquals(200L, videoRoomCreateRequest.getBitrate());
      assertTrue(videoRoomCreateRequest.getBitrateCap());
      assertEquals("vp8,h264,vp9,h265,av1", videoRoomCreateRequest.getVideoCodec());
    }
  }

  @Nested
  @DisplayName("Stop Meeting tests")
  class StopMeetingTests {

    @Test
    @DisplayName("Stop meeting test")
    void stopMeeting_testOk() {

    }
  }
}
