// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("V2_0_1 import of the real MongooseIM dump: stored times and ordering")
class MessageTimeIT {

  private static final String ONE_TO_ONE_NEW_ROOM = "fa0dfad7-7583-4b95-9bd4-a38e7431f1d9";
  private static final String ONE_TO_ONE_HISTORIC_ROOM = "cd093b9c-3434-4d5a-bd19-502d3b442faa";
  private static final String ONE_TO_ONE_ATTACHMENTS_ROOM = "9688feea-bf86-4f63-b08d-a45d72f59287";
  private static final String ONE_TO_ONE_ADDED_ONLY_ROOM = "435064f8-dd38-4dbd-aa41-6e2b689f23c3";
  private static final String GROUP_BURST_ROOM = "8a10332b-0a46-4a7f-8f1a-db54f0dabf02";
  private static final String GROUP_EDITS_ROOM = "5f6b4438-fdb8-48c1-a866-ff3505b8649a";
  private static final String GROUP_PINS_ROOM = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String GROUP_CLEARED_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String ONE_TO_ONE_CLEARED_ROOM = "1c6f38cf-d1a3-4947-a81d-ffb410acbbae";
  private static final String GROUP_ROOM_026D176C = "026d176c-f9e9-44e6-a488-7565d7ae4eef";
  private static final String GROUP_ROOM_158EEBE3 = "158eebe3-11e6-4b25-9148-d34a500621ed";
  private static final String GROUP_ROOM_6457D489 = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String GROUP_ROOM_7BE2CB4F = "7be2cb4f-4ce4-4530-b473-2df640007959";
  private static final String ONE_TO_ONE_ROOM_99B17723 = "99b17723-465e-4f87-bb1c-429687075b0d";
  private static final String GROUP_ROOM_A992E390 = "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String GROUP_ROOM_AF0B223E = "af0b223e-0c55-4dff-82cc-a69149c758b0";
  private static final String GROUP_ROOM_D1D43AA7 = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String GROUP_ROOM_D39BE5DC = "d39be5dc-cb00-4ad4-9ae3-03f4f0894b99";

  private static final long NEW_EMOJI_MESSAGE_MAM = 458595958524937217L;
  private static final long NEW_IOS_MESSAGE_MAM = 458595961492611585L;
  private static final long HISTORIC_FIRST_MESSAGE_MAM = 455227107423669505L;
  private static final long HISTORIC_LAST_MESSAGE_MAM = 455227488294423041L;
  private static final long JUNE_MESSAGE_MAM = 456003398292739585L;
  private static final long HISTORIC_TOMBSTONE_MAM = 456003378545263361L;
  private static final long HISTORIC_MEMBER_ADDED_MAM = 455149392603518465L;

  private static final long BURST_FIRST_MAM = 458597075442481153L;
  private static final long BURST_SECOND_MAM = 458597075442481409L;
  private static final long BURST_THIRD_MAM = 458597075442481665L;
  private static final long AFTER_BURST_MEMBER_REMOVED_MAM = 458597076409250049L;

  private static final long CHAIN_BEFORE_MAM = 456205291464486401L;
  private static final long CHAIN_FIRST_MAM = 456205291490045441L;
  private static final long CHAIN_SECOND_MAM = 456205291490072321L;
  private static final long CHAIN_THIRD_MAM = 456205291490151681L;
  private static final long CHAIN_FOURTH_MAM = 456205291490329601L;
  private static final long CHAIN_AFTER_MAM = 456205291491366657L;
  private static final long PAIR_ONE_FIRST_MAM = 456206541226633217L;
  private static final long PAIR_ONE_SECOND_MAM = 456206541226681345L;
  private static final long PAIR_TWO_FIRST_MAM = 456206541690207233L;
  private static final long PAIR_TWO_SECOND_MAM = 456206541690319873L;

  private static final long SINGLE_EDIT_TARGET_MAM = 455227125773336321L;
  private static final long SINGLE_EDIT_MAM = 455227128769140225L;
  private static final long PINS_ROOM_EDIT_TARGET_MAM = 458595355979477505L;
  private static final long PINS_ROOM_EDIT_MAM = 458595356045439233L;
  private static final long TRIPLE_EDIT_TARGET_MAM = 458596077475982849L;
  private static final long TRIPLE_EDIT_LAST_MAM = 458596078200317697L;

  private static final long LEGACY_TOMBSTONE_MAM = 458596917767526145L;
  private static final long PINNED_EVENT_MAM = 458595361294167553L;
  private static final long UNPINNED_EVENT_MAM = 458595361730871553L;

  private static final long CLEARED_ROOM_BEFORE_CLEAR_MESSAGE_MAM = 458597054747772929L;
  private static final long CLEARED_ROOM_AFTER_CLEAR_MESSAGE_MAM = 458597059874409473L;
  private static final long E0DF_BEFORE_CLEAR_REMOVED_MAM = 458597010183770369L;
  private static final long E0DF_AFTER_CLEAR_ADDED_MAM = 458597010782610689L;
  private static final long E0DF_AFTER_CLEAR_MESSAGE_MAM = 458597011178408193L;
  private static final long BURST_ROOM_BEFORE_CLEAR_REMOVED_MAM = 458597076489162241L;
  private static final long BURST_ROOM_AFTER_CLEAR_ADDED_MAM = 458597077563287553L;

  private static final LocalDateTime YEAR_START = LocalDateTime.of(2026, 1, 1, 0, 0);

