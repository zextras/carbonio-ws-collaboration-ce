// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EncodeException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketEndpoint {

  private static final String PING = "ping";
  private static final String PONG = "pong";
  private final Map<String, String> consumerTagMap;
  private final Channel channel;
  private final ObjectMapper objectMapper;
  private final ParticipantService participantService;
  private final WaitingParticipantService waitingParticipantService;

  @Inject
  public EventsWebSocketEndpoint(
      Channel channel,
      ObjectMapper objectMapper,
      ParticipantService participantService,
      WaitingParticipantService waitingParticipantService) {
    this.channel = channel;
    this.objectMapper = objectMapper;
    this.participantService = participantService;
    this.waitingParticipantService = waitingParticipantService;
    this.consumerTagMap = new ConcurrentHashMap<>();
  }

  @OnOpen
  public void onOpen(Session session) throws IOException, EncodeException {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;
    session.setMaxIdleTimeout(0L);
    session
        .getBasicRemote()
        .sendObject(objectMapper.writeValueAsString(SessionOutEvent.create(queueId)));
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to open session %s: event websocket handler channel is not up!", queueId));
      return;
    }
    try {
      channel.queueDeclare(userQueue, false, false, true, null);
      channel.exchangeDeclare(userId.toString(), "direct");
      channel.queueBind(userQueue, userId.toString(), "");
      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
              if (session.isOpen()) {
                session.getBasicRemote().sendObject(message);
              }
            } catch (EncodeException | IOException e) {
              ChatsLogger.warn(
                  String.format(
                      "Error sending event message to websocket for user/queue '%s'%nMessage: '%s'",
                      userQueue, message));
            }
          };
      String tag = channel.basicConsume(userQueue, true, deliverCallback, consumerTag -> {});
      consumerTagMap.put(session.getId(), tag);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error interacting with message broker for user/queue '%s'", userQueue));
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) {
    try {
      if (objectMapper.readValue(message, PingPongDtoEvent.class).getType().equals(PING)
          && session.isOpen()) {
        session
            .getBasicRemote()
            .sendObject(objectMapper.writeValueAsString(PingPongDtoEvent.create()));
      }
    } catch (Exception e) {
      UUID userId = UUID.fromString(getUserIdFromSession(session));
      UUID queueId = UUID.fromString(session.getId());
      String userQueue = userId + "/" + queueId;
      ChatsLogger.warn(String.format("Error sending pong to user/queue '%s'", userQueue));
    }
  }

  @OnClose
  public void onClose(Session session) {
    closeSession(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;
    closeSession(session);
    try {
      session.close();
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error closing websocket session for user/queue '%s'", userQueue));
    }
  }

  private String getUserIdFromSession(Session session) {
    return Optional.ofNullable(
            (String)
                ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
                    .getAttribute("userId"))
        .orElseThrow(() -> new NotFoundException("Session user not found!"));
  }

  private void closeSession(Session session) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    String sessionId = session.getId();
    UUID queueId = UUID.fromString(sessionId);
    String userQueue = userId + "/" + queueId;
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to close session %s: event websocket handler channel is not up!", queueId));
      return;
    }
    basicCancel(sessionId, userQueue);
    queueDeleteNoWait(userQueue);
    participantService.removeMeetingParticipant(queueId);
    waitingParticipantService.removeFromQueue(queueId);
  }

  private void queueDeleteNoWait(String userQueue) {
    try {
      channel.queueDeleteNoWait(userQueue, false, false);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Error deleting queue for user/queue '%s'", userQueue), e);
    }
  }

  private void basicCancel(String sessionId, String userQueue) {
    String tag = consumerTagMap.remove(sessionId);
    if (tag != null) {
      try {
        channel.basicCancel(tag);
      } catch (Exception e) {
        ChatsLogger.warn(
                String.format("Error cancelling consumer for user/queue '%s'", userQueue), e);
      }
    }
  }

  private static class SessionOutEvent {

    private static final String WEBSOCKET_CONNECTED = "websocketConnected";

    private final UUID queueId;

    public SessionOutEvent(UUID queueId) {
      this.queueId = queueId;
    }

    public static SessionOutEvent create(UUID queueId) {
      return new SessionOutEvent(queueId);
    }

    public UUID getQueueId() {
      return queueId;
    }

    public String getType() {
      return WEBSOCKET_CONNECTED;
    }
  }

  private static class PingPongDtoEvent {

    private String type;

    private static PingPongDtoEvent create() {
      return new PingPongDtoEvent().type(PONG);
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
