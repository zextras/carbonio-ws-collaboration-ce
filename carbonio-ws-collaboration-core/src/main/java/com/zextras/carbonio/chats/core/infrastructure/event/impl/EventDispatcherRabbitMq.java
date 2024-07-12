// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private final Channel channel;
  private final ObjectMapper objectMapper;

  @Inject
  public EventDispatcherRabbitMq(Channel channel, ObjectMapper objectMapper) {
    this.channel = channel;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    return channel != null && channel.isOpen();
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
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error("Event dispatcher channel is not up!");
      return;
    }
    String queueName = userId + "/" + queueId;
    try {
      channel.queueDeclare(queueName, false, false, true, null);
      channel.basicPublish(
          "",
          queueName,
          null,
          objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
    }
  }

  private void sendToExchange(String userId, String event) {
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error("Event dispatcher channel is not up!");
      return;
    }
    try {
      channel.exchangeDeclare(userId, "direct");
      channel.basicPublish(userId, "", null, event.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
    }
  }

  private void sendToExchange(List<String> usersIds, String event) {
    usersIds.forEach(userId -> sendToExchange(userId, event));
  }
}
