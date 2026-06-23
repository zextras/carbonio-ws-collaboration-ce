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
  private final SessionPingManager pingManager;

  @Inject
  public EventWebSocketSessions(SessionPingManager pingManager) {
    this.pingManager = pingManager;
  }

  public void add(Session session) {
    sessionInfoMap.put(session.getId(), new SessionInfo(new WeakReference<>(session)));
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
}
