// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.websocket.Session;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EventWebSocketSessions {

  record SessionInfo(WeakReference<Session> sessionRef) {}

  private final ConcurrentHashMap<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> userSessionIndex = new ConcurrentHashMap<>();
  private final SessionPingManager pingManager;

  @Inject
  public EventWebSocketSessions(SessionPingManager pingManager) {
    this.pingManager = pingManager;
  }

  public void add(Session session, String userId) {
    sessionInfoMap.put(session.getId(), new SessionInfo(new WeakReference<>(session)));
    userSessionIndex.put(userId, session.getId());
    pingManager.start(session);
  }

  public void remove(String sessionId) {
    sessionInfoMap.remove(sessionId);
    pingManager.stop(sessionId);
  }

  public void removeUser(String userId) {
    userSessionIndex.remove(userId);
  }

  public void broadcast(String message) {
    sessionInfoMap.forEach(
        (id, info) -> {
          Session s = info.sessionRef().get();
          if (s != null && s.isOpen()) {
            try {
              s.getAsyncRemote().sendText(message);
            } catch (Exception ignored) {
            }
          } else {
            sessionInfoMap.remove(id);
          }
        });
  }

  public boolean hasActiveSessionForUser(String userId) {
    return userSessionIndex.containsKey(userId);
  }

  public void registerUserSession(String userId, String sessionId) {
    userSessionIndex.put(userId, sessionId);
  }
}
