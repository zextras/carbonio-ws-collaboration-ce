// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.migration.mongooseimport.MarkerTranscriber.Marker;
import com.zextras.carbonio.chats.core.migration.mongooseimport.RoomRows.Row;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

@UnitTest
class RoomRowsTest {

  private static final int WINDOW = 8192;
  private static final String ROOM = "room-1";
  private static final Instant TIME = Instant.parse("2026-10-07T17:29:22.192297Z");

  private PreparedStatement stored;
  private PreparedStatement storedMessage;
  private ResultSet storedResult;
  private ResultSet storedMessageResult;
  private List<String> events;
  private RoomRows rows;

  @BeforeEach
  void setUp() throws SQLException {
    Connection connection = mock(Connection.class);
    stored = mock(PreparedStatement.class);
    storedMessage = mock(PreparedStatement.class);
    storedResult = mock(ResultSet.class);
    storedMessageResult = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(storedMessage);
    when(connection.prepareStatement(contains("UNION ALL"))).thenReturn(stored);
    when(stored.executeQuery()).thenReturn(storedResult);
    when(storedMessage.executeQuery()).thenReturn(storedMessageResult);
    events = new ArrayList<>();
    rows = new RoomRows(connection);
    rows.beginRoom(ROOM, mam -> "id-" + mam, () -> events.add("flush"), Map.of());
  }

  private static Row message(long mam) {
    return Row.message(mam, "sender", "item-" + mam, TIME, TIME, false);
  }

  private void recordMessages(long from, long to) {
    for (long mam = from; mam <= to; mam++) {
      rows.record(null, message(mam));
    }
  }

  private void storedFinds(String itemId, boolean deleted) throws SQLException {
    when(storedResult.next()).thenReturn(true);
    when(storedResult.getString(1)).thenReturn(itemId);
    when(storedResult.getBoolean(2)).thenReturn(deleted);
  }

  private void storedMessageFinds(String itemId, boolean deleted) throws SQLException {
    when(storedMessageResult.next()).thenReturn(true);
    when(storedMessageResult.getString(1)).thenReturn(itemId);
    when(storedMessageResult.getBoolean(2)).thenReturn(deleted);
  }

  @Test
  @DisplayName("A row inside the window of 8192 rows is resolved without touching the database")
  void rowInsideTheWindow() throws SQLException {
    recordMessages(1, WINDOW);

    assertEquals("item-1", rows.resolveStanza(1).itemId());
    assertEquals("item-" + WINDOW, rows.resolveStanza(WINDOW).itemId());
    verifyNoInteractions(stored, storedMessage);
    assertEquals(List.of(), events);
  }

  @Test
  @DisplayName("The row that falls out of the window is resolved through the database")
  void rowOutsideTheWindow() throws SQLException {
    recordMessages(1, WINDOW + 1);
    storedFinds("stored-1", false);

    Row evicted = rows.resolveStanza(1);

    assertEquals(RoomRows.MESSAGE, evicted.outcome());
    assertEquals("stored-1", evicted.itemId());
    assertEquals(1, evicted.mam());
    verify(stored).setString(1, "id-1");
    verify(stored).setLong(2, 1);
    verify(stored).setString(3, ROOM);
    verify(stored, times(1)).executeQuery();
    assertEquals("item-2", rows.resolveStanza(2).itemId());
    verify(stored, times(1)).executeQuery();
  }

  @Test
  @DisplayName("The database row keeps the stored deleted flag")
  void deletedFlagFromTheDatabase() throws SQLException {
    recordMessages(1, WINDOW + 1);
    storedFinds("stored-1", true);

    Row evicted = rows.resolveStanza(1);

    assertEquals(true, evicted.deleted());
  }

