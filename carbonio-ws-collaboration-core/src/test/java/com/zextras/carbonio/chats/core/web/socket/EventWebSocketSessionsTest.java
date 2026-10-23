// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class EventWebSocketSessionsTest {

  private SessionPingManager pingManager;
  private EventWebSocketSessions sessions;

  @BeforeEach
  void setUp() {
    pingManager = mock(SessionPingManager.class);
    sessions = new EventWebSocketSessions(pingManager);
  }

  private Session newOpenSession(String id) {
    Session s = mock(Session.class);
    RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
    when(s.getId()).thenReturn(id);
    when(s.isOpen()).thenReturn(true);
    when(s.getAsyncRemote()).thenReturn(async);
    return s;
  }

  @Test
  void add_storesSession_andStartsPing() {
    Session s = newOpenSession("sid-1");

    sessions.add(s, "user-1");

    assertTrue(sessions.isOpen("sid-1"));
    verify(pingManager).start(s);
  }

  @Test
  void remove_removesSession_andStopsPing() {
    Session s = newOpenSession("sid-2");
    sessions.add(s, "user-2");

    assertTrue(sessions.remove("sid-2"));

    verify(pingManager).stop("sid-2");
    assertFalse(sessions.remove("sid-2"));
  }

  @Test
  void broadcast_sendsToOpenSessions() {
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    sessions.add(a, "ua");
    sessions.add(b, "ub");

    sessions.broadcast("hello");

    verify(a.getAsyncRemote()).sendText("hello");
    verify(b.getAsyncRemote()).sendText("hello");
  }

  @Test
  void broadcast_skipsClosedSessions() {
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    sessions.add(a, "ua");
    sessions.add(b, "ub");
    when(b.isOpen()).thenReturn(false);

    sessions.broadcast("hello");

    verify(a.getAsyncRemote()).sendText("hello");
    verify(b.getAsyncRemote(), never()).sendText(any(String.class));
  }

  @Test
  void broadcast_keepsClosedSessionsRegisteredUntilTheirCleanup() {
    Session b = newOpenSession("b");
    sessions.add(b, "ub");
    when(b.isOpen()).thenReturn(false);

    sessions.broadcast("hello");

    assertTrue(sessions.remove("b"));
  }

  @Test
  void isOpen_trueForAddedOpenSession_falseAfterRemove() {
    Session s = newOpenSession("sid-4");
    sessions.add(s, "user-4");

    assertTrue(sessions.isOpen("sid-4"));

    sessions.remove("sid-4");

    assertFalse(sessions.isOpen("sid-4"));
  }

  @Test
  void isOpen_falseForClosedOrUnknownSession() {
    Session s = newOpenSession("sid-5");
    sessions.add(s, "user-5");
    when(s.isOpen()).thenReturn(false);

    assertFalse(sessions.isOpen("sid-5"));
    assertFalse(sessions.isOpen("unknown"));
  }

  @Test
  void openSessionUserIds_mapsOnlyOpenSessionsToTheirUsers() {
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    Session c = newOpenSession("c");
    sessions.add(a, "ua");
    sessions.add(b, "ub");
    sessions.add(c, "ua");
    when(b.isOpen()).thenReturn(false);

    assertEquals(Map.of("a", "ua", "c", "ua"), sessions.openSessionUserIds());
  }
}
