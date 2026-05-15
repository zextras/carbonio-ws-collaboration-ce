// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

@UnitTest
class SessionPingManagerTest {

  private Session newOpenSession(String id) {
    Session s = mock(Session.class);
    RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
    when(s.getId()).thenReturn(id);
    when(s.isOpen()).thenReturn(true);
    when(s.getAsyncRemote()).thenReturn(async);
    return s;
  }

  @Test
  void start_sendsPing_promptly() throws IOException {
    SessionPingManager pm = new SessionPingManager();
    Session s = newOpenSession("s1");

    pm.start(s);

    verify(s.getAsyncRemote(), timeout(2000).atLeastOnce()).sendPing(any(ByteBuffer.class));
  }

  @Test
  void stop_cancelsScheduledPings_andIsIdempotent() throws IOException {
    SessionPingManager pm = new SessionPingManager();
    Session s = newOpenSession("s2");
    pm.start(s);
    verify(s.getAsyncRemote(), timeout(2000).atLeastOnce()).sendPing(any(ByteBuffer.class));

    pm.stop("s2");
    assertDoesNotThrow(() -> pm.stop("s2"));
  }

  @Test
  void stop_unknownSession_isNoOp() {
    SessionPingManager pm = new SessionPingManager();
    assertDoesNotThrow(() -> pm.stop("never-registered"));
  }

  @Test
  void stopAll_cancelsEverything() throws IOException {
    SessionPingManager pm = new SessionPingManager();
    Session a = newOpenSession("a");
    Session b = newOpenSession("b");
    pm.start(a);
    pm.start(b);

    // Wait for at least one ping on each so we know the schedulers were live.
    verify(a.getAsyncRemote(), timeout(2000).atLeastOnce()).sendPing(any(ByteBuffer.class));
    verify(b.getAsyncRemote(), timeout(2000).atLeastOnce()).sendPing(any(ByteBuffer.class));

    clearInvocations(a.getAsyncRemote(), b.getAsyncRemote());

    pm.stopAll();

    // After stopAll, no further pings to a or b for at least 500ms.
    verify(a.getAsyncRemote(), after(500).never()).sendPing(any(ByteBuffer.class));
    verify(b.getAsyncRemote(), after(500).never()).sendPing(any(ByteBuffer.class));

    // And a brand-new session started after stopAll never gets a ping either.
    Session c = newOpenSession("c");
    assertDoesNotThrow(() -> pm.start(c));
    verify(c.getAsyncRemote(), after(200).never()).sendPing(any(ByteBuffer.class));
  }
}
