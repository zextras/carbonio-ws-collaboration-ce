// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private final Connection   connection;
  private final ObjectMapper objectMapper;

  @Inject
  public EventDispatcherRabbitMq(Optional<Connection> connection, ObjectMapper objectMapper) {
    this.connection = connection.orElse(null);
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    return connection != null && connection.isOpen();
  }

  @Override
  public void sendToUserExchange(String userId, DomainEvent event) {
    try {
      sendToExchange(userId, objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    }
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    try {
      sendToExchange(usersIds, objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    }
  }

  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    if (Optional.ofNullable(connection).isEmpty()) {
      ChatsLogger.error("RabbitMQ connection is not up!");
      return;
    }
    Channel channel;
    try {
      channel = connection.createChannel();
    } catch (IOException e) {
      ChatsLogger.error(String.format("Error creating RabbitMQ connection channel for user '%s'", userId), e);
      return;
    }
    try {
      channel.queueDeclare(userId + "/" + queueId,
        false,
        false,
        false,
        null
      );
      channel.basicPublish("",
        userId + "/" + queueId,
        null,
        objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8)
      );
      channel.close();
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.error(String.format("Error closing RabbitMQ connection channel for user '%s'", userId));
      }
    }
  }

  private void sendToExchange(String userId, String message) {
    if (Optional.ofNullable(connection).isEmpty()) {
      ChatsLogger.warn("RabbitMQ connection is not up!");
      return;
    }
    Channel channel;
    try {
      channel = connection.createChannel();
    } catch (IOException e) {
      ChatsLogger.error(String.format("Error creating RabbitMQ connection channel for user '%s'", userId), e);
      return;
    }
    try {
      channel.exchangeDeclare(userId, "direct");
      channel.basicPublish(userId, "", null, message.getBytes(StandardCharsets.UTF_8));
      channel.close();
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
      try {
        channel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.error(String.format("Error closing RabbitMQ connection channel for user '%s'", userId));
      }
    }
  }

  private void sendToExchange(List<String> usersIds, String message) {
    usersIds.forEach(userId -> sendToExchange(userId, message));
  }
}
