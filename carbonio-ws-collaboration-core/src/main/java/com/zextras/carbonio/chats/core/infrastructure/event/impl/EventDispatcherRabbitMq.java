// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker.QUEUE_ID_HEADER;
import static com.zextras.carbonio.chats.core.infrastructure.event.impl.EventBroker.USERS_HEADER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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

  private static final int MAX_PENDING_EVENTS = 10_000;
  private static final int MAX_USERS_PER_MESSAGE = 1_000;
  private static final long ENQUEUE_WAIT_MILLIS = 100;
  private static final long STALLED_PUBLISH_NANOS = TimeUnit.SECONDS.toNanos(1);
  private static final long IDLE = 0;

  private final ObjectMapper objectMapper;
  private final Connection connection;
  private volatile Channel channel;
  private volatile long publishingSince = IDLE;
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
  public EventDispatcherRabbitMq(
      @Named("publisher") Connection connection, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    this.connection = connection;
    try {
      this.channel = openChannel();
    } catch (IOException e) {
      throw new EventDispatcherException("Could not connect the event dispatcher to the broker", e);
    }
    Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "Event dispatcher shutdown"));
  }

  private Channel openChannel() throws IOException {
    Channel opened = connection.createChannel();
    EventBroker.declareExchange(opened);
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
    sendToUserExchange(List.of(userId), event);
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    serialize(event).ifPresent(body -> publishToUsers(usersIds, body));
  }

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    serialize(event).ifPresent(body -> publish(Map.of(QUEUE_ID_HEADER, queueId), body));
  }

  private Optional<byte[]> serialize(DomainEvent event) {
    try {
      return Optional.of(objectMapper.writeValueAsBytes(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn(String.format("Unable to serialize event '%s'", event.getType()), e);
      return Optional.empty();
    }
  }

  private void publishToUsers(List<String> usersIds, byte[] body) {
    for (int from = 0; from < usersIds.size(); from += MAX_USERS_PER_MESSAGE) {
      int to = Math.min(usersIds.size(), from + MAX_USERS_PER_MESSAGE);
      publish(Map.of(USERS_HEADER, List.copyOf(usersIds.subList(from, to))), body);
    }
  }

  private void publish(Map<String, Object> headers, byte[] body) {
    BasicProperties properties = new BasicProperties.Builder().headers(headers).build();
    publisher.execute(
        () -> {
          publishingSince = System.nanoTime();
          try {
            channel().basicPublish(EventBroker.EXCHANGE, "", properties, body);
          } catch (Exception e) {
            drop("the broker refused it: " + e.getMessage());
          } finally {
            publishingSince = IDLE;
          }
        });
  }

  private void waitOrDrop(Runnable event, ThreadPoolExecutor executor) {
    try {
      if (!executor.isShutdown()
          && !brokerStalled()
          && executor.getQueue().offer(event, ENQUEUE_WAIT_MILLIS, TimeUnit.MILLISECONDS)) return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    drop("too many events waiting for the broker");
  }

  private boolean brokerStalled() {
    long since = publishingSince;
    return since != IDLE && System.nanoTime() - since > STALLED_PUBLISH_NANOS;
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
