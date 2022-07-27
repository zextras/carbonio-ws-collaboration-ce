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
import java.util.Map;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class EventDispatcherRabbitMq implements EventDispatcher {

  private final Connection   connection;
  private final ObjectMapper objectMapper;

  @Inject
  public EventDispatcherRabbitMq(Connection connection, ObjectMapper objectMapper) {
    this.connection = connection;
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean isAlive() {
    return connection.isOpen();
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
      Channel channel = connection.createChannel();
      String queueName = userId;
      channel.queueDeclare(queueName, false, true, true, Map.of());
      channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
      channel.close();
    } catch (IOException | TimeoutException e) {
      ChatsLogger.warn(String.format("Unable to send message to %s", userId), e);
    }
  }

  private void send(List<String> usersIds, String message) {
    usersIds.forEach(userId -> send(userId, message));
  }
}
