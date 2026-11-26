// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.TopologyRecoveryException;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.io.IOException;
import org.junit.jupiter.api.Test;

@UnitTest
class BrokerExceptionHandlerTest {

  private final Runnable exit = mock(Runnable.class);
  private final BrokerExceptionHandler handler = new BrokerExceptionHandler(exit);
  private final Connection connection = mock(Connection.class);

  @Test
  void failedTopologyRecovery_exits_soTheServiceManagerRestartsWithEveryConsumer() {
    handler.handleTopologyRecoveryException(
        connection,
        mock(Channel.class),
        new TopologyRecoveryException("queue refused", new IOException("ACCESS_REFUSED")));

    verify(exit).run();
  }

  @Test
  void failedConnectionRecovery_doesNotExit_becauseTheClientKeepsRetrying() {
    handler.handleConnectionRecoveryException(connection, new IOException("broker down"));

    verify(exit, never()).run();
  }
}
