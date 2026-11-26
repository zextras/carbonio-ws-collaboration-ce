// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.cache.CacheVideoServerSession;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.core.infrastructure.websocket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.presence.WsPresenceManager;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionRouter;
import com.zextras.carbonio.chats.core.web.socket.impl.EventsWebSocketManagerImpl;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@UnitTest
class EventsWebSocketManagerTest {

  private final WebsocketVersionMigrator migrator = mock(WebsocketVersionMigrator.class);
  private final WsPresenceManager wsPresenceManager = mock(WsPresenceManager.class);
  private final CacheVideoServerSession cacheVideoServerSession =
      mock(CacheVideoServerSession.class);
  private final EventWebSocketSessions sessions = mock(EventWebSocketSessions.class);
  private final EventsWebSocketManager manager =
      new EventsWebSocketManagerImpl(
          new JacksonConfig().get(),
          migrator,
          cacheVideoServerSession,
          mock(ParticipantService.class),
          mock(WsActionRouter.class),
          wsPresenceManager,
          sessions,
          mock(MessageBrokerVideoserverHealthMonitor.class));

  private final String userId = UUID.randomUUID().toString();
  private final String sessionId = UUID.randomUUID().toString();
  private Session session;
  private RemoteEndpoint.Async remote;

  @BeforeEach
  void setUp() throws Exception {
    HttpSession httpSession = mock(HttpSession.class);
    when(httpSession.getAttribute("userId")).thenReturn(userId);
    Map<String, Object> userProperties = new HashMap<>();
    userProperties.put(HttpSession.class.getName(), httpSession);
    session = mock(Session.class);
    remote = mock(RemoteEndpoint.Async.class);
    when(session.getId()).thenReturn(sessionId);
    when(session.getUserProperties()).thenReturn(userProperties);
    when(session.getNegotiatedSubprotocol()).thenReturn("");
    when(session.getAsyncRemote()).thenReturn(remote);
    when(migrator.downgradeIfNeeded(any(DomainEvent.class), anyString()))
        .thenReturn("websocket-connected");
  }

  @Test
  void onOpen_sendsWebsocketConnectedOnlyOnceRegisteredForEvents() throws Exception {
    manager.onOpen(session);

    InOrder order = inOrder(sessions, remote, wsPresenceManager);
    order.verify(sessions).add(session);
    order.verify(remote).sendObject("websocket-connected");
    order.verify(wsPresenceManager).onConnect(userId, session);
  }

  @Test
  void onClose_releasesTheVideoServerSessionAndThePresence() {
    when(sessions.remove(sessionId)).thenReturn(true);

    manager.onClose(session);

    InOrder order = inOrder(cacheVideoServerSession, wsPresenceManager);
    order.verify(cacheVideoServerSession).add(UUID.fromString(userId), UUID.fromString(sessionId));
    order.verify(wsPresenceManager).onDisconnect(userId, session);
  }

  @Test
  void onClose_disconnectHookFails_stillReleasesThePresence() {
    when(sessions.remove(sessionId)).thenReturn(true);
    EventsWebSocketManager failingHook =
        new EventsWebSocketManagerImpl(
            new JacksonConfig().get(),
            migrator,
            cacheVideoServerSession,
            mock(ParticipantService.class),
            mock(WsActionRouter.class),
            wsPresenceManager,
            sessions,
            mock(MessageBrokerVideoserverHealthMonitor.class)) {
          @Override
          protected void onSessionDisconnected(UUID queueId) {
            throw new IllegalStateException("cleanup failed");
          }
        };

    assertThrows(IllegalStateException.class, () -> failingHook.onClose(session));

    verify(wsPresenceManager).onDisconnect(userId, session);
  }
}
