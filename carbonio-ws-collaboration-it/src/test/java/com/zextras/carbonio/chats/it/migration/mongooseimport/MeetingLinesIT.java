// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName(
    "V2_0_1 import: 1:1 call lines from meetingStarted, meetingEnded, meetingDeclined rows")
class MeetingLinesIT {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String ONE_TO_ONE_WITH_MEETING = "cd093b9c-3434-4d5a-bd19-502d3b442faa";
  private static final int ONE_TO_ONE_WITH_MEETING_ARCHIVE = 139;
  private static final String MEETING = "5574b88a-fb72-4f0f-8a01-643a1fb686c4";
  private static final String ONE_TO_ONE_WITHOUT_MEETING = "435064f8-dd38-4dbd-aa41-6e2b689f23c3";
  private static final int ONE_TO_ONE_WITHOUT_MEETING_ARCHIVE = 111;
  private static final String GROUP = "eeece340-593d-41aa-b135-7285e965582c";
  private static final int GROUP_ARCHIVE = 255;

  private static final String CALLER = "ffebb6fc-6c9b-58cf-86bb-6df35a3a30f8";
  private static final int CALLER_SENDER = 2;
  private static final String CALLEE = "1f9f9cc6-eac5-552d-b31b-f382633a1ad7";
  private static final int CALLEE_SENDER = 8;

  private static final long CALL_BASE_MAM = 456003383146132225L + micros(3_600_000_000L);
  private static final long STARTED_MAM = CALL_BASE_MAM;
  private static final long DECLINED_MAM = CALL_BASE_MAM + micros(20_000_000L);
  private static final long ENDED_MAM = CALL_BASE_MAM + micros(125_000_000L);
  private static final long ENDED_WITHOUT_DURATION_MAM = CALL_BASE_MAM + micros(300_000_000L);
  private static final long ENDED_WITH_BAD_DURATION_MAM = CALL_BASE_MAM + micros(310_000_000L);
  private static final long SECOND_STARTED_MAM = CALL_BASE_MAM + micros(400_000_000L);
  private static final long SECOND_ENDED_MAM = CALL_BASE_MAM + micros(430_000_000L);
  private static final long NO_MEETING_STARTED_MAM = 455227107284563969L + micros(60_000_000L);
  private static final long NO_MEETING_DECLINED_MAM = 455227107284563969L + micros(70_000_000L);
  private static final long GROUP_STARTED_MAM = 458597026847809281L + micros(60_000_000L);
  private static final long GROUP_ENDED_MAM = 458597026847809281L + micros(90_000_000L);

  private static ImportedChats chats;

  private static long micros(long value) {
    return value << 8;
  }

