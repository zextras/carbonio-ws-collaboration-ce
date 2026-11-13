// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker.EXCHANGE;
import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker.QUEUE_ID_HEADER;
import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker.USERS_HEADER;
import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.userIdOf;
import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.versionOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.websocket.Session;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Singleton
public class UserEventListener {

  private static final int MAX_QUEUE_LENGTH = 10_000;

  private final ConnectionFactory connectionFactory;
  private final EventWebSocketSessions sessions;
  private final WebsocketVersionMigrator migrator;
  private final MessageBrokerVideoserverHealthMonitor healthMonitor;
  private volatile Connection connection;

  @Inject
  public UserEventListener(
      ConnectionFactory connectionFactory,
      EventWebSocketSessions sessions,
      WebsocketVersionMigrator migrator,
      MessageBrokerVideoserverHealthMonitor healthMonitor) {
    this.connectionFactory = connectionFactory;
    this.sessions = sessions;
    this.migrator = migrator;
    this.healthMonitor = healthMonitor;
  }

  public void start() {
    connection = connect(connectionFactory);
  }

  private Connection connect(ConnectionFactory connectionFactory) {
    ConnectionFactory factory = connectionFactory.clone();
    factory.setTopologyRecoveryEnabled(true);
    try {
      Connection opened = factory.newConnection(EventBroker.connectionName("consumer"));
      watchBrokerHealth(opened);
      subscribe(opened);
      return opened;
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not start the user event listener", e);
    }
  }

  private void subscribe(Connection connection) throws IOException {
    Channel channel = connection.createChannel();
    EventBroker.declareExchange(channel);
    String queue =
        channel
            .queueDeclare("", false, true, true, Map.of("x-max-length", MAX_QUEUE_LENGTH))
            .getQueue();
    channel.queueBind(queue, EXCHANGE, "");
    channel.basicConsume(
        queue, true, (tag, delivery) -> deliver(delivery), tag -> resubscribe(connection, channel));
  }

  // closing the cancelled channel drops its queue and consumer from the recovery records
  private void resubscribe(Connection connection, Channel cancelled) {
    if (!connection.isOpen()) return;
    ChatsLogger.warn("User event listener lost its queue, subscribing again");
    try {
      cancelled.close();
      subscribe(connection);
    } catch (IOException | TimeoutException e) {
      ChatsLogger.error("User event listener could not subscribe again", e);
    }
  }

  public void stop() {
    try {
      if (connection != null && connection.isOpen()) connection.close();
    } catch (Exception e) {
      ChatsLogger.error("Error closing the user event listener connection", e);
    }
  }

  private void deliver(Delivery delivery) {
    try {
      send(recipientsOf(delivery.getProperties().getHeaders()), delivery.getBody());
    } catch (RuntimeException e) {
      ChatsLogger.warn("User event listener skipped an event it could not read", e);
    }
  }

  private void send(List<Session> recipients, byte[] body) {
    if (recipients.isEmpty()) return;
    String message = new String(body, StandardCharsets.UTF_8);
    Map<String, String> messageByVersion = new HashMap<>();
    for (Session session : recipients) {
      try {
        session
            .getAsyncRemote()
            .sendText(
                messageByVersion.computeIfAbsent(
                    versionOf(session), version -> downgrade(message, version)));
      } catch (Exception e) {
        ChatsLogger.warn(
            String.format(
                "Error sending event to user/queue '%s/%s'", userIdOf(session), session.getId()),
            e);
      }
    }
  }

  private List<Session> recipientsOf(Map<String, Object> headers) {
    if (headers == null) return List.of();
    Object queueId = headers.get(QUEUE_ID_HEADER);
    if (queueId != null) return sessions.get(queueId.toString()).stream().toList();
    if (!(headers.get(USERS_HEADER) instanceof List<?> users)) return List.of();
    return users.stream().flatMap(user -> sessions.sessionsOf(user.toString()).stream()).toList();
  }

  private String downgrade(String message, String version) {
    try {
      return migrator.downgradeIfNeeded(message, version);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
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
