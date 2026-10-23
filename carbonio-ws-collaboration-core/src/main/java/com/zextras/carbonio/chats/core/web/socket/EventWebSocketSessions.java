// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.websocket.Session;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EventWebSocketSessions {

  record SessionInfo(WeakReference<Session> sessionRef, String userId) {}

  private final ConcurrentHashMap<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();
  private final SessionPingManager pingManager;

  @Inject
  public EventWebSocketSessions(SessionPingManager pingManager) {
    this.pingManager = pingManager;
  }

  public void add(Session session, String userId) {
    sessionInfoMap.put(session.getId(), new SessionInfo(new WeakReference<>(session), userId));
    pingManager.start(session);
  }

  public boolean remove(String sessionId) {
    boolean existed = sessionInfoMap.remove(sessionId) != null;
    pingManager.stop(sessionId);
    return existed;
  }

  public void broadcast(String message) {
    sessionInfoMap.forEach(
        (id, info) -> {
          Session s = info.sessionRef().get();
          if (s == null) {
            sessionInfoMap.remove(id);
          } else if (s.isOpen()) {
            try {
              s.getAsyncRemote().sendText(message);
            } catch (Exception ignored) {
            }
          }
        });
  }

  public boolean isOpen(String sessionId) {
    SessionInfo info = sessionInfoMap.get(sessionId);
    return info != null && isOpen(info);
  }

  public Map<String, String> openSessionUserIds() {
    Map<String, String> open = new HashMap<>();
    sessionInfoMap.forEach(
        (id, info) -> {
          if (isOpen(info)) {
            open.put(id, info.userId());
          }
        });
    return open;
  }

  private static boolean isOpen(SessionInfo info) {
    Session s = info.sessionRef().get();
    return s != null && s.isOpen();
  }
}
