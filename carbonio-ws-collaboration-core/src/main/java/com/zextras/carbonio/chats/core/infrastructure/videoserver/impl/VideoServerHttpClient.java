// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerClient;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
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

@Singleton
public class VideoServerHttpClient implements VideoServerClient {

  private static final String JANUS_ENDPOINT = "/janus";
  private static final String JANUS_INFO_ENDPOINT = "/info";

  private final HttpClient httpClient;
  private final String videoServerURL;
  private final ObjectMapper objectMapper;

  @Inject
  public VideoServerHttpClient(
      HttpClient httpClient, String videoServerURL, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.videoServerURL = videoServerURL;
    this.objectMapper = objectMapper;
  }

  @Override
  public VideoServerResponse sendGetInfoRequest() {
    try (CloseableHttpResponse response =
        httpClient.sendGet(
            buildVideoServerUrl() + JANUS_INFO_ENDPOINT,
            Map.of("Content-Type", "application/json"))) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          VideoServerResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  @Override
  public VideoServerResponse sendVideoServerRequest(VideoServerMessageRequest request) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            buildVideoServerUrl(),
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(request))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          VideoServerResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON", e);
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    }
  }

  @Override
  public VideoServerResponse sendConnectionVideoServerRequest(
      String connectionId, VideoServerMessageRequest request) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            buildVideoServerUrl(connectionId),
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(request))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          VideoServerResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON", e);
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    }
  }

  @Override
  public VideoServerResponse sendHandleVideoServerRequest(
      String connectionId, String handleId, VideoServerMessageRequest request) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            buildVideoServerUrl(connectionId, handleId),
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(request))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          VideoServerResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON", e);
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    }
  }

  @Override
  public AudioBridgeResponse sendAudioBridgeRequest(
      String connectionId, String handleId, VideoServerMessageRequest request) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            buildVideoServerUrl(connectionId, handleId),
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(request))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          AudioBridgeResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON", e);
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    }
  }

  @Override
  public VideoRoomResponse sendVideoRoomRequest(
      String connectionId, String handleId, VideoServerMessageRequest request) {
    try (CloseableHttpResponse response =
        httpClient.sendPost(
            buildVideoServerUrl(connectionId, handleId),
            Map.of("Content-Type", "application/json"),
            objectMapper.writeValueAsString(request))) {

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != HttpStatus.SC_OK) {
        throw new VideoServerException("Video server returns error response: " + statusCode);
      }
      return objectMapper.readValue(
          IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8),
          VideoRoomResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON", e);
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request", e);
    }
  }

  private String buildVideoServerUrl() {
    return videoServerURL + JANUS_ENDPOINT;
  }

  private String buildVideoServerUrl(String connectionId) {
    return String.join("", buildVideoServerUrl() + String.format("/%s", connectionId));
  }

  private String buildVideoServerUrl(String connectionId, String handleId) {
    return String.join("", buildVideoServerUrl(), String.format("/%s/%s", connectionId, handleId));
  }
}
