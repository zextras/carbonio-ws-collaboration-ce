// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class SessionPingManager {

  private static final int PING_INTERVAL_SECONDS = 30;
  private static final ByteBuffer PING_PAYLOAD = ByteBuffer.wrap(new byte[] {0x01});

  private final AtomicInteger threadNumber = new AtomicInteger(1);
  private final ScheduledExecutorService scheduler;
  private final ConcurrentHashMap<String, ScheduledFuture<?>> futures = new ConcurrentHashMap<>();

  public SessionPingManager() {
    this.scheduler =
        Executors.newScheduledThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors()),
            r -> {
              Thread t = new Thread(r, "SessionPingManager-" + threadNumber.getAndIncrement());
              t.setDaemon(true);
              return t;
            });
    Runtime.getRuntime().addShutdownHook(new Thread(this::stopAll, "SessionPingManager shutdown"));
  }

  public void start(Session session) {
    if (scheduler.isShutdown()) return;
    ScheduledFuture<?> future =
        scheduler.scheduleAtFixedRate(
            () -> sendPing(session), 0, PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
    futures.put(session.getId(), future);
  }

  public void stop(String sessionId) {
    ScheduledFuture<?> future = futures.remove(sessionId);
    if (future != null) future.cancel(true);
  }

  public void stopAll() {
    futures.values().forEach(f -> f.cancel(true));
    futures.clear();
    scheduler.shutdownNow();
  }

  private void sendPing(Session session) {
    if (!session.isOpen()) {
      stop(session.getId());
      return;
    }
    try {
      session.getAsyncRemote().sendPing(PING_PAYLOAD.duplicate());
    } catch (IOException e) {
      ChatsLogger.warn("Error sending ping to websocket session " + session.getId());
    }
  }
}
