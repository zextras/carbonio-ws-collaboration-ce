// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event;

import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.util.List;

public interface EventDispatcher extends HealthIndicator {
  void sendToUserExchange(String userId, DomainEvent event);

  void sendToUserExchange(List<String> usersIds, DomainEvent event);

  void sendToUserQueue(String userId, String queueId, DomainEvent event);

  /**
   * Sends a raw JSON string to a user's RabbitMQ exchange. Used by the unified WebSocket protocol
   * to deliver custom event payloads (messaging, presence, reactions) that are not DomainEvent
   * subtypes.
   *
   * @param userId user identifier
   * @param jsonPayload raw JSON string to deliver to the user's WebSocket
   */
  void sendRawToUserExchange(String userId, String jsonPayload);

  /**
   * Sends a raw JSON string to multiple users' RabbitMQ exchanges.
   *
   * @param usersIds list of user identifiers
   * @param jsonPayload raw JSON string to deliver
   */
  void sendRawToUserExchange(List<String> usersIds, String jsonPayload);

  void stop();
}
