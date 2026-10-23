// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class WsPresenceBootCleanerTest {

  private static final OffsetDateTime LAST_SEEN = OffsetDateTime.parse("2026-10-13T10:00:00Z");

  private static Map<String, OffsetDateTime> lastSeenOf(String... users) {
    Map<String, OffsetDateTime> result = new LinkedHashMap<>();
    for (String user : users) {
      result.put(user, LAST_SEEN);
    }
    return result;
  }

  private WsPresenceRepository repo;
  private ServiceIdProvider serviceIdProvider;
  private PresenceBroadcaster presenceBroadcaster;
  private WsPresenceBootCleaner bootCleaner;

  private static final String SERVICE_ID = "service-uuid-boot";

  @BeforeEach
  void setUp() {
    repo = mock(WsPresenceRepository.class);
    serviceIdProvider = mock(ServiceIdProvider.class);
    presenceBroadcaster = mock(PresenceBroadcaster.class);
    when(serviceIdProvider.get()).thenReturn(SERVICE_ID);
    bootCleaner = new WsPresenceBootCleaner(repo, serviceIdProvider, presenceBroadcaster);
  }

  @AfterEach
  void cleanup() {
    reset(repo, serviceIdProvider, presenceBroadcaster);
  }

  @Nested
  @DisplayName("runOnce tests")
  class RunOnceTests {

    @Test
    @DisplayName("Users now offline: broadcastPresence(offline) called for each returned user")
    void runOnce_usersNowOffline_broadcast() {
      // The set-based recovery already deleted rows + upserted last_seen; it returns only the
      // users with zero remaining sessions anywhere. u1 is fully offline; u2 (still online
      // elsewhere) is intentionally NOT returned.
      when(repo.recoverStaleService(SERVICE_ID)).thenReturn(lastSeenOf("u1"));

      bootCleaner.runOnce();

      verify(repo).recoverStaleService(SERVICE_ID);
      verify(presenceBroadcaster).broadcastPresence("u1", false, LAST_SEEN);
      verify(presenceBroadcaster, never()).broadcastPresence("u2", false, LAST_SEEN);
    }

    @Test
    @DisplayName("No users offlined: no broadcast")
    void runOnce_noUsersOfflined_noBroadcast() {
      when(repo.recoverStaleService(SERVICE_ID)).thenReturn(Map.of());

      bootCleaner.runOnce();

      verify(repo).recoverStaleService(SERVICE_ID);
      verify(presenceBroadcaster, never())
          .broadcastPresence(
              org.mockito.ArgumentMatchers.anyString(),
              org.mockito.ArgumentMatchers.anyBoolean(),
              org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("Offlined user who reconnected elsewhere meanwhile gets no OFFLINE broadcast")
    void runOnce_offlinedUserReconnectedMeanwhile_notBroadcast() {
      when(repo.recoverStaleService(SERVICE_ID)).thenReturn(lastSeenOf("u1", "u2"));
      when(repo.countOnlineByUserId("u2")).thenReturn(1);

      bootCleaner.runOnce();

      verify(presenceBroadcaster).broadcastPresence("u1", false, LAST_SEEN);
      verify(presenceBroadcaster, never()).broadcastPresence("u2", false, LAST_SEEN);
    }

    @Test
    @DisplayName("Multiple users offlined: each gets a broadcast")
    void runOnce_multipleUsersOfflined_allBroadcast() {
      when(repo.recoverStaleService(SERVICE_ID)).thenReturn(lastSeenOf("u1", "u2"));

      bootCleaner.runOnce();

      verify(presenceBroadcaster).broadcastPresence("u1", false, LAST_SEEN);
      verify(presenceBroadcaster).broadcastPresence("u2", false, LAST_SEEN);
    }
  }
}
