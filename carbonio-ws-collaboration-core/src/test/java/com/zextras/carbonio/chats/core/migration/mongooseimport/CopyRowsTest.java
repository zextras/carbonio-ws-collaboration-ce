// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class CopyRowsTest {

  private final CopyRows rows = new CopyRows("t", "a, b, c");

  @Test
  @DisplayName("Escapes backslash, newline, carriage return and tab")
  void escaping() {
    rows.text("a\\b\nc\rd\te").endRow();
    assertEquals("a\\\\b\\nc\\rd\\te\n", rows.contents());
  }

  @Test
  @DisplayName("Keeps non-ASCII text as UTF-8")
  void unicode() {
    rows.text("caffè \uD83D\uDE00").endRow();
    assertEquals("caffè \uD83D\uDE00\n", rows.contents());
  }

  @Test
  @DisplayName("Writes null as the COPY null marker")
  void nulls() {
    rows.text(null).time(null).endRow();
    assertEquals("\\N\t\\N\n", rows.contents());
  }

  @Test
  @DisplayName("Writes non-positive nullable numbers as null")
  void nullableNumber() {
    rows.nullableNumber(0).nullableNumber(-5).nullableNumber(7).number(-3).endRow();
    assertEquals("\\N\t\\N\t7\t-3\n", rows.contents());
  }

  @Test
  @DisplayName("discardRow drops a partially written row and keeps the finished ones")
  void discardPartialRow() {
    rows.text("kept").text("x").text("y").endRow();
    rows.text("partial").number(1);
    rows.discardRow();
    rows.text("next").text("a").text("b").endRow();

    assertEquals("kept\tx\ty\nnext\ta\tb\n", rows.contents());
    assertEquals(2, rows.rows());
  }

  @Test
  @DisplayName("Writes booleans as t and f")
  void booleans() {
    rows.bool(true).bool(false).endRow();
    assertEquals("t\tf\n", rows.contents());
  }

  @Test
  @DisplayName("Formats timestamps with six fractional digits and a UTC offset")
  void timestamp() {
    rows.time(Instant.parse("2026-10-07T17:29:22.192297Z"))
        .time(Instant.parse("2026-01-02T03:04:05Z"))
        .endRow();
    assertEquals("2026-10-07 17:29:22.192297+00\t2026-01-02 03:04:05.000000+00\n", rows.contents());
  }

  @Test
  @DisplayName("Rounds nanoseconds to the microsecond like pgjdbc")
  void timestampRounding() {
    rows.time(Instant.parse("2026-01-01T00:00:00.000001499Z"))
        .time(Instant.parse("2026-01-01T00:00:00.000001500Z"))
        .time(Instant.parse("2026-01-01T00:00:00.999999500Z"))
        .time(Instant.parse("2026-12-31T23:59:59.999999999Z"))
        .endRow();
    assertEquals(
        "2026-01-01 00:00:00.000001+00\t2026-01-01 00:00:00.000002+00\t"
            + "2026-01-01 00:00:01.000000+00\t2027-01-01 00:00:00.000000+00\n",
        rows.contents());
  }

  @Test
  @DisplayName("Accumulates several rows and counts them")
  void severalRows() {
    rows.number(1).text("x").endRow();
    rows.number(2).text("y").endRow();
    assertEquals(2, rows.rows());
    assertEquals("1\tx\n2\ty\n", rows.contents());
  }

  @Test
  @DisplayName("Grows past the initial buffer and keeps the content")
  void growth() {
    String big = "z".repeat(200_000);
    rows.text(big).endRow();
    rows.text(big).endRow();
    assertEquals((big + "\n").repeat(2), rows.contents());
  }

  @Test
  @DisplayName("Clear drops buffered rows and a half-written row")
  void clear() {
    rows.number(1).endRow();
    rows.number(2);
    rows.clear();
    assertEquals(0, rows.rows());
    assertEquals("", rows.contents());
    rows.text("a").text("b").endRow();
    assertEquals("a\tb\n", rows.contents());
  }

  @Test
  @DisplayName("Keeps working after a huge batch is cleared")
  void afterHugeBatch() {
    rows.text("q".repeat(6_000_000)).endRow();
    rows.clear();
    rows.text("ok").endRow();
    assertEquals("ok\n", rows.contents());
  }

  @Test
  @DisplayName("Formats the first and last supported years, leap days and instants before 1970")
  void timestampRange() {
    rows.time(Instant.parse("0001-01-01T00:00:00Z"))
        .time(Instant.parse("9999-12-31T23:59:59.999999Z"))
        .time(Instant.parse("2024-02-29T12:00:00.000050Z"))
        .endRow();
    rows.time(Instant.parse("1969-12-31T23:59:59.500000Z"))
        .time(Instant.parse("1900-02-28T00:00:01.000009Z"))
        .time(Instant.parse("0999-07-08T09:10:11.012345Z"))
        .endRow();
    assertEquals(
        "0001-01-01 00:00:00.000000+00\t9999-12-31 23:59:59.999999+00\t"
            + "2024-02-29 12:00:00.000050+00\n"
            + "1969-12-31 23:59:59.500000+00\t1900-02-28 00:00:01.000009+00\t"
            + "0999-07-08 09:10:11.012345+00\n",
        rows.contents());
  }
}
