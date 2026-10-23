// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

@UnitTest
class VisibilityFloorTest {

  private static final OffsetDateTime T = OffsetDateTime.parse("2026-01-01T10:00:00Z");

  @Test
  void noClearedAtAllowsEverything() {
    assertTrue(new VisibilityFloor(null).canAccess(T.minusYears(10)));
    assertNull(new VisibilityFloor(null).clamp(null));
    assertEquals(T, new VisibilityFloor(null).clamp(T));
  }

  @Test
  void accessIsStrictlyAfterClearedAt() {
    VisibilityFloor floor = new VisibilityFloor(T);
    assertFalse(floor.canAccess(T));
    assertFalse(floor.canAccess(T.minusNanos(1000)));
    assertTrue(floor.canAccess(T.plusNanos(1000)));
  }

  @Test
  void clampTakesTheLaterOfRequestedAndClearedAt() {
    VisibilityFloor floor = new VisibilityFloor(T);
    assertEquals(T, floor.clamp(null));
    assertEquals(T, floor.clamp(T.minusHours(1)));
    assertEquals(T.plusHours(1), floor.clamp(T.plusHours(1)));
  }
}