  @BeforeAll
  static void importSyntheticDump() throws Exception {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String source = database.newSourceDatabase();
    try (Connection connection = database.connect(source)) {
      long startedAtMillis = ExpectedTimes.mamInstant(STARTED_MAM).toEpochMilli();
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLER_SENDER,
          STARTED_MAM,
          stanza(ONE_TO_ONE_WITH_MEETING, CALLER, "meetingStarted", "", 1));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLEE_SENDER,
          DECLINED_MAM,
          stanza(ONE_TO_ONE_WITH_MEETING, CALLEE, "meetingDeclined", "", 2));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLEE_SENDER,
          ENDED_MAM,
          stanza(
              ONE_TO_ONE_WITH_MEETING,
              CALLEE,
              "meetingEnded",
              "<startedAt>" + startedAtMillis + "</startedAt><duration>125</duration>",
              3));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLER_SENDER,
          ENDED_WITHOUT_DURATION_MAM,
          stanza(
              ONE_TO_ONE_WITH_MEETING,
              CALLER,
              "meetingEnded",
              "<startedAt>" + startedAtMillis + "</startedAt>",
              4));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLER_SENDER,
          ENDED_WITH_BAD_DURATION_MAM,
          stanza(
              ONE_TO_ONE_WITH_MEETING,
              CALLER,
              "meetingEnded",
              "<startedAt>" + startedAtMillis + "</startedAt><duration>soon</duration>",
              5));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLER_SENDER,
          SECOND_STARTED_MAM,
          stanza(ONE_TO_ONE_WITH_MEETING, CALLER, "meetingStarted", "", 6));
      insert(
          connection,
          ONE_TO_ONE_WITH_MEETING_ARCHIVE,
          CALLER_SENDER,
          SECOND_ENDED_MAM,
          stanza(ONE_TO_ONE_WITH_MEETING, CALLER, "meetingEnded", "<duration>30</duration>", 7));
      insert(
          connection,
          ONE_TO_ONE_WITHOUT_MEETING_ARCHIVE,
          CALLER_SENDER,
          NO_MEETING_STARTED_MAM,
          stanza(ONE_TO_ONE_WITHOUT_MEETING, CALLER, "meetingStarted", "", 8));
      insert(
          connection,
          ONE_TO_ONE_WITHOUT_MEETING_ARCHIVE,
          CALLEE_SENDER,
          NO_MEETING_DECLINED_MAM,
          stanza(ONE_TO_ONE_WITHOUT_MEETING, CALLEE, "meetingDeclined", "", 9));
      insert(
          connection,
          GROUP_ARCHIVE,
          CALLER_SENDER,
          GROUP_STARTED_MAM,
          stanza(GROUP, CALLER, "meetingStarted", "", 10));
      insert(
          connection,
          GROUP_ARCHIVE,
          CALLER_SENDER,
          GROUP_ENDED_MAM,
          stanza(
              GROUP,
              CALLER,
              "meetingEnded",
              "<startedAt>" + startedAtMillis + "</startedAt><duration>30</duration>",
              11));
    }
    ImportRun run =
        MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
    assertTrue(run.succeeded(), run::failureText);
    chats = run.chats();
  }

  private static String stanza(String room, String sender, String operation, String extra, int n) {
    return ("<message from='%s@muclight.carbonio/%s@carbonio' id='%016x' type='groupchat'>"
            + "<x xmlns='urn:xmpp:muclight:0#configuration'><operation>%s</operation>"
            + "<timestamp>1791394624541</timestamp>%s</x><body/>"
            + "<x xmlns='http://jabber.org/protocol/muc#user'><item affiliation='member'"
            + " jid='%s@carbonio' role='participant'/></x></message>")
        .formatted(room, sender, 0xa0854872L + n, operation, extra, sender);
  }

  private static void insert(Connection connection, int archive, int sender, long mam, String xml)
      throws SQLException {
    try (PreparedStatement statement =
        connection.prepareStatement(
            "INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
                + " origin_id) VALUES (?, ?, ?, ?, ?, NULL, NULL)")) {
      statement.setLong(1, mam);
      statement.setInt(2, archive);
      statement.setInt(3, sender);
      statement.setString(4, "synthetic");
      statement.setBytes(5, xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      statement.executeUpdate();
    }
  }

  private static SystemEvent event(String room, long mam) {
    return chats.systemEvent(room, mam).orElseThrow(() -> new AssertionError("no event " + mam));
  }

  private static void assertNeitherMessageNorEvent(String room, long mam) {
    assertAll(
        () -> assertTrue(chats.systemEvent(room, mam).isEmpty(), "event for " + mam),
        () -> assertTrue(chats.message(room, mam).isEmpty(), "message for " + mam));
  }

  private static Set<String> keys(JsonNode json) {
    return new java.util.TreeSet<>(
        java.util.stream.StreamSupport.stream(
                java.util.Spliterators.spliteratorUnknownSize(
                    json.fieldNames(), java.util.Spliterator.ORDERED),
                false)
            .toList());
  }

  @Test
  @DisplayName("TL3: meetingStarted in a 1:1 room becomes MEETING_STARTED with the live keys")
  void meetingStarted() {
    SystemEvent started = event(ONE_TO_ONE_WITH_MEETING, STARTED_MAM);
    JsonNode json = started.json();
    assertAll(
        () -> assertEquals("MEETING_STARTED", started.type()),
        () -> assertEquals(CALLER, started.actorId()),
        () -> assertEquals(ExpectedTimes.mamMillis(STARTED_MAM), started.createdAt()),
        () -> assertEquals(Set.of("startedBy", "meetingId"), keys(json)),
        () -> assertEquals(CALLER, json.get("startedBy").asText()),
        () -> assertEquals(MEETING, json.get("meetingId").asText()));
  }

  @Test
  @DisplayName("TL3: meetingDeclined in a 1:1 room becomes MEETING_DECLINED with the live keys")
  void meetingDeclined() {
    SystemEvent declined = event(ONE_TO_ONE_WITH_MEETING, DECLINED_MAM);
    JsonNode json = declined.json();
    assertAll(
        () -> assertEquals("MEETING_DECLINED", declined.type()),
        () -> assertEquals(CALLEE, declined.actorId()),
        () -> assertEquals(ExpectedTimes.mamMillis(DECLINED_MAM), declined.createdAt()),
        () -> assertEquals(Set.of("declinedBy", "meetingId"), keys(json)),
        () -> assertEquals(CALLEE, json.get("declinedBy").asText()),
        () -> assertEquals(MEETING, json.get("meetingId").asText()));
  }

  @Test
  @DisplayName("TL3: meetingEnded in a 1:1 room becomes MEETING_ENDED with duration and startedAt")
  void meetingEnded() {
    SystemEvent ended = event(ONE_TO_ONE_WITH_MEETING, ENDED_MAM);
    JsonNode json = ended.json();
    assertAll(
        () -> assertEquals("MEETING_ENDED", ended.type()),
        () -> assertEquals(CALLEE, ended.actorId()),
        () -> assertEquals(ExpectedTimes.mamMillis(ENDED_MAM), ended.createdAt()),
        () -> assertEquals(Set.of("endedBy", "meetingId", "startedAt", "durationSec"), keys(json)),
        () -> assertEquals(CALLEE, json.get("endedBy").asText()),
        () -> assertEquals(MEETING, json.get("meetingId").asText()),
        () -> assertTrue(json.get("durationSec").isIntegralNumber()),
        () -> assertEquals(125, json.get("durationSec").asLong()),
        () -> assertTrue(json.get("startedAt").isIntegralNumber()),
        () ->
            assertEquals(
                ExpectedTimes.mamInstant(STARTED_MAM).toEpochMilli(),
                json.get("startedAt").asLong()));
  }

  @Test
  @DisplayName("TL3: meetingEnded without startedAt derives it from the row time and the duration")
  void meetingEndedWithoutStartedAt() {
    JsonNode json = event(ONE_TO_ONE_WITH_MEETING, SECOND_ENDED_MAM).json();
    assertAll(
        () -> assertEquals(30, json.get("durationSec").asLong()),
        () ->
            assertEquals(
                ExpectedTimes.mamInstant(SECOND_ENDED_MAM).toEpochMilli() - 30_000L,
                json.get("startedAt").asLong()));
  }

  @Test
  @DisplayName("TL3: meetingEnded with a missing or invalid duration is not imported")
  void meetingEndedWithoutUsableDuration() {
    assertAll(
        () -> assertNeitherMessageNorEvent(ONE_TO_ONE_WITH_MEETING, ENDED_WITHOUT_DURATION_MAM),
        () -> assertNeitherMessageNorEvent(ONE_TO_ONE_WITH_MEETING, ENDED_WITH_BAD_DURATION_MAM));
  }

  @Test
  @DisplayName("TL3: meeting rows of a 1:1 room without a meeting are not imported")
  void oneToOneWithoutMeeting() {
    assertAll(
        () -> assertNeitherMessageNorEvent(ONE_TO_ONE_WITHOUT_MEETING, NO_MEETING_STARTED_MAM),
        () -> assertNeitherMessageNorEvent(ONE_TO_ONE_WITHOUT_MEETING, NO_MEETING_DECLINED_MAM));
  }

  @Test
  @DisplayName("TL3: meeting rows of a GROUP room stay skipped")
  void groupMeetingRowsAreSkipped() {
    assertAll(
        () -> assertNeitherMessageNorEvent(GROUP, GROUP_STARTED_MAM),
        () -> assertNeitherMessageNorEvent(GROUP, GROUP_ENDED_MAM));
  }

  @Test
  @DisplayName("TL3: call lines are ordered by row time and ROOM_CREATED stays first")
  void timeline() {
    List<String> types =
        chats.systemEvents(ONE_TO_ONE_WITH_MEETING).stream().map(SystemEvent::type).toList();
    assertFalse(types.isEmpty());
    assertEquals("ROOM_CREATED", types.get(0));
    assertEquals(
        List.of(
            "MEETING_STARTED",
            "MEETING_DECLINED",
            "MEETING_ENDED",
            "MEETING_STARTED",
            "MEETING_ENDED"),
        types.subList(1, types.size()));
  }

  @Test
  @DisplayName("TL3: the imported meeting events are the only MEETING_* events")
  void meetingEventTotals() {
    assertEquals(
        5, chats.count("SELECT count(*) FROM chats.system_event WHERE type LIKE 'MEETING\\_%'"));
  }
}
