// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PongResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VideoServerHttpClientTest {

  private HttpClient            httpClient;
  private ObjectMapper          objectMapper;
  private VideoServerHttpClient videoServerHttpClient;

  public VideoServerHttpClientTest() {
    this.httpClient = mock(HttpClient.class);
    this.objectMapper = new ObjectMapper();

    this.videoServerHttpClient = new VideoServerHttpClient(httpClient, objectMapper);
  }

  private String videoServerURL;
  private String janusEndpoint;
  private String janusInfoEndpoint;

  @BeforeEach
  public void init() {
    videoServerURL = "http://127.0.0.1:8088";
    janusEndpoint = "/janus";
    janusInfoEndpoint = "/info";
  }

  private void mockResponse(String url, int statusCode, Object bodyResponse) throws IOException {
    CloseableHttpResponse sessionResponse = mock(CloseableHttpResponse.class);
    StatusLine sessionStatusLine = mock(StatusLine.class);
    HttpEntity sessionHttpEntity = mock(HttpEntity.class);
    when(sessionResponse.getStatusLine()).thenReturn(sessionStatusLine);
    when(sessionResponse.getEntity()).thenReturn(sessionHttpEntity);
    when(sessionStatusLine.getStatusCode()).thenReturn(statusCode);
    when(sessionHttpEntity.getContent()).thenReturn(
      new ByteArrayInputStream(objectMapper.writeValueAsString(bodyResponse).getBytes(StandardCharsets.UTF_8)));
    when(httpClient.sendPost(eq(url), any(), anyString())).thenReturn(sessionResponse);
    when(httpClient.sendGet(eq(url), any())).thenReturn(sessionResponse);
  }

  @Test
  @DisplayName("Send get info http request to video server service")
  void sendIsAliveHttpRequestCorrectly() throws IOException {
    mockResponse(videoServerURL + janusEndpoint + janusInfoEndpoint, 200, PongResponse.create());

    VideoServerResponse videoServerResponse = videoServerHttpClient.sendGetInfoRequest(
      videoServerURL + janusEndpoint + janusInfoEndpoint);

    assertEquals(VideoServerResponse.create(), videoServerResponse);
  }

  @Test
  @DisplayName("throws video server exception if video server service returns error sending info request")
  void throwsVideoServerExceptionWhenErrorOccursSendingInfoRequest() throws IOException {
    mockResponse(videoServerURL + janusEndpoint + janusInfoEndpoint, 404, null);

    assertThrows(VideoServerException.class, () -> videoServerHttpClient.sendGetInfoRequest(
        videoServerURL + janusEndpoint + janusInfoEndpoint),
      "Could not get any response by video server");
  }

  @Test
  @DisplayName("Send video server http request to video server service")
  void sendVideoServerHttpRequestCorrectly() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 200, VideoServerResponse.create());

    VideoServerResponse videoServerResponse = videoServerHttpClient.sendVideoServerRequest(
      videoServerURL + janusEndpoint,
      VideoServerMessageRequest.create());

    assertEquals(VideoServerResponse.create(), videoServerResponse);
  }

  @Test
  @DisplayName("throws video server exception if video server service returns error sending video server request")
  void throwsVideoServerExceptionWhenErrorOccursSendingVideoServerRequest() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 404, null);

    assertThrows(VideoServerException.class, () -> videoServerHttpClient.sendVideoServerRequest(
        videoServerURL + janusEndpoint, VideoServerMessageRequest.create()),
      "Could not get any response by video server");
  }

  @Test
  @DisplayName("Send http request to video server service for audio bridge")
  void sendAudioBridgeHttpRequestCorrectly() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 200, AudioBridgeResponse.create());

    AudioBridgeResponse audioBridgeResponse = videoServerHttpClient.sendAudioBridgeRequest(
      videoServerURL + janusEndpoint,
      VideoServerMessageRequest.create());

    assertEquals(AudioBridgeResponse.create(), audioBridgeResponse);
  }

  @Test
  @DisplayName("throws video server exception if video server service returns error sending audio bridge request")
  void throwsVideoServerExceptionWhenErrorOccursSendingAudioBridgeRequest() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 404, null);

    assertThrows(VideoServerException.class, () -> videoServerHttpClient.sendAudioBridgeRequest(
        videoServerURL + janusEndpoint, VideoServerMessageRequest.create()),
      "Could not get any response by video server");
  }

  @Test
  @DisplayName("Send http request to video server service for video room")
  void sendVideoRoomHttpRequestCorrectly() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 200, VideoRoomResponse.create());

    VideoRoomResponse videoRoomResponse = videoServerHttpClient.sendVideoRoomRequest(
      videoServerURL + janusEndpoint,
      VideoServerMessageRequest.create());

    assertEquals(VideoRoomResponse.create(), videoRoomResponse);
  }

  @Test
  @DisplayName("throws video server exception if video server service returns error sending video room request")
  void throwsVideoServerExceptionWhenErrorOccursSendingVideoRoomRequest() throws IOException {
    mockResponse(videoServerURL + janusEndpoint, 404, null);

    assertThrows(VideoServerException.class, () -> videoServerHttpClient.sendVideoRoomRequest(
        videoServerURL + janusEndpoint, VideoServerMessageRequest.create()),
      "Could not get any response by video server");
  }
}