// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class MamTimeTest {

  private static final long MAM = 458596905521228033L;

  @Test
  @DisplayName("Derives the instant from the microseconds in the MAM id")
  void instant() {
    assertEquals(Instant.parse("2026-10-07T17:29:22.192297Z"), MamTime.instant(MAM));
  }

  @Test
  @DisplayName("Truncates to milliseconds in the given zone")
  void millis() {
    assertEquals(
        LocalDateTime.of(2026, 10, 7, 17, 29, 22, 192_000_000),
        MamTime.millis(MAM, ZoneOffset.UTC));
    assertEquals(
        LocalDateTime.of(2026, 10, 7, 19, 29, 22, 192_000_000),
        MamTime.millis(MAM, ZoneId.of("Europe/Rome")));
  }

  @Test
  @DisplayName("Keeps microsecond precision for epoch microseconds")
  void micros() {
    assertEquals(
        LocalDateTime.of(1970, 1, 1, 0, 0, 1, 123_456_000),
        MamTime.micros(1_123_456L, ZoneOffset.UTC));
  }
}
