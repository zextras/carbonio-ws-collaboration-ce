// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.ee10.websocket.jakarta.common.JakartaWebSocketSession;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.OpCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class SessionPingManagerTest {

  private ScheduledExecutorService scheduler;
  private SessionPingManager pingManager;

  @BeforeEach
  void setUp() {
    scheduler = mock(ScheduledExecutorService.class);
    pingManager = new SessionPingManager(scheduler);
  }

  private JakartaWebSocketSession newOpenSession(String id) {
    JakartaWebSocketSession session = mock(JakartaWebSocketSession.class);
    CoreSession coreSession = mock(CoreSession.class);
    when(session.getId()).thenReturn(id);
    when(coreSession.isOutputOpen()).thenReturn(true);
    when(session.getCoreSession()).thenReturn(coreSession);
    return session;
  }

  private void tick(int times) {
    for (int i = 0; i < times; i++) pingManager.tick();
  }

  private void verifyPinged(JakartaWebSocketSession session) {
    verifyPinged(session, 1);
  }

  private void verifyPinged(JakartaWebSocketSession session, int times) {
    verify(session.getCoreSession(), times(times))
        .sendFrame(argThat(frame -> frame.getOpCode() == OpCode.PING), any(), eq(false));
  }

  private void verifyNotPinged(JakartaWebSocketSession session) {
    verify(session.getCoreSession(), never()).sendFrame(any(), any(), anyBoolean());
  }

  @SuppressWarnings("unchecked")
  private MessageHandler.Whole<PongMessage> pongHandlerOf(JakartaWebSocketSession session) {
    ArgumentCaptor<MessageHandler.Whole<PongMessage>> captor =
        ArgumentCaptor.forClass(MessageHandler.Whole.class);
    verify(session).addMessageHandler(eq(PongMessage.class), captor.capture());
    return captor.getValue();
  }

  @Test
  void schedulesOneTickerAtThePingInterval() {
    verify(scheduler)
        .scheduleWithFixedDelay(
            any(Runnable.class), eq(15_000L), eq(15_000L), eq(TimeUnit.MILLISECONDS));
  }

  @Test
  void tick_pingsSession_withoutBlocking() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);

    pingManager.tick();

    verifyPinged(session);
    verify(session.getCoreSession(), never()).abort();
  }

  @Test
  void tick_toleratesMaxMissedPongs() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);

    tick(3);

    verifyPinged(session, 3);
    verify(session.getCoreSession(), never()).abort();
  }

  @Test
  void tick_abortsSessionPastMaxMissedPongs_once() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);

    tick(5);

    verify(session.getCoreSession()).abort();
    verifyPinged(session, 3);
  }

  @Test
  void pong_resetsMissedPongs() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);

    tick(3);
    pongHandlerOf(session).onMessage(mock(PongMessage.class));
    tick(3);

    verify(session.getCoreSession(), never()).abort();
    verifyPinged(session, 6);
  }

  @Test
  void tick_forgetsClosedSession() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);
    when(session.getCoreSession().isOutputOpen()).thenReturn(false);

    pingManager.tick();
    when(session.getCoreSession().isOutputOpen()).thenReturn(true);
    pingManager.tick();

    verifyNotPinged(session);
    verify(session.getCoreSession(), never()).abort();
  }

  @Test
  void stop_stopsPinging() {
    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);

    pingManager.stop("s1");
    pingManager.tick();

    verifyNotPinged(session);
  }

  @Test
  void tick_keepsPingingOthersWhenOneSessionFails() {
    JakartaWebSocketSession failing = newOpenSession("a");
    JakartaWebSocketSession healthy = newOpenSession("b");
    CoreSession failingCore = failing.getCoreSession();
    doThrow(new IllegalStateException("boom"))
        .when(failingCore)
        .sendFrame(any(), any(), anyBoolean());
    pingManager.start(failing);
    pingManager.start(healthy);

    pingManager.tick();

    verifyPinged(healthy);
  }

  @Test
  void start_rejectsNonJettySession() {
    Session session = mock(Session.class);

    assertThrows(IllegalStateException.class, () -> pingManager.start(session));
  }

  @Test
  void stopAll_shutsDownTheTicker_andIgnoresNewSessions() {
    pingManager.stopAll();
    when(scheduler.isShutdown()).thenReturn(true);

    JakartaWebSocketSession session = newOpenSession("s1");
    pingManager.start(session);
    pingManager.tick();

    verify(scheduler).shutdownNow();
    verifyNotPinged(session);
  }
}
