// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository.SessionDeletionResult;
import io.ebean.Database;
import io.ebean.SqlQuery;
import jakarta.websocket.Session;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@UnitTest
class WsPresenceManagerTest {

  private static final OffsetDateTime LAST_SEEN = OffsetDateTime.parse("2026-10-13T10:00:00Z");

  private WsPresenceRepository wsPresenceRepository;
  private ServiceIdProvider serviceIdProvider;
  private PresenceBroadcaster presenceBroadcaster;
  private Database db;
  private SqlQuery sqlQuery;
  private EventWebSocketSessions sessions;
  private WsPresenceManager wsPresenceManager;

  @BeforeEach
  void setUp() {
    wsPresenceRepository = mock(WsPresenceRepository.class);
    serviceIdProvider = mock(ServiceIdProvider.class);
    presenceBroadcaster = mock(PresenceBroadcaster.class);
    db = mock(Database.class);
    sqlQuery = mock(SqlQuery.class);
    sessions = mock(EventWebSocketSessions.class);

    when(serviceIdProvider.get()).thenReturn("service-uuid-1");

    // Stub the advisory lock chain: db.sqlQuery(...).setParameter(...).findOneOrEmpty()
    when(db.sqlQuery(anyString())).thenReturn(sqlQuery);
    when(sqlQuery.setParameter(anyString(), any())).thenReturn(sqlQuery);

    wsPresenceManager =
        new WsPresenceManager(
            wsPresenceRepository, serviceIdProvider, presenceBroadcaster, db, sessions);
  }

  @AfterEach
  void cleanup() {
    reset(wsPresenceRepository, serviceIdProvider, presenceBroadcaster, db, sqlQuery, sessions);
  }

  private Session mockSession(String sessionId) {
    Session session = mock(Session.class);
    when(session.getId()).thenReturn(sessionId);
    return session;
  }

  private void verifyNoBroadcast() {
    verify(presenceBroadcaster, never()).broadcastPresence(anyString(), anyBoolean(), any());
  }

  @Nested
  @DisplayName("onConnect tests")
  class OnConnectTests {

    @Test
    @DisplayName("First session (pre-insert count 0): broadcast online after the DB work")
    void onConnect_firstSession_broadcastsOnlineAfterInsert() {
      String userId = "user-1";
      Session session = mockSession("session-1");
      // pre-insert count == 0 means this is the 0 -> 1 online transition
      when(wsPresenceRepository.insertAndCountOnline("session-1", userId, "service-uuid-1"))
          .thenReturn(0L);
      when(wsPresenceRepository.findLastSeenForUser(userId)).thenReturn(Optional.of(LAST_SEEN));

      wsPresenceManager.onConnect(userId, session);

      InOrder order = inOrder(wsPresenceRepository, presenceBroadcaster);
      order
          .verify(wsPresenceRepository)
          .insertAndCountOnline("session-1", userId, "service-uuid-1");
      order.verify(presenceBroadcaster).broadcastPresence(userId, true, LAST_SEEN);
    }

    @Test
    @DisplayName("User never seen offline before: online broadcast without last activity")
    void onConnect_userNeverOffline_broadcastsWithoutLastActivity() {
      when(wsPresenceRepository.insertAndCountOnline(anyString(), anyString(), anyString()))
          .thenReturn(0L);
      when(wsPresenceRepository.findLastSeenForUser("user-1")).thenReturn(Optional.empty());

      wsPresenceManager.onConnect("user-1", mockSession("session-1"));

      verify(presenceBroadcaster).broadcastPresence("user-1", true, null);
    }

    @Test
    @DisplayName("Broadcast is not tied to the transaction (no post-commit callback)")
    void onConnect_firstSession_registersNoTransactionCallback() {
      when(wsPresenceRepository.insertAndCountOnline(anyString(), anyString(), anyString()))
          .thenReturn(0L);

      wsPresenceManager.onConnect("user-1", mockSession("session-1"));

      verify(db, never()).currentTransaction();
    }

