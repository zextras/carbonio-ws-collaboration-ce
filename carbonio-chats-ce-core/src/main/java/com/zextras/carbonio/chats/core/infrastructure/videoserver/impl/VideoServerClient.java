package com.zextras.carbonio.chats.core.infrastructure.videoserver.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.JanusEvent;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.entity.JanusMessage;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.JanusConnectionCreationRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.JanusConnectionInteractionRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.RoomRequest;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusEventsResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusIdResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusPluginResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusRoomErrorResponse;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.JanusRoomResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

  private static final String JANUS_ENDPOINT           = "/janus";
  public static final  String JANUS_CREATE             = "create";
  public static final  String JANUS_MESSAGE            = "message";
  public static final  String JANUS_ATTACH             = "attach";
  public static final  String JANUS_DETACH             = "detach";
  public static final  String JANUS_DESTROY            = "destroy";
  public static final  String JANUS_SUCCESS            = "success";
  public static final  String JANUS_ERROR              = "error";
  public static final  String JANUS_VIDEOROOM_PLUGIN   = "janus.plugin.videoroom";
  public static final  String JANUS_AUDIOBRIDGE_PLUGIN = "janus.plugin.audiobridge";

  private final String videoServerURL;

  VideoServerClient(String videoServerURL) {
    this.videoServerURL = videoServerURL;
  }

  public static VideoServerClient atURL(String url) {
    return new VideoServerClient(url);
  }

  public static VideoServerClient atURL(String protocol, String domain, Integer port) {
    return new VideoServerClient(protocol + "://" + domain + ":" + port);
  }

  public JanusResponse createConnection() {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    JanusConnectionCreationRequest janusRequest = new JanusConnectionCreationRequest(
      JANUS_CREATE,
      UUID.randomUUID().toString()
    );
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(janusRequest), StandardCharsets.UTF_8)
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
        return new ObjectMapper().readValue(bodyResponse, JanusIdResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, JanusErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
  }

  public JanusResponse getConnectionEvents(String connectionId) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpGet request = new HttpGet(videoServerURL + JANUS_ENDPOINT + "/" + connectionId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      ObjectMapper mapper = new ObjectMapper();
      if (statusCode == HttpStatus.SC_OK) {
        return new JanusEventsResponse(
          mapper.readValue(
            bodyResponse,
            mapper.getTypeFactory().constructCollectionType(List.class, JanusEvent.class)
          )
        );
      }
      return mapper.readValue(bodyResponse, JanusErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
  }

  public JanusResponse interactWithConnection(String connectionId, String action, @Nullable String pluginName) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT + "/" + connectionId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    JanusConnectionInteractionRequest janusRequest = new JanusConnectionInteractionRequest(
      action,
      UUID.randomUUID().toString(),
      pluginName
    );
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(janusRequest), StandardCharsets.UTF_8)
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
        return new ObjectMapper().readValue(bodyResponse, JanusIdResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, JanusErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
  }

  public JanusResponse destroyConnection(String connectionId) {
    return interactWithConnection(connectionId, JANUS_DESTROY, null);
  }

  public JanusResponse getHandleEvents(String connectionId, String handleId) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    try {
      CloseableHttpResponse response = httpClient.execute(request);
      String bodyResponse = IOUtils.toString(
        response.getEntity().getContent(),
        StandardCharsets.UTF_8
      );

      int statusCode = response.getStatusLine().getStatusCode();
      ObjectMapper mapper = new ObjectMapper();
      if (statusCode == HttpStatus.SC_OK) {
        return new JanusEventsResponse(
          mapper.readValue(
            bodyResponse,
            mapper.getTypeFactory().constructCollectionType(List.class, JanusEvent.class)
          )
        );
      }
      return mapper.readValue(bodyResponse, JanusErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
  }

  public JanusPluginResponse sendPluginMessage(String connectionId, String handleId, String messageType,
    @Nullable RoomRequest body) {
    CloseableHttpClient httpClient = HttpClients.createDefault();
    HttpPost request = new HttpPost(videoServerURL + JANUS_ENDPOINT + "/" + connectionId + "/" + handleId);
    request.setProtocolVersion(HttpVersion.HTTP_1_1);
    request.addHeader("content-type", "application/json");
    JanusMessage message = new JanusMessage(
      messageType,
      UUID.randomUUID().toString(),
      body
    );
    try {
      request.setEntity(
        new StringEntity(new ObjectMapper().writeValueAsString(message), StandardCharsets.UTF_8)
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
        return new ObjectMapper().readValue(bodyResponse, JanusRoomResponse.class);
      }
      return new ObjectMapper().readValue(bodyResponse, JanusRoomErrorResponse.class);
    } catch (IOException exception) {
      throw new RuntimeException();
    }
  }

  public JanusPluginResponse destroyPluginHandle(String connectionId, String handleId) {
    return sendPluginMessage(connectionId, handleId, JANUS_DETACH, null);
  }
}