  private static final Map<Long, LocalDateTime> REPAIRED_ROWS =
      Map.of(
          BURST_SECOND_MAM, LocalDateTime.parse("2026-10-07T17:40:25.948"),
          BURST_THIRD_MAM, LocalDateTime.parse("2026-10-07T17:40:25.949"),
          CHAIN_THIRD_MAM, LocalDateTime.parse("2026-06-21T14:25:19.884"),
          CHAIN_FOURTH_MAM, LocalDateTime.parse("2026-06-21T14:25:19.885"),
          PAIR_ONE_SECOND_MAM, LocalDateTime.parse("2026-06-21T15:46:41.667"),
          PAIR_TWO_SECOND_MAM, LocalDateTime.parse("2026-06-21T15:46:43.478"));

  private record Tombstone(String roomId, long mamId, String deletedAt) {}

  private static final List<Tombstone> TOMBSTONES =
      List.of(
          new Tombstone(GROUP_ROOM_026D176C, 458596092965907713L, "2026-10-07T16:36:37"),
          new Tombstone(GROUP_ROOM_026D176C, 458596093929128961L, "2026-10-07T16:36:42"),
          new Tombstone(GROUP_ROOM_026D176C, 458596094169649153L, "2026-10-07T16:36:45"),
          new Tombstone(GROUP_ROOM_026D176C, 458596094890013697L, "2026-10-07T16:37:01"),
          new Tombstone(GROUP_ROOM_026D176C, 458596095131025921L, "2026-10-07T16:36:47"),
          new Tombstone(GROUP_ROOM_026D176C, 458596095850036993L, "2026-10-07T16:36:42"),
          new Tombstone(GROUP_ROOM_158EEBE3, 458596943547121153L, "2026-10-07T17:31:55"),
          new Tombstone(GROUP_EDITS_ROOM, 458596076756919041L, "2026-10-07T16:35:25"),
          new Tombstone(GROUP_EDITS_ROOM, 458596079643774465L, "2026-10-07T16:35:37"),
          new Tombstone(GROUP_ROOM_6457D489, 458596165782417921L, "2026-10-07T16:41:15"),
          new Tombstone(GROUP_ROOM_6457D489, 458596167469571073L, "2026-10-07T16:41:21"),
          new Tombstone(GROUP_ROOM_6457D489, 458596168915563265L, "2026-10-07T16:41:26"),
          new Tombstone(GROUP_ROOM_6457D489, 458596171219829505L, "2026-10-07T16:41:39"),
          new Tombstone(GROUP_ROOM_7BE2CB4F, 458615896594573569L, "2026-10-08T14:05:50"),
          new Tombstone(GROUP_PINS_ROOM, 458595356077737473L, "2026-10-07T15:48:31"),
          new Tombstone(ONE_TO_ONE_ROOM_99B17723, 458596162688135169L, "2026-10-07T16:41:01"),
          new Tombstone(GROUP_ROOM_A992E390, 458596134044895233L, "2026-10-07T16:39:38"),
          new Tombstone(GROUP_ROOM_AF0B223E, 458596111461181441L, "2026-10-07T16:37:58"),
          new Tombstone(ONE_TO_ONE_HISTORIC_ROOM, HISTORIC_TOMBSTONE_MAM, "2026-06-12T11:20:17"),
          new Tombstone(GROUP_ROOM_D1D43AA7, 458596936993164289L, "2026-10-07T17:31:37"),
          new Tombstone(GROUP_ROOM_D1D43AA7, 458596937034779649L, "2026-10-07T17:31:41"),
          new Tombstone(GROUP_ROOM_D1D43AA7, 458596937078231041L, "2026-10-07T17:31:41"),
          new Tombstone(GROUP_ROOM_D39BE5DC, 458596132396325633L, "2026-10-07T16:39:29"),
          new Tombstone(GROUP_ROOM_D39BE5DC, 458596139843744513L, "2026-10-07T16:39:33"),
          new Tombstone(GROUP_ROOM_D39BE5DC, 458596159962899969L, "2026-10-07T16:40:57"),
          new Tombstone(GROUP_CLEARED_ROOM, 458596906494726913L, "2026-10-07T17:30:40"),
          new Tombstone(GROUP_CLEARED_ROOM, 458596906904507393L, "2026-10-07T17:29:33"),
          new Tombstone(GROUP_CLEARED_ROOM, 458596907231357953L, "2026-10-07T17:30:40"),
          new Tombstone(GROUP_CLEARED_ROOM, 458596907900401153L, "2026-10-07T17:30:37"),
          new Tombstone(GROUP_CLEARED_ROOM, 458596908001372673L, "2026-10-07T17:30:37"),
          new Tombstone(GROUP_CLEARED_ROOM, 458597018757274881L, "2026-10-07T17:37:00"));

  private static ImportRun run;
  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  private static ImportedChats.Message message(String roomId, long mamId) {
    return chats
        .message(roomId, mamId)
        .orElseThrow(
            () -> new AssertionError("message not imported: room " + roomId + " mam " + mamId));
  }

  private static ImportedChats.SystemEvent event(String roomId, long mamId) {
    return chats
        .systemEvent(roomId, mamId)
        .orElseThrow(
            () ->
                new AssertionError("system event not imported: room " + roomId + " mam " + mamId));
  }

