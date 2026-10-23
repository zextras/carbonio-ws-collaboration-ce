// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Message;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: room mapping and room lifecycle")
class RoomMappingIT {

  private static final String CANONICAL_UUID =
      "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";

  private static final int IMPORTABLE_ROOMS = 35;
  private static final int WSC_ROOMS = 36;
  private static final int WSC_SUBSCRIPTIONS = 110;
  private static final int WSC_ROOM_USER_SETTINGS = 8;
  private static final int WSC_MEETINGS = 33;
  private static final int ROOM_SENDER_ROWS = 125;
  private static final int IMPORTABLE_GROUP_ROOMS = 24;

  private static final String MSU_189_GROUP_ROOM = "61a47ae3-8c95-47f5-8ce8-2b8c22b46402";
  private static final String MLR_ID_189_GROUP_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final long MSU_189_TEXT_MAM = 458595984825790465L;
  private static final String MSU_189_TEXT_SENDER = "ac9fa7fc-0c1c-50ee-95ac-d027bce21970";

  private static final String MSU_221_GROUP_ROOM = "026d176c-f9e9-44e6-a488-7565d7ae4eef";
  private static final String MLR_ID_221_ONE_TO_ONE_ROOM = "1c6f38cf-d1a3-4947-a81d-ffb410acbbae";
  private static final long MSU_221_TEXT_MAM = 458596093205538049L;
  private static final String MSU_221_TEXT_SENDER = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";

  private static final String MSU_233_GROUP_ROOM = "ec84f534-92bd-4d88-923f-ab5f8e027526";
  private static final String MLR_ID_233_GROUP_ROOM = "7e7d6c0a-efce-4852-b197-8bf33d267290";
  private static final long MSU_233_TEXT_MAM = 458596928844845057L;
  private static final String MSU_233_TEXT_SENDER = "4f82bc10-f121-51ed-aed7-6738a8552323";

  private static final String MSU_225_GROUP_ROOM = "06877759-759d-48a8-bbff-854a7784179e";
  private static final String MLR_ID_225_GROUP_ROOM = "8a10332b-0a46-4a7f-8f1a-db54f0dabf02";
  private static final long MSU_225_FORWARD_MAM = 458596134729373953L;
  private static final String MSU_225_FORWARD_SENDER = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";

  private static final String MSU_232_GROUP_ROOM = "e6ff78bf-33d5-48e6-9db2-aa313e20b33e";
  private static final String MLR_ID_232_SINGLE_MEMBER_ROOM =
      "ade1af8a-10e9-4a72-be27-9735cdb6ac90";
  private static final long MSU_232_ATTACHMENT_MAM = 458596926087755777L;
  private static final String MSU_232_ATTACHMENT_SENDER = "4b86ebf8-3973-57a3-b5e8-aff96036c8c4";

  private static final String MSU_111_ONE_TO_ONE_ROOM = "435064f8-dd38-4dbd-aa41-6e2b689f23c3";
  private static final String MLR_ID_111_ONE_TO_ONE_ROOM = "cd093b9c-3434-4d5a-bd19-502d3b442faa";
  private static final long MSU_111_MEMBER_ADDED_MAM = 455149392603518465L;

  private static final String DESTROYED_ROOM_WITH_PIN = "3419545d-8834-4b0e-8d79-7b38f05ebc43";
  private static final String RACED_DESTROY_ROOM_WITH_ATTACHMENTS =
      "6171aad4-52b2-47a5-9d2b-83d556a3e6f9";
  private static final long RACED_DESTROY_FIRST_ATTACHMENT_MAM = 458596947235737601L;
  private static final String OLD_RESIDUE_ROOM = "8c973e62-10f4-486b-aad5-7fe8f638ccda";
  private static final String MIM_ONLY_ROOM_WITH_TEXT = "1d5706c9-4038-4200-8ea9-8d311752eea9";
  private static final long MIM_ONLY_TEXT_MAM = 453681069965618433L;
  private static final String MIM_ONLY_ROOM_WITH_TWO_TEXTS = "58f2fef8-2314-44b7-991f-039aeeae6349";
  private static final long MIM_ONLY_SECOND_TEXT_MAM = 455147915518328577L;
  private static final List<String> ROOMS_ABSENT_FROM_WSC =
      List.of(
          DESTROYED_ROOM_WITH_PIN,
          RACED_DESTROY_ROOM_WITH_ATTACHMENTS,
          OLD_RESIDUE_ROOM,
          MIM_ONLY_ROOM_WITH_TEXT,
          MIM_ONLY_ROOM_WITH_TWO_TEXTS);

  private static final String ZOMBIE_ROOM = "80e4de67-3d6c-4c15-8876-e6255ac0c1d5";
  private static final long ZOMBIE_TEXT_MAM = 458597003729571585L;
  private static final long ZOMBIE_AFFILIATION_MAM = 458597004226356993L;

  private static final String SINGLE_MEMBER_ROOM = "ade1af8a-10e9-4a72-be27-9735cdb6ac90";
  private static final String SINGLE_MEMBER_OWNER = "5853d06f-cfb3-5977-8888-2a67736a8c2a";
  private static final String SINGLE_MEMBER_REMOVED_USER_A = "3c0bc3ee-b41e-5b44-acfc-1b6869a260c7";
  private static final String SINGLE_MEMBER_REMOVED_USER_B = "4d844e6d-864d-503c-9047-32bd38cee22c";
  private static final long SINGLE_MEMBER_FIRST_TEXT_MAM = 458615898555257345L;
  private static final long SINGLE_MEMBER_LAST_TEXT_MAM = 458615899116432385L;
  private static final long SINGLE_MEMBER_AFFILIATION_MAM = 458615898314358017L;

  private static final String OWNER_LEFT_ROOM = "7be2cb4f-4ce4-4530-b473-2df640007959";
  private static final String FIRST_OWNER_WHO_LEFT = "3b6045dc-cb8c-5f87-8158-3c1a7b277a94";
  private static final String MEMBER_REMOVED_BY_FIRST_OWNER =
      "231e8c22-87e0-5817-8960-c0e71b070652";
  private static final String SECOND_OWNER = "4b86ebf8-3973-57a3-b5e8-aff96036c8c4";
  private static final String REMAINING_MEMBER = "4f82bc10-f121-51ed-aed7-6738a8552323";
  private static final long FIRST_OWNER_TEXT_MAM = 458615894558619137L;
  private static final long REMOVED_MEMBER_TEXT_MAM = 458615894569365505L;
  private static final long SECOND_OWNER_FIRST_TEXT_MAM = 458615895443214081L;
  private static final long SECOND_OWNER_LAST_TEXT_MAM = 458615898042111745L;
  private static final long OWNER_LEFT_AFFILIATION_MAM = 458615893654952961L;

