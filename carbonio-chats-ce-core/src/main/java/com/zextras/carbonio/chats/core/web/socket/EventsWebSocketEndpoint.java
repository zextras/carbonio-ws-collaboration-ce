package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.UnauthorizedException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/events")
public class EventsWebSocketEndpoint {

  private final Connection           connection;
  private final ObjectMapper         objectMapper;
  private final Map<String, Channel> sessionsMap = new HashMap<>();

  public EventsWebSocketEndpoint(Connection connection, ObjectMapper objectMapper) {
    this.connection = connection;
    this.objectMapper = objectMapper;
  }

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) throws IOException, EncodeException {
    String userId = Optional.ofNullable((String)
        ((HttpSession) config.getUserProperties()
          .get(HttpSession.class.getName()))
          .getAttribute("userId"))
      .orElseThrow(UnauthorizedException::new);
    session.getBasicRemote().sendObject(objectMapper.writeValueAsString(SessionOut.create(session.getId())));
    sessionsMap.put(session.getId(), startBridge(session, userId));
    session.setMaxIdleTimeout(-1);
  }

  private Channel startBridge(Session session, String userId) {
    try {
      Channel channel = connection.createChannel();
      channel.queueDeclare(userId, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        try {
          session.getBasicRemote().sendObject(message);
        } catch (EncodeException e) {
          ChatsLogger.error(
            String.format("Error sending RabbitMQ message to websocket for user '%s'. Message: ''%s",
              userId, message), e);
        }
      };
      channel.basicConsume(userId, true, deliverCallback, consumerTag -> {
      });
      return channel;
    } catch (IOException e) {
      throw new InternalErrorException(String.format("Error running RabbitMQ client for user '%s'", userId), e);
    }
  }

  @OnClose
  public void onClose(Session session) throws IOException, EncodeException, TimeoutException {
    if (sessionsMap.containsKey(session.getId())) {
      sessionsMap.get(session.getId()).close();
      sessionsMap.remove(session.getId());
    }
    session.getBasicRemote().sendObject("Disconnected");
  }

  @OnError
  public void onError(Session session, Throwable throwable) throws IOException, TimeoutException {
    if (sessionsMap.containsKey(session.getId())) {
      sessionsMap.get(session.getId()).close();
      sessionsMap.remove(session.getId());
    }
    ChatsLogger.error(EventsWebSocketEndpoint.class, "Error on Chats websocket", throwable);
  }

  private static class SessionOut {

    private final String sessionId;

    public SessionOut(String sessionId) {
      this.sessionId = sessionId;
    }

    public static SessionOut create(String sessionId) {
      return new SessionOut(sessionId);
    }

    public String getSessionId() {
      return sessionId;
    }
  }
}

