// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.jetty.ee10.websocket.jakarta.common.JakartaWebSocketSession;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.websocket.core.CoreSession;
import org.eclipse.jetty.websocket.core.Frame;
import org.eclipse.jetty.websocket.core.OpCode;

@Singleton
public class SessionPingManager {

  static final Duration PING_INTERVAL = Duration.ofSeconds(15);
  static final int MAX_MISSED_PONGS = 3;
  private static final ByteBuffer PING_PAYLOAD = ByteBuffer.wrap(new byte[] {0x01});

  private record TrackedSession(CoreSession coreSession, AtomicInteger missedPongs) {}

  private final ConcurrentHashMap<String, TrackedSession> sessions = new ConcurrentHashMap<>();
  private final ScheduledExecutorService scheduler;

  public SessionPingManager() {
    this(
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "SessionPingManager");
              t.setDaemon(true);
              return t;
            }));
    Runtime.getRuntime().addShutdownHook(new Thread(this::stopAll, "SessionPingManager shutdown"));
  }

  SessionPingManager(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
    scheduler.scheduleWithFixedDelay(
        this::tick, PING_INTERVAL.toMillis(), PING_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
  }

  public void start(Session session) {
    if (scheduler.isShutdown()) return;
    if (!(session instanceof JakartaWebSocketSession jettySession)) {
      throw new IllegalStateException(
          "Websocket session is not a Jetty session: " + session.getClass().getName());
    }
    String sessionId = session.getId();
    sessions.put(sessionId, new TrackedSession(jettySession.getCoreSession(), new AtomicInteger()));
    session.addMessageHandler(PongMessage.class, pong -> onPong(sessionId));
  }

  public void stop(String sessionId) {
    sessions.remove(sessionId);
  }

  public void stopAll() {
    sessions.clear();
    scheduler.shutdownNow();
  }

  private void onPong(String sessionId) {
    TrackedSession tracked = sessions.get(sessionId);
    if (tracked != null) tracked.missedPongs().set(0);
  }

  void tick() {
    sessions.forEach(
        (sessionId, tracked) -> {
          try {
            if (!tracked.coreSession().isOutputOpen()) {
              sessions.remove(sessionId);
            } else if (tracked.missedPongs().getAndIncrement() >= MAX_MISSED_PONGS) {
              sessions.remove(sessionId);
              ChatsLogger.warn(
                  String.format(
                      "Websocket session '%s' missed %d pongs, aborting it",
                      sessionId, MAX_MISSED_PONGS));
              tracked.coreSession().abort();
            } else {
              tracked
                  .coreSession()
                  .sendFrame(
                      new Frame(OpCode.PING, PING_PAYLOAD.duplicate()), Callback.NOOP, false);
            }
          } catch (Exception e) {
            ChatsLogger.warn("Error pinging websocket session " + sessionId);
          }
        });
  }
}
