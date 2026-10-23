// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.infrastructure.consul.DiscoveryService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Scheduled backstop that recovers presence for WSC instances that went <em>down and did not come
 * back up</em>.
 *
 * <p><strong>Why this exists.</strong> When an instance restarts, {@link WsPresenceBootCleaner}
 * runs on boot and self-heals that instance's own orphaned {@code WS_SESSION} rows. With {@code
 * systemctl} auto-restart, the came-back-up case is therefore handled almost immediately by the
 * boot cleaner, not by this janitor. This janitor only matters for the rare case where an instance
 * crashes (or is decommissioned) and <em>never returns</em>: its rows would otherwise linger
 * forever and the affected users would appear online indefinitely. In a healthy cluster this
 * janitor normally finds nothing to do: it runs the single {@code DISTINCT service_id} scan, sees
 * every service is healthy according to Consul, and exits.
 *
 * <p><strong>How it works.</strong> Runs every {@value INTERVAL_MINUTES} minutes. It compares the
 * distinct service/node identifiers present in {@code WS_SESSION} against the healthy instances
 * reported by Consul; any identifier present in the DB but absent from Consul is considered stale.
 * For each stale service/node it deletes the orphaned sessions and broadcasts an OFFLINE event for
 * every affected user whose total session count drops to zero — both done set-based in O(1) queries
 * per stale service (no per-user count loop).
 *
 * <p>The same sweep reconciles this instance's own rows with its open sessions. DB rows are read
 * before open sessions: a session is registered before its row is written and unregistered before
 * its row is deleted, so a row without an open session is closing or orphaned.
 */
@Singleton
public class WsPresenceJanitorService {

  private static final long INTERVAL_MINUTES = 5;
  private static final String SERVICE_NAME = "carbonio-ws-collaboration";
  private static final String SERVICE_METADATA_ID = "service_id";

  private final WsPresenceRepository repo;
  private final DiscoveryService discoveryService;
  private final ServiceIdProvider serviceIdProvider;
  private final PresenceBroadcaster presenceBroadcaster;
  private final WsPresenceManager wsPresenceManager;
  private final EventWebSocketSessions sessions;
  private final ScheduledExecutorService scheduler;

  @Inject
  public WsPresenceJanitorService(
      WsPresenceRepository repo,
      DiscoveryService discoveryService,
      ServiceIdProvider serviceIdProvider,
      PresenceBroadcaster presenceBroadcaster,
      WsPresenceManager wsPresenceManager,
      EventWebSocketSessions sessions) {
    this.repo = repo;
    this.discoveryService = discoveryService;
    this.serviceIdProvider = serviceIdProvider;
    this.presenceBroadcaster = presenceBroadcaster;
    this.wsPresenceManager = wsPresenceManager;
    this.sessions = sessions;
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r, "WSC-PresenceJanitor");
              t.setDaemon(true);
              return t;
            });
  }

  /** Starts the periodic sweep. Called explicitly from {@code Boot} after the server starts. */
  public void start() {
    scheduler.scheduleWithFixedDelay(
        this::sweep, INTERVAL_MINUTES, INTERVAL_MINUTES, TimeUnit.MINUTES);
    ChatsLogger.info(
        "WsPresenceJanitorService started, running every %d minutes".formatted(INTERVAL_MINUTES));
  }

  /**
   * Recovers stale services, then repairs this instance's own sessions; the steps are independent.
   */
  void sweep() {
    recoverStaleServices();
    repairOwnSessions();
  }

  /**
   * Compares the set of service/node identifiers found in the DB against the healthy instances
   * reported by Consul. Any service/node identifier present in the DB but absent from Consul is
   * considered stale and its sessions are cleaned up.
   *
   * <p>This node's own {@code service_id} is always kept in the healthy set as a defensive measure
   * against transient Consul reporting gaps.
   */
  private void recoverStaleServices() {
    try {
      Set<String> healthy =
          discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID).stream()
              .map(UUID::toString)
              .collect(Collectors.toSet());
      // Defensive: never drop our own service_id, even if Consul transiently reports unhealthy
      healthy.add(serviceIdProvider.get());

      Set<String> onlineServiceIds = repo.findDistinctOnlineServiceIds();
      Set<String> stale = new HashSet<>(onlineServiceIds);
      stale.removeAll(healthy);

      if (stale.isEmpty()) {
        ChatsLogger.debug("WsPresenceJanitor: no stale instances");
        return;
      }

      for (String staleServiceId : stale) {
        // Set-based: one round-trip deletes the stale service's rows, upserts USER_LAST_SEEN for
        // the users that drop to zero sessions anywhere (anti-join), and returns exactly those
        // users. No per-user COUNT(*) loop. We then make one broadcast pass over them.
        Map<String, OffsetDateTime> nowOfflineUsers = repo.recoverStaleService(staleServiceId);
        ChatsLogger.info(
            "WsPresenceJanitor: cleaned stale WS_SESSION rows for service_id=%s, %d users now offline"
                .formatted(staleServiceId, nowOfflineUsers.size()));
        for (Map.Entry<String, OffsetDateTime> offline : nowOfflineUsers.entrySet()) {
          // Re-checked right before each OFFLINE: the user may have reconnected on another
          // instance after the recovery query, which has no per-user lock against onConnect.
          if (repo.countOnlineByUserId(offline.getKey()) == 0) {
            presenceBroadcaster.broadcastPresence(offline.getKey(), false, offline.getValue());
          }
        }
      }
    } catch (Exception e) {
      ChatsLogger.warn("WsPresenceJanitor: error during sweep: " + e.getMessage());
    }
  }

  private void repairOwnSessions() {
    try {
      Map<String, String> dbSessions = repo.findSessionsByServiceId(serviceIdProvider.get());
      Map<String, String> openSessions = sessions.openSessionUserIds();
      int missing = 0;
      for (Map.Entry<String, String> open : openSessions.entrySet()) {
        if (!dbSessions.containsKey(open.getKey())) {
          wsPresenceManager.restoreSession(open.getValue(), open.getKey());
          missing++;
        }
      }
      int orphans = 0;
      for (Map.Entry<String, String> row : dbSessions.entrySet()) {
        if (!openSessions.containsKey(row.getKey())) {
          wsPresenceManager.removeOrphanSession(row.getValue(), row.getKey());
          orphans++;
        }
      }
      if (missing > 0 || orphans > 0) {
        ChatsLogger.warn(
            "WsPresenceJanitor: own sessions out of sync with DB, %d missing and %d orphan rows repaired"
                .formatted(missing, orphans));
      }
    } catch (Exception e) {
      ChatsLogger.warn("WsPresenceJanitor: error during own sessions repair: " + e.getMessage());
    }
  }
}
