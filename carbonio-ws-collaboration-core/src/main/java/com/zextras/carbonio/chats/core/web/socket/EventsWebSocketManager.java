// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.WebsocketConnected;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketManager {

  private final EventWebSocketSessions sessions;
  private final RabbitConnectionPoolService rabbitPool;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator migrator;
  private final CacheVideoServerSession cacheVideoServerSession;
  private final ParticipantService participantService;

  @Inject
  public EventsWebSocketManager(
      RabbitConnectionPoolService rabbitPool,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator migrator,
      CacheVideoServerSession cacheVideoServerSession,
      ParticipantService participantService,
      EventWebSocketSessions sessions) {
    this.rabbitPool = rabbitPool;
    this.objectMapper = objectMapper;
    this.migrator = migrator;
    this.cacheVideoServerSession = cacheVideoServerSession;
    this.participantService = participantService;
    this.sessions = sessions;
  }

  @OnOpen
  public void onOpen(Session session) throws IOException {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;

    DomainEvent wsConnected =
        WebsocketConnected.create()
            .queueId(queueId)
            .type(EventType.WEBSOCKET_CONNECTED)
            .sentDate(OffsetDateTime.now());
    session
        .getAsyncRemote()
        .sendObject(migrator.downgradeIfNeeded(wsConnected, getVersion(session)));

    sessions.add(session);

    DeliverCallback deliverCallback =
        (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          ChatsLogger.info(
              String.format(
                  "Received message from broker for user/queue '%s'%nMessage: '%s'",
                  userQueue, message));
          try {
            if (session.isOpen()) {
              session
                  .getAsyncRemote()
                  .sendObject(migrator.downgradeIfNeeded(message, getVersion(session)));
            }
          } catch (Exception e) {
            ChatsLogger.warn(
                String.format(
                    "Error sending event message to websocket for user/queue '%s'%nMessage: '%s'",
                    userQueue, message));
          }
        };

    try {
      rabbitPool.setupSession(
          userId.toString(),
          queueId.toString(),
          deliverCallback,
          msg -> {
            if (session.isOpen()) {
              session.getAsyncRemote().sendText(msg);
            }
          });
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error interacting with message broker for user/queue '%s'", userQueue));
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) {
    if (message == null || message.isBlank()) return;

    try {
      ObjectNode node = objectMapper.readValue(message, ObjectNode.class);
      Optional<JsonNode> optTypeKey = getKey(node, "type");
      if (optTypeKey.isEmpty()) return;
      String type = optTypeKey.get().asText();
      if (session.isOpen()) {
        switch (type) {
          case "ping", "PING", "Ping" -> {
            var pong = DomainEvent.create().type(EventType.PONG).sentDate(OffsetDateTime.now());
            session
                .getAsyncRemote()
                .sendObject(migrator.downgradeIfNeeded(pong, getVersion(session)));
          }
          case "IceRestart" -> {
            Optional<JsonNode> optMeetingIdKey = getKey(node, "meetingId");
            if (optMeetingIdKey.isEmpty()) return;
            String meetingId = optMeetingIdKey.get().asText();
            cacheVideoServerSession.remove(
                UUID.fromString(getUserIdFromSession(session)), meetingId);
            participantService.updateParticipantQueueId(
                UUID.fromString(getUserIdFromSession(session)),
                UUID.fromString(meetingId),
                UUID.fromString(session.getId()));
          }
          default ->
              ChatsLogger.info(
                  String.format("Unknown event type '%s' when parsing websocket message", type));
        }
      }
    } catch (Exception e) {
      UUID userId = UUID.fromString(getUserIdFromSession(session));
      UUID queueId = UUID.fromString(session.getId());
      String userQueue = userId + "/" + queueId;
      ChatsLogger.warn(String.format("Error sending pong to user/queue '%s'", userQueue));
      try {
        session.close();
      } catch (Exception ignored) {
      }
    }
  }

  @OnClose
  public void onClose(Session session) {
    handleSessionClose(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    handleSessionClose(session);
    try {
      session.close();
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format(
              "Error closing websocket session for user/queue '%s/%s'",
              getUserIdFromSession(session), session.getId()));
    }
  }

  private void handleSessionClose(Session session) {
    if (sessions.remove(session.getId())) {
      closeSessionWithBroker(session);
    }
  }

  private Optional<JsonNode> getKey(ObjectNode node, String key) {
    try {
      return Optional.of(node.get(key));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }

  private void closeSessionWithBroker(Session session) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    cacheVideoServerSession.add(userId, queueId);
    rabbitPool.teardownSession(session.getId());
  }

  private String getUserIdFromSession(Session session) {
    return (String)
        ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
            .getAttribute("userId");
  }

  private String getVersion(Session session) {
    return !session.getNegotiatedSubprotocol().isBlank()
        ? session.getNegotiatedSubprotocol()
        : "1.6.0";
  }
}
