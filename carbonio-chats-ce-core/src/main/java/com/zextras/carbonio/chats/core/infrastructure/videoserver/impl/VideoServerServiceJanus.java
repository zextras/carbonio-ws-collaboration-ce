// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.VideoServerMeeting;
import com.zextras.carbonio.chats.core.data.entity.VideoServerSession;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.AudioBridgeRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.RoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoRoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerIdResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerPluginResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerRoomErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerRoomResponse;
import com.zextras.carbonio.chats.core.repository.VideoServerMeetingRepository;
import com.zextras.carbonio.chats.core.repository.VideoServerSessionRepository;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import io.ebean.annotation.Transactional;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;

@Singleton
public class VideoServerServiceJanus implements VideoServerService {

  private static final String                       JANUS_ENDPOINT           = "/janus";
  private static final String                       JANUS_INFO               = "info";
  private static final String                       JANUS_CREATE             = "create";
  private static final String                       JANUS_MESSAGE            = "message";
  private static final String                       JANUS_ATTACH             = "attach";
  private static final String                       JANUS_DETACH             = "detach";
  private static final String                       JANUS_DESTROY            = "destroy";
  private static final String                       JANUS_SUCCESS            = "success";
  private static final String                       JANUS_VIDEOROOM_PLUGIN   = "janus.plugin.videoroom";
  private static final String                       JANUS_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";
  private final        ObjectMapper                 objectMapper;
  private final        String                       videoServerURL;
  private final        HttpClient                   httpClient;
  private final        VideoServerMeetingRepository videoServerMeetingRepository;
  private final        VideoServerSessionRepository videoServerSessionRepository;

