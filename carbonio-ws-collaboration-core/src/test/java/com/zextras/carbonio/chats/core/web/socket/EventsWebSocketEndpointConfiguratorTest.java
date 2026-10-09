// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.web.socket.impl.EventsWebSocketManagerImpl;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import org.junit.jupiter.api.Test;

@UnitTest
class EventsWebSocketEndpointConfiguratorTest {

  @Test
  void acceptsAManagerAnnotatedAsServerEndpoint() {
    EventsWebSocketManager manager =
        new EventsWebSocketManagerImpl(
            mock(RabbitConnectionPoolService.class),
            mock(ObjectMapper.class),
            mock(WebsocketVersionMigrator.class),
            mock(CacheVideoServerSession.class),
            mock(ParticipantService.class),
            mock(EventWebSocketSessions.class));

    assertDoesNotThrow(() -> new EventsWebSocketEndpointConfigurator(manager));
  }

  @Test
  void rejectsAManagerWithoutServerEndpointAtStartup() {
    EventsWebSocketManager notAnnotated = mock(EventsWebSocketManager.class);

    assertThrows(
        IllegalStateException.class, () -> new EventsWebSocketEndpointConfigurator(notAnnotated));
  }
}
