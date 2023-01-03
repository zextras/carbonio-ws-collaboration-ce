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
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
  public void sendToUserQueue(String userId, DomainEvent event) {
    try {
      send(userId, objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    }
  }

  @Override
  public void sendToUserQueue(List<String> usersIds, DomainEvent event) {
    try {
      send(usersIds, objectMapper.writeValueAsString(event));
    } catch (JsonProcessingException e) {
      ChatsLogger.warn("Unable to convert event to json", e);
    }
  }

  private void send(String userId, String message) {
    try {
      if (Optional.ofNullable(connection).isEmpty()) {
        return;
      }
      Channel channel = connection.createChannel();
      channel.queueDeclare(userId, true, false, false, Map.of());
      channel.basicPublish("", userId, null, message.getBytes(StandardCharsets.UTF_8));
      channel.close();
    } catch (Exception e) {
      ChatsLogger.warn(String.format("Unable to send message to %s", userId), e);
    }
  }

  private void send(List<String> usersIds, String message) {
    usersIds.forEach(userId -> send(userId, message));
  }
}
