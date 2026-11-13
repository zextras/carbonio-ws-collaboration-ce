// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public final class EventBroker {

  public static final String EXCHANGE = "wsc.events";
  public static final String USERS_HEADER = "users";
  public static final String QUEUE_ID_HEADER = "queueId";

  private EventBroker() {}

  public static void declareExchange(Channel channel) throws IOException {
    channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.FANOUT, true);
  }

  public static String connectionName(String role) {
    try {
      return "carbonio-ws-collaboration-" + role + "@" + InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return "carbonio-ws-collaboration-" + role;
    }
  }
}
