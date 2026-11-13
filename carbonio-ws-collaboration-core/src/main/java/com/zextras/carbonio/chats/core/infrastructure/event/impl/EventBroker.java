// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import java.io.IOException;

public final class EventBroker {

  public static final String EXCHANGE = "wsc.events";
  public static final String USERS_HEADER = "users";
  public static final String QUEUE_ID_HEADER = "queueId";

  private EventBroker() {}

  public static void declareExchange(Channel channel) throws IOException {
    channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT, true);
  }
}
