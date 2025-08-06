// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.WebsocketConnected;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@ServerEndpoint(value = "/events")
public class EventsWebSocketManager {

  private static final String USER_ROUTING_KEY = "user-events";

  private final Map<String, String> consumerTagMap;
  private final Channel channel;
  private final ObjectMapper objectMapper;
  private final WebsocketVersionMigrator migrator;
  private final ParticipantService participantService;

  @Inject
  public EventsWebSocketManager(
      Channel channel,
      ObjectMapper objectMapper,
      WebsocketVersionMigrator migrator,
      ParticipantService participantService) {
    this.channel = channel;
    this.objectMapper = objectMapper;
    this.migrator = migrator;
    this.participantService = participantService;
    this.consumerTagMap = new ConcurrentHashMap<>();
    Runtime.getRuntime()
        .addShutdownHook(new Thread(this::stop, "Event websocket manager shutdown hook"));
  }

  @OnOpen
  public void onOpen(Session session) throws IOException {
    SessionPingManager.add(session);

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
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to open websocket session %s: event websocket manager channel is not up!",
              queueId));
      return;
    }
    try {
      channel.exchangeDeclare(userId.toString(), BuiltinExchangeType.DIRECT, false, false, null);
      channel.queueDeclare(queueId.toString(), false, false, true, null);
      channel.queueBind(queueId.toString(), userId.toString(), USER_ROUTING_KEY);
      DeliverCallback deliverCallback =
          (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
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
      String tag =
          channel.basicConsume(queueId.toString(), true, deliverCallback, consumerTag -> {});
      consumerTagMap.put(queueId.toString(), tag);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error interacting with message broker for user/queue '%s'", userQueue));
    }
  }

  @OnMessage
  public void onMessage(Session session, String message) {
    if (message == null || message.isBlank()) return;

    try {
      /** Necessary for naming retro compatibility */
      ObjectNode node = objectMapper.readValue(message, ObjectNode.class);
      Optional<JsonNode> optTypeKey = getKey(node, "type");
      if (optTypeKey.isEmpty()) return;

      String type = optTypeKey.get().asText();
      if (type.equalsIgnoreCase("ping") && session.isOpen()) {
        /**
         * TODO: The Ping event is not websocket native. It is not needed anymore so events
         * (EventType.PING, EventType.PONG) will be removed as soon as possible.
         */
        var pong = DomainEvent.create().type(EventType.PONG).sentDate(OffsetDateTime.now());
        session.getAsyncRemote().sendObject(migrator.downgradeIfNeeded(pong, getVersion(session)));
      }
    } catch (Exception e) {
      SessionPingManager.remove(session);
      UUID userId = UUID.fromString(getUserIdFromSession(session));
      UUID queueId = UUID.fromString(session.getId());
      String userQueue = userId + "/" + queueId;
      ChatsLogger.warn(String.format("Error sending pong to user/queue '%s'", userQueue));
    }
  }

  private Optional<JsonNode> getKey(ObjectNode node, String key) {
    try {
      return Optional.of(node.get(key));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }

  @OnClose
  public void onClose(Session session) {
    SessionPingManager.remove(session);
    closeSession(session);
  }

  @OnError
  public void onError(Session session, Throwable throwable) {
    SessionPingManager.remove(session);
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    UUID queueId = UUID.fromString(session.getId());
    String userQueue = userId + "/" + queueId;
    try {
      session.close();
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error closing websocket session for user/queue '%s'", userQueue));
    }
  }

  private String getUserIdFromSession(Session session) {
    return (String)
        ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
            .getAttribute("userId");
  }

  private String getVersion(Session session) {
    // OLD_CLIENT_FALLBACK
    // If the requested sub-protocol is an empty string, it means the clients aren't passing any
    // sub-protocols,
    // so they are old clients. We use 1.6.0 version to execute event migrations and ensure
    // retro-compatibility.
    return !session.getNegotiatedSubprotocol().isBlank()
        ? session.getNegotiatedSubprotocol()
        : "1.6.0";
  }

  private void closeSession(Session session) {
    UUID userId = UUID.fromString(getUserIdFromSession(session));
    String sessionId = session.getId();
    UUID queueId = UUID.fromString(sessionId);
    String userQueue = userId + "/" + queueId;

    participantService.removeMeetingParticipant(queueId);

    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error(
          String.format(
              "Unable to close websocket session %s: event websocket handler channel is not up!",
              queueId));
      return;
    }

    queueConsumerCleanup(userQueue, queueId.toString(), userId.toString());
  }

  private void queueConsumerCleanup(String userQueue, String queueId, String userId) {
    if (channel != null && channel.isOpen()) {
      basicCancel(queueId, userQueue);
      queueUnBind(userQueue, queueId, userId);
      queueDeleteNoWait(userQueue, queueId);
    }
  }

  private void basicCancel(String queueId, String userQueue) {
    String tag = consumerTagMap.get(queueId);
    if (tag != null) {
      try {
        channel.basicCancel(tag);
        consumerTagMap.remove(queueId);
      } catch (Exception e) {
        ChatsLogger.warn(String.format("Error cancelling consumer for user/queue '%s'", userQueue));
      }
    }
  }

  private void queueUnBind(String userQueue, String queueId, String userId) {
    try {
      channel.queueUnbind(queueId, userId, USER_ROUTING_KEY);
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error unbinding queue from exchange for user/queue '%s'", userQueue));
    }
  }

  private void queueDeleteNoWait(String userQueue, String queueId) {
    try {
      channel.queueDeleteNoWait(queueId, false, false);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Error deleting queue for user/queue '%s'", userQueue));
    }
  }

  public void stop() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
        ChatsLogger.info("Event websocket manager channel closed successfully.");
      }
    } catch (Exception e) {
      ChatsLogger.error("Error during stopping event websocket manager", e);
    }
  }
}