  private static LocalDateTime roomCreatedAt(String roomId) {
    return chats
        .query(
            "SELECT created_at FROM chats.room WHERE id = ?",
            rs -> rs.getObject(1, LocalDateTime.class),
            roomId)
        .get(0);
  }

  private static LocalDateTime clearedAt(String userId, String roomId) {
    return chats
        .query(
            "SELECT cleared_at FROM chats.room_user_settings WHERE user_id = ? AND room_id = ?",
            rs -> rs.getObject(1, LocalDateTime.class),
            userId,
            roomId)
        .get(0);
  }

  private static LocalDateTime ms(LocalDateTime value) {
    return value.truncatedTo(ChronoUnit.MILLIS);
  }

  private static Map<String, List<Long>> sourceMamIdsByRoom() throws SQLException {
    Map<String, List<Long>> ids = new LinkedHashMap<>();
    try (Connection connection =
            MongooseImportDatabase.get().connect(MongooseImportDatabase.SOURCE_DATABASE);
        Statement statement = connection.createStatement();
        ResultSet rs =
            statement.executeQuery(
                "SELECT u.user_name, m.id FROM mam_muc_message m JOIN mam_server_user u ON u.id ="
                    + " m.room_id AND u.server = 'muclight.carbonio' ORDER BY u.user_name, m.id")) {
      while (rs.next()) {
        ids.computeIfAbsent(rs.getString(1), room -> new ArrayList<>()).add(rs.getLong(2));
      }
    }
    return ids;
  }

