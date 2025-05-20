// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.impl;

import static org.junit.jupiter.api.Assertions.*;
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
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VideoRecorderHttpClientTest {

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final VideoRecorderHttpClient videoRecorderHttpClient;

  private final String videoRecorderURL = "http://127.78.0.4:20007";

  private final String postProcessorEndpoint = "/PostProcessor";
  private final String meetingEndpoint = "/meeting_%s";
  private final String videoServerRoutingQueryParam = "?service_id=%s";

  public VideoRecorderHttpClientTest() {
    this.httpClient = mock(HttpClient.class);
    this.objectMapper = new ObjectMapper();
    this.videoRecorderHttpClient =
        new VideoRecorderHttpClient(httpClient, videoRecorderURL, objectMapper);
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
  @DisplayName("Send http request to video recorder service for the post processing phase")
  void sendVideoRecorderHttpRequestCorrectly() throws IOException {
    UUID meetingId = UUID.randomUUID();
    String url =
        videoRecorderURL
            + postProcessorEndpoint
            + String.format(meetingEndpoint, meetingId)
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 200, null);

    videoRecorderHttpClient.sendVideoRecorderRequest(
        "serverId", meetingId.toString(), VideoRecorderRequest.create());

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoRecorderRequest.create()));
  }

  @Test
  @DisplayName(
      "throws video server exception if video recorder service returns error sending post"
          + " processing request")
  void throwsVideoServerExceptionWhenErrorOccursSendingVideoRecorderRequest() throws IOException {
    UUID meetingId = UUID.randomUUID();
    String url =
        videoRecorderURL
            + postProcessorEndpoint
            + String.format(meetingEndpoint, meetingId)
            + String.format(videoServerRoutingQueryParam, "serverId");
    mockResponse(url, 500, null);

    assertThrows(
        VideoServerException.class,
        () ->
            videoRecorderHttpClient.sendVideoRecorderRequest(
                "serverId", meetingId.toString(), VideoRecorderRequest.create()));

    verify(httpClient, times(1))
        .sendPost(
            url,
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(VideoRecorderRequest.create()));
  }
}
