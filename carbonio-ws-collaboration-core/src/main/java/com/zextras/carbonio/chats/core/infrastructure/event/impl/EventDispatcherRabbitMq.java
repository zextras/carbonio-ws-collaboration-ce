// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private final Optional<Channel> channel;
  private final ObjectMapper objectMapper;

  @Inject
  public EventDispatcherRabbitMq(Optional<Channel> channel, ObjectMapper objectMapper) {
    this.channel = channel;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    return channel.isPresent() && channel.get().isOpen();
  }

  @Override
  public Optional<Channel> getChannel() {
    return channel;
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

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    if (channel.isEmpty()) {
      ChatsLogger.error("RabbitMQ connection channel is not up!");
      return;
    }
    Channel eventChannel = channel.get();
    try {
      String queueName = userId + "/" + queueId;
      eventChannel.queueDeclare(queueName, false, false, true, null);
      eventChannel.basicPublish(
          "",
          queueName,
          null,
          objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
      try {
        eventChannel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.error(
            String.format("Error closing RabbitMQ connection channel for user '%s'", userId));
      }
    }
  }

  private void sendToExchange(String userId, String event) {
    if (channel.isEmpty()) {
      ChatsLogger.warn("RabbitMQ connection channel is not up!");
      return;
    }
    Channel eventChannel = channel.get();
    try {
      eventChannel.exchangeDeclare(userId, "direct");
      eventChannel.basicPublish(userId, "", null, event.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
      try {
        eventChannel.close();
      } catch (IOException | TimeoutException ignored) {
        ChatsLogger.error(
            String.format("Error closing RabbitMQ connection channel for user '%s'", userId));
      }
    }
  }

  private void sendToExchange(List<String> usersIds, String event) {
    usersIds.forEach(userId -> sendToExchange(userId, event));
  }
}
