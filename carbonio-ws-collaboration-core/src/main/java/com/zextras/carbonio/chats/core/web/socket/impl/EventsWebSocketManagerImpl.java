// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionRouter;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketManagerImpl implements EventsWebSocketManager {

  private final EventWebSocketSessions sessions;
  private final RabbitConnectionPoolService rabbitPool;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator migrator;
  private final CacheVideoServerSession cacheVideoServerSession;
  private final ParticipantService participantService;
  private final WsActionRouter wsActionRouter;
  private final WsPresenceManager wsPresenceManager;

  @Inject
  public EventsWebSocketManagerImpl(
      RabbitConnectionPoolService rabbitPool,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator migrator,
      CacheVideoServerSession cacheVideoServerSession,
      ParticipantService participantService,
      WsActionRouter wsActionRouter,
      WsPresenceManager wsPresenceManager,
      EventWebSocketSessions sessions) {
    this.rabbitPool = rabbitPool;
    this.objectMapper = objectMapper;
    this.migrator = migrator;
    this.cacheVideoServerSession = cacheVideoServerSession;
    this.participantService = participantService;
    this.wsActionRouter = wsActionRouter;
    this.wsPresenceManager = wsPresenceManager;
    this.sessions = sessions;
  }

  @Override
  @OnOpen
  public void onOpen(Session session) throws IOException {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;

    sessions.add(session, userId.toString());

    DeliverCallback deliverCallback =
        (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
          ChatsLogger.debug(
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

    // Sent once subscribed: events published after the client receives it reach this socket.
    DomainEvent wsConnected =
        WebsocketConnected.create()
            .queueId(queueId)
            .type(EventType.WEBSOCKET_CONNECTED)
            .sentDate(OffsetDateTime.now());
    session
        .getAsyncRemote()
        .sendObject(migrator.downgradeIfNeeded(wsConnected, getVersion(session)));

    wsPresenceManager.onConnect(userId.toString(), session);
  }

  @OnMessage
  public void onPong(Session session, PongMessage pongMessage) {
    // Pong is handled by SessionPingManager via EventWebSocketSessions
  }

  @Override
  @OnMessage
  public void onMessage(Session session, String message) {
    if (message == null || message.isBlank()) return;

    try {
      ObjectNode node = objectMapper.readValue(message, ObjectNode.class);

      Optional<JsonNode> optActionKey = getKey(node, "action");
      if (optActionKey.isPresent()) {
        wsActionRouter.route(getUserIdFromSession(session), message, session);
        return;
      }

      Optional<JsonNode> optTypeKey = getKey(node, "type");
      if (optTypeKey.isEmpty() || !session.isOpen()) return;
      String type = optTypeKey.get().asText();
      switch (type) {
        case "ping", "PING", "Ping" -> sendPong(session);
        case "IceRestart" -> onIceRestart(session, node);
        case "UplinkStatusUpdate" -> onUplinkStatusUpdate(session, node);
        default ->
            ChatsLogger.warn(
                String.format("Unknown event type '%s' when parsing websocket message", type));
      }
    } catch (Exception e) {
      UUID userId = UUID.fromString(getUserIdFromSession(session));
      UUID queueId = UUID.fromString(session.getId());
      String userQueue = userId + "/" + queueId;
      ChatsLogger.warn(
          String.format("Error parsing websocket message for user/queue '%s'", userQueue));
    }
  }

  private void sendPong(Session session) throws JsonProcessingException {
    var pong = DomainEvent.create().type(EventType.PONG).sentDate(OffsetDateTime.now());
    session.getAsyncRemote().sendObject(migrator.downgradeIfNeeded(pong, getVersion(session)));
  }

  private void onIceRestart(Session session, ObjectNode node) {
    Optional<JsonNode> optMeetingIdKey = getKey(node, "meetingId");
    if (optMeetingIdKey.isEmpty()) return;
    String meetingId = optMeetingIdKey.get().asText();
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    cacheVideoServerSession.remove(userId, meetingId);
    participantService.updateParticipantQueueId(
        userId, UUID.fromString(meetingId), UUID.fromString(session.getId()));
  }

  private void onUplinkStatusUpdate(Session session, ObjectNode node) {
    try {
      Optional<JsonNode> optMeetingIdNode = getKey(node, "meetingId");
      Optional<JsonNode> optChangedAtNode = getKey(node, "changedAt");
      if (optMeetingIdNode.isEmpty() || optChangedAtNode.isEmpty()) return;

      participantService.relayUplinkStatus(
          optMeetingIdNode.get().asText(),
          parseNetworkScore(node),
          optChangedAtNode.get().asLong(),
          parseToUserId(node),
          getUserIdFromSession(session),
          parseTier(node, "maxUplinkTier"),
          parseTier(node, "maxHardwareTier"));
    } catch (Exception e) {
      ChatsLogger.debug("Dropped malformed UplinkStatusUpdate message: " + e.getMessage(), e);
    }
  }

  private String parseToUserId(ObjectNode node) {
    return getKey(node, "to")
        .filter(to -> !to.isNull() && !to.asText().isEmpty())
        .map(JsonNode::asText)
        .orElse(null);
  }

  private BigDecimal parseNetworkScore(ObjectNode node) {
    JsonNode networkScoreNode = node.get("networkScore");
    return networkScoreNode != null && networkScoreNode.isNumber()
        ? networkScoreNode.decimalValue()
        : null;
  }

  private Integer parseTier(ObjectNode node, String field) {
    JsonNode tierNode = node.get(field);
    if (tierNode == null || !tierNode.isInt()) return null;
    int tier = tierNode.intValue();
    return tier >= 0 && tier <= 2 ? tier : null;
  }

  @Override
  @OnClose
  public void onClose(Session session) {
    handleSessionClose(session);
  }

  @Override
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
    try {
      cacheVideoServerSession.add(userId, queueId);
      onSessionDisconnected(queueId);
    } finally {
      wsPresenceManager.onDisconnect(userId.toString(), session);
      rabbitPool.teardownSession(session.getId());
    }
  }

  protected void onSessionDisconnected(UUID queueId) {}

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
