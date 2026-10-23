// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Cleans up stale {@code WS_SESSION} rows left by a previous instance run (e.g., a crash).
 *
 * <p>Must be called after {@code flyway.migrate()} and before the WebSocket endpoint starts
 * accepting connections. There is a brief race window: if another WSC instance is running and a
 * user has sessions on both this node and that node, the user may appear OFFLINE briefly before
 * reconnecting.
 */
@Singleton
public class WsPresenceBootCleaner {

  private final WsPresenceRepository repo;
  private final ServiceIdProvider serviceIdProvider;
  private final PresenceBroadcaster presenceBroadcaster;

  @Inject
  public WsPresenceBootCleaner(
      WsPresenceRepository repo,
      ServiceIdProvider serviceIdProvider,
      PresenceBroadcaster presenceBroadcaster) {
    this.repo = repo;
    this.serviceIdProvider = serviceIdProvider;
    this.presenceBroadcaster = presenceBroadcaster;
  }

  /**
   * Deletes all {@code WS_SESSION} rows for this service/node, then broadcasts an OFFLINE event for
   * any user whose total session count drops to zero as a result.
   *
   * <p>The delete, the now-offline detection and the {@code USER_LAST_SEEN} upsert are done
   * set-based in a single SQL round-trip (see {@link WsPresenceRepository#recoverStaleService}); we
   * then make one broadcast pass over the returned users. No per-user count loop.
   */
  public void runOnce() {
    String serviceId = serviceIdProvider.get();
    Map<String, OffsetDateTime> nowOfflineUsers = repo.recoverStaleService(serviceId);
    ChatsLogger.info(
        "Boot cleanup: cleaned stale WS_SESSION rows for service_id=%s, %d users now offline"
            .formatted(serviceId, nowOfflineUsers.size()));
    for (Map.Entry<String, OffsetDateTime> offline : nowOfflineUsers.entrySet()) {
      // Re-checked right before each OFFLINE: the user may have reconnected on another instance
      // after the recovery query, which has no per-user lock against onConnect.
      if (repo.countOnlineByUserId(offline.getKey()) == 0) {
        presenceBroadcaster.broadcastPresence(offline.getKey(), false, offline.getValue());
      }
    }
  }
}
