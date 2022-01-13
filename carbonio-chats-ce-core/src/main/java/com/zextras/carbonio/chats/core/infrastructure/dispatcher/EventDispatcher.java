package com.zextras.carbonio.chats.core.infrastructure.dispatcher;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.util.UUID;

public interface EventDispatcher extends HealthIndicator {

  // TODO: 24/11/21 versioning

  void sendToTopic(UUID sender, String topic, DomainEvent domainEvent);
  void sendToQueue(UUID sender, String queueName, DomainEvent domainEvent);
}
