// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private static final String EXCHANGE = "wsc.events";
  private static final String USERS_HEADER = "users";
  private static final String QUEUE_ID_HEADER = "queueId";
  private static final String VIDEO_SERVER_HEADER = "videoserver";
  private static final int MAX_PENDING_EVENTS = 10_000;

  private final ObjectMapper objectMapper;
  private final Connection connection;
  private volatile Channel channel;
  private final AtomicLong dropped = new AtomicLong();
  private final ThreadPoolExecutor publisher =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(MAX_PENDING_EVENTS),
          Thread.ofPlatform().name("wsc-event-publisher").daemon().factory(),
          (event, executor) -> drop("too many events waiting for the broker"));

  @Inject
  public EventDispatcherRabbitMq(
      @Named("publisher") Connection connection, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.connection = connection;
    try {
      this.channel = openChannel();
    } catch (IOException e) {
      throw new EventDispatcherException("Could not connect the event dispatcher to the broker", e);
    }
    watchConnection();
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Event dispatcher shutdown"));
  }

  private void watchConnection() {
    connection.addShutdownListener(
        cause -> {
          if (!cause.isInitiatedByApplication())
            ChatsLogger.warn(
                "Event dispatcher lost the message broker connection, dropping events until it is"
                    + " back");
        });
    if (!(connection instanceof Recoverable recoverable)) return;
    recoverable.addRecoveryListener(
        new RecoveryListener() {
          @Override
          public void handleRecovery(Recoverable recoverable) {
            ChatsLogger.info("Event dispatcher recovered the message broker connection");
          }

          @Override
          public void handleRecoveryStarted(Recoverable recoverable) {
            ChatsLogger.info("Event dispatcher is recovering the message broker connection");
          }
        });
  }

  private Channel openChannel() throws IOException {
    return connection.createChannel();
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
    sendToUserExchange(List.of(userId), event);
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    if (usersIds.isEmpty()) return;
    serialize(event).ifPresent(body -> publish(Map.of(USERS_HEADER, List.copyOf(usersIds)), body));
  }

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    serialize(event).ifPresent(body -> publish(Map.of(QUEUE_ID_HEADER, queueId), body));
  }

  @Override
  public void sendVideoServerHeartbeat(String emitter) {
    publish(Map.of(VIDEO_SERVER_HEADER, emitter), new byte[0]);
  }

  private Optional<byte[]> serialize(DomainEvent event) {
    try {
      return Optional.of(objectMapper.writeValueAsBytes(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn(String.format("Unable to serialize event '%s'", event.getType()), e);
      return Optional.empty();
    }
  }

  private void publish(Map<String, Object> headers, byte[] body) {
    BasicProperties properties = new BasicProperties.Builder().headers(headers).build();
    publisher.execute(
        () -> {
          try {
            channel().basicPublish(EXCHANGE, "", properties, body);
          } catch (Exception e) {
            drop("the broker refused it: " + e.getMessage());
          }
        });
  }

  private void drop(String reason) {
    long count = dropped.incrementAndGet();
    if (count % 1_000 == 1)
      ChatsLogger.warn(String.format("Event dropped, %s (%d dropped so far)", reason, count));
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