  @Test
  @DisplayName("Pending writes are flushed before a database lookup and not for a window hit")
  void pendingFlushedBeforeTheLookup() throws SQLException {
    recordMessages(1, WINDOW + 1);
    storedFinds("stored-1", false);

    rows.resolveStanza(WINDOW + 1);
    assertEquals(List.of(), events);

    rows.resolveStanza(1);
    assertEquals(List.of("flush"), events);
    InOrder order = inOrder(stored);
    order.verify(stored).setString(1, "id-1");
    order.verify(stored).executeQuery();
  }

  @Test
  @DisplayName("The pending flush runs once per lookup, before the statement is executed")
  void flushRunsBeforeEachExecution() throws SQLException {
    List<String> sequence = new ArrayList<>();
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet result = mock(ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    when(statement.executeQuery())
        .thenAnswer(
            invocation -> {
              sequence.add("query");
              return result;
            });
    RoomRows ordered = new RoomRows(connection);
    ordered.beginRoom(ROOM, mam -> "id-" + mam, () -> sequence.add("flush"), Map.of());

    ordered.resolveStanza(1);
    ordered.resolveStanza(2);

    assertEquals(List.of("flush", "query", "flush", "query"), sequence);
  }

  @Test
  @DisplayName("A row missing from the window and from the database resolves to null")
  void missingRow() throws SQLException {
    when(storedResult.next()).thenReturn(false);

    assertNull(rows.resolveStanza(99));

    verify(stored).setString(1, "id-99");
  }

  @Test
  @DisplayName("An edit row in the window resolves to its original in the window")
  void editRowWithOriginalInTheWindow() throws SQLException {
    Row original = message(10);
    rows.record(null, original);
    rows.record(null, Row.edit(11, "sender", 10, TIME));

    assertSame(original, rows.resolveStanza(11));
    verifyNoInteractions(stored, storedMessage);
  }

  @Test
  @DisplayName("An edit row in the window whose original left the window resolves through the DB")
  void editRowWithEvictedOriginal() throws SQLException {
    rows.record(null, message(10));
    recordMessages(100, 100 + WINDOW - 2);
    rows.record(null, Row.edit(11, "sender", 10, TIME));
    storedMessageFinds("stored-10", true);

    Row resolved = rows.resolveStanza(11);

    assertEquals(RoomRows.MESSAGE, resolved.outcome());
    assertEquals("stored-10", resolved.itemId());
    assertEquals(10, resolved.mam());
    assertEquals(true, resolved.deleted());
    verify(storedMessage).setString(1, "id-10");
    verify(stored, never()).executeQuery();
    assertEquals(List.of("flush"), events);
  }

  @Test
  @DisplayName("An edit row whose original is not stored resolves to the edit row itself")
  void editRowWithMissingOriginal() throws SQLException {
    Row edit = Row.edit(11, "sender", 10, TIME);
    rows.record(null, edit);
    when(storedMessageResult.next()).thenReturn(false);

    assertSame(edit, rows.resolveStanza(11));
  }

  @Test
  @DisplayName("An evicted edit row resolves to its original through the edits table")
  void evictedEditRow() throws SQLException {
    rows.record(null, Row.edit(11, "sender", 10, TIME));
    recordMessages(100, 100 + WINDOW - 1);
    storedFinds("stored-10", false);

    Row resolved = rows.resolveStanza(11);

    assertEquals("stored-10", resolved.itemId());
    assertEquals(RoomRows.MESSAGE, resolved.outcome());
    verify(stored).setString(1, "id-11");
    verify(stored).setLong(2, 11);
    verify(stored).setString(3, ROOM);
  }

  @Test
  @DisplayName("Starting a room drops the rows, the counters and the senders of the previous one")
  void beginRoomResets() throws SQLException {
    rows.record(null, message(1));
    when(storedResult.next()).thenReturn(false);

    rows.beginRoom("room-2", mam -> "other-" + mam, () -> events.add("flush-2"), Map.of());

    assertEquals(0, rows.recorded());
    assertEquals(Map.of(), rows.lastRowTimeBySender());
    assertNull(rows.resolveStanza(1));
    verify(stored).setString(1, "other-1");
    verify(stored).setString(3, "room-2");
    assertEquals(List.of("flush-2"), events);
  }

  @Test
  @DisplayName("The latest row with the client id at or before the marker time is kept")
  void latestRowUpToTheMarker() {
    rows.beginRoom(
        ROOM,
        mam -> "id-" + mam,
        () -> {},
        Map.of("u1", new Marker("c1", 1000), "u2", new Marker("c1", 2000)));

    rows.record("c1", row(500));
    rows.record("c1", row(1000));
    rows.record("other", row(1200));
    rows.record(null, row(1300));
    rows.record("c1", row(1500));
    rows.record("c1", row(2500));

    assertEquals(1000L << 8, rows.latestByClientIdUpTo("c1", 1000).mam());
    assertEquals(1500L << 8, rows.latestByClientIdUpTo("c1", 2000).mam());
  }

  @Test
  @DisplayName("A marker older than every row with its client id resolves to null")
  void markerBeforeEveryRow() {
    rows.beginRoom(ROOM, mam -> "id-" + mam, () -> {}, Map.of("u1", new Marker("c1", 100)));

    rows.record("c1", row(500));

    assertNull(rows.latestByClientIdUpTo("c1", 100));
  }

  @Test
  @DisplayName("A marker row stays resolved after it left the window")
  void markerRowSurvivesEviction() throws SQLException {
    rows.beginRoom(ROOM, mam -> "id-" + mam, () -> {}, Map.of("u1", new Marker("c1", 1000)));
    Row marked = row(900);
    rows.record("c1", marked);
    for (long mam = 2_000; mam < 2_000 + WINDOW + 1; mam++) {
      rows.record(null, message(mam));
    }
    when(storedResult.next()).thenReturn(false);

    assertSame(marked, rows.latestByClientIdUpTo("c1", 1000));
    assertNull(rows.resolveStanza(900L << 8));
    verifyNoInteractions(storedMessage);
  }

  @Test
  @DisplayName("A marker that was not registered at the start of the room is rejected")
  void unregisteredMarker() {
    rows.beginRoom(
        ROOM,
        mam -> "id-" + mam,
        () -> {},
        Map.of("u1", new Marker("c1", 1000), "u2", new Marker("", 5)));

    assertThrows(IllegalStateException.class, () -> rows.latestByClientIdUpTo("c1", 1234));
    assertThrows(IllegalStateException.class, () -> rows.latestByClientIdUpTo("c2", 1000));
    assertThrows(IllegalStateException.class, () -> rows.latestByClientIdUpTo("", 5));
  }

  @Test
  @DisplayName("Markers of the previous room are not carried over")
  void markersAreRoomScoped() {
    rows.beginRoom(ROOM, mam -> "id-" + mam, () -> {}, Map.of("u1", new Marker("c1", 1000)));
    rows.beginRoom("room-2", mam -> "id-" + mam, () -> {}, Map.of());

    assertThrows(IllegalStateException.class, () -> rows.latestByClientIdUpTo("c1", 1000));
  }

  @Test
  @DisplayName("The last row time of a sender ignores later rows with an earlier time")
  void lastRowTimeBySender() {
    Instant early = Instant.parse("2026-01-01T00:00:00Z");
    Instant late = Instant.parse("2026-01-02T00:00:00Z");
    rows.record(null, Row.message(1, "a", "i1", late, late, false));
    rows.record(null, Row.message(2, "a", "i2", early, early, false));
    rows.record(null, Row.notImported(3, "b", early));
    rows.record(null, Row.notImported(4, null, late));

    assertEquals(Map.of("a", late, "b", early), rows.lastRowTimeBySender());
    assertEquals(4, rows.recorded());
  }

  private static Row row(long micros) {
    return message(micros << 8);
  }
}
