// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class RoomClockTest {

  private static final Instant T = Instant.parse("2026-01-01T10:00:00Z");

  @Test
  @DisplayName("Has no first value before use")
  void empty() {
    assertNull(new RoomClock().first());
  }

  @Test
  @DisplayName("Returns increasing candidates unchanged and remembers the first")
  void increasing() {
    RoomClock clock = new RoomClock();
    assertEquals(T, clock.next(T));
    assertEquals(T.plusSeconds(1), clock.next(T.plusSeconds(1)));
    assertEquals(T, clock.first());
  }

  @Test
  @DisplayName("Bumps equal or older candidates by one microsecond over the last value")
  void bumps() {
    RoomClock clock = new RoomClock();
    clock.next(T);
    assertEquals(T.plusNanos(1_000), clock.next(T));
    assertEquals(T.plusNanos(2_000), clock.next(T.minusSeconds(5)));
    assertEquals(T, clock.first());
  }
}
