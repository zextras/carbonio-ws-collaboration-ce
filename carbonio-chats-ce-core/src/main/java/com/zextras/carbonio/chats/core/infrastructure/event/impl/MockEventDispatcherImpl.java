// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MockEventDispatcherImpl implements EventDispatcher {

  private final ObjectMapper objectMapper;

  @Inject
  public MockEventDispatcherImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Send an event to a topic
   *
   * @param sender      identifier of the user who sent the message  {@link UUID }
   * @param topic       topic identifier (hypothesis the room identifier)
   * @param domainEvent event to send
   */
  @Override
  public void sendToTopic(UUID sender, String topic, DomainEvent domainEvent) {
    try {
      ChatsLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToTopic - event(%s)", objectMapper.writeValueAsString(domainEvent)));
    } catch (JsonProcessingException e) {
      ChatsLogger.error(MockEventDispatcherImpl.class,
        "sentToTopic - unable to parse the event", e);
    }
  }

  @Override
  public void sendToQueue(UUID sender, String queueName, DomainEvent domainEvent) {
    Map<String, Object> map = new HashMap<>();
    map.put("sender", sender);
    map.put("queueName", queueName);
    map.put("event", domainEvent);
    try {
      ChatsLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToQueue - json: %s", objectMapper.writeValueAsString(map)));
    } catch (JsonProcessingException e) {
      ChatsLogger.error(MockEventDispatcherImpl.class,
        "sentToQueue - unable to parse the event", e);
    }
  }

  @Override
  public void sendToQueue(UUID sender, List<String> queues, DomainEvent domainEvent) {
    queues.forEach(queue -> sendToQueue(sender, queue, domainEvent));
  }

  @Override
  public boolean isAlive() {
    return true;
  }

}
