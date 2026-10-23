// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import jakarta.websocket.Session;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Manages user presence based on WebSocket connection lifecycle.
 *
 * <p>Presence uses transition-based events (online/offline) rather than continuous heartbeats. The
 * DB state ({@code WS_SESSION}, {@code USER_LAST_SEEN}) is authoritative and converges within one
 * janitor sweep, also after an instance crash; transition events are best-effort (a client that was
 * not connected, or a broker outage, misses them), so clients must reload presence when their
 * WebSocket (re)connects instead of relying only on the received transitions. This approach
 * minimizes WebSocket traffic compared to periodic heartbeats.
 */
@Singleton
public class WsPresenceManager {

  private final WsPresenceRepository wsPresenceRepository;
  private final ServiceIdProvider serviceIdProvider;
  private final PresenceBroadcaster presenceBroadcaster;
  private final Database db;
  private final EventWebSocketSessions sessions;

  @Inject
  public WsPresenceManager(
      WsPresenceRepository wsPresenceRepository,
      ServiceIdProvider serviceIdProvider,
      PresenceBroadcaster presenceBroadcaster,
      Database db,
      EventWebSocketSessions sessions) {
    this.wsPresenceRepository = wsPresenceRepository;
    this.serviceIdProvider = serviceIdProvider;
    this.presenceBroadcaster = presenceBroadcaster;
    this.db = db;
    this.sessions = sessions;
  }

  /**
   * Called when a user opens a WebSocket connection.
   *
   * <p>The advisory lock ensures that the session insert and cluster-wide count read are atomic per
   * user, so two concurrent connections for the same user cannot both observe the online transition
   * and both broadcast ONLINE.
   *
   * <p>The insert and the count are fused into a single data-modifying CTE round-trip: the returned
   * count is the user's session count <em>before</em> this insert, so a value of {@code 0} means
   * this is the {@code 0 -> 1} online transition.
   *
   * @param userId user identifier
   * @param session WebSocket session
   */
  public void onConnect(String userId, Session session) {
    try {
      if (registerSession(userId, session.getId())) {
        presenceBroadcaster.broadcastPresence(
            userId, true, wsPresenceRepository.findLastSeenForUser(userId).orElse(null));
      }
    } catch (Exception e) {
      ChatsLogger.warn(
          "Could not insert session '%s' for user '%s': %s"
              .formatted(session.getId(), userId, e.getMessage()));
    }
  }

  /**
   * Called when a user's WebSocket connection closes.
   *
   * <p>The advisory lock ensures that the session deletion and cluster-wide count read are atomic
   * per user, so two concurrent disconnections cannot both observe the offline transition and both
   * broadcast OFFLINE.
   *
   * <p>The delete and the count are fused into a single data-modifying CTE round-trip: the returned
   * count is the affected user's session count <em>before</em> this delete (the deleted row is
   * still included), so a value of {@code 1} means this is the {@code 1 -> 0} offline transition.
   * If the session was not found, the operation is a no-op and no transition is emitted.
   *
   * @param userId user identifier
   * @param session WebSocket session
   */
  public void onDisconnect(String userId, Session session) {
    try {
      unregisterSession(userId, session.getId())
          .ifPresent(lastSeen -> presenceBroadcaster.broadcastPresence(userId, false, lastSeen));
    } catch (Exception e) {
      ChatsLogger.warn(
          "Could not delete session '%s' for user '%s': %s"
              .formatted(session.getId(), userId, e.getMessage()));
    }
  }

  public void restoreSession(String userId, String sessionId) {
    if (restoreIfOpen(userId, sessionId)) {
      presenceBroadcaster.broadcastPresence(
          userId, true, wsPresenceRepository.findLastSeenForUser(userId).orElse(null));
    }
  }

  public void removeOrphanSession(String userId, String sessionId) {
    removeIfClosed(userId, sessionId)
        .ifPresent(lastSeen -> presenceBroadcaster.broadcastPresence(userId, false, lastSeen));
  }

  @Transactional
  private boolean registerSession(String userId, String sessionId) {
    lockUser(userId);
    return insertSession(userId, sessionId);
  }

  @Transactional
  private Optional<OffsetDateTime> unregisterSession(String userId, String sessionId) {
    lockUser(userId);
    return deleteSession(userId, sessionId);
  }

  /**
   * Re-checks the session under the user lock: {@code onClose} unregisters it before {@link
   * #onDisconnect} locks, so a session still open here is deleted later by its own disconnect.
   */
  @Transactional
  private boolean restoreIfOpen(String userId, String sessionId) {
    lockUser(userId);
    return sessions.isOpen(sessionId) && insertSession(userId, sessionId);
  }

  @Transactional
  private Optional<OffsetDateTime> removeIfClosed(String userId, String sessionId) {
    lockUser(userId);
    return sessions.isOpen(sessionId) ? Optional.empty() : deleteSession(userId, sessionId);
  }

  private boolean insertSession(String userId, String sessionId) {
    return wsPresenceRepository.insertAndCountOnline(sessionId, userId, serviceIdProvider.get())
        == 0;
  }

  private Optional<OffsetDateTime> deleteSession(String userId, String sessionId) {
    if (!wsPresenceRepository.deleteAndCountRemaining(sessionId).wasLastSession()) {
      return Optional.empty();
    }
    return Optional.of(wsPresenceRepository.upsertLastSeen(userId));
  }

  private void lockUser(String userId) {
    db.sqlQuery("SELECT pg_advisory_xact_lock(hashtext(:userId))")
        .setParameter("userId", userId)
        .findOneOrEmpty();
  }

  /**
   * Checks if a user has any active WebSocket sessions cluster-wide.
   *
   * @param userId user identifier
   * @return true if the user has at least one open session
   */
  public boolean isOnline(String userId) {
    return wsPresenceRepository.countOnlineByUserId(userId) > 0;
  }
}
