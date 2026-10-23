// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class WsPresenceRepositoryIT {

  private final WsPresenceRepository wsPresenceRepository;

  public WsPresenceRepositoryIT(WsPresenceRepository wsPresenceRepository) {
    this.wsPresenceRepository = wsPresenceRepository;
  }

  @Nested
  @DisplayName("Insert and count online tests")
  class InsertAndCountOnlineTest {

    @Test
    @DisplayName("Returns the user's session count before the insert")
    void insertAndCountOnline_returnsPreInsertCount() {
      assertEquals(0L, wsPresenceRepository.insertAndCountOnline("s1", "u1", "svc-a"));
      assertEquals(1L, wsPresenceRepository.insertAndCountOnline("s2", "u1", "svc-b"));
    }

    @Test
    @DisplayName("Re-inserting an existing session is a no-op that does not report a transition")
    void insertAndCountOnline_existingSession_isIdempotent() {
      wsPresenceRepository.insertAndCountOnline("s1", "u1", "svc-a");

      assertEquals(1L, wsPresenceRepository.insertAndCountOnline("s1", "u1", "svc-a"));
      assertEquals(1, wsPresenceRepository.countOnlineByUserId("u1"));
    }
  }

  @Nested
  @DisplayName("Find sessions by service id tests")
  class FindSessionsByServiceIdTest {

    @Test
    @DisplayName("Returns only the sessions owned by the given service, mapped to their users")
    void findSessionsByServiceId_returnsOwnSessionsOnly() {
      wsPresenceRepository.insertAndCountOnline("s1", "u1", "svc-a");
      wsPresenceRepository.insertAndCountOnline("s2", "u2", "svc-a");
      wsPresenceRepository.insertAndCountOnline("s3", "u1", "svc-b");

      assertEquals(
          Map.of("s1", "u1", "s2", "u2"), wsPresenceRepository.findSessionsByServiceId("svc-a"));
    }

    @Test
    @DisplayName("Returns an empty map when the service owns no sessions")
    void findSessionsByServiceId_noSessions_returnsEmpty() {
      assertEquals(Map.of(), wsPresenceRepository.findSessionsByServiceId("svc-a"));
    }
  }

  @Nested
  @DisplayName("Last seen tests")
  class LastSeenTest {

    @Test
    @DisplayName("Upserting the last seen returns the stored value")
    void upsertLastSeen_returnsStoredValue() {
      OffsetDateTime returned = wsPresenceRepository.upsertLastSeen("u1");

      assertTrue(returned.isEqual(wsPresenceRepository.findLastSeenForUser("u1").orElseThrow()));
    }

    @Test
    @DisplayName(
        "Recovering a stale service returns the offlined users with their stored last seen")
    void recoverStaleService_returnsOfflinedUsersWithStoredLastSeen() {
      wsPresenceRepository.insertAndCountOnline("s1", "u1", "svc-a");
      wsPresenceRepository.insertAndCountOnline("s2", "u2", "svc-a");
      wsPresenceRepository.insertAndCountOnline("s3", "u2", "svc-b");

      Map<String, OffsetDateTime> offlined = wsPresenceRepository.recoverStaleService("svc-a");

      assertEquals(java.util.Set.of("u1"), offlined.keySet());
      assertTrue(
          offlined.get("u1").isEqual(wsPresenceRepository.findLastSeenForUser("u1").orElseThrow()));
    }
  }
}