    @Test
    @DisplayName("Subsequent session (pre-insert count 1): insert but no broadcast")
    void onConnect_subsequentSession_insertsButNoBroadcast() {
      String userId = "user-1";
      Session session = mockSession("session-2");
      // pre-insert count == 1 means the user was already online; not a transition
      when(wsPresenceRepository.insertAndCountOnline("session-2", userId, "service-uuid-1"))
          .thenReturn(1L);

      wsPresenceManager.onConnect(userId, session);

      verify(wsPresenceRepository).insertAndCountOnline("session-2", userId, "service-uuid-1");
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Insert failure: does not throw and does not broadcast")
    void onConnect_insertFailure_noThrowNoBroadcast() {
      doThrow(new RuntimeException("DB error"))
          .when(wsPresenceRepository)
          .insertAndCountOnline(anyString(), anyString(), anyString());

      assertDoesNotThrow(() -> wsPresenceManager.onConnect("user-1", mockSession("session-1")));

      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Lock failure: does not throw, nothing inserted, no broadcast")
    void onConnect_lockFailure_noThrowNoInsertNoBroadcast() {
      when(sqlQuery.findOneOrEmpty()).thenThrow(new RuntimeException("DB down"));

      assertDoesNotThrow(() -> wsPresenceManager.onConnect("user-1", mockSession("session-1")));

      verify(wsPresenceRepository, never())
          .insertAndCountOnline(anyString(), anyString(), anyString());
      verifyNoBroadcast();
    }
  }

  @Nested
  @DisplayName("onDisconnect tests")
  class OnDisconnectTests {

    @Test
    @DisplayName("Last session (pre-delete count 1): upsert last seen, then broadcast offline")
    void onDisconnect_lastSession_upsertsThenBroadcastsOffline() {
      String userId = "user-1";
      Session session = mockSession("session-1");
      // pre-delete count == 1 means after delete it is 0 -> the 1 -> 0 offline transition
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult(userId, 1L));
      when(wsPresenceRepository.upsertLastSeen(userId)).thenReturn(LAST_SEEN);

      wsPresenceManager.onDisconnect(userId, session);

      InOrder order = inOrder(wsPresenceRepository, presenceBroadcaster);
      order.verify(wsPresenceRepository).deleteAndCountRemaining("session-1");
      order.verify(wsPresenceRepository).upsertLastSeen(userId);
      order.verify(presenceBroadcaster).broadcastPresence(userId, false, LAST_SEEN);
    }

    @Test
    @DisplayName("Not last session (pre-delete count 2): no upsertLastSeen, no broadcast")
    void onDisconnect_notLastSession_deletesOnlyNoUpsertNoBroadcast() {
      String userId = "user-1";
      Session session = mockSession("session-1");
      // pre-delete count == 2 means the user still has another session after delete
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult(userId, 2L));

      wsPresenceManager.onDisconnect(userId, session);

      verify(wsPresenceRepository).deleteAndCountRemaining("session-1");
      verify(wsPresenceRepository, never()).upsertLastSeen(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Session not found: no-op, no upsert, no broadcast")
    void onDisconnect_sessionNotFound_noOp() {
      String userId = "user-1";
      Session session = mockSession("session-1");
      // null userId == nothing was deleted -> no transition
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult(null, 0L));

      wsPresenceManager.onDisconnect(userId, session);

      verify(wsPresenceRepository).deleteAndCountRemaining("session-1");
      verify(wsPresenceRepository, never()).upsertLastSeen(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Delete failure: does not throw, no upsert, no broadcast")
    void onDisconnect_deleteFailure_noThrowNoUpsertNoBroadcast() {
      doThrow(new RuntimeException("DB error"))
          .when(wsPresenceRepository)
          .deleteAndCountRemaining(anyString());

      assertDoesNotThrow(() -> wsPresenceManager.onDisconnect("user-1", mockSession("session-1")));

      verify(wsPresenceRepository, never()).upsertLastSeen(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Lock failure: does not throw, nothing deleted, no broadcast")
    void onDisconnect_lockFailure_noThrowNoDeleteNoBroadcast() {
      when(sqlQuery.findOneOrEmpty()).thenThrow(new RuntimeException("DB down"));

      assertDoesNotThrow(() -> wsPresenceManager.onDisconnect("user-1", mockSession("session-1")));

      verify(wsPresenceRepository, never()).deleteAndCountRemaining(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Last-seen upsert failure: does not throw, no broadcast")
    void onDisconnect_upsertFailure_noThrowNoBroadcast() {
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult("user-1", 1L));
      doThrow(new RuntimeException("DB error"))
          .when(wsPresenceRepository)
          .upsertLastSeen(anyString());

      assertDoesNotThrow(() -> wsPresenceManager.onDisconnect("user-1", mockSession("session-1")));

      verifyNoBroadcast();
    }
  }

  @Nested
  @DisplayName("restoreSession tests")
  class RestoreSessionTests {

    @Test
    @DisplayName(
        "Open session missing in DB, user had no rows: lock, insert, then broadcast online")
    void restoreSession_openAndFirstSession_insertsUnderLockThenBroadcasts() {
      when(sessions.isOpen("session-1")).thenReturn(true);
      when(wsPresenceRepository.insertAndCountOnline("session-1", "user-1", "service-uuid-1"))
          .thenReturn(0L);
      when(wsPresenceRepository.findLastSeenForUser("user-1")).thenReturn(Optional.of(LAST_SEEN));

      wsPresenceManager.restoreSession("user-1", "session-1");

      verify(sqlQuery).findOneOrEmpty();
      InOrder order = inOrder(wsPresenceRepository, presenceBroadcaster);
      order
          .verify(wsPresenceRepository)
          .insertAndCountOnline("session-1", "user-1", "service-uuid-1");
      order.verify(presenceBroadcaster).broadcastPresence("user-1", true, LAST_SEEN);
    }

    @Test
    @DisplayName("Open session, user already online elsewhere: insert, no broadcast")
    void restoreSession_openAndUserAlreadyOnline_insertsWithoutBroadcast() {
      when(sessions.isOpen("session-1")).thenReturn(true);
      when(wsPresenceRepository.insertAndCountOnline("session-1", "user-1", "service-uuid-1"))
          .thenReturn(1L);

      wsPresenceManager.restoreSession("user-1", "session-1");

      verify(wsPresenceRepository).insertAndCountOnline("session-1", "user-1", "service-uuid-1");
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Session closed in the meantime: nothing inserted")
    void restoreSession_sessionNoLongerOpen_noInsert() {
      when(sessions.isOpen("session-1")).thenReturn(false);

      wsPresenceManager.restoreSession("user-1", "session-1");

      verify(wsPresenceRepository, never())
          .insertAndCountOnline(anyString(), anyString(), anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Repository failure propagates to the caller")
    void restoreSession_repositoryFailure_propagates() {
      when(sessions.isOpen("session-1")).thenReturn(true);
      doThrow(new RuntimeException("DB error"))
          .when(wsPresenceRepository)
          .insertAndCountOnline(anyString(), anyString(), anyString());

      assertThrows(
          RuntimeException.class, () -> wsPresenceManager.restoreSession("user-1", "session-1"));
    }
  }

  @Nested
  @DisplayName("removeOrphanSession tests")
  class RemoveOrphanSessionTests {

    @Test
    @DisplayName("Orphan row, last session: lock, delete, upsert last seen, then broadcast offline")
    void removeOrphanSession_lastSession_deletesUpsertsThenBroadcasts() {
      when(sessions.isOpen("session-1")).thenReturn(false);
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult("user-1", 1L));
      when(wsPresenceRepository.upsertLastSeen("user-1")).thenReturn(LAST_SEEN);

      wsPresenceManager.removeOrphanSession("user-1", "session-1");

      verify(sqlQuery).findOneOrEmpty();
      InOrder order = inOrder(wsPresenceRepository, presenceBroadcaster);
      order.verify(wsPresenceRepository).deleteAndCountRemaining("session-1");
      order.verify(wsPresenceRepository).upsertLastSeen("user-1");
      order.verify(presenceBroadcaster).broadcastPresence("user-1", false, LAST_SEEN);
    }

    @Test
    @DisplayName("Orphan row, user still has other sessions: delete only")
    void removeOrphanSession_notLastSession_deletesOnly() {
      when(sessions.isOpen("session-1")).thenReturn(false);
      when(wsPresenceRepository.deleteAndCountRemaining("session-1"))
          .thenReturn(new SessionDeletionResult("user-1", 2L));

      wsPresenceManager.removeOrphanSession("user-1", "session-1");

      verify(wsPresenceRepository).deleteAndCountRemaining("session-1");
      verify(wsPresenceRepository, never()).upsertLastSeen(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Session is open: row is not an orphan, nothing deleted")
    void removeOrphanSession_sessionOpen_noDelete() {
      when(sessions.isOpen("session-1")).thenReturn(true);

      wsPresenceManager.removeOrphanSession("user-1", "session-1");

      verify(wsPresenceRepository, never()).deleteAndCountRemaining(anyString());
      verifyNoBroadcast();
    }

    @Test
    @DisplayName("Repository failure propagates to the caller")
    void removeOrphanSession_repositoryFailure_propagates() {
      when(sessions.isOpen("session-1")).thenReturn(false);
      doThrow(new RuntimeException("DB error"))
          .when(wsPresenceRepository)
          .deleteAndCountRemaining(anyString());

      assertThrows(
          RuntimeException.class,
          () -> wsPresenceManager.removeOrphanSession("user-1", "session-1"));
    }
  }
}
