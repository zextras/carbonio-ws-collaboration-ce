// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.util.List;
import java.util.UUID;

public interface EventDispatcher extends HealthIndicator {

  // TODO: 24/11/21 versioning

  void sendToTopic(UUID sender, String topic, DomainEvent domainEvent);
  void sendToQueue(UUID sender, String queueName, DomainEvent domainEvent);
  void sendToQueue(UUID sender, List<String> queues, DomainEvent domainEvent);
}
