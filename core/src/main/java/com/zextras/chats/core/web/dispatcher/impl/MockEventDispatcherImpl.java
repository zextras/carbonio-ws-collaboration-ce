package com.zextras.chats.core.web.dispatcher.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.chats.core.data.event.Event;
import com.zextras.chats.core.logging.ChatsLogger;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
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
   * @param event  event to send
   */
  @Override
  public void sentToTopic(UUID sender, UUID topic, Event event) {
    try {
      ChatsLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToTopic - event(%s)", objectMapper.writeValueAsString(event)));
    } catch (JsonProcessingException e) {
      ChatsLogger.error(MockEventDispatcherImpl.class,
        "sentToTopic - unable to parse the event", e);
    }
  }

  @Override
  public void sentToQueue(UUID sender, UUID receiver, Event event) {
    Map<String, Object> map = new HashMap<>();
    map.put("sender", sender);
    map.put("receiver", receiver);
    map.put("event", event);
    try {
      ChatsLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToQueue - json: %s", objectMapper.writeValueAsString(map)));
    } catch (JsonProcessingException e) {
      ChatsLogger.error(MockEventDispatcherImpl.class,
        "sentToQueue - unable to parse the event", e);
    }
  }

}
