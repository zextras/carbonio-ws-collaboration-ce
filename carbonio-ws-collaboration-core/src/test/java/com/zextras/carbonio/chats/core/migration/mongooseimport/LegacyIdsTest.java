// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class LegacyIdsTest {

  private static final byte[] KEY =
      "mongoose-import-it-run-key-2026!".getBytes(StandardCharsets.US_ASCII);
  private static final String ROOM = "e0df7f4a-0000-4000-8000-000000000001";
  private static final long MAM = 458596905521228033L;

  private final LegacyIds ids = new LegacyIds(KEY);

  @Test
  @DisplayName("Matches the pinned oracle values for every slot")
  void pinnedValues() {
    assertEquals("109cbbfc-5017-475c-ad38-57e9dd2db96c", ids.of(LegacyIds.MESSAGE, ROOM, MAM));
    assertEquals("5650ae34-3e0e-44d0-bb91-d823e6014d34", ids.of(LegacyIds.SYSTEM_EVENT, ROOM, MAM));
    assertEquals("a91e6252-7c4d-41c0-911c-0f89ebda56ae", ids.of(LegacyIds.ROOM_CREATED, ROOM, MAM));
  }

  @Test
  @DisplayName("Produces lowercase UUIDv4 with the RFC 4122 variant")
  void format() {
    String id = ids.of(LegacyIds.MESSAGE, ROOM, 42L);
    UUID uuid = UUID.fromString(id);
    assertEquals(4, uuid.version());
    assertEquals(2, uuid.variant());
    assertEquals(id.toLowerCase(Locale.ROOT), id);
  }

  @Test
  @DisplayName("Is deterministic per key and differs across keys, slots and rooms")
  void determinism() {
    assertEquals(ids.of("m", ROOM, 1L), new LegacyIds(KEY).of("m", ROOM, 1L));
    assertNotEquals(ids.of("m", ROOM, 1L), new LegacyIds(new byte[32]).of("m", ROOM, 1L));
    assertNotEquals(ids.of("m", ROOM, 1L), ids.of("s", ROOM, 1L));
    assertNotEquals(ids.of("m", ROOM, 1L), ids.of("m", ROOM, 2L));
    assertNotEquals(ids.of("m", ROOM, 1L), ids.of("m", "other", 1L));
  }
}
