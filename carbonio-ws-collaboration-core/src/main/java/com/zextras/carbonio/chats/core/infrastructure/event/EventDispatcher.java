// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event;

import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.util.List;

public interface EventDispatcher extends HealthIndicator {
  void sendToUserExchange(String userId, DomainEvent event);

  void sendToUserExchange(List<String> usersIds, DomainEvent event);

  void sendToUserQueue(String userId, String queueId, DomainEvent event);

  void sendVideoServerHeartbeat(String emitter);

  /**
   * Serializes a unified WebSocket protocol event (messaging, presence, reactions, typing) once and
   * delivers it to a user's RabbitMQ exchange.
   *
   * @param userId user identifier
   * @param event event to deliver to the user's WebSocket
   */
  void sendWsEvent(String userId, WsEvent event);

  /**
   * Serializes a unified WebSocket protocol event once and delivers the same payload to the
   * exchange of every given user.
   *
   * @param usersIds list of user identifiers
   * @param event event to deliver
   */
  void sendWsEvent(List<String> usersIds, WsEvent event);

  void stop();
}