  private static Map<String, LocalDateTime> createdAtById(String table) {
    return chats
        .query(
            "SELECT id, created_at FROM chats." + table,
            rs -> Map.entry(rs.getString(1), rs.getObject(2, LocalDateTime.class)))
        .stream()
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static LocalDateTime importedCreatedAt(
      Map<String, LocalDateTime> messages,
      Map<String, LocalDateTime> events,
      String roomId,
      long mamId) {
    LocalDateTime asMessage = messages.get(ExpectedIds.message(roomId, mamId));
    return asMessage != null ? asMessage : events.get(ExpectedIds.systemEvent(roomId, mamId));
  }

  @Nested
  @DisplayName("TIME-1: the MAM id is decoded as id>>8 microseconds (B5)")
  class Decoder {

    @Test
    @DisplayName("TIME-1: October 2026 messages carry their real 2026-10-07 date, not 2023")
    void octoberMessagesCarryTheirRealDate() {
      ImportedChats.Message emoji = message(ONE_TO_ONE_NEW_ROOM, NEW_EMOJI_MESSAGE_MAM);
      ImportedChats.Message ios = message(ONE_TO_ONE_NEW_ROOM, NEW_IOS_MESSAGE_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-10-07T16:27:42.988"), emoji.createdAt()),
          () -> assertEquals(ExpectedTimes.mamMillis(NEW_EMOJI_MESSAGE_MAM), emoji.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T16:27:54.580"), ios.createdAt()),
          () -> assertEquals(ExpectedTimes.mamMillis(NEW_IOS_MESSAGE_MAM), ios.createdAt()));
    }

    @Test
    @DisplayName("TIME-1: historical messages carry their May and June 2026 dates")
    void historicalMessagesCarryTheirSpringDates() {
      ImportedChats.Message first = message(ONE_TO_ONE_HISTORIC_ROOM, HISTORIC_FIRST_MESSAGE_MAM);
      ImportedChats.Message last = message(ONE_TO_ONE_HISTORIC_ROOM, HISTORIC_LAST_MESSAGE_MAM);
      ImportedChats.Message june = message(ONE_TO_ONE_ATTACHMENTS_ROOM, JUNE_MESSAGE_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-05-08T09:01:28.373"), first.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-05-08T09:26:16.150"), last.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-06-12T11:21:14.581"), june.createdAt()),
          () ->
              assertEquals(ExpectedTimes.mamMillis(HISTORIC_FIRST_MESSAGE_MAM), first.createdAt()),
          () -> assertEquals(ExpectedTimes.mamMillis(JUNE_MESSAGE_MAM), june.createdAt()));
    }

    @Test
    @DisplayName(
        "TIME-1: a tombstone of a historical room uses its own MAM time and the memberAdded row of"
            + " a 1:1 room is not imported")
    void tombstoneUsesItsOwnMamTimeAndOneToOneMemberAddedIsNotImported() {
      ImportedChats.Message tombstone = message(ONE_TO_ONE_HISTORIC_ROOM, HISTORIC_TOMBSTONE_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-06-12T11:19:57.442"), tombstone.createdAt()),
          () ->
              assertTrue(
                  chats
                      .systemEvent(ONE_TO_ONE_ADDED_ONLY_ROOM, HISTORIC_MEMBER_ADDED_MAM)
                      .isEmpty()));
    }

    @Test
    @DisplayName(
        "TIME-1: no imported row is dated before 2026 as the old (id>>22)+2020 decoder did")
    void noRowIsDatedByTheOldDecoder() {
      long messages = chats.count("SELECT count(*) FROM chats.message");
      long events = chats.count("SELECT count(*) FROM chats.system_event");

      assertAll(
          () -> assertTrue(messages > 600, "messages imported: " + messages),
          () -> assertTrue(events > 100, "system events imported: " + events),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE created_at < ? OR updated_at < ?",
                      YEAR_START,
                      YEAR_START)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE created_at < ?", YEAR_START)));
    }

    @Test
    @DisplayName(
        "TIME-1/TIME-3: every imported row without a same-millisecond predecessor is exactly its"
            + " MAM id time in UTC milliseconds")
    void everyUnrepairedRowIsExactlyItsMamMillis() throws SQLException {
      Map<String, LocalDateTime> messages = createdAtById("message");
      Map<String, LocalDateTime> events = createdAtById("system_event");
      List<String> mismatches = new ArrayList<>();
      int checked = 0;
      for (Map.Entry<String, List<Long>> room : sourceMamIdsByRoom().entrySet()) {
        for (long mamId : room.getValue()) {
          LocalDateTime actual = importedCreatedAt(messages, events, room.getKey(), mamId);
          if (actual == null) {
            continue;
          }
          checked++;
          LocalDateTime expected =
              REPAIRED_ROWS.getOrDefault(mamId, ExpectedTimes.mamMillis(mamId));
          if (!expected.equals(actual)) {
            mismatches.add(
                room.getKey() + "/" + mamId + " expected " + expected + " got " + actual);
          }
        }
      }

      assertTrue(checked > 700, "timeline rows matched to a MAM row: " + checked);
      assertTrue(mismatches.isEmpty(), () -> mismatches.stream().limit(10).toList().toString());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("TIME-1: room.created_at is never rewritten by the import")
    @CsvSource({
      "435064f8-dd38-4dbd-aa41-6e2b689f23c3, 2026-05-04T20:41:54.944",
      "cd093b9c-3434-4d5a-bd19-502d3b442faa, 2026-05-08T09:01:27.930",
      "9688feea-bf86-4f63-b08d-a45d72f59287, 2026-06-12T11:21:14.310",
      "8a10332b-0a46-4a7f-8f1a-db54f0dabf02, 2026-10-07T17:40:19.172"
    })
    void roomCreatedAtIsNeverRewritten(String roomId, String createdAt) {
      assertTrue(
          chats.count("SELECT count(*) FROM chats.system_event WHERE room_id = ?", roomId) > 0);
      assertEquals(LocalDateTime.parse(createdAt), roomCreatedAt(roomId));
    }
  }

  @Nested
  @DisplayName("TIME-3: created_at strictly increasing per room in MAM order")
  class Ordering {

    @Test
    @DisplayName(
        "TIME-3: in all 35 imported rooms created_at strictly increases in MAM order across"
            + " messages and system events, with ROOM_CREATED first")
    void everyRoomIsStrictlyIncreasingInMamOrder() throws SQLException {
      Map<String, LocalDateTime> messages = createdAtById("message");
      Map<String, LocalDateTime> events = createdAtById("system_event");
      Map<String, LocalDateTime> roomCreated =
          chats
              .query(
                  "SELECT room_id, created_at FROM chats.system_event WHERE type = 'ROOM_CREATED'",
                  rs -> Map.entry(rs.getString(1), rs.getObject(2, LocalDateTime.class)))
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
      List<String> violations = new ArrayList<>();
      int roomsWithItems = 0;
      for (Map.Entry<String, List<Long>> room : sourceMamIdsByRoom().entrySet()) {
        LocalDateTime previous = roomCreated.get(room.getKey());
        if (previous == null) {
          continue;
        }
        roomsWithItems++;
        for (long mamId : room.getValue()) {
          LocalDateTime current = importedCreatedAt(messages, events, room.getKey(), mamId);
          if (current == null) {
            continue;
          }
          if (!current.isAfter(previous)) {
            violations.add(
                room.getKey() + "/" + mamId + " " + current + " is not after " + previous);
          }
          previous = current;
        }
      }

      assertEquals(35, roomsWithItems);
      assertTrue(violations.isEmpty(), () -> violations.stream().limit(10).toList().toString());
    }

    @Test
    @DisplayName("TIME-3: no two timeline items of one room share the same created_at")
    void noTwoTimelineItemsShareACreatedAt() {
      long items =
          chats.count(
              "SELECT count(*) FROM (SELECT room_id, created_at FROM chats.message UNION ALL SELECT"
                  + " room_id, created_at FROM chats.system_event) t");
      long duplicated =
          chats.count(
              "SELECT count(*) FROM (SELECT room_id, created_at FROM (SELECT room_id, created_at"
                  + " FROM chats.message UNION ALL SELECT room_id, created_at FROM"
                  + " chats.system_event) t GROUP BY room_id, created_at HAVING count(*) > 1) d");

      assertAll(
          () -> assertTrue(items > 800, "timeline items imported: " + items),
          () -> assertEquals(0, duplicated));
    }

    @Test
    @DisplayName("TIME-3: message created_at is stored at millisecond precision")
    void messageCreatedAtHasMillisecondPrecision() {
      assertAll(
          () -> assertTrue(chats.count("SELECT count(*) FROM chats.message") > 600),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE created_at <>"
                          + " date_trunc('milliseconds', created_at)")));
    }

    @Test
    @DisplayName(
        "TIME-3/C78: three messages inside one millisecond get consecutive milliseconds in MAM"
            + " order")
    void sameMillisecondMessagesGetConsecutiveMilliseconds() {
      ImportedChats.Message first = message(GROUP_BURST_ROOM, BURST_FIRST_MAM);
      ImportedChats.Message second = message(GROUP_BURST_ROOM, BURST_SECOND_MAM);
      ImportedChats.Message third = message(GROUP_BURST_ROOM, BURST_THIRD_MAM);
      ImportedChats.SystemEvent removed = event(GROUP_BURST_ROOM, AFTER_BURST_MEMBER_REMOVED_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:40:25.947"), first.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:40:25.948"), second.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:40:25.949"), third.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:40:29.723"), removed.createdAt()));
    }

    @Test
    @DisplayName("TIME-3/C78: a burst repair cascades onto the next row and stops after it")
    void burstRepairCascadesToTheNextMillisecond() {
      List<Long> chain =
          List.of(
              CHAIN_BEFORE_MAM,
              CHAIN_FIRST_MAM,
              CHAIN_SECOND_MAM,
              CHAIN_THIRD_MAM,
              CHAIN_FOURTH_MAM,
              CHAIN_AFTER_MAM);
      List<LocalDateTime> actual =
          chain.stream()
              .map(mamId -> message(ONE_TO_ONE_ATTACHMENTS_ROOM, mamId).createdAt())
              .toList();

      assertEquals(
          List.of(
              LocalDateTime.parse("2026-06-21T14:25:19.783"),
              LocalDateTime.parse("2026-06-21T14:25:19.882"),
              LocalDateTime.parse("2026-06-21T14:25:19.883"),
              LocalDateTime.parse("2026-06-21T14:25:19.884"),
              LocalDateTime.parse("2026-06-21T14:25:19.885"),
              LocalDateTime.parse("2026-06-21T14:25:19.888")),
          actual);
    }

    @Test
    @DisplayName("TIME-3/C78: two attachment pairs in the same millisecond are separated by 1 ms")
    void sameMillisecondPairsAreSeparatedByOneMillisecond() {
      assertAll(
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-06-21T15:46:41.666"),
                  message(ONE_TO_ONE_ATTACHMENTS_ROOM, PAIR_ONE_FIRST_MAM).createdAt()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-06-21T15:46:41.667"),
                  message(ONE_TO_ONE_ATTACHMENTS_ROOM, PAIR_ONE_SECOND_MAM).createdAt()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-06-21T15:46:43.477"),
                  message(ONE_TO_ONE_ATTACHMENTS_ROOM, PAIR_TWO_FIRST_MAM).createdAt()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-06-21T15:46:43.478"),
                  message(ONE_TO_ONE_ATTACHMENTS_ROOM, PAIR_TWO_SECOND_MAM).createdAt()));
    }

    @Test
    @DisplayName(
        "TIME-3: the burst room timeline keeps MAM order with its system events interleaved")
    void burstRoomTimelineKeepsMamOrder() {
      List<String> ids =
          chats.timeline(GROUP_BURST_ROOM).stream().map(ImportedChats.TimelineItem::id).toList();

      int first = ids.indexOf(ExpectedIds.message(GROUP_BURST_ROOM, BURST_FIRST_MAM));
      int second = ids.indexOf(ExpectedIds.message(GROUP_BURST_ROOM, BURST_SECOND_MAM));
      int third = ids.indexOf(ExpectedIds.message(GROUP_BURST_ROOM, BURST_THIRD_MAM));
      int removed =
          ids.indexOf(ExpectedIds.systemEvent(GROUP_BURST_ROOM, AFTER_BURST_MEMBER_REMOVED_MAM));
      assertAll(
          () -> assertTrue(first >= 0, "first burst message missing"),
          () -> assertEquals(first + 1, second),
          () -> assertEquals(second + 1, third),
          () -> assertTrue(removed > third, "member removed must come after the burst"),
          () -> assertEquals("ROOM_CREATED", chats.timeline(GROUP_BURST_ROOM).get(0).kind()));
    }
  }

  @Nested
  @DisplayName("TIME-5: updated_at and deleted_at derived from the source")
  class DerivedTimes {

    @Test
    @DisplayName("TIME-5: never-edited, never-deleted messages have updated_at = created_at")
    void neverEditedRowsHaveUpdatedAtEqualToCreatedAt() {
      assertAll(
          () ->
              assertTrue(
                  chats.count("SELECT count(*) FROM chats.message WHERE NOT edited AND NOT deleted")
                      > 400),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE NOT edited AND NOT deleted AND"
                          + " updated_at <> created_at")));
    }

    @Test
    @DisplayName("TIME-5/C92: an edited message keeps created_at and updated_at is the edit time")
    void editedMessagesCarryTheEditTime() {
      ImportedChats.Message single = message(ONE_TO_ONE_HISTORIC_ROOM, SINGLE_EDIT_TARGET_MAM);
      ImportedChats.Message inPinsRoom = message(GROUP_PINS_ROOM, PINS_ROOM_EDIT_TARGET_MAM);
      ImportedChats.Message tripled = message(GROUP_EDITS_ROOM, TRIPLE_EDIT_TARGET_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-05-08T09:02:40.052"), single.createdAt()),
          () ->
              assertEquals(LocalDateTime.parse("2026-05-08T09:02:51.754"), ms(single.updatedAt())),
          () -> assertEquals(ExpectedTimes.mamMillis(SINGLE_EDIT_MAM), ms(single.updatedAt())),
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T15:48:29.294"), inPinsRoom.createdAt()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T15:48:29.552"), ms(inPinsRoom.updatedAt())),
          () ->
              assertEquals(ExpectedTimes.mamMillis(PINS_ROOM_EDIT_MAM), ms(inPinsRoom.updatedAt())),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T16:35:27.640"), tripled.createdAt()),
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T16:35:30.469"), ms(tripled.updatedAt())),
          () ->
              assertEquals(ExpectedTimes.mamMillis(TRIPLE_EDIT_LAST_MAM), ms(tripled.updatedAt())));
    }

    @Test
    @DisplayName(
        "TIME-5/C89: all 31 stanza-id tombstones get deleted_at from the retracted stamp and"
            + " updated_at = deleted_at")
    void tombstonesCarryTheirRetractedStamp() {
      List<Executable> checks = new ArrayList<>();
      assertEquals(31, TOMBSTONES.size());
      for (Tombstone tombstone : TOMBSTONES) {
        ImportedChats.Message row = message(tombstone.roomId(), tombstone.mamId());
        LocalDateTime expectedDeletedAt = LocalDateTime.parse(tombstone.deletedAt());
        checks.add(
            () ->
                assertAll(
                    tombstone.roomId() + "/" + tombstone.mamId(),
                    () -> assertTrue(row.deleted()),
                    () -> assertEquals(expectedDeletedAt, row.deletedAt()),
                    () -> assertEquals(row.deletedAt(), row.updatedAt()),
                    () -> assertEquals(ExpectedTimes.mamMillis(tombstone.mamId()), row.createdAt()),
                    () -> assertFalse(row.deletedAt().isBefore(row.createdAt()))));
      }
      assertAll(checks);
    }

    @Test
    @DisplayName(
        "TIME-5/C89: spot check of a tombstone retracted 0.168 s and one retracted 74 s later")
    void tombstoneSpotChecks() {
      ImportedChats.Message quick = message(GROUP_EDITS_ROOM, 458596076756919041L);
      ImportedChats.Message slow = message(GROUP_CLEARED_ROOM, 458596906494726913L);
      ImportedChats.Message historic = message(ONE_TO_ONE_HISTORIC_ROOM, HISTORIC_TOMBSTONE_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-10-07T16:35:24.831"), quick.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T16:35:25"), quick.deletedAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:29:25.995"), slow.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:30:40"), slow.deletedAt()),
          () -> assertEquals(LocalDateTime.parse("2026-06-12T11:20:17"), historic.deletedAt()),
          () -> assertEquals(LocalDateTime.parse("2026-06-12T11:20:17"), historic.updatedAt()));
    }

    @Test
    @DisplayName("TIME-5/C89: the legacy origin-id tombstone also takes deleted_at from its stamp")
    void legacyOriginIdTombstoneCarriesItsStamp() {
      ImportedChats.Message legacy = message(GROUP_CLEARED_ROOM, LEGACY_TOMBSTONE_MAM);

      assertAll(
          () -> assertTrue(legacy.deleted()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:30:10.029"), legacy.createdAt()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:30:11"), legacy.deletedAt()),
          () -> assertEquals(legacy.deletedAt(), legacy.updatedAt()));
    }

    @Test
    @DisplayName("TIME-5/C94: pin notification events use the MAM time of the notification row")
    void pinNotificationEventsUseMamTime() {
      ImportedChats.SystemEvent pinned = event(GROUP_PINS_ROOM, PINNED_EVENT_MAM);
      ImportedChats.SystemEvent unpinned = event(GROUP_PINS_ROOM, UNPINNED_EVENT_MAM);

      assertAll(
          () -> assertEquals("MESSAGE_PINNED", pinned.type()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T15:48:50.055"), pinned.createdAt()),
          () -> assertEquals("MESSAGE_UNPINNED", unpinned.type()),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T15:48:51.761"), unpinned.createdAt()));
    }
  }

  @Nested
  @DisplayName("TIME-4: plausible bounds and rows older than their room (C88)")
  class Bounds {

    @Test
    @DisplayName("TIME-4: no imported time is before 2026-01-01 or after the migration start")
    void noImportedTimeIsOutOfBounds() {
      LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

      assertAll(
          () -> assertTrue(chats.count("SELECT count(*) FROM chats.user_last_seen") > 0),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE created_at < ? OR created_at > ? OR"
                          + " updated_at < ? OR updated_at > ? OR deleted_at < ? OR deleted_at > ?",
                      YEAR_START,
                      now,
                      YEAR_START,
                      now,
                      YEAR_START,
                      now)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE created_at < ? OR created_at"
                          + " > ?",
                      YEAR_START,
                      now)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.user_last_seen WHERE last_seen_at < ? OR"
                          + " last_seen_at > ?",
                      YEAR_START,
                      now)));
    }

    @Test
    @DisplayName(
        "TIME-4: the latest imported row is the last MAM row of the dump, not the import time")
    void latestImportedRowIsTheLastMamRow() {
      LocalDateTime latest =
          chats
              .query(
                  "SELECT max(created_at) FROM (SELECT created_at FROM chats.message UNION ALL"
                      + " SELECT created_at FROM chats.system_event) t",
                  rs -> rs.getObject(1, LocalDateTime.class))
              .get(0);

      assertEquals(LocalDateTime.parse("2026-10-08T14:06:15.711"), latest);
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName(
        "TIME-4/C88: the first imported row of a room that precedes room.created_at is still"
            + " imported with its own MAM time")
    @CsvSource({
      "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6, 458596905521228033, 2026-10-07T17:29:22.192,"
          + " 2026-10-07T17:29:22.244",
      "8a10332b-0a46-4a7f-8f1a-db54f0dabf02, 458597073649314305, 2026-10-07T17:40:18.942,"
          + " 2026-10-07T17:40:19.172"
    })
    void firstRowOlderThanTheRoomIsStillImported(
        String roomId, long firstMamId, String firstCreatedAt, String roomCreatedLiteral) {
      ImportedChats.SystemEvent first = event(roomId, firstMamId);

      assertAll(
          () -> assertEquals("MEMBER_ADDED", first.type()),
          () -> assertEquals(LocalDateTime.parse(firstCreatedAt), first.createdAt()),
          () -> assertEquals(LocalDateTime.parse(roomCreatedLiteral), roomCreatedAt(roomId)),
          () -> assertTrue(first.createdAt().isBefore(roomCreatedAt(roomId))));
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName(
        "TL10: the memberAdded row of a 1:1 room that precedes room.created_at is not imported")
    @CsvSource({
      "435064f8-dd38-4dbd-aa41-6e2b689f23c3, 455149392603518465",
      "6f2684a7-f8c9-4c14-a924-6e196241acb7, 455159356856465153",
      "9dec2504-4279-45ea-a503-7800efc2563e, 455159357188054273",
      "cd093b9c-3434-4d5a-bd19-502d3b442faa, 455227107284563969",
      "9688feea-bf86-4f63-b08d-a45d72f59287, 456003398209645313",
      "fa0dfad7-7583-4b95-9bd4-a38e7431f1d9, 458595753481673985",
      "99b17723-465e-4f87-bb1c-429687075b0d, 458596130542834689"
    })
    void oneToOneMemberAddedOlderThanTheRoomIsNotImported(String roomId, long mamId) {
      assertAll(
          () -> assertTrue(chats.systemEvent(roomId, mamId).isEmpty()),
          () -> assertTrue(chats.message(roomId, mamId).isEmpty()));
    }

    @Test
    @DisplayName("TIME-4/C88: ROOM_CREATED sorts before the first imported row of a room")
    void roomCreatedSortsBeforeTheFirstImportedRow() {
      List<ImportedChats.TimelineItem> timeline = chats.timeline(GROUP_CLEARED_ROOM);

      assertAll(
          () -> assertEquals("ROOM_CREATED", timeline.get(0).kind()),
          () -> assertEquals("MEMBER_ADDED", timeline.get(1).kind()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T17:29:22.192"), timeline.get(1).createdAt()),
          () -> assertTrue(timeline.get(0).createdAt().isBefore(timeline.get(1).createdAt())));
    }

    @Test
    @DisplayName("TL10: a 1:1 room whose only row is memberAdded has just ROOM_CREATED")
    void oneToOneRoomWithOnlyMemberAddedHasJustRoomCreated() {
      List<ImportedChats.TimelineItem> timeline = chats.timeline(ONE_TO_ONE_ADDED_ONLY_ROOM);

      assertAll(
          () -> assertEquals(1, timeline.size()),
          () -> assertEquals("ROOM_CREATED", timeline.get(0).kind()),
          () ->
              assertEquals(roomCreatedAt(ONE_TO_ONE_ADDED_ONLY_ROOM), timeline.get(0).createdAt()));
    }
  }

  @Nested
  @DisplayName("TIME-V2: imported times against the per-user cleared_at floor (C79)")
  class ClearFloors {

    @ParameterizedTest(name = "{0} {1}")
    @DisplayName("TIME-1/C79: cleared_at of every user is kept exactly as in the release database")
    @CsvSource({
      "a8ea4b6d-982e-5666-95bb-f1e6ae637742, f3f01bc3-0de5-4785-acc6-7858551460fb,"
          + " 2026-10-07T17:32:32.900378",
      "1e49bba9-89c2-5a09-8eeb-efe969b5389d, f3f01bc3-0de5-4785-acc6-7858551460fb,"
          + " 2026-10-07T17:32:38.138626",
      "f65a7fdc-542e-53bf-9722-5e580cdcca42, e0df7f4a-7a19-4ab7-9af7-49be3b7874b6,"
          + " 2026-10-07T17:36:13.356775",
      "b26c4bc8-1334-53a0-a372-becd06eb598b, 1c6f38cf-d1a3-4947-a81d-ffb410acbbae,"
          + " 2026-10-07T17:39:22.242058",
      "5c89638f-9a69-5c9b-8fa6-b4b64e10351e, 8a10332b-0a46-4a7f-8f1a-db54f0dabf02,"
          + " 2026-10-07T17:40:34.220250"
    })
    void clearedAtIsNeverRewritten(String userId, String roomId, String clearedAt) {
      assertTrue(
          chats.count("SELECT count(*) FROM chats.system_event WHERE room_id = ?", roomId) > 0);
      assertEquals(LocalDateTime.parse(clearedAt), clearedAt(userId, roomId));
    }

    @Test
    @DisplayName("TIME-V2: messages around a clear are all imported and fall on the right side")
    void messagesAroundAClearFallOnTheRightSide() {
      LocalDateTime cleared =
          clearedAt("b26c4bc8-1334-53a0-a372-becd06eb598b", ONE_TO_ONE_CLEARED_ROOM);
      ImportedChats.Message before =
          message(ONE_TO_ONE_CLEARED_ROOM, CLEARED_ROOM_BEFORE_CLEAR_MESSAGE_MAM);
      ImportedChats.Message after =
          message(ONE_TO_ONE_CLEARED_ROOM, CLEARED_ROOM_AFTER_CLEAR_MESSAGE_MAM);

      assertAll(
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:39:22.242058"), cleared),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:39:05.108"), before.createdAt()),
          () -> assertFalse(before.createdAt().isAfter(cleared)),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:39:25.134"), after.createdAt()),
          () -> assertTrue(after.createdAt().isAfter(cleared)));
    }

    @Test
    @DisplayName(
        "TIME-V2: rows a few milliseconds after a clear stay strictly after it, rows before stay"
            + " before")
    void rowsJustAfterAClearStayStrictlyAfterIt() {
      LocalDateTime e0dfCleared =
          clearedAt("f65a7fdc-542e-53bf-9722-5e580cdcca42", GROUP_CLEARED_ROOM);
      LocalDateTime burstCleared =
          clearedAt("5c89638f-9a69-5c9b-8fa6-b4b64e10351e", GROUP_BURST_ROOM);
      ImportedChats.SystemEvent e0dfBefore =
          event(GROUP_CLEARED_ROOM, E0DF_BEFORE_CLEAR_REMOVED_MAM);
      ImportedChats.SystemEvent e0dfAfter = event(GROUP_CLEARED_ROOM, E0DF_AFTER_CLEAR_ADDED_MAM);
      ImportedChats.Message e0dfMessage = message(GROUP_CLEARED_ROOM, E0DF_AFTER_CLEAR_MESSAGE_MAM);
      ImportedChats.SystemEvent burstBefore =
          event(GROUP_BURST_ROOM, BURST_ROOM_BEFORE_CLEAR_REMOVED_MAM);
      ImportedChats.SystemEvent burstAfter =
          event(GROUP_BURST_ROOM, BURST_ROOM_AFTER_CLEAR_ADDED_MAM);

      assertAll(
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T17:36:11.030"), e0dfBefore.createdAt()),
          () -> assertTrue(e0dfBefore.createdAt().isBefore(e0dfCleared)),
          () -> assertEquals(LocalDateTime.parse("2026-10-07T17:36:13.369"), e0dfAfter.createdAt()),
          () -> assertTrue(e0dfAfter.createdAt().isAfter(e0dfCleared)),
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T17:36:14.915"), e0dfMessage.createdAt()),
          () -> assertTrue(e0dfMessage.createdAt().isAfter(e0dfCleared)),
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T17:40:30.035"), burstBefore.createdAt()),
          () -> assertTrue(burstBefore.createdAt().isBefore(burstCleared)),
          () ->
              assertEquals(LocalDateTime.parse("2026-10-07T17:40:34.231"), burstAfter.createdAt()),
          () -> assertTrue(burstAfter.createdAt().isAfter(burstCleared)));
    }

    @Test
    @DisplayName("TIME-1/C79: rows before a clear are imported regardless of cleared_at")
    void rowsBeforeAClearAreStillImported() {
      LocalDateTime burstCleared =
          clearedAt("5c89638f-9a69-5c9b-8fa6-b4b64e10351e", GROUP_BURST_ROOM);
      List<ImportedChats.Message> burstRoomMessages = chats.messages(GROUP_BURST_ROOM);

      assertAll(
          () -> assertEquals(3, burstRoomMessages.size()),
          () ->
              assertTrue(
                  burstRoomMessages.stream().allMatch(m -> m.createdAt().isBefore(burstCleared))),
          () -> assertNotNull(message(GROUP_BURST_ROOM, BURST_FIRST_MAM)));
    }
  }

  @Nested
  @DisplayName("TIME-7: user_last_seen from last.seconds in UTC")
  class LastSeen {

    @ParameterizedTest(name = "{0}")
    @DisplayName("TIME-7/C96: last_seen_at is the source last.seconds as a UTC wall clock")
    @CsvSource({
      "feebaebf-9b1a-5b3b-9d80-8e6d1a4045c8, 2026-02-28T11:30:43",
      "ffebb6fc-6c9b-58cf-86bb-6df35a3a30f8, 2026-05-08T09:26:19",
      "a0f0e228-9e1e-5aef-b2ac-1616214742c8, 2026-06-27T12:47:54",
      "1f9f9cc6-eac5-552d-b31b-f382633a1ad7, 2026-10-07T15:31:16",
      "965fd8d7-bb3d-5e8b-9c46-8d6414b81d70, 2026-10-07T16:38:11",
      "a793945e-d600-5362-8ecf-bb6b1d88dbc3, 2026-10-07T17:35:30",
      "69e94bcc-f52d-550d-bee1-b40dc0368d0f, 2026-10-08T14:05:33",
      "1e49bba9-89c2-5a09-8eeb-efe969b5389d, 2026-10-08T14:06:15"
    })
    void lastSeenIsTheSourceSecondsInUtc(String userId, String lastSeen) {
      assertEquals(LocalDateTime.parse(lastSeen), chats.lastSeen(userId).orElseThrow());
    }

    @Test
    @DisplayName("TIME-7: all 44 source last rows are imported, spanning Feb to Oct 2026")
    void everySourceLastRowIsImported() {
      assertAll(
          () -> assertEquals(44, chats.count("SELECT count(*) FROM chats.user_last_seen")),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-02-28T11:30:43"),
                  chats
                      .query(
                          "SELECT min(last_seen_at) FROM chats.user_last_seen",
                          rs -> rs.getObject(1, LocalDateTime.class))
                      .get(0)),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-08T14:06:15"),
                  chats
                      .query(
                          "SELECT max(last_seen_at) FROM chats.user_last_seen",
                          rs -> rs.getObject(1, LocalDateTime.class))
                      .get(0)));
    }
  }
}
