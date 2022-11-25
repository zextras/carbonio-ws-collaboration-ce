package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketEndpoint {

  private static final int MAX_IDLE_TIMEOUT = 90000;

  private final Connection                 rabbitMqConnection;
  private final ObjectMapper               objectMapper;
  private final Map<String, List<Session>> userSessionsMap = new HashMap<>();

  @Inject
  public EventsWebSocketEndpoint(Optional<Connection> rabbitMqConnection, ObjectMapper objectMapper) {
    this.rabbitMqConnection = rabbitMqConnection.orElse(null);
    this.objectMapper = objectMapper;
  }

  @OnOpen
  public void onOpen(Session session) throws IOException, EncodeException {
    String userId = getSessionUser(session);
    session.setMaxIdleTimeout(MAX_IDLE_TIMEOUT);
    session.getBasicRemote().sendObject(objectMapper.writeValueAsString(SessionOutEvent.create(session.getId())));
    userSessionsMap.computeIfAbsent(userId, k -> new ArrayList<>());
    userSessionsMap.get(userId).add(session);
    startBridge(userId);
  }

  @OnMessage
  public void onMessage(Session session, String message) throws EncodeException, IOException {
    try {
      if (objectMapper.readValue(message, PingPongDtoEvent.class).getType().equals("ping")) {
        session.getBasicRemote().sendObject(objectMapper.writeValueAsString(PingPongDtoEvent.create("pong")));
      }
    } catch (JsonProcessingException e) {
      // intentionally left blank
    }
  }

  @OnClose
  public void onClose(Session session) throws IOException, EncodeException {
    closeAndRemoveSession(session);
    session.getBasicRemote().sendObject("Disconnected");
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    closeAndRemoveSession(session);
  }

  private String getSessionUser(Session session) {
    return Optional.ofNullable((String)
      ((HttpSession) session.getUserProperties()
        .get(HttpSession.class.getName()))
        .getAttribute("userId")).orElseThrow(() ->
      new InternalErrorException("Session user not found for closeAndRemoveSession"));
  }

  public void closeAndRemoveSession(Session session) {
    String userId = getSessionUser(session);
    if (userSessionsMap.containsKey(userId)) {
      Optional<Session> toRemove = userSessionsMap.get(userId).stream()
        .filter(s -> s.getId().equals(session.getId()))
        .findAny();
      if (toRemove.isPresent()) {
        if (session.isOpen()) {
          try {
            toRemove.get().close();
          } catch (IOException ignored) {
          }
        }
        userSessionsMap.get(userId).remove(toRemove.get());
        if (userSessionsMap.get(userId).isEmpty()) {
          userSessionsMap.remove(userId);
        }
      }
    }
  }

  private void startBridge(String userId) {
    try {
      Channel channel = rabbitMqConnection.createChannel();
      channel.queueDeclare(userId, true, false, false, null);
      DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
        userSessionsMap.get(userId).forEach(session -> {
            try {
              session.getBasicRemote().sendObject(message);
            } catch (EncodeException | IOException e) {
              ChatsLogger.error(
                String.format("Error sending RabbitMQ message to websocket for user '%s'. Message: ''%s",
                  userId, message), e);
            }
          }
        );
      };
      channel.basicConsume(userId, true, deliverCallback, consumerTag -> {
      });
    } catch (IOException e) {
      throw new InternalErrorException(String.format("Error running RabbitMQ client for user '%s'", userId), e);
    }
  }

  private static class SessionOutEvent {

    private final String sessionId;
    private final String type = "connected";

    public SessionOutEvent(String sessionId) {
      this.sessionId = sessionId;
    }

    public static SessionOutEvent create(String sessionId) {
      return new SessionOutEvent(sessionId);
    }

    public String getSessionId() {
      return sessionId;
    }

    public String getType() {
      return type;
    }
  }

  private static class PingPongDtoEvent {

    private String type;

    private static PingPongDtoEvent create(String type) {
      return new PingPongDtoEvent().type(type);
    }

    public String getType() {
      return type;
    }

    public PingPongDtoEvent type(String type) {
      this.type = type;
      return this;
    }
  }
}

