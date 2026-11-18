// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private static final String USER_ROUTING_KEY = "user-events";

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
    sendToExchange(Collections.singletonList(userId), event);
  }

  @Override
  public void sendToUserExchange(List<String> usersIds, DomainEvent event) {
    sendToExchange(usersIds, event);
  }

  @Override
  public void sendToUserQueue(String userId, String queueId, DomainEvent event) {
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error("Unable to send event to user queue: event dispatcher channel is not up!");
      return;
    }
    String userQueue = userId + "/" + queueId;
    try {
      channel.queueDeclare(queueId, false, false, true, null);
      channel.basicPublish(
          "",
          queueId,
          null,
          objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user queue '%s'", userQueue), e);
    }
  }

  @Override
  public void sendWsEvent(String userId, WsEvent event) {
    sendToExchange(Collections.singletonList(userId), event);
  }

  @Override
  public void sendWsEvent(List<String> usersIds, WsEvent event) {
    sendToExchange(usersIds, event);
  }

  private void sendToExchange(List<String> usersIds, Object event) {
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsString(event).getBytes(StandardCharsets.UTF_8);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to serialize event '%s'", event.getClass()), e);
      return;
    }
    usersIds.forEach(userId -> publishToExchange(userId, payload));
  }

  private void publishToExchange(String userId, byte[] payload) {
    if (channel == null || !channel.isOpen()) {
      ChatsLogger.error("Unable to send event to exchange: event dispatcher channel is not up!");
      return;
    }
    try {
      channel.exchangeDeclare(userId, BuiltinExchangeType.DIRECT, false, false, null);
      channel.basicPublish(userId, USER_ROUTING_KEY, null, payload);
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to user '%s'", userId), e);
    }
  }

  @Override
  public void stop() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
        ChatsLogger.info("Event dispatcher channel closed successfully.");
      }
    } catch (Exception e) {
      ChatsLogger.error("Error during stopping event dispatcher", e);
    }
  }
}
