package com.zextras.team.core.web.dispatcher.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.team.core.web.dispatcher.model.Event;
import com.zextras.team.core.logging.TeamLogger;
import com.zextras.team.core.web.dispatcher.EventDispatcher;
import javax.inject.Inject;

public class MockEventDispatcherImpl implements EventDispatcher {

  private final ObjectMapper objectMapper;

  @Inject
  public MockEventDispatcherImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void sentToTopic(Event event) {
    try {
      TeamLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToTopic - event(%s)", objectMapper.writeValueAsString(event)));
    } catch (JsonProcessingException e) {
      TeamLogger.error(MockEventDispatcherImpl.class,
        "sentToTopic - unable to parse the event", e);
    }
  }

  @Override
  public void sentToQueue(Event event) {
    try {
      TeamLogger.info(MockEventDispatcherImpl.class,
        String.format("sentToQueue - event(%s)", objectMapper.writeValueAsString(event)));
    } catch (JsonProcessingException e) {
      TeamLogger.error(MockEventDispatcherImpl.class,
        "sentToQueue - unable to parse the event", e);
    }
  }
}
