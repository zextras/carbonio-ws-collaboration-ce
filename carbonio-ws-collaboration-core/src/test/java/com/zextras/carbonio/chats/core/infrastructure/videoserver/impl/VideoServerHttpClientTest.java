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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VideoServerHttpClientTest {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final VideoServerHttpClient videoServerHttpClient;

  private final String videoServerURL = "http://127.78.0.4:20006";

  private final String janusEndpoint = "/janus";
  private final String janusInfoEndpoint = "/info";
  private final String videoServerRoutingQueryParam = "?service_id=%s";

  public VideoServerHttpClientTest() {
    this.httpClient = mock(HttpClient.class);
    this.objectMapper = new ObjectMapper();
    this.videoServerHttpClient =
        new VideoServerHttpClient(httpClient, videoServerURL, objectMapper);
  }

  @AfterEach
  void mocksCheck() {
    verifyNoMoreInteractions(httpClient);
  }

  private void mockResponse(String url, int statusCode, Object bodyResponse) throws IOException {
    CloseableHttpResponse sessionResponse = mock(CloseableHttpResponse.class);
    StatusLine sessionStatusLine = mock(StatusLine.class);
    HttpEntity sessionHttpEntity = mock(HttpEntity.class);
    when(sessionResponse.getStatusLine()).thenReturn(sessionStatusLine);
    when(sessionResponse.getEntity()).thenReturn(sessionHttpEntity);
    when(sessionStatusLine.getStatusCode()).thenReturn(statusCode);
    when(sessionHttpEntity.getContent())
        .thenReturn(
            new ByteArrayInputStream(
                objectMapper.writeValueAsString(bodyResponse).getBytes(StandardCharsets.UTF_8)));
    when(httpClient.sendPost(eq(url), any(), anyString())).thenReturn(sessionResponse);
    when(httpClient.sendGet(eq(url), any())).thenReturn(sessionResponse);
  }

  @Test
  @DisplayName("Send get info http request to video server service")
  void sendIsAliveHttpRequestCorrectly() throws IOException {
    String url = videoServerURL + janusEndpoint + janusInfoEndpoint;
    mockResponse(url, 200, PongResponse.create());

    VideoServerResponse videoServerResponse = videoServerHttpClient.sendGetInfoRequest();

    assertEquals(VideoServerResponse.create(), videoServerResponse);

    verify(httpClient, times(1)).sendGet(url, Map.of("Content-Type", "application/json"));
  }

  @Test
  @DisplayName(
      "throws video server exception if video server service returns error sending info request")
  void throwsVideoServerExceptionWhenErrorOccursSendingInfoRequest() throws IOException {
    String url = videoServerURL + janusEndpoint + janusInfoEndpoint;
    mockResponse(url, 404, null);

    assertThrows(
        VideoServerException.class,
        videoServerHttpClient::sendGetInfoRequest,
        "Could not get any response by video server");

    verify(httpClient, times(1)).sendGet(url, Map.of("Content-Type", "application/json"));
  }

  @Test
  @DisplayName("Send video server http request to video server service")
  void sendVideoServerHttpRequestCorrectly() throws IOException {
    String url =
        videoServerURL + janusEndpoint + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 200, VideoServerResponse.create());

    VideoServerResponse videoServerResponse =
        videoServerHttpClient
            .sendVideoServerRequest("serverId", VideoServerMessageRequest.create())
            .join();

    assertEquals(VideoServerResponse.create(), videoServerResponse);

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }

  @Test
  @DisplayName(
      "throws video server exception if video server service returns error sending video server"
          + " request")
  void throwsVideoServerExceptionWhenErrorOccursSendingVideoServerRequest() throws IOException {
    String url =
        videoServerURL + janusEndpoint + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 404, null);

    assertThrows(
        CompletionException.class,
        () ->
            videoServerHttpClient
                .sendVideoServerRequest("serverId", VideoServerMessageRequest.create())
                .join());

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }

  @Test
  @DisplayName("Send http request to video server service for audio bridge")
  void sendAudioBridgeHttpRequestCorrectly() throws IOException {
    String url =
        videoServerURL
            + janusEndpoint
            + "/connectionId"
            + "/handleId"
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 200, AudioBridgeResponse.create());

    AudioBridgeResponse audioBridgeResponse =
        videoServerHttpClient
            .sendAudioBridgeRequest(
                "connectionId", "handleId", "serverId", VideoServerMessageRequest.create())
            .join();

    assertEquals(AudioBridgeResponse.create(), audioBridgeResponse);

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }

  @Test
  @DisplayName(
      "throws video server exception if video server service returns error sending audio bridge"
          + " request")
  void throwsVideoServerExceptionWhenErrorOccursSendingAudioBridgeRequest() throws IOException {
    String url =
        videoServerURL
            + janusEndpoint
            + "/connectionId"
            + "/handleId"
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 404, null);

    assertThrows(
        CompletionException.class,
        () ->
            videoServerHttpClient
                .sendAudioBridgeRequest(
                    "connectionId", "handleId", "serverId", VideoServerMessageRequest.create())
                .join());

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }

  @Test
  @DisplayName("Send http request to video server service for video room")
  void sendVideoRoomHttpRequestCorrectly() throws IOException {
    String url =
        videoServerURL
            + janusEndpoint
            + "/connectionId"
            + "/handleId"
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 200, VideoRoomResponse.create());

    VideoRoomResponse videoRoomResponse =
        videoServerHttpClient
            .sendVideoRoomRequest(
                "connectionId", "handleId", "serverId", VideoServerMessageRequest.create())
            .join();

    assertEquals(VideoRoomResponse.create(), videoRoomResponse);

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }

  @Test
  @DisplayName(
      "throws video server exception if video server service returns error sending video room"
          + " request")
  void throwsVideoServerExceptionWhenErrorOccursSendingVideoRoomRequest() throws IOException {
    String url =
        videoServerURL
            + janusEndpoint
            + "/connectionId"
            + "/handleId"
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 404, null);

    assertThrows(
        CompletionException.class,
        () ->
            videoServerHttpClient
                .sendVideoRoomRequest(
                    "connectionId", "handleId", "serverId", VideoServerMessageRequest.create())
                .join());

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoServerMessageRequest.create()));
  }
}
