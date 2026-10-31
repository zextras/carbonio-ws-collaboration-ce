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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  public static final String EXCHANGE = "wsc.events";
  public static final String QUEUE_ID_HEADER = "queueId";

  private static final int MAX_PENDING_EVENTS = 10_000;
  private static final BasicProperties NO_PROPERTIES = new BasicProperties.Builder().build();

  private final ObjectMapper objectMapper;
  private final Connection connection;
  private volatile Channel channel;
  private final ThreadPoolExecutor publisher =
      new ThreadPoolExecutor(
          1,
          1,
          0L,
          TimeUnit.MILLISECONDS,
          new ArrayBlockingQueue<>(MAX_PENDING_EVENTS),
          Thread.ofPlatform().name("wsc-event-publisher").daemon().factory(),
          (event, executor) ->
              ChatsLogger.warn("Event dropped: too many events waiting for the broker"));

  @Inject
  public EventDispatcherRabbitMq(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    try {
      this.connection = connectionFactory.newConnection(connectionName("publisher"));
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
    publish(userId, NO_PROPERTIES, event);
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    usersIds.forEach(userId -> publish(userId, NO_PROPERTIES, event));
  }

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    publish(
        userId,
        new BasicProperties.Builder().headers(Map.of(QUEUE_ID_HEADER, queueId)).build(),
        event);
  }

  private void publish(String userId, BasicProperties properties, DomainEvent event) {
    byte[] body;
    try {
      body = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
    } catch (JsonProcessingException e) {
      ChatsLogger.warn(String.format("Unable to serialize event for user '%s'", userId), e);
      return;
    }
    publisher.execute(
        () -> {
          try {
            channel().basicPublish(EXCHANGE, userId, properties, body);
          } catch (Exception e) {
            ChatsLogger.warn(String.format("Unable to send event to user '%s'", userId), e);
          }
        });
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
