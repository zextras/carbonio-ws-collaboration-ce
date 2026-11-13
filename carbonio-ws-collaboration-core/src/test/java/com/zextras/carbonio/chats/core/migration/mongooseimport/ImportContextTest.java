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
class ImportContextTest {

  private final ImportContext context =
      new ImportContext(Instant.parse("2026-11-01T00:00:00.123456789Z"));

  @Test
  @DisplayName("T0 is truncated to the microsecond")
  void t0IsTruncated() {
    assertEquals(Instant.parse("2026-11-01T00:00:00.123456Z"), context.t0());
  }

  @Test
  @DisplayName("The summary is empty without counters")
  void emptySummary() {
    assertEquals("", context.summary());
  }

  @Test
  @DisplayName("The summary lists every non-zero reason with at most three samples")
  void summaryWithSamples() {
    for (int mam = 1; mam <= 5; mam++) {
      context.count(SkipReason.INVALID_SENDER, "room-a", mam);
    }
    context.count(SkipReason.ROW_NULL);
    context.count(SkipReason.EDIT_DROPPED, 4);
    context.count(SkipReason.REACTION_DROPPED, 0);

    String summary = context.summary();

    assertEquals(5, context.of(SkipReason.INVALID_SENDER));
    assertTrue(summary.contains("invalid_sender=5 (e.g. room-a/1, room-a/2, room-a/3)"), summary);
    assertFalse(summary.contains("room-a/4"), summary);
    assertTrue(summary.contains("row_null=1"), summary);
    assertTrue(summary.contains("edit_dropped=4"), summary);
    assertFalse(summary.contains("reaction_dropped"), summary);
  }

  @Test
  @DisplayName("A sample without a MAM id is the bare room or user id")
  void sampleWithoutMam() {
    context.count(SkipReason.ORPHAN_ARCHIVE, "room-z", 0);
    assertTrue(context.summary().contains("orphan_archive=1 (e.g. room-z)"), context.summary());
  }
}
