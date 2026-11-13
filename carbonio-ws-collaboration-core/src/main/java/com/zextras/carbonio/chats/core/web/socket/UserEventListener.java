// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.userIdOf;
import static com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions.versionOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
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

  private static final String EXCHANGE = "wsc.events";
  private static final String USERS_HEADER = "users";
  private static final String QUEUE_ID_HEADER = "queueId";
  private static final int MAX_QUEUE_LENGTH = 10_000;

  private final Connection connection;
  private final EventWebSocketSessions sessions;
  private final WebsocketVersionMigrator migrator;
  private final MessageBrokerVideoserverHealthMonitor healthMonitor;

  @Inject
  public UserEventListener(
      @Named("consumer") Connection connection,
      EventWebSocketSessions sessions,
      WebsocketVersionMigrator migrator,
      MessageBrokerVideoserverHealthMonitor healthMonitor) {
    this.connection = connection;
    this.sessions = sessions;
    this.migrator = migrator;
    this.healthMonitor = healthMonitor;
  }

  public void start() {
    watchBrokerHealth();
    try {
      subscribe();
    } catch (IOException e) {
      throw new EventDispatcherException("Could not start the user event listener", e);
    }
  }

  private void subscribe() throws IOException {
    Channel channel = connection.createChannel();
    channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT, true);
    String queue =
        channel
            .queueDeclare("", false, true, true, Map.of("x-max-length", MAX_QUEUE_LENGTH))
            .getQueue();
    channel.queueBind(queue, EXCHANGE, "");
    channel.basicConsume(
        queue, true, (tag, delivery) -> deliver(delivery), tag -> resubscribe(channel));
  }

  // closing the cancelled channel drops its queue and consumer from the recovery records
  private void resubscribe(Channel cancelled) {
    if (!connection.isOpen()) return;
    ChatsLogger.warn("User event listener lost its queue, subscribing again");
    try {
      cancelled.close();
      subscribe();
    } catch (IOException | TimeoutException e) {
      ChatsLogger.error("User event listener could not subscribe again", e);
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

  private void watchBrokerHealth() {
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
