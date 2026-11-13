// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class MessagingModuleTest {

  private final MessagingModule module = new MessagingModule();
  private final ConnectionFactory factory = mock(ConnectionFactory.class);

  @Test
  void publisherConnection_isNamedAfterItsRole() throws Exception {
    module.getPublisherConnection(factory);

    verify(factory).newConnection(startsWith("carbonio-ws-collaboration-publisher@"));
  }

  @Test
  void videoServerConnection_isNamedAfterItsRole() throws Exception {
    module.getVideoServerConnection(factory);

    verify(factory).newConnection(startsWith("carbonio-ws-collaboration-videoserver@"));
  }

  @Test
  void consumerConnection_recoversItsQueue_withoutChangingTheSharedFactory() throws Exception {
    ConnectionFactory recovering = mock(ConnectionFactory.class);
    when(factory.clone()).thenReturn(recovering);

    module.getConsumerConnection(factory);

    verify(recovering).setTopologyRecoveryEnabled(true);
    verify(recovering).newConnection(startsWith("carbonio-ws-collaboration-consumer@"));
    verify(factory, never()).setTopologyRecoveryEnabled(anyBoolean());
  }
}
