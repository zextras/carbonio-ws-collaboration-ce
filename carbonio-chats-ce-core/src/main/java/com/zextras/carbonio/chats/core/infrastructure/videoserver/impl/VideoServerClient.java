package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.RoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.VideoServerMessageRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerIdResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerPluginResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerRoomErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.VideoServerRoomResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class VideoServerClient {

  private static final String JANUS_ENDPOINT           = "/janus";
  public static final  String JANUS_CREATE             = "create";
  public static final  String JANUS_MESSAGE            = "message";
  public static final  String JANUS_ATTACH             = "attach";
  public static final  String JANUS_DETACH             = "detach";
  public static final  String JANUS_DESTROY            = "destroy";
  public static final  String JANUS_SUCCESS            = "success";
  public static final  String JANUS_VIDEOROOM_PLUGIN   = "janus.plugin.videoroom";
  public static final  String JANUS_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";

  private final String videoServerURL;

  VideoServerClient(String videoServerURL) {
    this.videoServerURL = videoServerURL;
  }

  public static VideoServerClient atURL(String url) {
    return new VideoServerClient(url);
  }

  /**
   * This method creates a 'connection' (session) on the VideoServer
   *
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerResponse createConnection() {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    VideoServerMessageRequest messageRequest = VideoServerMessageRequest.create()
      .messageRequest(JANUS_CREATE)
      .transactionId(UUID.randomUUID().toString());
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(messageRequest), StandardCharsets.UTF_8)
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return new ObjectMapper().readValue(bodyResponse, VideoServerIdResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, VideoServerErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
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
  public VideoServerResponse interactWithConnection(String connectionId, String action, @Nullable String pluginName) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT + "/" + connectionId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    VideoServerMessageRequest messageRequest = VideoServerMessageRequest.create()
      .messageRequest(action)
      .transactionId(UUID.randomUUID().toString())
      .pluginName(pluginName);
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(messageRequest), StandardCharsets.UTF_8)
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return new ObjectMapper().readValue(bodyResponse, VideoServerIdResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, VideoServerErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
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
   * This method allows you to send a message on a plugin previously attached
   *
   * @param connectionId the 'connection' (session) id
   * @param handleId     the previously attached plugin handle id
   * @param messageType  the type of message you want to send
   * @param body         the body you want to send with the message
   * @return {@link VideoServerPluginResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerPluginResponse sendPluginMessage(String connectionId, String handleId, String messageType,
    @Nullable RoomRequest body) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    VideoServerMessageRequest messageRequest = VideoServerMessageRequest.create()
      .messageRequest(messageType)
      .transactionId(UUID.randomUUID().toString())
      .body(body);
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(messageRequest), StandardCharsets.UTF_8)
      );
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return new ObjectMapper().readValue(bodyResponse, VideoServerRoomResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, VideoServerRoomErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
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
  public VideoServerPluginResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendPluginMessage(connectionId, handleId, JANUS_DETACH, null);
  }
}
