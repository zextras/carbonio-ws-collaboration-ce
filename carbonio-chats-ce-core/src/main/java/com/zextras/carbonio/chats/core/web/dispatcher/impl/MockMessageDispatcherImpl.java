package com.zextras.carbonio.chats.core.web.dispatcher.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.web.dispatcher.MessageDispatcher;
import com.zextras.carbonio.chats.core.web.dispatcher.model.Message;
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
      ChatsLogger.info(MockMessageDispatcherImpl.class,
        String.format("send - event(%s)", objectMapper.writeValueAsString(message)));
    } catch (JsonProcessingException e) {
      ChatsLogger.error(MockMessageDispatcherImpl.class,
        "send - unable to parse the event", e);
    }
  }
}
