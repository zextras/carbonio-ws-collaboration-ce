// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  public static final String EXCHANGE = "wsc.events";
  public static final String QUEUE_ID_HEADER = "queueId";
  public static final String USERS_HEADER = "users";

  private static final int MAX_PENDING_EVENTS = 10_000;
  private static final int MAX_USERS_PER_MESSAGE = 1_000;
  private static final long ENQUEUE_WAIT_MILLIS = 100;
  private static final BasicProperties NO_PROPERTIES = new BasicProperties.Builder().build();

  private final ObjectMapper objectMapper;
  private final Connection connection;
  private volatile Channel channel;
  private volatile boolean brokerBlocked;
  private final AtomicLong dropped = new AtomicLong();
  private final ThreadPoolExecutor publisher =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(MAX_PENDING_EVENTS),
          Thread.ofPlatform().name("wsc-event-publisher").daemon().factory(),
          this::waitOrDrop);

  @Inject
  public EventDispatcherRabbitMq(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    try {
      this.connection = connectionFactory.newConnection(connectionName("publisher"));
      connection.addBlockedListener(reason -> brokerBlocked = true, () -> brokerBlocked = false);
      this.channel = openChannel();
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not connect the event dispatcher to the broker", e);
    }
  }

  public static String connectionName(String role) {
    try {
      return "carbonio-ws-collaboration-" + role + "@" + InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "carbonio-ws-collaboration-" + role;
    }
  }

  private Channel openChannel() throws IOException {
    Channel opened = connection.createChannel();
    opened.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC, true);
    return opened;
  }

  private Channel channel() throws IOException {
    if (!channel.isOpen()) channel = openChannel();
    return channel;
  }

  @Override
  public boolean isAlive() {
    return channel.isOpen();
  }

  @Override
  public void sendToUserExchange(String userId, DomainEvent event) {
    serialize(event).ifPresent(body -> publish(userId, NO_PROPERTIES, body));
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    if (usersIds.size() == 1) {
      sendToUserExchange(usersIds.getFirst(), event);
      return;
    }
    serialize(event)
        .ifPresent(
            body -> {
              for (int from = 0; from < usersIds.size(); from += MAX_USERS_PER_MESSAGE) {
                List<String> users =
                    List.copyOf(
                        usersIds.subList(
                            from, Math.min(usersIds.size(), from + MAX_USERS_PER_MESSAGE)));
                publish(
                    USERS_HEADER,
                    new BasicProperties.Builder().headers(Map.of(USERS_HEADER, users)).build(),
                    body);
              }
            });
  }

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    serialize(event)
        .ifPresent(
            body ->
                publish(
                    userId,
                    new BasicProperties.Builder().headers(Map.of(QUEUE_ID_HEADER, queueId)).build(),
                    body));
  }

  private Optional<byte[]> serialize(DomainEvent event) {
    try {
      return Optional.of(objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn(String.format("Unable to serialize event '%s'", event.getType()), e);
      return Optional.empty();
    }
  }

  private void publish(String routingKey, BasicProperties properties, byte[] body) {
    publisher.execute(
        () -> {
          try {
            channel().basicPublish(EXCHANGE, routingKey, properties, body);
          } catch (Exception e) {
            ChatsLogger.warn("Unable to send event to the message broker", e);
          }
        });
  }

  private void waitOrDrop(Runnable event, ThreadPoolExecutor executor) {
    try {
      if (!brokerBlocked
          && !executor.isShutdown()
          && executor.getQueue().offer(event, ENQUEUE_WAIT_MILLIS, TimeUnit.MILLISECONDS)) return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    long count = dropped.incrementAndGet();
    if (count % 1_000 == 1)
      ChatsLogger.warn(
          String.format(
              "Event dropped: too many events waiting for the broker (%d dropped so far)", count));
  }

  @Override
  public void stop() {
    publisher.shutdown();
    try {
      publisher.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    try {
      if (connection.isOpen()) connection.close();
    } catch (Exception e) {
      ChatsLogger.error("Error closing the event dispatcher connection", e);
    }
  }
}
