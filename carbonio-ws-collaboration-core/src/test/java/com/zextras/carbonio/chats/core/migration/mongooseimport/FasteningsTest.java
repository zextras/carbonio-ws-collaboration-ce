// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class FasteningsTest {

  private static final int BATCH = 500;
  private static final Instant TIME = Instant.parse("2026-10-07T17:29:22.192297Z");

  private CopyCapture capture;
  private Fastenings fastenings;

  @BeforeEach
  void setUp() throws SQLException {
    capture = new CopyCapture();
    fastenings = new Fastenings(capture.connection());
  }

  private void stageEdits(int from, int count) throws SQLException {
    for (int i = from; i < from + count; i++) {
      fastenings.stageEdit("room", i, 1, "target", "user", "body", TIME);
    }
  }

  @Test
  @DisplayName("Edits are copied to the temp table when the 500th row is staged")
  void editBatchBoundary() throws SQLException {
    stageEdits(1, BATCH - 1);
    assertEquals(0, capture.copies().size());

    stageEdits(BATCH, 1);

    assertEquals(1, capture.copies().size());
    assertEquals(BATCH, capture.copies().get(0).rows());
  }

  @Test
  @DisplayName("Reactions are copied to the temp table when the 500th row is staged")
  void reactionBatchBoundary() throws SQLException {
    for (int i = 1; i < BATCH; i++) {
      fastenings.stageReaction(i, 1, "target", "user", "x", TIME);
    }
    assertEquals(0, capture.copies().size());

    fastenings.stageReaction(BATCH, 1, "target", "user", "x", TIME);

    assertEquals(1, capture.copies().size());
    assertEquals(BATCH, capture.copies().get(0).rows());
  }

  @Test
  @DisplayName("flushEdits copies the staged edits so a database lookup can see them")
  void flushEditsCopiesWhatIsStaged() throws SQLException {
    stageEdits(1, 3);

    fastenings.flushEdits();

    assertEquals(1, capture.copies().size());
    assertEquals(3, capture.copies().get(0).rows());
    fastenings.flushEdits();
    assertEquals(1, capture.copies().size());
  }
}
