// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.util.List;

public interface EventDispatcher extends HealthIndicator {

  void sendToUserExchange(String userId, DomainEvent event);

  void sendToUserExchange(List<String> usersIds, DomainEvent event);

  void sendToUserQueue(String userId, String queueId, DomainEvent event);
}
