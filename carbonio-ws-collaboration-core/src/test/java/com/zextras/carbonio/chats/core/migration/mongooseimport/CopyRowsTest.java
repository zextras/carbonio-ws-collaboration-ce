// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.time.LocalDateTime;
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
  @DisplayName("Writes booleans as t and f")
  void booleans() {
    rows.bool(true).bool(false).endRow();
    assertEquals("t\tf\n", rows.contents());
  }

  @Test
  @DisplayName("Formats timestamps with six fractional digits")
  void timestamp() {
    rows.time(LocalDateTime.of(2026, 10, 7, 17, 29, 22, 192_297_000))
        .time(LocalDateTime.of(2026, 1, 2, 3, 4, 5))
        .endRow();
    assertEquals("2026-10-07 17:29:22.192297\t2026-01-02 03:04:05.000000\n", rows.contents());
  }

  @Test
  @DisplayName("Rounds nanoseconds to the microsecond like pgjdbc")
  void timestampRounding() {
    rows.time(LocalDateTime.of(2026, 1, 1, 0, 0, 0, 1_499))
        .time(LocalDateTime.of(2026, 1, 1, 0, 0, 0, 1_500))
        .time(LocalDateTime.of(2026, 1, 1, 0, 0, 0, 999_999_500))
        .time(LocalDateTime.of(2026, 12, 31, 23, 59, 59, 999_999_999))
        .endRow();
    assertEquals(
        "2026-01-01 00:00:00.000001\t2026-01-01 00:00:00.000002\t"
            + "2026-01-01 00:00:01.000000\t2027-01-01 00:00:00.000000\n",
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
}