  private static final String EX_MEMBER_TEXTS_ROOM = "5f6b4438-fdb8-48c1-a866-ff3505b8649a";
  private static final String EX_MEMBER = "42ad6d39-3dfc-55de-a8db-f01680df2db3";
  private static final long EX_MEMBER_FIRST_TEXT_MAM = 458596074681969153L;
  private static final long EX_MEMBER_SECOND_TEXT_MAM = 458596075163083777L;

  private static final String EX_MEMBER_TOMBSTONE_ROOM = "af0b223e-0c55-4dff-82cc-a69149c758b0";
  private static final String EX_MEMBER_OF_TOMBSTONE_ROOM = "ba2c72b5-0128-5656-806c-8a163b58450e";
  private static final long EX_MEMBER_TOMBSTONE_ROOM_TEXT_MAM = 458596112523522049L;

  private static final String TEMPORARY_ROOM = "332d2b24-6fbc-45de-a6d3-9e0b16b69aea";
  private static final String TEMPORARY_OWNER = "69e94bcc-f52d-550d-bee1-b40dc0368d0f";
  private static final String TEMPORARY_OTHER_MEMBER = "d7f47520-8d8f-5609-bb75-f7fc23f33ded";
  private static final long TEMPORARY_PRE_CLEAR_REACTION_MAM = 458615892168619521L;
  private static final long TEMPORARY_PRE_CLEAR_FIRST_TEXT_MAM = 458615892177532929L;
  private static final long TEMPORARY_PRE_CLEAR_SECOND_TEXT_MAM = 458615892187423489L;
  private static final long TEMPORARY_CLEAR_MAM = 458615892476687361L;
  private static final long TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM = 458615893127340289L;
  private static final long TEMPORARY_POST_CLEAR_LAST_TEXT_MAM = 458615893454507265L;
  private static final long TEMPORARY_POST_CLEAR_AFFILIATION_MAM = 458615893171657985L;
  private static final long TEMPORARY_SECOND_CLEAR_MAM = 458615893300000001L;

  private static final String CLEARED_ONE_TO_ONE_ROOM = "1c6f38cf-d1a3-4947-a81d-ffb410acbbae";
  private static final LocalDateTime CLEARED_ONE_TO_ONE_FLOOR =
      LocalDateTime.parse("2026-10-07T17:39:22.242058");
  private static final long ONE_TO_ONE_TEXT_BEFORE_FLOOR_MAM = 458597054021541889L;
  private static final long ONE_TO_ONE_SECOND_TEXT_BEFORE_FLOOR_MAM = 458597054747772929L;
  private static final long ONE_TO_ONE_TEXT_AFTER_FLOOR_MAM = 458597059874409473L;
  private static final long ONE_TO_ONE_LAST_TEXT_AFTER_FLOOR_MAM = 458597061894126849L;

  private static final String CLEARED_GROUP_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final LocalDateTime CLEARED_GROUP_FLOOR =
      LocalDateTime.parse("2026-10-07T17:36:13.356775");
  private static final long GROUP_TEXT_BEFORE_FLOOR_MAM = 458597004902338305L;
  private static final long GROUP_TEXT_AFTER_FLOOR_MAM = 458597011178408193L;

  private static final String CLEARED_BIG_GROUP_ROOM = "8a10332b-0a46-4a7f-8f1a-db54f0dabf02";
  private static final LocalDateTime CLEARED_BIG_GROUP_FLOOR =
      LocalDateTime.parse("2026-10-07T17:40:34.220250");
  private static final long BIG_GROUP_TEXT_BEFORE_FLOOR_MAM = 458597075442481153L;
  private static final long BIG_GROUP_MEMBER_ADDED_AFTER_FLOOR_MAM = 458597077598418177L;

  private static final String CLEARED_AFTER_LAST_ROW_ROOM = "f3f01bc3-0de5-4785-acc6-7858551460fb";
  private static final LocalDateTime EARLIEST_FLOOR_OF_CLEARED_AFTER_LAST_ROW_ROOM =
      LocalDateTime.parse("2026-10-07T17:32:32.900378");
  private static final long CLEARED_AFTER_LAST_ROW_MEMBER_REMOVED_MAM = 458596949121500417L;

  private static final List<String> ROOM_USER_SETTINGS_BEFORE_IMPORT =
      List.of(
          "b26c4bc8-1334-53a0-a372-becd06eb598b|1c6f38cf-d1a3-4947-a81d-ffb410acbbae|0001-01-03"
              + " 00:00:00|2026-10-07 17:39:22.242058|2026-10-07 17:39:19.425|2026-10-07"
              + " 17:39:22.242",
          "1f4645c9-5446-519d-81e0-2a9ee24ef783|8a10332b-0a46-4a7f-8f1a-db54f0dabf02||2026-10-07"
              + " 17:40:34.22025|2026-10-07 17:40:34.393|2026-10-07 17:40:34.393",
          "5c89638f-9a69-5c9b-8fa6-b4b64e10351e|8a10332b-0a46-4a7f-8f1a-db54f0dabf02||2026-10-07"
              + " 17:40:34.22025|2026-10-07 17:40:34.315|2026-10-07 17:40:34.315",
          "b26c4bc8-1334-53a0-a372-becd06eb598b|8a10332b-0a46-4a7f-8f1a-db54f0dabf02||2026-10-07"
              + " 17:40:34.22025|2026-10-07 17:40:34.248|2026-10-07 17:40:34.248",
          "97c353af-4374-5ff3-a3eb-3056c0b4dba9|e0df7f4a-7a19-4ab7-9af7-49be3b7874b6|||2026-10-07"
              + " 17:36:19.2|2026-10-07 17:36:19.255",
          "f65a7fdc-542e-53bf-9722-5e580cdcca42|e0df7f4a-7a19-4ab7-9af7-49be3b7874b6||2026-10-07"
              + " 17:36:13.356775|2026-10-07 17:36:13.384|2026-10-07 17:36:13.384",
          "1e49bba9-89c2-5a09-8eeb-efe969b5389d|f3f01bc3-0de5-4785-acc6-7858551460fb||2026-10-07"
              + " 17:32:38.138626|2026-10-07 17:32:38.138|2026-10-07 17:32:38.138",
          "a8ea4b6d-982e-5666-95bb-f1e6ae637742|f3f01bc3-0de5-4785-acc6-7858551460fb||2026-10-07"
              + " 17:32:32.900378|2026-10-07 17:32:18.185|2026-10-07 17:32:32.9");

