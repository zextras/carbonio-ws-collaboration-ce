// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
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
}
