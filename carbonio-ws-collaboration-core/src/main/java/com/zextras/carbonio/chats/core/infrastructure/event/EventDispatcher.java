// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import io.ebeaninternal.server.expression.Op;
import java.util.List;
import java.util.Optional;

public interface EventDispatcher extends HealthIndicator {

  Optional<Connection> getConnection();

  Optional<Channel> createChannel();

  void sendToUserExchange(String userId, DomainEvent event);

  void sendToUserExchange(List<String> usersIds, DomainEvent event);

  void sendToUserQueue(String userId, String queueId, DomainEvent event);
}
