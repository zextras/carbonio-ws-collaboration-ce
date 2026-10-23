// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.ServiceIdProvider;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ConsulMockServer;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class WsPresenceRepairIT {

  private final WsPresenceManager wsPresenceManager;
  private final WsPresenceRepository wsPresenceRepository;
  private final EventWebSocketSessions sessions;
  private final ServiceIdProvider serviceIdProvider;

  private final String sessionId = UUID.randomUUID().toString();
  private final String userId = UUID.randomUUID().toString();

  public WsPresenceRepairIT(
      WsPresenceManager wsPresenceManager,
      WsPresenceRepository wsPresenceRepository,
      EventWebSocketSessions sessions,
      ServiceIdProvider serviceIdProvider) {
    this.wsPresenceManager = wsPresenceManager;
    this.wsPresenceRepository = wsPresenceRepository;
    this.sessions = sessions;
    this.serviceIdProvider = serviceIdProvider;
  }

  @BeforeAll
  static void setUpConsul(ConsulMockServer consulMockServer) {
    consulMockServer.mockServiceIdResponse(UUID.randomUUID());
  }

  @AfterEach
  void unregisterSession() {
    sessions.remove(sessionId);
  }

  private void registerOpenSession() {
    Session session = mock(Session.class);
    when(session.getId()).thenReturn(sessionId);
    when(session.isOpen()).thenReturn(true);
    when(session.getAsyncRemote()).thenReturn(mock(RemoteEndpoint.Async.class));
    sessions.add(session, userId);
  }

  @Test
  @DisplayName("An open session missing in DB is restored on this service")
  void restoreSession_openSession_insertsOwnRow() {
    registerOpenSession();

    wsPresenceManager.restoreSession(userId, sessionId);

    assertEquals(
        Map.of(sessionId, userId),
        wsPresenceRepository.findSessionsByServiceId(serviceIdProvider.get()));
  }

  @Test
  @DisplayName("A closed session is not restored")
  void restoreSession_closedSession_insertsNothing() {
    wsPresenceManager.restoreSession(userId, sessionId);

    assertEquals(0, wsPresenceRepository.countOnlineByUserId(userId));
  }

  @Test
  @DisplayName("An orphan row is removed and the user's last seen is recorded")
  void removeOrphanSession_closedSession_deletesRowAndRecordsLastSeen() {
    wsPresenceRepository.insertAndCountOnline(sessionId, userId, serviceIdProvider.get());

    wsPresenceManager.removeOrphanSession(userId, sessionId);

    assertEquals(0, wsPresenceRepository.countOnlineByUserId(userId));
    assertTrue(wsPresenceRepository.findLastSeenForUser(userId).isPresent());
  }

  @Test
  @DisplayName("The row of an open session is not an orphan")
  void removeOrphanSession_openSession_keepsRow() {
    registerOpenSession();
    wsPresenceRepository.insertAndCountOnline(sessionId, userId, serviceIdProvider.get());

    wsPresenceManager.removeOrphanSession(userId, sessionId);

    assertEquals(1, wsPresenceRepository.countOnlineByUserId(userId));
  }
}
