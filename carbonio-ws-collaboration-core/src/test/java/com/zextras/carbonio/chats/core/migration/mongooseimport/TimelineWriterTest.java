// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.sql.SQLException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class TimelineWriterTest {

  private static final int BATCH = 10_000;
  private static final Instant TIME = Instant.parse("2026-10-07T17:29:22.192297Z");

  private CopyCapture capture;
  private TimelineWriter writer;

  @BeforeEach
  void setUp() throws SQLException {
    capture = new CopyCapture();
    writer = new TimelineWriter(capture.connection(), mock(FileLinker.class));
    writer.beginRoom("room-1");
  }

  private void addMessages(int from, int count) throws SQLException {
    for (int i = from; i < from + count; i++) {
      writer.addMessage(i, TimelineWriter.Message.text("m" + i, "user", "text", null, TIME));
    }
  }

  @Test
  @DisplayName("Messages are copied in one batch only when the 10000th row is added")
  void messageBatchBoundary() throws SQLException {
    addMessages(1, BATCH - 1);
    assertEquals(0, capture.copies().size());

    addMessages(BATCH, 1);

    assertEquals(1, capture.copies().size());
    assertEquals(BATCH, capture.copies().get(0).rows());
  }

  @Test
  @DisplayName("The row after a full batch starts the next one")
  void messageBatchSplit() throws SQLException {
    addMessages(1, BATCH + 1);
    assertEquals(1, capture.copies().size());

    writer.flush();

    assertEquals(2, capture.copies().size());
    assertEquals(BATCH, capture.copies().get(0).rows());
    assertEquals(1, capture.copies().get(1).rows());
    assertEquals(BATCH + 1, writer.messages());
  }

  @Test
  @DisplayName("System events are copied in one batch only when the 10000th row is added")
  void eventBatchBoundary() throws SQLException {
    for (int i = 1; i < BATCH; i++) {
      writer.addEvent(i, "e" + i, "MEMBER_ADDED", "user", "{}", TIME);
    }
    assertEquals(0, capture.copies().size());

    writer.addEvent(BATCH, "e" + BATCH, "MEMBER_ADDED", "user", "{}", TIME);

    assertEquals(1, capture.copies().size());
    assertEquals(BATCH, capture.copies().get(0).rows());
  }

  @Test
  @DisplayName("Rows of several rooms share one batch and keep their own room id")
  void rowsOfSeveralRoomsShareOneBatch() throws SQLException {
    addMessages(1, 3);
    writer.beginRoom("room-2");
    addMessages(4, 2);
    assertEquals(0, capture.copies().size());

    writer.flush();

    assertEquals(1, capture.copies().size());
    String[] lines = capture.copies().get(0).payload().split("\n");
    assertEquals(5, lines.length);
    assertEquals("room-1", lines[0].split("\t")[1]);
    assertEquals("room-1", lines[2].split("\t")[1]);
    assertEquals("room-2", lines[3].split("\t")[1]);
    assertEquals("room-2", lines[4].split("\t")[1]);
  }

  @Test
  @DisplayName("A failed batch is attributed to the room and row that opened it")
  void failureNamesTheFirstRowOfTheBatch() throws SQLException {
    addMessages(7, 1);
    writer.beginRoom("room-2");
    addMessages(8, 1);
    capture.failNextCopies(new SQLException("boom"));

    assertThrows(SQLException.class, writer::flush);

    assertEquals(7, writer.failedMam());
    assertEquals("room-1", writer.failedRoom());
    writer.beginRoom("room-3");
    assertEquals(0, writer.failedMam());
    assertTrue(writer.failedRoom() == null);
  }

  @Test
  @DisplayName("A failed system event batch is attributed to the room that opened it")
  void eventFailureNamesTheFirstRowOfTheBatch() throws SQLException {
    writer.addEvent(5, "e5", "MEMBER_ADDED", "user", "{}", TIME);
    writer.beginRoom("room-2");
    writer.addEvent(6, "e6", "MEMBER_ADDED", "user", "{}", TIME);
    capture.failNextCopies(new SQLException("boom"));

    assertThrows(SQLException.class, writer::flush);

    assertEquals(5, writer.failedMam());
    assertEquals("room-1", writer.failedRoom());
  }
}
