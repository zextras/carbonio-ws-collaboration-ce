package com.zextras.team.core.web.dispatcher.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.team.core.web.dispatcher.model.Message;
import com.zextras.team.core.logging.TeamLogger;
import com.zextras.team.core.web.dispatcher.MessageDispatcher;
import javax.inject.Inject;

public class MockMessageDispatcherImpl implements MessageDispatcher {

  private final ObjectMapper objectMapper;

  @Inject
  public MockMessageDispatcherImpl(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void send(Message message) {
    try {
      TeamLogger.info(MockMessageDispatcherImpl.class,
        String.format("send - event(%s)", objectMapper.writeValueAsString(message)));
    } catch (JsonProcessingException e) {
      TeamLogger.error(MockMessageDispatcherImpl.class,
        "send - unable to parse the event", e);
    }
  }
}
