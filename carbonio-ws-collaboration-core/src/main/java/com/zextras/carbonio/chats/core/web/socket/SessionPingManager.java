// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionPingManager {

  private static final int PING_INTERVAL = 15;
  private static final int THREAD_POOL_SIZE = 100;

  private static final ScheduledExecutorService scheduler =
      Executors.newScheduledThreadPool(THREAD_POOL_SIZE);

  private static final Cache<Session, Long> sessionCache =
      Caffeine.newBuilder()
          .expireAfterWrite(60, TimeUnit.SECONDS)
          .scheduler(Scheduler.systemScheduler())
          .removalListener(
              (session, timestamp, cause) -> {
                if ((RemovalCause.EXPIRED.equals(cause) || RemovalCause.EXPLICIT.equals(cause))
                    && session != null) {
                  Session s = (Session) session;
                  closeSession(s);
                }
              })
          .build();

  private SessionPingManager() {
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(SessionPingManager::shutdown, "Session ping manager shutdown hook"));
  }

  public static void add(Session session) {
    session.addMessageHandler(
        PongMessage.class,
        pongMessage -> {
          sessionCache.put(session, System.currentTimeMillis());
        });

    scheduler.scheduleAtFixedRate(
        () -> {
          if (session.isOpen()) {
            try {
              session
                  .getAsyncRemote()
                  .sendPing(ByteBuffer.wrap("ping".getBytes(StandardCharsets.UTF_8)));
            } catch (IOException e) {
              ChatsLogger.warn("Error sending ping to websocket session " + session.getId());
              sessionCache.invalidate(session);
            }
          }
        },
        0,
        PING_INTERVAL,
        TimeUnit.SECONDS);
    sessionCache.put(session, System.currentTimeMillis());
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
    sessionCache.invalidate(session);
  }

  public static void shutdown() {
    sessionCache.invalidateAll();
    scheduler.shutdownNow();
  }
}
