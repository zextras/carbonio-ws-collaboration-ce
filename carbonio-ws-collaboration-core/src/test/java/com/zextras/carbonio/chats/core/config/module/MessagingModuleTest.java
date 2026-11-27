// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import java.io.IOException;
import java.util.Optional;
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
  void consumerConnection_isNamedAfterItsRole() throws Exception {
    module.getConsumerConnection(factory);

    verify(factory).newConnection(startsWith("carbonio-ws-collaboration-consumer@"));
  }

  @Test
  void connection_failsAtOnceWhenTheBrokerIsDown_soTheServiceManagerRetriesTheStart()
      throws Exception {
    when(factory.newConnection(anyString())).thenThrow(new IOException("down"));

    assertThrows(EventDispatcherException.class, () -> module.getPublisherConnection(factory));

    verify(factory).newConnection(anyString());
  }

  @Test
  void everyConnectionRecoversItselfAndWhatItDeclared() {
    AppConfig appConfig = mock(AppConfig.class);
    when(appConfig.get(eq(String.class), any(ConfigName.class))).thenReturn(Optional.of("value"));
    when(appConfig.get(eq(Integer.class), any(ConfigName.class))).thenReturn(Optional.of(1));

    ConnectionFactory configured = module.getRabbitMqConnectionFactory(appConfig);

    assertTrue(configured.isAutomaticRecoveryEnabled());
    assertTrue(configured.isTopologyRecoveryEnabled());
  }
}