  private static final String FIRST_ROW =
      "(SELECT min(c) FROM (SELECT created_at AS c FROM chats.message WHERE room_id = r.id UNION"
          + " ALL SELECT created_at FROM chats.system_event WHERE room_id = r.id AND type <>"
          + " 'ROOM_CREATED') x)";

  private static ImportRun run;
  private static ImportedChats chats;
  private static Map<String, List<Long>> archive;
  private static ImportRun withoutMucLightRoomsRun;
  private static ImportRun twoClearsRun;
  private static ImportRun temporaryAsGroupRun;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  @Nested
  @DisplayName("ROOM-1: rows are resolved through mam_server_user, never through muc_light_rooms")
  class ArchiveKeyJoin {

    @Test
    @DisplayName("ROOM-1: a text of a room whose msu id is another room's muc_light_rooms id (189)")
    void textOfRoomCollidingWithAnotherGroupLandsInItsOwnRoom() {
      assertMessageOnlyInOwnRoom(
          MSU_189_GROUP_ROOM, MLR_ID_189_GROUP_ROOM, MSU_189_TEXT_MAM, MSU_189_TEXT_SENDER);
    }

    @Test
    @DisplayName(
        "ROOM-1: a text of a group whose msu id is a one-to-one's muc_light_rooms id (221)")
    void textOfGroupCollidingWithOneToOneLandsInItsOwnRoom() {
      assertMessageOnlyInOwnRoom(
          MSU_221_GROUP_ROOM, MLR_ID_221_ONE_TO_ONE_ROOM, MSU_221_TEXT_MAM, MSU_221_TEXT_SENDER);
    }

    @Test
    @DisplayName(
        "ROOM-1: a text of a room whose msu id is another group's muc_light_rooms id (233)")
    void textOfRoomCollidingWithAnotherGroupWithPinLandsInItsOwnRoom() {
      assertMessageOnlyInOwnRoom(
          MSU_233_GROUP_ROOM, MLR_ID_233_GROUP_ROOM, MSU_233_TEXT_MAM, MSU_233_TEXT_SENDER);
    }

    @Test
    @DisplayName("ROOM-1: a forward of a room whose msu id is a bigger group's mlr id (225)")
    void forwardOfRoomCollidingWithBigGroupLandsInItsOwnRoom() {
      assertMessageOnlyInOwnRoom(
          MSU_225_GROUP_ROOM, MLR_ID_225_GROUP_ROOM, MSU_225_FORWARD_MAM, MSU_225_FORWARD_SENDER);
    }

    @Test
    @DisplayName("ROOM-1: an attachment of a room whose msu id is the single-member group's mlr id")
    void attachmentOfRoomCollidingWithSingleMemberGroupLandsInItsOwnRoom() {
      assertMessageOnlyInOwnRoom(
          MSU_232_GROUP_ROOM,
          MLR_ID_232_SINGLE_MEMBER_ROOM,
          MSU_232_ATTACHMENT_MAM,
          MSU_232_ATTACHMENT_SENDER);
    }

    @Test
    @DisplayName(
        "ROOM-1: the memberAdded row of a one-to-one whose msu id is another room's mlr id is"
            + " imported nowhere")
    void memberAddedOfOneToOneCollidingWithAnotherOneToOneIsImportedNowhere() {
      assertTrue(chats.systemEvent(MSU_111_ONE_TO_ONE_ROOM, MSU_111_MEMBER_ADDED_MAM).isEmpty());
      assertTrue(chats.systemEvent(MLR_ID_111_ONE_TO_ONE_ROOM, MSU_111_MEMBER_ADDED_MAM).isEmpty());
      assertTrue(
          chats.systemEvents(MLR_ID_111_ONE_TO_ONE_ROOM).stream()
              .noneMatch(
                  event ->
                      event
                          .id()
                          .equals(
                              ExpectedIds.systemEvent(
                                  MSU_111_ONE_TO_ONE_ROOM, MSU_111_MEMBER_ADDED_MAM))));
      assertEquals(
          List.of("ROOM_CREATED"),
          chats.systemEvents(MSU_111_ONE_TO_ONE_ROOM).stream()
              .map(ImportedChats.SystemEvent::type)
              .toList());
    }

    @Test
    @DisplayName("ROOM-1: every imported message and system event belongs to the room of its key")
    void everyImportedRowIsInTheRoomOfItsArchiveKey() {
      Map<String, Set<String>> messageIdsByRoom = expectedIdsByRoom(ExpectedIds.MESSAGE_SLOT);
      Map<String, Set<String>> eventIdsByRoom = expectedIdsByRoom(ExpectedIds.SYSTEM_EVENT_SLOT);
      List<String[]> messages =
          chats.query(
              "SELECT id, room_id FROM chats.message",
              rs -> new String[] {rs.getString(1), rs.getString(2)});
      List<String[]> events =
          chats.query(
              "SELECT id, room_id FROM chats.system_event WHERE type <> 'ROOM_CREATED'",
              rs -> new String[] {rs.getString(1), rs.getString(2)});

      assertFalse(messages.isEmpty());
      assertFalse(events.isEmpty());
      List<String> misplaced = new ArrayList<>();
      messages.stream()
          .filter(m -> !messageIdsByRoom.getOrDefault(m[1], Set.of()).contains(m[0]))
          .forEach(m -> misplaced.add("message " + m[0] + " in " + m[1]));
      events.stream()
          .filter(e -> !eventIdsByRoom.getOrDefault(e[1], Set.of()).contains(e[0]))
          .forEach(e -> misplaced.add("event " + e[0] + " in " + e[1]));
      assertTrue(misplaced.isEmpty(), () -> misplaced.stream().limit(10).toList().toString());
    }

