// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.Instant;
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
  @DisplayName("Keeps microsecond precision for epoch microseconds")
  void micros() {
    assertEquals(Instant.parse("1970-01-01T00:00:01.123456Z"), MamTime.micros(1_123_456L));
  }

  @Test
  @DisplayName("Accepts years 1 to 9999 only")
  void range() {
    assertTrue(MamTime.inRange(Instant.parse("0001-01-01T00:00:00Z")));
    assertTrue(MamTime.inRange(Instant.parse("9999-12-31T23:59:59.999999Z")));
    assertFalse(MamTime.inRange(Instant.parse("0000-12-31T23:59:59.999999Z")));
    assertFalse(MamTime.inRange(Instant.parse("+10000-01-01T00:00:00Z")));
  }
}
