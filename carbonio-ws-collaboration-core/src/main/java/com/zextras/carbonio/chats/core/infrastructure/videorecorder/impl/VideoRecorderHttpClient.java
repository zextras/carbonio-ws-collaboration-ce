// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videorecorder.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.VideoRecorderClient;
import com.zextras.carbonio.chats.core.infrastructure.videorecorder.data.request.VideoRecorderRequest;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

@Singleton
public class VideoRecorderHttpClient implements VideoRecorderClient {

  private static final String POST_PROCESSOR_ENDPOINT = "/PostProcessor";
  private static final String MEETING_ENDPOINT = "/meeting_%s";
  private static final String VIDEORECORDER_ROUTING_QUERYPARAM = "?service_id=%s";

  private final HttpClient httpClient;
  private final String videoRecorderURL;
  private final ObjectMapper objectMapper;

  @Inject
  public VideoRecorderHttpClient(
      HttpClient httpClient, String videoRecorderURL, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.videoRecorderURL = videoRecorderURL;
    this.objectMapper = objectMapper;
  }

  @Override
  public CompletableFuture<Void> sendVideoRecorderRequest(
      String serverId, String meetingId, VideoRecorderRequest request) {
    return CompletableFuture.runAsync(
        () -> {
          try (CloseableHttpResponse response =
              httpClient.sendPost(
                  buildVideoRecorderUrl(meetingId)
                      + String.format(VIDEORECORDER_ROUTING_QUERYPARAM, serverId),
                  Map.of("Content-Type", "application/json"),
                  objectMapper.writeValueAsString(request))) {

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
              throw new VideoServerException(
                  "Video recorder returns error response: " + statusCode);
            }
          } catch (JsonProcessingException e) {
            throw new VideoServerException("Unable to convert request body to JSON", e);
          } catch (IOException e) {
            throw new VideoServerException("Something went wrong executing request", e);
          }
        });
  }

  private String buildVideoRecorderUrl(String meetingId) {
    return String.join(
        "", videoRecorderURL, POST_PROCESSOR_ENDPOINT, String.format(MEETING_ENDPOINT, meetingId));
  }
}
