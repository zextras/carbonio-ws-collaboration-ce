package com.zextras.carbonio.chats.core.web.dispatcher;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import java.util.UUID;

public interface EventDispatcher {

  // TODO: 24/11/21 versioning

  void sendToTopic(UUID sender, String topic, DomainEvent domainEvent);
  void sendToQueue(UUID sender, String queueName, DomainEvent domainEvent);
}
