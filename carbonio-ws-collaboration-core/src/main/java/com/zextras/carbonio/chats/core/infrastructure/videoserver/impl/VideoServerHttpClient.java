// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.PongResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge.AudioBridgeResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.videoroom.VideoRoomResponse;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

public class VideoServerHttpClient implements VideoServerClient {

  private final HttpClient   httpClient;
  private final ObjectMapper objectMapper;

  @Inject
  public VideoServerHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public PongResponse sendIsAliveRequest(String url, VideoServerMessageRequest request) {
    try {
      CloseableHttpResponse response = httpClient.sendPost(url, Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(request));
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(
        IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8), PongResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  @Override
  public VideoServerResponse sendVideoServerRequest(String url, VideoServerMessageRequest request) {
    try {
      CloseableHttpResponse response = httpClient.sendPost(url, Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(request));

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
        VideoServerResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  @Override
  public AudioBridgeResponse sendAudioBridgeRequest(String url, VideoServerMessageRequest request) {
    try {
      CloseableHttpResponse response = httpClient.sendPost(url, Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(request));
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
        AudioBridgeResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  @Override
  public VideoRoomResponse sendVideoRoomRequest(String url, VideoServerMessageRequest request) {
    try {
      CloseableHttpResponse response = httpClient.sendPost(url, Map.of("content-type", "application/json"),
        objectMapper.writeValueAsString(request));
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Could not get any response by video server");
      }
      return objectMapper.readValue(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
        VideoRoomResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }
}
