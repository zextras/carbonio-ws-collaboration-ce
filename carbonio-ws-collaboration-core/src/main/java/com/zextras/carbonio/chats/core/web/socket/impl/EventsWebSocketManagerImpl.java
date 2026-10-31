// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.impl;

import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.userIdOf;
import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.versionOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.WebsocketConnected;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketManagerImpl implements EventsWebSocketManager {

  private final EventWebSocketSessions sessions;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator migrator;
  private final CacheVideoServerSession cacheVideoServerSession;
  private final ParticipantService participantService;

  @Inject
  public EventsWebSocketManagerImpl(
      ObjectMapper objectMapper,
      WebsocketVersionMigrator migrator,
      CacheVideoServerSession cacheVideoServerSession,
      ParticipantService participantService,
      EventWebSocketSessions sessions) {
    this.objectMapper = objectMapper;
    this.migrator = migrator;
    this.cacheVideoServerSession = cacheVideoServerSession;
    this.participantService = participantService;
    this.sessions = sessions;
  }

  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  public EventsWebSocketManagerImpl(
      RabbitConnectionPoolService rabbitPool,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator migrator,
      CacheVideoServerSession cacheVideoServerSession,
      ParticipantService participantService,
      EventWebSocketSessions sessions) {
    this(objectMapper, migrator, cacheVideoServerSession, participantService, sessions);
  }

  @Override
  @OnOpen
  public void onOpen(Session session) throws IOException {
    sessions.add(session);
    DomainEvent wsConnected =
        WebsocketConnected.create()
            .queueId(UUID.fromString(session.getId()))
            .type(EventType.WEBSOCKET_CONNECTED)
            .sentDate(OffsetDateTime.now());
    session
        .getAsyncRemote()
        .sendObject(migrator.downgradeIfNeeded(wsConnected, versionOf(session)));
  }

  @Override
  @OnMessage
  public void onMessage(Session session, String message) {
    if (message == null || message.isBlank()) return;

    try {
      ObjectNode node = objectMapper.readValue(message, ObjectNode.class);
      Optional<JsonNode> optTypeKey = getKey(node, "type");
      if (optTypeKey.isEmpty() || !session.isOpen()) return;
      String type = optTypeKey.get().asText();
      switch (type) {
        case "ping", "PING", "Ping" -> sendPong(session);
        case "IceRestart" -> onIceRestart(session, node);
        case "UplinkStatusUpdate" -> onUplinkStatusUpdate(session, node);
        default ->
            ChatsLogger.info(
                String.format("Unknown event type '%s' when parsing websocket message", type));
      }
    } catch (Exception e) {
      UUID userId = UUID.fromString(userIdOf(session));
      UUID queueId = UUID.fromString(session.getId());
      String userQueue = userId + "/" + queueId;
      ChatsLogger.warn(String.format("Error sending pong to user/queue '%s'", userQueue));
      try {
        session.close();
      } catch (Exception closeException) {
        ChatsLogger.warn(
            String.format(
                "Error closing websocket session after a message failure for user/queue '%s'",
                userQueue));
      }
    }
  }

  private void sendPong(Session session) throws JsonProcessingException {
    var pong = DomainEvent.create().type(EventType.PONG).sentDate(OffsetDateTime.now());
    session.getAsyncRemote().sendObject(migrator.downgradeIfNeeded(pong, versionOf(session)));
  }

  private void onIceRestart(Session session, ObjectNode node) {
    Optional<JsonNode> optMeetingIdKey = getKey(node, "meetingId");
    if (optMeetingIdKey.isEmpty()) return;
    String meetingId = optMeetingIdKey.get().asText();
    UUID userId = UUID.fromString(userIdOf(session));
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
          userIdOf(session),
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
              userIdOf(session), session.getId()));
    }
  }

  private void handleSessionClose(Session session) {
    if (sessions.remove(session.getId())) {
      forgetSession(session);
    }
  }

  private Optional<JsonNode> getKey(ObjectNode node, String key) {
    try {
      return Optional.of(node.get(key));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }

  private void forgetSession(Session session) {
    UUID userId = UUID.fromString(userIdOf(session));
    UUID queueId = UUID.fromString(session.getId());
    cacheVideoServerSession.add(userId, queueId);
    onSessionDisconnected(queueId);
  }

  protected void onSessionDisconnected(UUID queueId) {}
}
