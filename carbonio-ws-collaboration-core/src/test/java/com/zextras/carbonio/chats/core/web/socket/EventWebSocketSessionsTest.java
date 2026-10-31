// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@UnitTest
class EventWebSocketSessionsTest {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String USER_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

  private SessionPingManager pingManager;
  private EventWebSocketSessions sessions;

  @BeforeEach
  void setUp() {
    pingManager = mock(SessionPingManager.class);
    sessions = new EventWebSocketSessions(pingManager);
  }

  private Session newOpenSession(String id) {
    return newOpenSession(id, USER_A);
  }

  private Session newOpenSession(String id, String userId) {
    Session s = mock(Session.class);
    RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> userProps = new HashMap<>();
    userProps.put(HttpSession.class.getName(), httpSession);
    when(httpSession.getAttribute("userId")).thenReturn(userId);
    when(s.getUserProperties()).thenReturn(userProps);
    when(s.getId()).thenReturn(id);
    when(s.isOpen()).thenReturn(true);
    when(s.getAsyncRemote()).thenReturn(async);
    return s;
  }

  @Test
  void add_storesSession_andStartsPing() {
    Session s = newOpenSession("sid-1");

    sessions.add(s);

    verify(pingManager).start(s);
  }

  @Test
  void remove_removesSession_andStopsPing() {
    Session s = newOpenSession("sid-2");
    sessions.add(s);

    sessions.remove("sid-2");

    verify(pingManager).stop("sid-2");
  }

  @Test
  void broadcast_sendsToOpenSessions() {
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    sessions.add(a);
    sessions.add(b);

    sessions.broadcast("hello");

    verify(a.getAsyncRemote()).sendText("hello");
    verify(b.getAsyncRemote()).sendText("hello");
  }

  @Test
  void broadcast_skipsClosedSessions() {
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    sessions.add(a);
    sessions.add(b);
    when(b.isOpen()).thenReturn(false);

    sessions.broadcast("hello");

    verify(a.getAsyncRemote()).sendText("hello");
    verify(b.getAsyncRemote(), never()).sendText(any(String.class));
  }

  @Test
  void sessionsOf_returnsOnlyThatUsersSessions() {
    Session a1 = newOpenSession("a1", USER_A);
    Session a2 = newOpenSession("a2", USER_A);
    Session b1 = newOpenSession("b1", USER_B);
    sessions.add(a1);
    sessions.add(a2);
    sessions.add(b1);

    assertEquals(Set.of(a1, a2), Set.copyOf(sessions.sessionsOf(USER_A)));
  }

  @Test
  void sessionsOf_excludesRemovedSessions() {
    Session a1 = newOpenSession("a1", USER_A);
    Session a2 = newOpenSession("a2", USER_A);
    sessions.add(a1);
    sessions.add(a2);

    sessions.remove("a1");

    assertEquals(Set.of(a2), Set.copyOf(sessions.sessionsOf(USER_A)));
  }

  @Test
  void sessionsOf_excludesClosedSessions() {
    Session a1 = newOpenSession("a1", USER_A);
    Session a2 = newOpenSession("a2", USER_A);
    sessions.add(a1);
    sessions.add(a2);
    when(a1.isOpen()).thenReturn(false);

    assertEquals(Set.of(a2), Set.copyOf(sessions.sessionsOf(USER_A)));
  }

  @Test
  void sessionsOf_userWithoutSessions_isEmpty() {
    sessions.add(newOpenSession("a1", USER_A));
    sessions.remove("a1");

    assertTrue(sessions.sessionsOf(USER_A).isEmpty());
    assertTrue(sessions.sessionsOf(USER_B).isEmpty());
  }

  @Test
  void get_returnsOpenSessionById() {
    Session a1 = newOpenSession("a1", USER_A);
    sessions.add(a1);

    assertEquals(Optional.of(a1), sessions.get("a1"));
    assertTrue(sessions.get("unknown").isEmpty());
  }

  @Test
  void get_closedSession_isEmpty() {
    Session a1 = newOpenSession("a1", USER_A);
    sessions.add(a1);
    when(a1.isOpen()).thenReturn(false);

    assertTrue(sessions.get("a1").isEmpty());
  }
}
