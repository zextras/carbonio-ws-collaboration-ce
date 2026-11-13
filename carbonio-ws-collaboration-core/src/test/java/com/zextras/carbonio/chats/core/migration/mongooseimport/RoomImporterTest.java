// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ParsedRow.Kind;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class RoomImporterTest {

  private static final Instant T0 = Instant.parse("2026-11-01T00:00:00Z");
  private static final String ROOM = "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String OTHER_ROOM = "b992e390-3e44-4536-aed5-a5e13572d047";
  private static final String USER = "ba2c72b5-0128-5656-806c-8a163b58450e";
  private static final String MEETING = "5574b88a-fb72-4f0f-8a01-643a1fb686c4";
  private static final Instant PAST = Instant.parse("2026-10-07T17:29:22.192297Z");
  private static final String PAST_TEXT = "2026-10-07 17:29:22.192297+00";
  private static final String T0_TEXT = "2026-11-01 00:00:00.000000+00";

  private CopyCapture capture;
  private ImportContext context;
  private RoomImporter importer;

  private static long mam(Instant time) {
    long micros = ChronoUnit.MICROS.between(Instant.EPOCH, time);
    return micros << 8;
  }

  @BeforeEach
  void setUp() throws SQLException {
    capture = new CopyCapture();
    context = new ImportContext(T0);
    importer =
        new RoomImporter(
            capture.connection(),
            new LegacyIds(new byte[32]),
            mock(FileLinker.class),
            mock(PinImporter.class),
            mock(MarkerTranscriber.class),
            context,
            Set.of(ROOM, OTHER_ROOM));
  }

  private void begin(String type, String meetingId) throws SQLException {
    importer.beginRoom(
        new TargetRoom(ROOM, type, Instant.parse("2020-01-01T00:00:00Z"), USER, meetingId),
        0,
        List.of(),
        Map.of());
  }

  private static ParsedRow row(Kind kind) {
    return new ParsedRow(
        kind, "client", "text", null, null, null, null, null, null, null, null, null, null, null);
  }

  private static ParsedRow tombstone(Instant stamp) {
    return new ParsedRow(
        Kind.TOMBSTONE,
        "client",
        "",
        null,
        null,
        stamp,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static ParsedRow forward(String from, Instant stamp) {
    return new ParsedRow(
        Kind.FORWARD,
        "client",
        "fwd",
        null,
        null,
        null,
        null,
        null,
        from,
        stamp,
        null,
        null,
        null,
        null);
  }

  private static ParsedRow meetingEnded(Long duration, Long startedAt) {
    return new ParsedRow(
        Kind.MEETING_ENDED,
        "client",
        "",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        duration,
        startedAt);
  }

  private List<String[]> lines(String table) throws SQLException {
    importer.finish();
    return capture.copies().stream()
        .filter(copy -> copy.sql().contains(table + " ("))
        .flatMap(copy -> java.util.Arrays.stream(copy.payload().split("\n")))
        .map(line -> line.split("\t", -1))
        .toList();
  }

  private String[] onlyMessage() throws SQLException {
    List<String[]> messages = lines("chats.message");
    assertEquals(1, messages.size());
    return messages.get(0);
  }

  @Test
  @DisplayName("A MAM time later than T0 is clamped to T0 and counted")
  void futureMamTimeIsClamped() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(Instant.parse("2099-01-01T00:00:00Z")), "carbonio", USER, row(Kind.TEXT));

    String[] message = onlyMessage();

    assertEquals(T0_TEXT, message[11]);
    assertEquals(1, context.of(SkipReason.TIME_CLAMPED));
  }

  @Test
  @DisplayName("A retraction stamp earlier than the message is raised to its created_at")
  void retractionBeforeCreationIsRaised() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(PAST), "carbonio", USER, tombstone(Instant.parse("2020-01-01T00:00:00Z")));

    String[] message = onlyMessage();

    assertEquals(PAST_TEXT, message[10]);
    assertEquals(PAST_TEXT, message[11]);
    assertEquals(1, context.of(SkipReason.RETRACT_STAMP_RAISED));
  }

  @Test
  @DisplayName("A retraction stamp later than T0 is clamped to T0")
  void retractionAfterT0IsClamped() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(PAST), "carbonio", USER, tombstone(Instant.parse("2099-01-01T00:00:00Z")));

    String[] message = onlyMessage();

    assertEquals(T0_TEXT, message[10]);
    assertEquals(1, context.of(SkipReason.STAMP_CLAMPED));
  }

  @Test
  @DisplayName("A forward whose author resolves to a room id is imported as a plain message")
  void forwardFromRoomIsPlain() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(PAST), "carbonio", USER, forward(OTHER_ROOM, PAST));

    String[] message = onlyMessage();

    assertEquals("fwd", message[3]);
    assertEquals("f", message[5]);
    assertEquals("\\N", message[6]);
    assertEquals("\\N", message[7]);
    assertEquals(1, context.of(SkipReason.FORWARD_AS_PLAIN));
  }

  @Test
  @DisplayName("A forward without a usable stamp has a NULL forwarded_at")
  void forwardWithoutStampHasNullForwardedAt() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(PAST), "carbonio", USER, forward(USER, null));

    String[] message = onlyMessage();

    assertEquals("t", message[5]);
    assertEquals(USER, message[6]);
    assertEquals("\\N", message[7]);
    assertEquals(1, context.of(SkipReason.FORWARD_STAMP_MISSING));
  }

  @Test
  @DisplayName("A forward stamp later than T0 is clamped to T0")
  void forwardStampAfterT0IsClamped() throws SQLException {
    begin("GROUP", null);
    importer.handle(
        mam(PAST), "carbonio", USER, forward(USER, Instant.parse("2099-01-01T00:00:00Z")));

    assertEquals(T0_TEXT, onlyMessage()[7]);
    assertEquals(1, context.of(SkipReason.STAMP_CLAMPED));
  }

  @Test
  @DisplayName(
      "A meeting duration out of range drops the line, a startedAt out of range is recomputed")
  void meetingBounds() throws SQLException {
    begin("ONE_TO_ONE", MEETING);
    importer.handle(mam(PAST), "carbonio", USER, meetingEnded(Long.MAX_VALUE / 2, 1_000L));
    importer.handle(
        mam(PAST.plusSeconds(10)), "carbonio", USER, meetingEnded(125L, Long.MAX_VALUE));

    List<String[]> events = lines("chats.system_event");

    assertEquals(1, events.size());
    assertEquals("MEETING_ENDED", events.get(0)[2]);
    assertTrue(
        events.get(0)[4].contains(
            "\"startedAt\":" + (PAST.plusSeconds(10).toEpochMilli() - 125_000)),
        events.get(0)[4]);
    assertEquals(1, context.of(SkipReason.MEETING_DURATION_DROPPED));
    assertEquals(1, context.of(SkipReason.MEETING_STARTED_AT_DROPPED));
    assertEquals(1, context.of(SkipReason.MEETING_LINE_SKIPPED));
  }

  @Test
  @DisplayName("A member id is lowercased before the UUID check, an invalid one is counted")
  void memberIds() throws SQLException {
    begin("GROUP", null);
    ParsedRow upper =
        new ParsedRow(
            Kind.MEMBER_ADDED,
            "c1",
            "",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            USER.toUpperCase(java.util.Locale.ROOT),
            null,
            null,
            null);
    ParsedRow broken =
        new ParsedRow(
            Kind.MEMBER_REMOVED,
            "c2",
            "",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "nope",
            null,
            null,
            null);
    importer.handle(mam(PAST), "carbonio", USER, upper);
    importer.handle(mam(PAST.plusSeconds(1)), "carbonio", USER, broken);

    List<String[]> events = lines("chats.system_event");

    assertEquals(1, events.stream().filter(e -> e[2].equals("MEMBER_ADDED")).count());
    assertTrue(events.stream().anyMatch(e -> e[4].contains("\"addedUserIds\":[\"" + USER + "\"]")));
    assertEquals(1, context.of(SkipReason.INVALID_MEMBER_ID));
  }

  @Test
  @DisplayName("Rows with an invalid sender and parser skip reasons are counted")
  void skipReasonsAreCounted() throws SQLException {
    begin("GROUP", null);
    importer.handle(mam(PAST), "carbonio", "not-a-uuid", row(Kind.TEXT));
    importer.handle(
        mam(PAST.plusSeconds(1)), "carbonio", USER, ParsedRow.skip(SkipReason.ROW_BINARY));
    importer.handle(0, "carbonio", USER, row(Kind.TEXT));

    assertEquals(1, context.of(SkipReason.INVALID_SENDER));
    assertEquals(1, context.of(SkipReason.ROW_BINARY));
    assertEquals(1, context.of(SkipReason.INVALID_MAM_TIME));
    assertEquals(0, importer.counts().messages());
  }
}
