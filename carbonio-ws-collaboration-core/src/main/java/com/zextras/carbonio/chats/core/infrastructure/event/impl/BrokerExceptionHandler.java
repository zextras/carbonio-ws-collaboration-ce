// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.TopologyRecoveryException;
import com.rabbitmq.client.impl.DefaultExceptionHandler;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;

@Singleton
public class BrokerExceptionHandler extends DefaultExceptionHandler {

  private final Runnable exit;

  @Inject
  public BrokerExceptionHandler() {
    // on another thread: System.exit waits for the shutdown hooks that close the connections
    this(
        () ->
            Thread.ofPlatform()
                .name("Exit after a failed broker recovery")
                .start(() -> System.exit(1)));
  }

  BrokerExceptionHandler(Runnable exit) {
    this.exit = exit;
  }

  @Override
  public void handleTopologyRecoveryException(
      Connection connection, Channel channel, TopologyRecoveryException exception) {
    ChatsLogger.error(
        "Could not declare the queues and consumers again after a broker outage, exiting",
        exception);
    exit.run();
  }
}
