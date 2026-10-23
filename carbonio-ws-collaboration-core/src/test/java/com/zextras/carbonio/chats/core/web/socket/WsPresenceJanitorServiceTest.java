// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.infrastructure.consul.DiscoveryService;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class WsPresenceJanitorServiceTest {

  private static final OffsetDateTime LAST_SEEN = OffsetDateTime.parse("2026-10-13T10:00:00Z");

  private static Map<String, OffsetDateTime> lastSeenOf(String... users) {
    Map<String, OffsetDateTime> result = new LinkedHashMap<>();
    for (String user : users) {
      result.put(user, LAST_SEEN);
    }
    return result;
  }

  private WsPresenceRepository repo;
  private DiscoveryService discoveryService;
  private ServiceIdProvider serviceIdProvider;
  private PresenceBroadcaster presenceBroadcaster;
  private WsPresenceManager wsPresenceManager;
  private EventWebSocketSessions sessions;
  private WsPresenceJanitorService janitorService;

  private static final String SERVICE_NAME = "carbonio-ws-collaboration";
  private static final String SERVICE_METADATA_ID = "service_id";

  @BeforeEach
  void setUp() {
    repo = mock(WsPresenceRepository.class);
    discoveryService = mock(DiscoveryService.class);
    serviceIdProvider = mock(ServiceIdProvider.class);
    presenceBroadcaster = mock(PresenceBroadcaster.class);
    wsPresenceManager = mock(WsPresenceManager.class);
    sessions = mock(EventWebSocketSessions.class);
    janitorService =
        new WsPresenceJanitorService(
            repo,
            discoveryService,
            serviceIdProvider,
            presenceBroadcaster,
            wsPresenceManager,
            sessions);
  }

  @AfterEach
  void cleanup() {
    reset(
        repo,
        discoveryService,
        serviceIdProvider,
        presenceBroadcaster,
        wsPresenceManager,
        sessions);
  }

  @Nested
  @DisplayName("sweep tests")
  class SweepTests {

    @Test
    @DisplayName(
        "Stale service: set-based recovery deletes rows and broadcasts offline for offlined users")
    void sweep_staleServicePresent_recoversAndBroadcasts() {
      UUID uuidA = UUID.randomUUID();
      UUID uuidB = UUID.randomUUID();
      String staleServiceId = uuidB.toString();
      String myServiceId = uuidA.toString();

      when(serviceIdProvider.get()).thenReturn(myServiceId);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(List.of(uuidA));
      when(repo.findDistinctOnlineServiceIds()).thenReturn(Set.of(myServiceId, staleServiceId));
      // The set-based query already deleted rows + upserted last_seen, and returns the offlined
      // user
      when(repo.recoverStaleService(staleServiceId)).thenReturn(lastSeenOf("u1"));

      janitorService.sweep();

      verify(repo).recoverStaleService(staleServiceId);
      verify(repo, never()).recoverStaleService(myServiceId);
      verify(presenceBroadcaster).broadcastPresence("u1", false, LAST_SEEN);
    }

    @Test
    @DisplayName(
        "Self-protection: own service_id never recovered even if Consul returns empty list")
    void sweep_consulReturnsEmpty_ownServiceIdProtected() {
      UUID myUuid = UUID.randomUUID();
      UUID staleUuid = UUID.randomUUID();
      String myServiceId = myUuid.toString();
      String staleServiceId = staleUuid.toString();

      when(serviceIdProvider.get()).thenReturn(myServiceId);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(Collections.emptyList());
      when(repo.findDistinctOnlineServiceIds()).thenReturn(Set.of(myServiceId, staleServiceId));
      when(repo.recoverStaleService(staleServiceId)).thenReturn(lastSeenOf("u2"));

      janitorService.sweep();

      verify(repo).recoverStaleService(staleServiceId);
      verify(repo, never()).recoverStaleService(myServiceId);
      verify(presenceBroadcaster).broadcastPresence("u2", false, LAST_SEEN);
    }

    @Test
    @DisplayName("No stale services: no recovery, no broadcasts")
    void sweep_noStale_noRecoveryNoBroadcasts() {
      UUID uuidA = UUID.randomUUID();
      String serviceId = uuidA.toString();

      when(serviceIdProvider.get()).thenReturn(serviceId);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(List.of(uuidA));
      when(repo.findDistinctOnlineServiceIds()).thenReturn(Set.of(serviceId));

      janitorService.sweep();

      verify(repo, never()).recoverStaleService(org.mockito.ArgumentMatchers.anyString());
      verify(presenceBroadcaster, never())
          .broadcastPresence(
              org.mockito.ArgumentMatchers.anyString(),
              org.mockito.ArgumentMatchers.anyBoolean(),
              org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Offlined user who reconnected elsewhere meanwhile gets no OFFLINE broadcast")
    void sweep_offlinedUserReconnectedMeanwhile_notBroadcast() {
      UUID uuidA = UUID.randomUUID();
      String myServiceId = uuidA.toString();
      String staleServiceId = UUID.randomUUID().toString();

      when(serviceIdProvider.get()).thenReturn(myServiceId);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(List.of(uuidA));
      when(repo.findDistinctOnlineServiceIds()).thenReturn(Set.of(myServiceId, staleServiceId));
      when(repo.recoverStaleService(staleServiceId)).thenReturn(lastSeenOf("u1", "u2"));
      when(repo.countOnlineByUserId("u2")).thenReturn(1);

      janitorService.sweep();

      verify(presenceBroadcaster).broadcastPresence("u1", false, LAST_SEEN);
      verify(presenceBroadcaster, never()).broadcastPresence("u2", false, LAST_SEEN);
    }

    @Test
    @DisplayName("Stale service but no user fully offline: rows recovered, no broadcast")
    void sweep_staleUserHasOtherSessions_noBroadcast() {
      UUID uuidA = UUID.randomUUID();
      UUID uuidB = UUID.randomUUID();
      String healthyServiceId = uuidA.toString();
      String staleServiceId = uuidB.toString();

      when(serviceIdProvider.get()).thenReturn(healthyServiceId);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(List.of(uuidA));
      when(repo.findDistinctOnlineServiceIds())
          .thenReturn(Set.of(healthyServiceId, staleServiceId));
      // All affected users still have sessions elsewhere -> the anti-join returns no offlined users
      when(repo.recoverStaleService(staleServiceId)).thenReturn(Map.of());

      janitorService.sweep();

      verify(repo).recoverStaleService(staleServiceId);
      verify(presenceBroadcaster, never())
          .broadcastPresence(
              org.mockito.ArgumentMatchers.anyString(),
              org.mockito.ArgumentMatchers.anyBoolean(),
              org.mockito.ArgumentMatchers.any());
    }
  }

  @Nested
  @DisplayName("own sessions repair tests")
  class RepairTests {

    private static final String MY_SERVICE_ID = "my-service";

    @BeforeEach
    void stubHealthyCluster() {
      when(serviceIdProvider.get()).thenReturn(MY_SERVICE_ID);
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenReturn(List.of());
      when(repo.findDistinctOnlineServiceIds()).thenReturn(Set.of(MY_SERVICE_ID));
    }

    @Test
    @DisplayName("Open session without a DB row is restored")
    void sweep_openSessionMissingInDb_restoresIt() {
      when(repo.findSessionsByServiceId(MY_SERVICE_ID)).thenReturn(Map.of());
      when(sessions.openSessionUserIds()).thenReturn(Map.of("s1", "u1"));

      janitorService.sweep();

      verify(wsPresenceManager).restoreSession("u1", "s1");
      verify(wsPresenceManager, never()).removeOrphanSession(anyString(), anyString());
    }

    @Test
    @DisplayName("DB row without an open session is removed as orphan")
    void sweep_dbRowWithoutOpenSession_removesOrphan() {
      when(repo.findSessionsByServiceId(MY_SERVICE_ID)).thenReturn(Map.of("s1", "u1"));
      when(sessions.openSessionUserIds()).thenReturn(Map.of());

      janitorService.sweep();

      verify(wsPresenceManager).removeOrphanSession("u1", "s1");
      verify(wsPresenceManager, never()).restoreSession(anyString(), anyString());
    }

    @Test
    @DisplayName("DB and open sessions in sync: nothing repaired")
    void sweep_inSync_noRepair() {
      when(repo.findSessionsByServiceId(MY_SERVICE_ID)).thenReturn(Map.of("s1", "u1"));
      when(sessions.openSessionUserIds()).thenReturn(Map.of("s1", "u1"));

      janitorService.sweep();

      verify(wsPresenceManager, never()).restoreSession(anyString(), anyString());
      verify(wsPresenceManager, never()).removeOrphanSession(anyString(), anyString());
    }

    @Test
    @DisplayName("DB rows are read before the open sessions")
    void sweep_readsDbRowsBeforeOpenSessions() {
      janitorService.sweep();

      InOrder order = inOrder(repo, sessions);
      order.verify(repo).findSessionsByServiceId(MY_SERVICE_ID);
      order.verify(sessions).openSessionUserIds();
    }

    @Test
    @DisplayName("Consul failure skips the stale cleanup but still repairs own sessions")
    void sweep_consulFailure_stillRepairs() {
      when(discoveryService.getHealthyServices(SERVICE_NAME, SERVICE_METADATA_ID))
          .thenThrow(new RuntimeException("consul down"));
      when(repo.findSessionsByServiceId(MY_SERVICE_ID)).thenReturn(Map.of());
      when(sessions.openSessionUserIds()).thenReturn(Map.of("s1", "u1"));

      janitorService.sweep();

      verify(repo, never()).recoverStaleService(anyString());
      verify(wsPresenceManager).restoreSession("u1", "s1");
    }

    @Test
    @DisplayName("Repair failure stops the repair without escaping the sweep")
    void sweep_repairFailure_stopsWithoutThrowing() {
      when(repo.findSessionsByServiceId(MY_SERVICE_ID)).thenReturn(Map.of("s1", "u1", "s2", "u2"));
      when(sessions.openSessionUserIds()).thenReturn(Map.of());
      doThrow(new RuntimeException("DB down"))
          .when(wsPresenceManager)
          .removeOrphanSession(anyString(), anyString());

      janitorService.sweep();

      verify(wsPresenceManager).removeOrphanSession(anyString(), anyString());
    }
  }
}