    @Test
    @DisplayName("ROOM-1: every importable room gets its own rows and the other rooms none")
    void everyImportableRoomHasItsOwnTimeline() {
      Set<String> importable = importableRooms();

      assertEquals(IMPORTABLE_ROOMS, importable.size());
      assertAll(
          importable.stream()
              .map(
                  room ->
                      () ->
                          assertFalse(
                              chats.timeline(room).isEmpty(), "no imported row in room " + room)));
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message WHERE room_id NOT IN (SELECT id FROM chats.room r"
                  + " WHERE EXISTS (SELECT 1 FROM chats.subscription s WHERE s.room_id = r.id))"));
    }

    @Test
    @DisplayName("ROOM-1: the import does not depend on muc_light_rooms (all of its rows removed)")
    void importResultDoesNotDependOnMucLightRooms() {
      ImportRun without = withoutMucLightRooms();

      assertTrue(without.succeeded(), without::failureText);
      Set<String> expected = messageIds(chats);
      assertFalse(expected.isEmpty());
      assertEquals(expected, messageIds(without.chats()));
    }
  }

  @Nested
  @DisplayName("ROOM-2: archives without a WSC room are never imported")
  class ArchivesWithoutWscRoom {

    @Test
    @DisplayName("ROOM-2: destroyed-room residue and raced-destroy archives get no rows at all")
    void destroyedRoomArchivesAreNotImported() {
      assertImportHappened();
      assertAll(
          assertNoRowsFor(DESTROYED_ROOM_WITH_PIN),
          assertNoRowsFor(RACED_DESTROY_ROOM_WITH_ATTACHMENTS),
          assertNoRowsFor(OLD_RESIDUE_ROOM));
    }

    @Test
    @DisplayName("ROOM-2: the attachment rows of the raced-destroy room are not imported anywhere")
    void racedDestroyAttachmentRowsAreNotImported() {
      assertImportHappened();
      assertTrue(
          archive()
              .get(RACED_DESTROY_ROOM_WITH_ATTACHMENTS)
              .contains(RACED_DESTROY_FIRST_ATTACHMENT_MAM));
      assertTrue(
          chats
              .messageById(
                  ExpectedIds.message(
                      RACED_DESTROY_ROOM_WITH_ATTACHMENTS, RACED_DESTROY_FIRST_ATTACHMENT_MAM))
              .isEmpty());
    }

    @Test
    @DisplayName("ROOM-2: live MongooseIM rooms that have no WSC room get no rows")
    void mongooseOnlyRoomsAreNotImported() {
      assertImportHappened();
      assertAll(
          assertNoRowsFor(MIM_ONLY_ROOM_WITH_TEXT), assertNoRowsFor(MIM_ONLY_ROOM_WITH_TWO_TEXTS));
      assertTrue(
          chats
              .messageById(ExpectedIds.message(MIM_ONLY_ROOM_WITH_TEXT, MIM_ONLY_TEXT_MAM))
              .isEmpty());
      assertTrue(
          chats
              .messageById(
                  ExpectedIds.message(MIM_ONLY_ROOM_WITH_TWO_TEXTS, MIM_ONLY_SECOND_TEXT_MAM))
              .isEmpty());
    }

    @Test
    @DisplayName("ROOM-2: no WSC room is created for an archive without one")
    void noRoomIsCreatedForOrphanArchives() {
      assertImportHappened();
      assertEquals(WSC_ROOMS, chats.count("SELECT count(*) FROM chats.room"));
      assertAll(
          ROOMS_ABSENT_FROM_WSC.stream()
              .map(
                  room ->
                      () ->
                          assertEquals(
                              0,
                              chats.count("SELECT count(*) FROM chats.room WHERE id = ?", room),
                              room)));
    }

    @Test
    @DisplayName("ROOM-2: pins and read markers that point to rooms without a WSC room are skipped")
    void pinsAndMarkersOfRoomsWithoutWscRoomAreNotImported() {
      assertImportHappened();
      assertAll(
          ROOMS_ABSENT_FROM_WSC.stream()
              .flatMap(
                  room ->
                      java.util.stream.Stream.<org.junit.jupiter.api.function.Executable>of(
                          () ->
                              assertEquals(
                                  0,
                                  chats.count(
                                      "SELECT count(*) FROM chats.message_pin WHERE room_id = ?",
                                      room),
                                  room),
                          () ->
                              assertEquals(
                                  0,
                                  chats.count(
                                      "SELECT count(*) FROM chats.read_marker WHERE room_id = ?",
                                      room),
                                  room))));
    }
  }

  @Nested
  @DisplayName("ROOM-3: rooms that exist but cannot be used")
  class UnusableRooms {

    @Test
    @DisplayName("ROOM-3: the zombie room with no subscription gets no rows")
    void zombieRoomGetsNothing() {
      assertImportHappened();
      assertTrue(archive().get(ZOMBIE_ROOM).contains(ZOMBIE_TEXT_MAM));
      assertAll(
          assertNoRowsFor(ZOMBIE_ROOM),
          () ->
              assertTrue(
                  chats.messageById(ExpectedIds.message(ZOMBIE_ROOM, ZOMBIE_TEXT_MAM)).isEmpty()),
          () -> assertTrue(chats.systemEvent(ZOMBIE_ROOM, ZOMBIE_AFFILIATION_MAM).isEmpty()),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message_pin WHERE room_id = ?", ZOMBIE_ROOM)));
    }

    @Test
    @DisplayName(
        "ROOM-3: the zombie room, its meeting and its missing members are left as they are")
    void zombieRoomAndItsMeetingAreUntouched() {
      assertImportHappened();
      assertEquals(1, chats.count("SELECT count(*) FROM chats.room WHERE id = ?", ZOMBIE_ROOM));
      assertEquals(
          0, chats.count("SELECT count(*) FROM chats.subscription WHERE room_id = ?", ZOMBIE_ROOM));
      assertEquals(
          1, chats.count("SELECT count(*) FROM chats.meeting WHERE room_id = ?", ZOMBIE_ROOM));
      assertEquals(WSC_MEETINGS, chats.count("SELECT count(*) FROM chats.meeting"));
    }

    @Test
    @DisplayName("ROOM-3: a group with a single remaining member is imported")
    void groupWithSingleRemainingMemberIsImported() {
      assertEquals(
          1,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ?", SINGLE_MEMBER_ROOM));
      assertEquals(
          SINGLE_MEMBER_OWNER,
          chats
              .query(
                  "SELECT user_id FROM chats.subscription WHERE room_id = ?",
                  rs -> rs.getString(1),
                  SINGLE_MEMBER_ROOM)
              .get(0));

      Message first = chats.message(SINGLE_MEMBER_ROOM, SINGLE_MEMBER_FIRST_TEXT_MAM).orElseThrow();
      Message last = chats.message(SINGLE_MEMBER_ROOM, SINGLE_MEMBER_LAST_TEXT_MAM).orElseThrow();
      assertEquals(SINGLE_MEMBER_ROOM, first.roomId());
      assertEquals(SINGLE_MEMBER_OWNER, first.senderId());
      assertEquals(SINGLE_MEMBER_ROOM, last.roomId());
      assertEquals(SINGLE_MEMBER_OWNER, last.senderId());
      assertEquals(2, chats.messages(SINGLE_MEMBER_ROOM).size());
    }

    @Test
    @DisplayName("ROOM-3: the members removed from the single-member group are not subscribers")
    void removedMembersOfSingleMemberGroupStayRemoved() {
      assertTrue(chats.message(SINGLE_MEMBER_ROOM, SINGLE_MEMBER_FIRST_TEXT_MAM).isPresent());
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id IN (?, ?)",
              SINGLE_MEMBER_ROOM,
              SINGLE_MEMBER_REMOVED_USER_A,
              SINGLE_MEMBER_REMOVED_USER_B));
      assertTrue(chats.message(SINGLE_MEMBER_ROOM, SINGLE_MEMBER_AFFILIATION_MAM).isEmpty());
    }
  }

  @Nested
  @DisplayName("ROOM-4: senders")
  class Senders {

    @Test
    @DisplayName("ROOM-4: rows whose sender is the room itself never become messages")
    void roomSenderRowsAreNeverMessages() {
      Set<String> importable = importableRooms();
      List<String[]> roomSenderRows = roomSenderRows();

      assertEquals(ROOM_SENDER_ROWS, roomSenderRows.size());
      assertFalse(chats.messages(OWNER_LEFT_ROOM).isEmpty());
      List<String> imported = new ArrayList<>();
      for (String[] row : roomSenderRows) {
        if (importable.contains(row[0])) {
          long mam = Long.parseLong(row[1]);
          if (chats.message(row[0], mam).isPresent()) {
            imported.add("message " + row[0] + "/" + mam);
          }
          if (chats.systemEvent(row[0], mam).isPresent()) {
            imported.add("event " + row[0] + "/" + mam);
          }
        }
      }
      assertTrue(imported.isEmpty(), () -> imported.stream().limit(10).toList().toString());
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message m WHERE m.sender_id = m.room_id OR m.sender_id IN"
                  + " (SELECT id FROM chats.room)"));
    }

    @Test
    @DisplayName("ROOM-4: the affiliation rows of three real rooms are not imported")
    void affiliationRowsOfRealRoomsAreNotImported() {
      assertTrue(chats.messages(OWNER_LEFT_ROOM).size() > 0);
      assertAll(
          () -> assertTrue(chats.message(OWNER_LEFT_ROOM, OWNER_LEFT_AFFILIATION_MAM).isEmpty()),
          () ->
              assertTrue(chats.systemEvent(OWNER_LEFT_ROOM, OWNER_LEFT_AFFILIATION_MAM).isEmpty()),
          () ->
              assertTrue(
                  chats.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_AFFILIATION_MAM).isEmpty()),
          () ->
              assertTrue(
                  chats
                      .systemEvent(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_AFFILIATION_MAM)
                      .isEmpty()));
    }

    @Test
    @DisplayName("ROOM-4: every sender of an imported message is a canonical user uuid")
    void everyImportedSenderIsAUserUuid() {
      assertFalse(chats.messages(OWNER_LEFT_ROOM).isEmpty());
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message WHERE sender_id !~ '" + CANONICAL_UUID + "'"));
    }

    @Test
    @DisplayName("ROOM-4: texts of a user who left the room are kept with their sender")
    void textsOfExMemberAreKeptWithTheirSender() {
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ?",
              EX_MEMBER_TEXTS_ROOM,
              EX_MEMBER));
      Message first = chats.message(EX_MEMBER_TEXTS_ROOM, EX_MEMBER_FIRST_TEXT_MAM).orElseThrow();
      Message second = chats.message(EX_MEMBER_TEXTS_ROOM, EX_MEMBER_SECOND_TEXT_MAM).orElseThrow();
      assertEquals(EX_MEMBER, first.senderId());
      assertEquals(EX_MEMBER_TEXTS_ROOM, first.roomId());
      assertEquals(EX_MEMBER, second.senderId());
      assertEquals(EX_MEMBER_TEXTS_ROOM, second.roomId());
    }

    @Test
    @DisplayName("ROOM-4: a text of a user who left a room with pins and a tombstone is kept")
    void textOfExMemberInRoomWithPinsAndTombstoneIsKept() {
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ?",
              EX_MEMBER_TOMBSTONE_ROOM,
              EX_MEMBER_OF_TOMBSTONE_ROOM));
      Message text =
          chats.message(EX_MEMBER_TOMBSTONE_ROOM, EX_MEMBER_TOMBSTONE_ROOM_TEXT_MAM).orElseThrow();
      assertEquals(EX_MEMBER_OF_TOMBSTONE_ROOM, text.senderId());
      assertEquals(EX_MEMBER_TOMBSTONE_ROOM, text.roomId());
    }

    @Test
    @DisplayName("ROOM-4: texts of the first owner who left and of a removed member are kept")
    void textsOfFirstOwnerAndRemovedMemberAreKept() {
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id IN (?, ?)",
              OWNER_LEFT_ROOM,
              FIRST_OWNER_WHO_LEFT,
              MEMBER_REMOVED_BY_FIRST_OWNER));
      Message owner = chats.message(OWNER_LEFT_ROOM, FIRST_OWNER_TEXT_MAM).orElseThrow();
      Message removed = chats.message(OWNER_LEFT_ROOM, REMOVED_MEMBER_TEXT_MAM).orElseThrow();
      assertEquals(FIRST_OWNER_WHO_LEFT, owner.senderId());
      assertEquals(OWNER_LEFT_ROOM, owner.roomId());
      assertEquals(MEMBER_REMOVED_BY_FIRST_OWNER, removed.senderId());
      assertEquals(OWNER_LEFT_ROOM, removed.roomId());
    }

    @Test
    @DisplayName("ROOM-4: texts of the remaining second owner are imported in the same room")
    void textsOfRemainingSecondOwnerAreImported() {
      assertEquals(
          2,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ?", OWNER_LEFT_ROOM));
      assertEquals(
          1,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ? AND owner",
              OWNER_LEFT_ROOM,
              SECOND_OWNER));
      assertEquals(
          1,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ? AND NOT"
                  + " owner",
              OWNER_LEFT_ROOM,
              REMAINING_MEMBER));
      Message first = chats.message(OWNER_LEFT_ROOM, SECOND_OWNER_FIRST_TEXT_MAM).orElseThrow();
      Message last = chats.message(OWNER_LEFT_ROOM, SECOND_OWNER_LAST_TEXT_MAM).orElseThrow();
      assertEquals(SECOND_OWNER, first.senderId());
      assertEquals(SECOND_OWNER, last.senderId());
      assertEquals(OWNER_LEFT_ROOM, first.roomId());
      assertEquals(OWNER_LEFT_ROOM, last.roomId());
    }
  }

  @Nested
  @DisplayName("ROOM-5 and ROOM-7: rooms, subscriptions and settings are not written")
  class MembershipUntouched {

    @Test
    @DisplayName("ROOM-5: rooms, subscriptions and meetings keep exactly their pre-import counts")
    void roomsSubscriptionsAndMeetingsAreUntouched() {
      assertImportHappened();
      assertAll(
          () -> assertEquals(WSC_ROOMS, chats.count("SELECT count(*) FROM chats.room")),
          () ->
              assertEquals(
                  WSC_SUBSCRIPTIONS, chats.count("SELECT count(*) FROM chats.subscription")),
          () -> assertEquals(WSC_MEETINGS, chats.count("SELECT count(*) FROM chats.meeting")),
          () ->
              assertEquals(
                  WSC_ROOM_USER_SETTINGS,
                  chats.count("SELECT count(*) FROM chats.room_user_settings")));
    }

    @Test
    @DisplayName("ROOM-5: the subscriptions of the TEMPORARY room are exactly its two members")
    void subscriptionsOfTemporaryRoomAreUntouched() {
      assertImportHappened();
      List<String> members =
          chats.query(
              "SELECT user_id FROM chats.subscription WHERE room_id = ? ORDER BY user_id",
              rs -> rs.getString(1),
              TEMPORARY_ROOM);
      assertEquals(List.of(TEMPORARY_OWNER, TEMPORARY_OTHER_MEMBER), members);
    }

    @Test
    @DisplayName("ROOM-7: after the import the room tables accept native writes again")
    void roomTablesAcceptNativeWritesAfterImport() throws SQLException {
      assertImportHappened();
      try (Connection connection = MongooseImportDatabase.get().connect(run.targetDatabase());
          Statement statement = connection.createStatement()) {
        connection.setAutoCommit(false);
        assertEquals(
            1,
            statement.executeUpdate(
                "INSERT INTO chats.subscription (user_id, room_id, owner, created_at, updated_at)"
                    + " VALUES ('00000000-0000-4000-8000-000000000001', '"
                    + SINGLE_MEMBER_ROOM
                    + "', false, now(), now())"));
        connection.rollback();
      }
    }
  }

  @Nested
  @DisplayName("ROOM-6: clear-history semantics")
  class ClearHistory {

    @Test
    @DisplayName("ROOM-6: in the TEMPORARY room only the texts after the clear are imported")
    void temporaryRoomKeepsOnlyRowsAfterTheClear() {
      Message first =
          chats.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM).orElseThrow();
      Message last =
          chats.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_LAST_TEXT_MAM).orElseThrow();

      assertEquals(TEMPORARY_ROOM, first.roomId());
      assertEquals(TEMPORARY_OWNER, first.senderId());
      assertEquals(TEMPORARY_ROOM, last.roomId());
      assertEquals(TEMPORARY_OTHER_MEMBER, last.senderId());
      assertEquals(2, chats.messages(TEMPORARY_ROOM).size());
    }

    @Test
    @DisplayName("ROOM-6: the texts before the clear of the TEMPORARY room are skipped")
    void temporaryRoomSkipsTextsBeforeTheClear() {
      assertTrue(chats.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM).isPresent());
      assertAll(
          () ->
              assertTrue(
                  chats.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_FIRST_TEXT_MAM).isEmpty()),
          () ->
              assertTrue(
                  chats.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_SECOND_TEXT_MAM).isEmpty()),
          () ->
              assertTrue(
                  chats.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_REACTION_MAM).isEmpty()));
    }

    @Test
    @DisplayName("ROOM-6 / C3: the roomHistoryCleared row itself is a ROOM_HISTORY_CLEARED event")
    void clearRowIsAnImportedEvent() {
      assertTrue(chats.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM).isPresent());
      assertTrue(chats.message(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).isEmpty());
      assertEquals(
          "ROOM_HISTORY_CLEARED",
          chats.systemEvent(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).orElseThrow().type());
    }

    @Test
    @DisplayName("C3: the clear event sorts before the first text after the clear")
    void clearEventPrecedesThePostClearTexts() {
      assertTrue(
          chats
              .systemEvent(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM)
              .orElseThrow()
              .createdAt()
              .isBefore(
                  chats
                      .message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM)
                      .orElseThrow()
                      .createdAt()));
    }

    @Test
    @DisplayName("ROOM-6: the last of two clears of a TEMPORARY room is the floor")
    void lastOfTwoClearsIsTheFloor() {
      ImportRun two = twoClears();

      assertTrue(two.succeeded(), two::failureText);
      ImportedChats imported = two.chats();
      Message last =
          imported.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_LAST_TEXT_MAM).orElseThrow();
      assertEquals(TEMPORARY_OTHER_MEMBER, last.senderId());
      assertEquals(1, imported.messages(TEMPORARY_ROOM).size());
      assertAll(
          () ->
              assertTrue(
                  imported.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM).isEmpty()),
          () ->
              assertTrue(
                  imported.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_FIRST_TEXT_MAM).isEmpty()),
          () -> assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).isEmpty()),
          () -> assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_SECOND_CLEAR_MAM).isEmpty()),
          () -> assertTrue(imported.systemEvent(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).isEmpty()),
          () ->
              assertTrue(
                  imported.systemEvent(TEMPORARY_ROOM, TEMPORARY_SECOND_CLEAR_MAM).isPresent()));
    }

    @Test
    @DisplayName("ROOM-6: the same rows in a room that is not TEMPORARY apply no floor")
    void roomThatIsNotTemporaryAppliesNoFloor() {
      ImportRun asGroup = temporaryAsGroup();

      assertTrue(asGroup.succeeded(), asGroup::failureText);
      ImportedChats imported = asGroup.chats();
      Message pre =
          imported.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_FIRST_TEXT_MAM).orElseThrow();
      assertEquals(TEMPORARY_OTHER_MEMBER, pre.senderId());
      assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_PRE_CLEAR_SECOND_TEXT_MAM).isPresent());
      assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_FIRST_TEXT_MAM).isPresent());
      assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_POST_CLEAR_LAST_TEXT_MAM).isPresent());
      assertTrue(imported.message(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).isEmpty());
      assertTrue(imported.systemEvent(TEMPORARY_ROOM, TEMPORARY_CLEAR_MAM).isPresent());
    }

    @Test
    @DisplayName("ROOM-6: the per-user room settings, cleared_at included, are not changed")
    void roomUserSettingsAreUntouched() {
      assertTrue(
          chats.message(CLEARED_ONE_TO_ONE_ROOM, ONE_TO_ONE_TEXT_AFTER_FLOOR_MAM).isPresent());
      List<String> settings =
          chats.query(
              "SELECT user_id || '|' || room_id || '|' || coalesce(muted_until::text, '') || '|' ||"
                  + " coalesce(cleared_at::text, '') || '|' || created_at::text || '|' ||"
                  + " updated_at::text FROM chats.room_user_settings ORDER BY room_id, user_id",
              rs -> rs.getString(1));
      assertEquals(ROOM_USER_SETTINGS_BEFORE_IMPORT, settings);
    }

    @Test
    @DisplayName("ROOM-6: the clear of a one-to-one splits its imported texts around the floor")
    void oneToOneTextsAreOnBothSidesOfTheClearedAt() {
      assertAll(
          () ->
              assertTrue(
                  created(CLEARED_ONE_TO_ONE_ROOM, ONE_TO_ONE_TEXT_BEFORE_FLOOR_MAM)
                      .isBefore(CLEARED_ONE_TO_ONE_FLOOR)),
          () ->
              assertTrue(
                  created(CLEARED_ONE_TO_ONE_ROOM, ONE_TO_ONE_SECOND_TEXT_BEFORE_FLOOR_MAM)
                      .isBefore(CLEARED_ONE_TO_ONE_FLOOR)),
          () ->
              assertTrue(
                  created(CLEARED_ONE_TO_ONE_ROOM, ONE_TO_ONE_TEXT_AFTER_FLOOR_MAM)
                      .isAfter(CLEARED_ONE_TO_ONE_FLOOR)),
          () ->
              assertTrue(
                  created(CLEARED_ONE_TO_ONE_ROOM, ONE_TO_ONE_LAST_TEXT_AFTER_FLOOR_MAM)
                      .isAfter(CLEARED_ONE_TO_ONE_FLOOR)));
    }

    @Test
    @DisplayName("ROOM-6: the clear of a group splits its imported texts around the floor")
    void groupTextsAreOnBothSidesOfTheClearedAt() {
      assertAll(
          () ->
              assertTrue(
                  created(CLEARED_GROUP_ROOM, GROUP_TEXT_BEFORE_FLOOR_MAM)
                      .isBefore(CLEARED_GROUP_FLOOR)),
          () ->
              assertTrue(
                  created(CLEARED_GROUP_ROOM, GROUP_TEXT_AFTER_FLOOR_MAM)
                      .isAfter(CLEARED_GROUP_FLOOR)));
    }

    @Test
    @DisplayName("ROOM-6: a clear shared by three users splits the rows of the big group")
    void bigGroupRowsAreOnBothSidesOfTheSharedClearedAt() {
      assertAll(
          () ->
              assertTrue(
                  created(CLEARED_BIG_GROUP_ROOM, BIG_GROUP_TEXT_BEFORE_FLOOR_MAM)
                      .isBefore(CLEARED_BIG_GROUP_FLOOR)),
          () ->
              assertTrue(
                  chats
                      .systemEvent(CLEARED_BIG_GROUP_ROOM, BIG_GROUP_MEMBER_ADDED_AFTER_FLOOR_MAM)
                      .orElseThrow()
                      .createdAt()
                      .isAfter(CLEARED_BIG_GROUP_FLOOR)));
    }

    @Test
    @DisplayName("ROOM-6: a clear after the last archived row leaves every row imported below it")
    void clearAfterTheLastRowStillImportsTheRows() {
      LocalDateTime createdAt =
          chats
              .systemEvent(CLEARED_AFTER_LAST_ROW_ROOM, CLEARED_AFTER_LAST_ROW_MEMBER_REMOVED_MAM)
              .orElseThrow()
              .createdAt();

      assertTrue(createdAt.isBefore(EARLIEST_FLOOR_OF_CLEARED_AFTER_LAST_ROW_ROOM));
    }
  }

  @Nested
  @DisplayName("ROOM-V1: time base of the imported rows against the existing WSC timestamps")
  class ZoneParity {

    @Test
    @DisplayName("ROOM-V1: the import under the release zone passes the zone check and completes")
    void importUnderTheReleaseZoneCompletes() {
      assertTrue(run.succeeded(), run::failureText);
      assertEquals(
          1,
          chats.count(
              "SELECT count(*) FROM chats.flyway_schema_history WHERE version = '2.0.1' AND"
                  + " success"));
      assertEquals("UTC", run.jvmZone().getId());
      assertFalse(chats.messages(OWNER_LEFT_ROOM).isEmpty());
    }

    @Test
    @DisplayName(
        "ROOM-V1: the first imported row of every GROUP room is within 2 s of room.created_at")
    void firstImportedRowIsInTheTimeBaseOfRoomCreatedAt() {
      String base =
          "SELECT count(*) FROM chats.room r WHERE r.type = 'GROUP' AND EXISTS (SELECT 1 FROM"
              + " chats.subscription s WHERE s.room_id = r.id) AND "
              + "%s";
      assertEquals(
          IMPORTABLE_GROUP_ROOMS,
          chats.count(base.formatted("%s IS NOT NULL".formatted(FIRST_ROW))));
      assertEquals(
          0,
          chats.count(
              base.formatted(
                  "%s NOT BETWEEN r.created_at - interval '2 seconds' AND r.created_at + interval '2 seconds'"
                      .formatted(FIRST_ROW))));
    }
  }

  private static LocalDateTime created(String room, long mam) {
    return chats.message(room, mam).orElseThrow().createdAt();
  }

  private static void assertImportHappened() {
    assertTrue(run.succeeded(), run::failureText);
    assertTrue(chats.message(OWNER_LEFT_ROOM, SECOND_OWNER_FIRST_TEXT_MAM).isPresent());
  }

  private static void assertMessageOnlyInOwnRoom(
      String room, String wrongRoom, long mam, String sender) {
    Message message = chats.message(room, mam).orElseThrow();
    assertEquals(room, message.roomId());
    assertEquals(sender, message.senderId());
    assertTrue(chats.messageById(ExpectedIds.message(wrongRoom, mam)).isEmpty());
    assertTrue(chats.messages(wrongRoom).stream().noneMatch(m -> m.id().equals(message.id())));
  }

  private static org.junit.jupiter.api.function.Executable assertNoRowsFor(String room) {
    return () -> {
      assertAll(
          () ->
              assertEquals(
                  0,
                  chats.count("SELECT count(*) FROM chats.message WHERE room_id = ?", room),
                  "messages of " + room),
          () ->
              assertEquals(
                  0,
                  chats.count("SELECT count(*) FROM chats.system_event WHERE room_id = ?", room),
                  "system events of " + room),
          () -> {
            for (long mam : archive().get(room)) {
              assertTrue(
                  chats.messageById(ExpectedIds.message(room, mam)).isEmpty(), room + "/" + mam);
              assertTrue(chats.systemEvent(room, mam).isEmpty(), room + "/" + mam);
            }
          });
    };
  }

  private static Set<String> importableRooms() {
    Set<String> withSubscriptions =
        new HashSet<>(
            chats.query(
                "SELECT id FROM chats.room r WHERE EXISTS (SELECT 1 FROM chats.subscription s WHERE"
                    + " s.room_id = r.id)",
                rs -> rs.getString(1)));
    withSubscriptions.retainAll(archive().keySet());
    return withSubscriptions;
  }

  private static Set<String> messageIds(ImportedChats imported) {
    return new TreeSet<>(imported.query("SELECT id FROM chats.message", rs -> rs.getString(1)));
  }

  private static Map<String, Set<String>> expectedIdsByRoom(String slot) {
    Map<String, Set<String>> expected = new HashMap<>();
    archive()
        .forEach(
            (room, mams) -> {
              Set<String> ids = new HashSet<>();
              mams.forEach(mam -> ids.add(ExpectedIds.of(slot, room, mam)));
              expected.put(room, ids);
            });
    return expected;
  }

  private static synchronized Map<String, List<Long>> archive() {
    if (archive == null) {
      Map<String, List<Long>> rows = new HashMap<>();
      for (String[] row :
          sourceRows(
              "SELECT u.user_name, m.id FROM mam_muc_message m JOIN mam_server_user u ON u.id ="
                  + " m.room_id WHERE u.server = 'muclight.carbonio' ORDER BY m.id")) {
        rows.computeIfAbsent(row[0], k -> new ArrayList<>()).add(Long.parseLong(row[1]));
      }
      archive = rows;
    }
    return archive;
  }

  private static List<String[]> roomSenderRows() {
    return sourceRows(
        "SELECT u.user_name, m.id FROM mam_muc_message m JOIN mam_server_user u ON u.id ="
            + " m.room_id JOIN mam_server_user s ON s.id = m.sender_id WHERE u.server ="
            + " 'muclight.carbonio' AND s.server = 'muclight.carbonio' ORDER BY m.id");
  }

  private static List<String[]> sourceRows(String sql) {
    List<String[]> rows = new ArrayList<>();
    try (Connection connection =
            MongooseImportDatabase.get().connect(MongooseImportDatabase.SOURCE_DATABASE);
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      while (rs.next()) {
        rows.add(new String[] {rs.getString(1), rs.getString(2)});
      }
    } catch (SQLException e) {
      throw new IllegalStateException(e);
    }
    return rows;
  }

  private static synchronized ImportRun withoutMucLightRooms() {
    if (withoutMucLightRoomsRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      String source = database.newSourceDatabase();
      database.execute(source, "TRUNCATE muc_light_rooms CASCADE");
      withoutMucLightRoomsRun =
          MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
    }
    return withoutMucLightRoomsRun;
  }

  private static synchronized ImportRun twoClears() {
    if (twoClearsRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      String source = database.newSourceDatabase();
      database.execute(
          source,
          ("INSERT INTO mam_muc_message (id, room_id, sender_id, nick_name, message, search_body,"
                  + " origin_id) SELECT %d, room_id, sender_id, nick_name, message, search_body,"
                  + " origin_id FROM mam_muc_message WHERE id = %d")
              .formatted(TEMPORARY_SECOND_CLEAR_MAM, TEMPORARY_CLEAR_MAM));
      twoClearsRun =
          MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
    }
    return twoClearsRun;
  }

  private static synchronized ImportRun temporaryAsGroup() {
    if (temporaryAsGroupRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      String targetDatabase = database.newTargetDatabase();
      database.execute(
          targetDatabase,
          "UPDATE chats.room SET type = 'GROUP' WHERE id = '" + TEMPORARY_ROOM + "'");
      temporaryAsGroupRun = MongooseImport.run(targetDatabase, database.sourceCredentials());
    }
    return temporaryAsGroupRun;
  }
}