  @Inject
  public VideoServerServiceJanus(
    AppConfig appConfig,
    ObjectMapper objectMapper,
    HttpClient httpClient,
    VideoServerMeetingRepository videoServerMeetingRepository,
    VideoServerSessionRepository videoServerSessionRepository
  ) {
    this.videoServerURL = String.format("http://%s:%s",
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_HOST).orElseThrow(),
      appConfig.get(String.class, ConfigName.VIDEO_SERVER_PORT).orElseThrow()
    );
    this.objectMapper = objectMapper;
    this.httpClient = httpClient;
    this.videoServerMeetingRepository = videoServerMeetingRepository;
    this.videoServerSessionRepository = videoServerSessionRepository;
  }

  private String writeValueAsAString(Object value) throws JsonProcessingException {
    return objectMapper.writeValueAsString(value);
  }

  @Override
  @Transactional
  public void createMeeting(String meetingId) {
    if (videoServerMeetingRepository.getById(meetingId).isPresent()) {
      throw new VideoServerException("Videoserver meeting " + meetingId + " is already present");
    }
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection for the meeting " + meetingId);
    }
    VideoServerIdResponse response = (VideoServerIdResponse) videoServerResponse;
    String connectionId = response.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH,
      JANUS_AUDIOBRIDGE_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the audiobridge plugin for the connection " + connectionId +
          " for the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String audioHandleId = response.getDataId();
    AudioBridgeRoomRequest audioBridgeRoomRequest = AudioBridgeRoomRequest.create(JANUS_CREATE);
    VideoServerPluginResponse audioPluginResponse = sendPluginMessage(
      connectionId,
      audioHandleId,
      JANUS_MESSAGE,
      audioBridgeRoomRequest
    );
    if (!audioPluginResponse.statusOK()) {
      throw new VideoServerException(
        "An error occurred when creating an audiobridge room for the connection " + connectionId + " with plugin "
          + audioHandleId + " for the meeting " + meetingId);
    }
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId
          + " for the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String videoHandleId = response.getDataId();
    VideoRoomRequest videoRoomRequest = VideoRoomRequest.create(JANUS_CREATE);
    VideoServerPluginResponse videoPluginResponse = sendPluginMessage(
      connectionId,
      videoHandleId,
      JANUS_MESSAGE,
      videoRoomRequest
    );
    if (!videoPluginResponse.statusOK()) {
      throw new VideoServerException(
        "An error occurred when creating a videoroom room for the connection " + connectionId + " with plugin "
          + videoHandleId + " for the meeting " + meetingId);
    }
    videoServerMeetingRepository.insert(
      VideoServerMeeting.create()
        .meetingId(meetingId)
        .connectionId(connectionId)
        .audioHandleId(audioHandleId)
        .videoHandleId(videoHandleId)
        .audioRoomId(audioPluginResponse.getRoom())
        .videoRoomId(videoPluginResponse.getRoom())
    );
  }

  @Override
  @Transactional
  public void deleteMeeting(String meetingId) {
    VideoServerMeeting videoServerMeetingToRemove = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerPluginResponse videoServerPluginResponse = destroyPluginHandle(
      videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getAudioHandleId());
    if (!videoServerPluginResponse.statusOK()) {
      throw new VideoServerException("An error occurred when destroying the audioroom plugin handle for the connection "
        + videoServerMeetingToRemove.getConnectionId() + " with plugin "
        + videoServerMeetingToRemove.getAudioHandleId() + " for the meeting " + meetingId);
    }
    videoServerPluginResponse = destroyPluginHandle(
      videoServerMeetingToRemove.getConnectionId(),
      videoServerMeetingToRemove.getVideoHandleId());
    if (!videoServerPluginResponse.statusOK()) {
      throw new VideoServerException("An error occurred when destroying the videoroom plugin handle for the connection "
        + videoServerMeetingToRemove.getConnectionId() + " with plugin "
        + videoServerMeetingToRemove.getVideoHandleId() + " for the meeting " + meetingId);
    }
    VideoServerResponse videoServerResponse = destroyConnection(
      videoServerMeetingToRemove.getConnectionId());
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the videoserver connection "
          + videoServerMeetingToRemove.getConnectionId() + " for the meeting " + meetingId);
    }
    videoServerMeetingRepository.deleteById(meetingId);
  }

  @Override
  @Transactional
  public void joinMeeting(String sessionId, String meetingId, boolean videoStreamOn, boolean audioStreamOn) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    if (videoServerMeeting.getVideoServerSessions().stream()
      .anyMatch(videoServerSessionUser -> videoServerSessionUser.getSessionId().equals(sessionId))) {
      throw new VideoServerException(
        "Videoserver session user with sessionId " + sessionId + "is already present in the videoserver meeting "
          + meetingId);
    }
    VideoServerResponse videoServerResponse = createConnection();
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when creating a videoserver connection for sessionId " + sessionId + " on the meeting "
          + meetingId);
    }
    VideoServerIdResponse response = (VideoServerIdResponse) videoServerResponse;
    String connectionId = response.getDataId();
    videoServerResponse = interactWithConnection(connectionId, JANUS_ATTACH, JANUS_VIDEOROOM_PLUGIN);
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when attaching to the videoroom plugin for the connection " + connectionId
          + " with sessionId " + sessionId + " on the meeting " + meetingId);
    }
    response = (VideoServerIdResponse) videoServerResponse;
    String videoHandleId = response.getDataId();
    videoServerSessionRepository.insert(
      VideoServerSession.create(sessionId, videoServerMeeting)
        .connectionId(connectionId)
        .videoHandleId(videoHandleId)
        .videoStreamOn(videoStreamOn)
        .audioStreamOn(audioStreamOn)
    );
  }

  @Override
  @Transactional
  public void leaveMeeting(String sessionId, String meetingId) {
    VideoServerMeeting videoServerMeeting = videoServerMeetingRepository.getById(meetingId)
      .orElseThrow(() -> new VideoServerException("No videoserver meeting found for the meeting " + meetingId));
    VideoServerSession videoServerSession = videoServerMeeting.getVideoServerSessions().stream()
      .filter(sessionUser -> sessionUser.getSessionId().equals(sessionId))
      .findAny().orElseThrow(() -> new VideoServerException(
        "No Videoserver session user found for session " + sessionId + " for the meeting " + meetingId));
    VideoServerResponse videoServerResponse = destroyConnection(
      videoServerSession.getConnectionId());
    if (!JANUS_SUCCESS.equals(videoServerResponse.getStatus())) {
      throw new VideoServerException(
        "An error occurred when destroying the videoserver connection " + videoServerSession.getConnectionId()
          + " with session " + sessionId + " on the meeting " + meetingId);
    }
    videoServerSessionRepository.remove(videoServerSession);
  }

  @Override
  public void enableVideoStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void enableAudioStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public void enableScreenShareStream(String sessionId, String meetingId, boolean enable) {

  }

  @Override
  public boolean isAlive() {
    try (CloseableHttpResponse response = httpClient.sendGet(
      videoServerURL + JANUS_ENDPOINT + "/" + JANUS_INFO,
      Map.of("content-type", "application/json")
    )) {
      return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method creates a 'connection' (session) on the VideoServer
   *
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse createConnection() {
    try (CloseableHttpResponse response = httpClient.sendPost(
      videoServerURL + JANUS_ENDPOINT,
      Map.of("content-type", "application/json"),
      writeValueAsAString(
        VideoServerMessageRequest.create().messageRequest(JANUS_CREATE).transactionId(UUID.randomUUID().toString())))) {
      return getVideoServerResponse(response);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method destroys the previously attached plugin handle
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId     the plugin handle id
   * @return {@link VideoServerPluginResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerPluginResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendPluginMessage(connectionId, handleId, JANUS_DETACH, null);
  }

  /**
   * This method destroys a specified connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerResponse destroyConnection(String connectionId) {
    return interactWithConnection(connectionId, JANUS_DESTROY, null);
  }

  /**
   * This method allows you to interact with the connection previously created on the VideoServer.
   *
   * @param connectionId the 'connection' (session) id created on the VideoServer
   * @param action       the action you want to perform on this 'connection' (session)
   * @param pluginName   the plugin name you want to use to perform the action
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerResponse interactWithConnection(String connectionId, String action, @Nullable String pluginName) {
    try (CloseableHttpResponse response = httpClient.sendPost(
      videoServerURL + JANUS_ENDPOINT + "/" + connectionId,
      Map.of("content-type", "application/json"),
      writeValueAsAString(
        VideoServerMessageRequest.create().messageRequest(action).transactionId(UUID.randomUUID().toString())
          .pluginName(pluginName)))) {
      return getVideoServerResponse(response);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  /**
   * This method allows you to send a message on a plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId     the previously attached plugin handle id
   * @param messageType  the type of message you want to send
   * @param body         the body you want to send with the message
   * @return {@link VideoServerPluginResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  private VideoServerPluginResponse sendPluginMessage(String connectionId, String handleId, String messageType,
    @Nullable RoomRequest body) {
    try (CloseableHttpResponse response = httpClient.sendPost(
      videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId,
      Map.of("content-type", "application/json"),
      writeValueAsAString(
        VideoServerMessageRequest.create().messageRequest(messageType).transactionId(UUID.randomUUID().toString())
          .body(body)))) {
      return getVideoServerPluginResponse(response);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }

  private VideoServerResponse getVideoServerResponse(CloseableHttpResponse response) throws IOException {
    String bodyResponse = IOUtils.toString(
      response.getEntity().getContent(),
      StandardCharsets.UTF_8
    );

    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_OK) {
      return objectMapper.readValue(bodyResponse, VideoServerIdResponse.class);
    }
    return objectMapper.readValue(bodyResponse, VideoServerErrorResponse.class);
  }

  private VideoServerPluginResponse getVideoServerPluginResponse(CloseableHttpResponse response) throws IOException {
    String bodyResponse = IOUtils.toString(
      response.getEntity().getContent(),
      StandardCharsets.UTF_8
    );

    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_OK) {
      return objectMapper.readValue(bodyResponse, VideoServerRoomResponse.class);
    }
    return objectMapper.readValue(bodyResponse, VideoServerRoomErrorResponse.class);
  }

}
