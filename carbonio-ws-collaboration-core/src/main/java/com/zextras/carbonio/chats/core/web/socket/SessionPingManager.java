// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

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

public class SessionPingManager {

  private static final int PING_INTERVAL = 30;
  private static final int ARBITRARY_BYTE = 0x01;
  private static final ByteBuffer PING_PAYLOAD = ByteBuffer.wrap(new byte[] {ARBITRARY_BYTE});

  private static final AtomicInteger THREAD_NUMBER = new AtomicInteger(1);
  private static final int THREAD_POOL_SIZE =
      Math.max(4, Runtime.getRuntime().availableProcessors());

  private static final ConcurrentHashMap<Session, ScheduledFuture<?>> ACTIVE_SESSIONS =
      new ConcurrentHashMap<>();

  private static final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(THREAD_POOL_SIZE, r -> createDaemonThread(r));

  public static void add(Session session) {
    ScheduledFuture<?> future =
        scheduler.scheduleAtFixedRate(
            () -> {
              if (session.isOpen()) {
                try {
                  session.getAsyncRemote().sendPing(PING_PAYLOAD);
                } catch (IOException e) {
                  ChatsLogger.warn("Error sending ping to websocket session " + session.getId());
                  remove(session);
                }
              } else {
                remove(session);
              }
            },
            0,
            PING_INTERVAL,
            TimeUnit.SECONDS);
    ACTIVE_SESSIONS.put(session, future);
  }

  private static void closeSession(Session session) {
    if (session.isOpen()) {
      try {
        session.close();
      } catch (IOException e) {
        ChatsLogger.warn("Error closing websocket session: " + session.getId(), e);
      }
    }
  }

  public static void remove(Session session) {
    ScheduledFuture<?> future = ACTIVE_SESSIONS.remove(session);
    if (future != null) {
      future.cancel(true);
    }
    closeSession(session);
  }

  private static Thread createDaemonThread(Runnable r) {
    Thread thread =
        new Thread(r, "SessionPingManager-Scheduler-" + THREAD_NUMBER.getAndIncrement());
    thread.setDaemon(true);
    return thread;
  }
}
