// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class DatabaseModuleZoneTest {

  @Test
  @DisplayName("A region zone keeps its id")
  void regionZone() {
    assertEquals("'Europe/Rome'", DatabaseModule.migrationZone(ZoneId.of("Europe/Rome")));
  }

  @Test
  @DisplayName("Zero offsets map to UTC")
  void zeroOffsets() {
    assertEquals("'UTC'", DatabaseModule.migrationZone(ZoneOffset.UTC));
    assertEquals("'UTC'", DatabaseModule.migrationZone(ZoneId.of("UTC")));
    assertEquals("'UTC'", DatabaseModule.migrationZone(ZoneId.of("GMT")));
    assertEquals("'UTC'", DatabaseModule.migrationZone(ZoneId.of("+00:00")));
  }

  @Test
  @DisplayName("A non-zero fixed offset becomes an interval with the ISO sign")
  void fixedOffset() {
    assertEquals("INTERVAL '+01:00'", DatabaseModule.migrationZone(ZoneOffset.ofHours(1)));
    assertEquals("INTERVAL '+01:00'", DatabaseModule.migrationZone(ZoneId.of("GMT+1")));
    assertEquals(
        "INTERVAL '-05:30'", DatabaseModule.migrationZone(ZoneOffset.ofHoursMinutes(-5, -30)));
  }

  @Test
  @DisplayName("Zones that are fixed offsets in disguise are normalized")
  void normalized() {
    assertEquals("'UTC'", DatabaseModule.migrationZone(ZoneId.of("Etc/UTC")));
  }
}
