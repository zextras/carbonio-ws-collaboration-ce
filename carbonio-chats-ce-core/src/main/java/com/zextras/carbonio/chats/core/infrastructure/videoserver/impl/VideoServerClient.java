package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.exception.VideoServerException;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class VideoServerClient {

  private static final String VIDEO_SERVER_ENDPOINT           = "/janus";
  private static final String VIDEO_SERVER_INFO               = "info";
  static final         String VIDEO_SERVER_CREATE             = "create";
  static final         String VIDEO_SERVER_MESSAGE            = "message";
  static final         String VIDEO_SERVER_ATTACH             = "attach";
  static final         String VIDEO_SERVER_DETACH             = "detach";
  static final         String VIDEO_SERVER_DESTROY            = "destroy";
  static final         String VIDEO_SERVER_SUCCESS            = "success";
  static final         String VIDEO_SERVER_VIDEOROOM_PLUGIN   = "janus.plugin.videoroom";
  static final         String VIDEO_SERVER_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";

  private final String       videoServerURL;
  private final ObjectMapper objectMapper;

  public VideoServerClient(String videoServerURL, ObjectMapper objectMapper) {
    this.videoServerURL = videoServerURL;
    this.objectMapper = objectMapper;
  }

  public static VideoServerClient atURL(String url, ObjectMapper objectMapper) {
    return new VideoServerClient(url, objectMapper);
  }

  private HttpPost getHttpPost(String url, String body) {
    HttpPost request = new HttpPost(url);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    request.setEntity(new StringEntity(body, StandardCharsets.UTF_8));
    return request;
  }

  /**
   * This method creates a 'connection' (session) on the VideoServer
   *
   * @return {@link VideoServerResponse}
   * @see <a href="https://janus.conf.meetecho.com/docs/rest.html">JanusRestApi</a>
   */
  public VideoServerResponse createConnection() {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    try {
      CloseableHttpResponse response = httpClient.execute(
        getHttpPost(videoServerURL + VIDEO_SERVER_ENDPOINT,
          objectMapper.writeValueAsString(
            VideoServerMessageRequest.create()
              .messageRequest(VIDEO_SERVER_CREATE)
              .transactionId(UUID.randomUUID().toString())
          )
        )
      );
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return objectMapper.readValue(bodyResponse, VideoServerIdResponse.class);
      }
      return objectMapper.readValue(bodyResponse, VideoServerErrorResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
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
    try {
      CloseableHttpResponse response = httpClient.execute(
        getHttpPost(videoServerURL + VIDEO_SERVER_ENDPOINT + "/" + connectionId,
          objectMapper.writeValueAsString(
            VideoServerMessageRequest.create()
              .messageRequest(action)
              .transactionId(UUID.randomUUID().toString())
              .pluginName(pluginName)
          )
        )
      );
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return objectMapper.readValue(bodyResponse, VideoServerIdResponse.class);
      }
      return objectMapper.readValue(bodyResponse, VideoServerErrorResponse.class);
    } catch (JsonProcessingException e) {
      throw new VideoServerException("Unable to convert request body to JSON");
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
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
    return interactWithConnection(connectionId, VIDEO_SERVER_DESTROY, null);
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
    try {
      CloseableHttpResponse response = httpClient.execute(
        getHttpPost(videoServerURL + VIDEO_SERVER_ENDPOINT + "/" + connectionId + "/" + handleId,
          objectMapper.writeValueAsString(
            VideoServerMessageRequest.create()
              .messageRequest(messageType)
              .transactionId(UUID.randomUUID().toString())
              .body(body)
          )
        )
      );
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        return objectMapper.readValue(bodyResponse, VideoServerRoomResponse.class);
      }
      return objectMapper.readValue(bodyResponse, VideoServerRoomErrorResponse.class);
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
  public VideoServerPluginResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendPluginMessage(connectionId, handleId, VIDEO_SERVER_DETACH, null);
  }

  /**
   * This method returns if the VideoServer is alive sending a http GET request to the info endpoint
   *
   * @return true if VideoServer is alive, false otherwise
   */
  public boolean isAlive() {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(videoServerURL + VIDEO_SERVER_ENDPOINT + "/" + VIDEO_SERVER_INFO);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      int statusCode = response.getStatusLine().getStatusCode();
      return statusCode == HttpStatus.SC_OK;
    } catch (IOException e) {
      throw new VideoServerException("Something went wrong executing request");
    }
  }
}
