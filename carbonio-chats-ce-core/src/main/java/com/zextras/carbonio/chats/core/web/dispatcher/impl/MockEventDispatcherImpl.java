package com.zextras.carbonio.chats.core.web.dispatcher.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.dispatcher.EventDispatcher;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.inject.Inject;

public class MockEventDispatcherImpl implements EventDispatcher {

  private final ObjectMapper objectMapper;

  @Inject
  public MockEventDispatcherImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Send an event to a topic
   *
   * @param sender identifier of the user who sent the message  {@link UUID }
   * @param topic  topic identifier (hypothesis the room identifier)
   * @param domainEvent  event to send
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

}
