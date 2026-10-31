// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class EventWebSocketSessions {

  private static final String DEFAULT_VERSION = "1.6.0";

  record SessionInfo(String userId, WeakReference<Session> sessionRef) {}

  private final ConcurrentHashMap<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Set<String>> sessionIdsByUser = new ConcurrentHashMap<>();
  private final SessionPingManager pingManager;

  @Inject
  public EventWebSocketSessions(SessionPingManager pingManager) {
    this.pingManager = pingManager;
  }

  public static String userIdOf(Session session) {
    return (String)
        ((HttpSession) session.getUserProperties().get(HttpSession.class.getName()))
            .getAttribute("userId");
  }

  public static String versionOf(Session session) {
    String subprotocol = session.getNegotiatedSubprotocol();
    return subprotocol != null && !subprotocol.isBlank() ? subprotocol : DEFAULT_VERSION;
  }

  public void add(Session session) {
    String userId = userIdOf(session);
    sessionInfoMap.put(session.getId(), new SessionInfo(userId, new WeakReference<>(session)));
    sessionIdsByUser.compute(
        userId,
        (key, ids) -> {
          Set<String> result = ids != null ? ids : ConcurrentHashMap.newKeySet();
          result.add(session.getId());
          return result;
        });
    pingManager.start(session);
  }

  public boolean remove(String sessionId) {
    boolean existed = forget(sessionId);
    pingManager.stop(sessionId);
    return existed;
  }

  public List<Session> sessionsOf(String userId) {
    Set<String> ids = sessionIdsByUser.get(userId);
    if (ids == null) return List.of();
    return ids.stream().map(this::get).flatMap(Optional::stream).toList();
  }

  public Optional<Session> get(String sessionId) {
    SessionInfo info = sessionInfoMap.get(sessionId);
    if (info == null) return Optional.empty();
    return Optional.ofNullable(info.sessionRef().get()).filter(Session::isOpen);
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
            forget(id);
          }
        });
  }

  private boolean forget(String sessionId) {
    SessionInfo info = sessionInfoMap.remove(sessionId);
    if (info == null) return false;
    sessionIdsByUser.computeIfPresent(
        info.userId(),
        (key, ids) -> {
          ids.remove(sessionId);
          return ids.isEmpty() ? null : ids;
        });
    return true;
  }
}
