// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq.EXCHANGE;
import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq.QUEUE_ID_HEADER;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Singleton
public class UserEventListener {

  private static final int MAX_QUEUE_LENGTH = 10_000;

  private final EventWebSocketSessions sessions;
  private final WebsocketVersionMigrator migrator;
  private final MessageBrokerVideoserverHealthMonitor healthMonitor;
  private final Connection connection;

  @Inject
  public UserEventListener(
      ConnectionFactory connectionFactory,
      EventWebSocketSessions sessions,
      WebsocketVersionMigrator migrator,
      MessageBrokerVideoserverHealthMonitor healthMonitor) {
    this.sessions = sessions;
    this.migrator = migrator;
    this.healthMonitor = healthMonitor;
    this.connection = connect(connectionFactory);
  }

  private Connection connect(ConnectionFactory connectionFactory) {
    ConnectionFactory factory = connectionFactory.clone();
    factory.setTopologyRecoveryEnabled(true);
    try {
      Connection opened = factory.newConnection(EventDispatcherRabbitMq.connectionName("consumer"));
      watchBrokerHealth(opened);
      Channel channel = opened.createChannel();
      channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true);
      String queue =
          channel
              .queueDeclare("", false, true, true, Map.of("x-max-length", MAX_QUEUE_LENGTH))
              .getQueue();
      channel.queueBind(queue, EXCHANGE, "#");
      channel.basicConsume(queue, true, (tag, delivery) -> deliver(delivery), tag -> {});
      return opened;
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not start the user event listener", e);
    }
  }

  public void stop() {
    try {
      if (connection.isOpen()) connection.close();
    } catch (Exception e) {
      ChatsLogger.error("Error closing the user event listener connection", e);
    }
  }

  private void deliver(Delivery delivery) {
    String userId = delivery.getEnvelope().getRoutingKey();
    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
    targetSessions(userId, queueIdOf(delivery)).forEach(session -> send(session, userId, message));
  }

  private List<Session> targetSessions(String userId, Optional<String> queueId) {
    return queueId
        .map(id -> sessions.get(id).stream().toList())
        .orElse(sessions.sessionsOf(userId));
  }

  private static Optional<String> queueIdOf(Delivery delivery) {
    Map<String, Object> headers = delivery.getProperties().getHeaders();
    if (headers == null || headers.get(QUEUE_ID_HEADER) == null) return Optional.empty();
    return Optional.of(headers.get(QUEUE_ID_HEADER).toString());
  }

  private void send(Session session, String userId, String message) {
    try {
      session
          .getAsyncRemote()
          .sendText(migrator.downgradeIfNeeded(message, EventWebSocketSessions.versionOf(session)));
      ChatsLogger.info(String.format("Sent event to user/queue '%s/%s'", userId, session.getId()));
    } catch (Exception e) {
      ChatsLogger.warn(
          String.format("Error sending event to user/queue '%s/%s'", userId, session.getId()), e);
    }
  }

  private void watchBrokerHealth(Connection connection) {
    connection.addShutdownListener(
        cause -> {
          if (!cause.isInitiatedByApplication()) {
            ChatsLogger.warn("User event listener lost the message broker connection");
            healthMonitor.notifyBrokerDown();
          }
        });
    if (!(connection instanceof Recoverable recoverable)) return;
    recoverable.addRecoveryListener(
        new RecoveryListener() {
          @Override
          public void handleRecovery(Recoverable recoverable) {
            ChatsLogger.warn("User event listener recovered the message broker connection");
            healthMonitor.notifyBrokerRecovered();
          }

          @Override
          public void handleRecoveryStarted(Recoverable recoverable) {
            ChatsLogger.warn("User event listener is recovering the message broker connection");
          }
        });
  }
}
