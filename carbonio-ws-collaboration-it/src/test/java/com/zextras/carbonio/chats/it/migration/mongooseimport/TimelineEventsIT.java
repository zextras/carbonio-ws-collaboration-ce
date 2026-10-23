// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Pin;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.TimelineItem;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: system events, ROOM_CREATED and pins")
class TimelineEventsIT {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Pattern CANONICAL_UUID =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
  private static final String CANONICAL_UUID_SQL =
      "'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'";

  private static final String ROOM_CREATED = "ROOM_CREATED";
  private static final String MEMBER_ADDED = "MEMBER_ADDED";
  private static final String MEMBER_REMOVED = "MEMBER_REMOVED";
  private static final String MESSAGE_PINNED = "MESSAGE_PINNED";
  private static final String MESSAGE_UNPINNED = "MESSAGE_UNPINNED";
  private static final String ROOM_NAME_CHANGED = "ROOM_NAME_CHANGED";
  private static final String ROOM_DESCRIPTION_CHANGED = "ROOM_DESCRIPTION_CHANGED";
  private static final String ROOM_PICTURE_UPDATED = "ROOM_PICTURE_UPDATED";
  private static final String ROOM_PICTURE_DELETED = "ROOM_PICTURE_DELETED";
  private static final String ROOM_HISTORY_CLEARED = "ROOM_HISTORY_CLEARED";

  private static final String GROUP_PINNED_BY_REMOVED_MEMBER_ROOM =
      "af0b223e-0c55-4dff-82cc-a69149c758b0";
  private static final String GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM =
      "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM =
      "99b17723-465e-4f87-bb1c-429687075b0d";
  private static final String GROUP_PIN_ON_TEXT_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String GROUP_PIN_GONE_FILE_ROOM = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String GROUP_PINNED_BY_NON_MEMBER_ROOM =
      "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String GROUP_PIN_ON_FORWARD_ROOM = "f0cab8a6-2991-4ccb-a59b-49ada7879299";
  private static final String GROUP_PIN_ON_REPLY_ROOM = "7e7d6c0a-efce-4852-b197-8bf33d267290";
  private static final String GROUP_PIN_NOTIFICATIONS_ONLY_ROOM =
      "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String GROUP_PROMOTED_OWNER_ROOM = "7be2cb4f-4ce4-4530-b473-2df640007959";
  private static final String TEMPORARY_ROOM = "332d2b24-6fbc-45de-a6d3-9e0b16b69aea";
  private static final String GROUP_BURST_ROOM = "8a10332b-0a46-4a7f-8f1a-db54f0dabf02";
  private static final String GROUP_OWNER_REMOVES_ALL_ROOM = "ade1af8a-10e9-4a72-be27-9735cdb6ac90";
  private static final String GROUP_CONFIG_ROWS_ROOM = "f3f01bc3-0de5-4785-acc6-7858551460fb";
  private static final String GROUP_MEETING_DECLINED_ROOM = "eeece340-593d-41aa-b135-7285e965582c";
  private static final String ONE_TO_ONE_MEETING_ROOM = "cd093b9c-3434-4d5a-bd19-502d3b442faa";
  private static final String ONE_TO_ONE_CREATOR_ROOM = "1c6f38cf-d1a3-4947-a81d-ffb410acbbae";
  private static final String ZOMBIE_ROOM = "80e4de67-3d6c-4c15-8876-e6255ac0c1d5";
  private static final String ORPHAN_ARCHIVE_ROOM = "3419545d-8834-4b0e-8d79-7b38f05ebc43";

  private static final long AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM = 458596110979159553L;
  private static final long AF0B_TEXT_PINNED_THEN_UNPINNED_MAM = 458596111218734849L;
  private static final long AF0B_TOMBSTONE_MAM = 458596111461181441L;
  private static final long AF0B_TEXT_EDITED_BY_PINNED_EDIT_MAM = 458596111702221569L;
  private static final long AF0B_TEXT_LAST_PINNED_MAM = 458596111943188481L;
  private static final long AF0B_ATTACHMENT_MAM = 458596112255084033L;
  private static final long AF0B_REPLY_MAM = 458596112523522049L;
  private static final long R99B1_MEMBER_ADDED_MAM = 458596130542834689L;
  private static final long RCD09_MEMBER_ADDED_MAM = 455227107284563969L;
  private static final long AF0B_MEMBER_ADDED_1_MAM = 458596110560755969L;
  private static final long AF0B_MEMBER_ADDED_2_MAM = 458596110565536769L;
  private static final long AF0B_MEMBER_ADDED_3_MAM = 458596110571220737L;
  private static final long AF0B_PINNED_TEXT_NOTIFICATION_MAM = 458596113273188865L;
  private static final long AF0B_PINNED_2_NOTIFICATION_MAM = 458596113719026433L;
  private static final long AF0B_UNPINNED_TEXT_NOTIFICATION_MAM = 458596114144042497L;
  private static final long AF0B_PINNED_3_NOTIFICATION_MAM = 458596114597151745L;
  private static final long AF0B_PINNED_EDIT_ROW_NOTIFICATION_MAM = 458596114833033473L;
  private static final long AF0B_PINNED_ATTACHMENT_NOTIFICATION_MAM = 458596115071506177L;
  private static final long AF0B_PINNED_REPLY_NOTIFICATION_MAM = 458596115307867649L;
  private static final long AF0B_PINNED_TOMBSTONE_NOTIFICATION_MAM = 458596115541138177L;
  private static final long AF0B_UNPINNED_TOMBSTONE_NOTIFICATION_MAM = 458596115761383937L;
  private static final long AF0B_PINNED_TOMBSTONE_AGAIN_NOTIFICATION_MAM = 458596116235224577L;
  private static final long AF0B_PINNED_FIRST_TEXT_NOTIFICATION_MAM = 458596116900812801L;
  private static final long AF0B_PINNED_FIRST_TEXT_AGAIN_NOTIFICATION_MAM = 458596117347719681L;
  private static final long AF0B_UNPINNED_FIRST_TEXT_NOTIFICATION_MAM = 458596117774173953L;
  private static final long AF0B_PINNED_LAST_TEXT_NOTIFICATION_MAM = 458596118007807745L;
  private static final long AF0B_MEMBER_REMOVED_MAM = 458596118446585345L;
  private static final long AF0B_AFFILIATION_1_MAM = 458596110556191745L;
  private static final long AF0B_AFFILIATION_2_MAM = 458596110562855681L;
  private static final long AF0B_AFFILIATION_3_MAM = 458596110568387329L;
  private static final long AF0B_AFFILIATION_REMOVAL_MAM = 458596118442881281L;

  private static final long A992_FORWARD_ORIGINAL_MAM = 458596133384694529L;
  private static final long A992_PINNED_EDIT_ROW_NOTIFICATION_MAM = 458596141560433153L;

  private static final long R99B1_ATTACHMENT_MAM = 458596162421294593L;
  private static final long R6457_TEXT_MAM = 458596167950717441L;

  private static final long D1D4_ATTACHMENT_FILE_GONE_MAM = 458596937466224641L;

  private static final long E0DF_TEXT_MAM = 458596906805728513L;
  private static final long E0DF_REPLY_MAM = 458596907590704385L;
  private static final long E0DF_ATTACHMENT_MAM = 458596909203672833L;
  private static final long E0DF_TOMBSTONE_PINNED_1_MAM = 458596906904507393L;
  private static final long E0DF_TOMBSTONE_PINNED_2_MAM = 458596907900401153L;
  private static final long E0DF_PIN_NOTIFICATION_CROSS_ROOM_MAM = 458597005635540737L;
  private static final long E0DF_PIN_NOTIFICATION_AFFILIATION_TARGET_MAM = 458597006051358465L;
  private static final long E0DF_PIN_NOTIFICATION_TOMBSTONE_1_MAM = 458597006466580225L;
  private static final long E0DF_PIN_NOTIFICATION_TOMBSTONE_2_MAM = 458597006881180929L;
  private static final long E0DF_PIN_NOTIFICATION_REPLY_MAM = 458597008080940033L;
  private static final long E0DF_PIN_NOTIFICATION_TEXT_MAM = 458597008495655937L;
  private static final long E0DF_PIN_NOTIFICATION_ATTACHMENT_MAM = 458597008910059265L;
  private static final long E0DF_UNPIN_NOTIFICATION_TEXT_MAM = 458597009892877057L;

  private static final long F0CA_FORWARD_MAM = 458615900807871233L;
  private static final long F0CA_MEMBER_ADDED_1_MAM = 458615899281732865L;
  private static final long F0CA_MEMBER_ADDED_2_MAM = 458615899287594753L;

  private static final long R7E7D_REPLY_MAM = 458615899939171329L;

  private static final long R9552_TEXT_MAM = 458595355979477505L;
  private static final long R9552_UNPIN_EDIT_ROW_NOTIFICATION_MAM = 458595361730871553L;
  private static final long R9552_PIN_TEXT_NOTIFICATION_MAM = 458595361294167553L;

  private static final long R7BE2_MEMBER_ADDED_1_MAM = 458615893658177793L;
  private static final long R7BE2_MEMBER_ADDED_2_MAM = 458615893663905537L;
  private static final long R7BE2_MEMBER_ADDED_3_MAM = 458615893669326081L;
  private static final long R7BE2_TEXT_1_MAM = 458615894558619137L;
  private static final long R7BE2_TEXT_2_MAM = 458615894569365505L;
  private static final long R7BE2_REMOVED_BY_PROMOTED_OWNER_1_MAM = 458615895174474497L;
  private static final long R7BE2_TEXT_3_MAM = 458615895443214081L;
  private static final long R7BE2_REMOVED_BY_PROMOTED_OWNER_2_MAM = 458615895718419969L;
  private static final long R7BE2_RE_ADDED_MAM = 458615896044660481L;
  private static final long R7BE2_TOMBSTONE_MAM = 458615896594573569L;
  private static final long R7BE2_TEXT_4_MAM = 458615898042111745L;

  private static final long TEMP_MEMBER_REMOVED_MAM = 458615893175090433L;
  private static final long TEMP_HISTORY_CLEARED_MAM = 458615892476687361L;
  private static final long TEMP_TEXT_AFTER_CLEAR_MAM = 458615893127340289L;

  private static final long R8A10_REMOVED_1_MAM = 458597076409250049L;
  private static final long R8A10_REMOVED_2_MAM = 458597076432454913L;
  private static final long R8A10_REMOVED_3_MAM = 458597076451521025L;
  private static final long R8A10_REMOVED_4_MAM = 458597076471078145L;
  private static final long R8A10_REMOVED_5_MAM = 458597076489162241L;

  private static final long ADE1_REMOVED_1_MAM = 458615898574637569L;
  private static final long ADE1_REMOVED_2_MAM = 458615898850513153L;

  private static final long F3F0_NAME_CHANGED_MAM = 458596928317188353L;
  private static final long F3F0_DESCRIPTION_CHANGED_MAM = 458596932440193793L;
  private static final long F3F0_PICTURE_UPDATED_MAM = 458596935919222017L;
  private static final long F3F0_PICTURE_DELETED_MAM = 458596938721894145L;
  private static final long F3F0_EMPTY_DESCRIPTION_MAM = 458596933128727297L;

  private static final long EEEC_MEETING_DECLINED_1_MAM = 458597023883382529L;
  private static final long EEEC_MEETING_DECLINED_2_MAM = 458597026847809281L;

  private static final String USER_556F = "556fe629-f88e-5d3e-bc5e-c54915c7adfa";
  private static final String USER_6FE7 = "6fe7e7cd-1ec7-517d-9902-c4b83886fe3f";
  private static final String USER_EC31 = "ec31dfc9-bc07-5474-86f2-ffa20548dbe4";
  private static final String USER_BA2C = "ba2c72b5-0128-5656-806c-8a163b58450e";
  private static final String USER_A793 = "a793945e-d600-5362-8ecf-bb6b1d88dbc3";
  private static final String USER_BEA9 = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";
  private static final String USER_2DAD = "2dad62c6-7fe9-58e8-8458-216e530ea81d";
  private static final String USER_7D03 = "7d03b0c9-cd6e-5392-9186-1b9646f07fef";
  private static final String USER_E7A2 = "e7a245ba-f1e6-5f32-ba77-90798e7f219b";
  private static final String USER_1E49 = "1e49bba9-89c2-5a09-8eeb-efe969b5389d";
  private static final String USER_B26C = "b26c4bc8-1334-53a0-a372-becd06eb598b";
  private static final String USER_69E9 = "69e94bcc-f52d-550d-bee1-b40dc0368d0f";
  private static final String USER_3B60 = "3b6045dc-cb8c-5f87-8158-3c1a7b277a94";
  private static final String USER_4B86 = "4b86ebf8-3973-57a3-b5e8-aff96036c8c4";
  private static final String USER_4F82 = "4f82bc10-f121-51ed-aed7-6738a8552323";
  private static final String USER_231E = "231e8c22-87e0-5817-8960-c0e71b070652";
  private static final String USER_8D00 = "8d008362-92c7-516e-8bb2-1ad1c6bfd354";
  private static final String USER_184D = "184dbd20-51ed-5451-ae7f-7a13e3995c8d";
  private static final String USER_5C89 = "5c89638f-9a69-5c9b-8fa6-b4b64e10351e";
  private static final String USER_8817 = "8817c09b-3e70-5e64-95c4-dbc5567a4165";
  private static final String USER_1F46 = "1f4645c9-5446-519d-81e0-2a9ee24ef783";
  private static final String USER_5853 = "5853d06f-cfb3-5977-8888-2a67736a8c2a";
  private static final String USER_3C0B = "3c0bc3ee-b41e-5b44-acfc-1b6869a260c7";
  private static final String USER_4D84 = "4d844e6d-864d-503c-9047-32bd38cee22c";

  private static final int IMPORTED_ROOMS = 35;

  private static ImportRun run;
  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  private static String eventId(String room, long mam) {
    return ExpectedIds.systemEvent(room, mam);
  }

  private static String messageId(String room, long mam) {
    return ExpectedIds.message(room, mam);
  }

  private static SystemEvent event(String room, long mam, String type) {
    SystemEvent event =
        chats
            .systemEvent(room, mam)
            .orElseThrow(() -> new AssertionError("missing " + type + " for " + room + "/" + mam));
    assertEquals(type, event.type());
    assertEquals(room, event.roomId());
    return event;
  }

  private static Set<String> keys(JsonNode node) {
    Set<String> keys = new TreeSet<>();
    node.fieldNames().forEachRemaining(keys::add);
    return keys;
  }

  private static void assertNeitherMessageNorEvent(String room, long mam) {
    assertAll(
        () -> assertFalse(chats.message(room, mam).isPresent(), "message for " + mam),
        () -> assertFalse(chats.systemEvent(room, mam).isPresent(), "system event for " + mam));
  }

  private static long countType(String type) {
    return chats.count("SELECT count(*) FROM chats.system_event WHERE type = ?", type);
  }

  private static long countTypeInRoom(String room, String type) {
    return chats.count(
        "SELECT count(*) FROM chats.system_event WHERE room_id = ? AND type = ?", room, type);
  }

  private static void assertPin(
      String room, long targetMam, String pinnedBy, String pinnedAtFromPinMessage) {
    Pin pin =
        chats.pin(room).orElseThrow(() -> new AssertionError("no message_pin row for " + room));
    LocalDateTime expectedAt = LocalDateTime.parse(pinnedAtFromPinMessage);
    assertAll(
        () -> assertEquals(messageId(room, targetMam), pin.messageId()),
        () -> assertEquals(pinnedBy, pin.pinnedBy()),
        () ->
            assertTrue(
                Duration.between(expectedAt, pin.pinnedAt()).abs().toMillis() <= 5,
                "pinned_at " + pin.pinnedAt() + " expected about " + expectedAt),
        () ->
            assertTrue(
                chats.message(room, targetMam).isPresent(),
                "the pinned message must exist as a chat message"));
  }

  private static List<String> ids(List<TimelineItem> timeline) {
    return timeline.stream().map(TimelineItem::id).toList();
  }

  @Nested
  @DisplayName("SEM-6 / APP-11: pins imported from pin_message")
  class Pins {

    @Test
    @DisplayName("SEM-6: a pin by a removed member (no notification) on a text is kept")
    void pinByRemovedMemberOnTextIsKept() {
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ?",
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              USER_BA2C));
      assertPin(
          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
          AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM,
          USER_BA2C,
          "2026-10-07T16:38:09.815657");
    }

    @Test
    @DisplayName("SEM-6: a pin by a never-subscribed user on a text is kept")
    void pinByNonSubscriberOnTextIsKept() {
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.subscription WHERE room_id = ? AND user_id = ?",
              GROUP_PINNED_BY_NON_MEMBER_ROOM,
              USER_2DAD));
      assertPin(
          GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_TEXT_MAM, USER_2DAD, "2026-10-07T17:36:10.664725");
    }

    @Test
    @DisplayName("SEM-6: a pin stored on an edit row resolves to the original forward")
    void pinOnEditRowPointsToOriginalForward() {
      assertPin(
          GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM,
          A992_FORWARD_ORIGINAL_MAM,
          USER_6FE7,
          "2026-10-07T16:39:37.971228");
      assertTrue(
          chats
              .message(GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM, A992_FORWARD_ORIGINAL_MAM)
              .orElseThrow()
              .forwarded());
    }

    @Test
    @DisplayName("SEM-6: a pin on a reply is kept and points to the reply message")
    void pinOnReplyIsKept() {
      assertPin(GROUP_PIN_ON_REPLY_ROOM, R7E7D_REPLY_MAM, USER_1E49, "2026-10-08T14:06:15.716199");
      assertNotNull(
          chats.message(GROUP_PIN_ON_REPLY_ROOM, R7E7D_REPLY_MAM).orElseThrow().replyToId());
    }

    @Test
    @DisplayName("SEM-6: a pin on a forward is kept and points to the forward message")
    void pinOnForwardIsKept() {
      assertPin(
          GROUP_PIN_ON_FORWARD_ROOM, F0CA_FORWARD_MAM, USER_E7A2, "2026-10-08T14:06:05.318806");
      assertTrue(
          chats.message(GROUP_PIN_ON_FORWARD_ROOM, F0CA_FORWARD_MAM).orElseThrow().forwarded());
    }

    @Test
    @DisplayName("SEM-6: a pin on a text of a group room is imported with the user uuid")
    void pinOnTextIsImported() {
      assertPin(GROUP_PIN_ON_TEXT_ROOM, R6457_TEXT_MAM, USER_BA2C, "2026-10-07T16:41:26.779573");
    }

    @Test
    @DisplayName(
        "A pin with its archived pin row takes that row's time, like the MESSAGE_PINNED line")
    void pinTimeComesFromTheArchivedPinRow() {
      assertEquals(
          1,
          chats.count(
              "SELECT count(*) FROM chats.message_pin p JOIN chats.system_event e"
                  + " ON e.room_id = p.room_id AND e.type = 'MESSAGE_PINNED'"
                  + " AND e.content::jsonb ->> 'messageId' = p.message_id"
                  + " AND e.actor_id = p.pinned_by"
                  + " WHERE p.room_id = ?"
                  + " AND abs(extract(epoch FROM e.created_at - p.pinned_at)) < 0.03",
              GROUP_PIN_ON_TEXT_ROOM));
    }

    @Test
    @DisplayName("SEM-6: a pin on an attachment message of a one-to-one room is imported")
    void pinOnAttachmentOfOneToOneRoomIsImported() {
      assertPin(
          ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM,
          R99B1_ATTACHMENT_MAM,
          USER_BEA9,
          "2026-10-07T16:41:04.233776");
      assertEquals(
          "fe9948df-9864-4c71-94f5-d01ebb6089ca",
          chats
              .message(ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM, R99B1_ATTACHMENT_MAM)
              .orElseThrow()
              .attachmentId());
    }

    @Test
    @DisplayName("SEM-6: a pin whose attachment file is gone (deleted placeholder) is skipped")
    void pinOnDeletedPlaceholderIsSkipped() {
      assertTrue(
          chats
              .message(GROUP_PIN_GONE_FILE_ROOM, D1D4_ATTACHMENT_FILE_GONE_MAM)
              .orElseThrow()
              .deleted());
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message_pin WHERE room_id = ?",
              GROUP_PIN_GONE_FILE_ROOM));
    }

    @Test
    @DisplayName("SEM-6: exactly seven pins, one per room, each on a live message of its room")
    void pinsAreSevenOnePerRoomOnLiveMessagesOfTheSameRoom() {
      assertAll(
          () -> assertEquals(7, chats.count("SELECT count(*) FROM chats.message_pin")),
          () ->
              assertEquals(7, chats.count("SELECT count(DISTINCT room_id) FROM chats.message_pin")),
          () ->
              assertEquals(
                  7,
                  chats.count(
                      "SELECT count(*) FROM chats.message_pin p JOIN chats.message m ON m.id ="
                          + " p.message_id AND m.room_id = p.room_id WHERE m.deleted = false")),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message_pin WHERE pinned_by !~ "
                          + CANONICAL_UUID_SQL)));
    }

    @Test
    @DisplayName("SEM-6: pin notifications alone never create a pin (pin_message is authoritative)")
    void pinNotificationsAloneCreateNoPin() {
      event(GROUP_PIN_NOTIFICATIONS_ONLY_ROOM, R9552_PIN_TEXT_NOTIFICATION_MAM, MESSAGE_PINNED);
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message_pin WHERE room_id = ?",
              GROUP_PIN_NOTIFICATIONS_ONLY_ROOM));
    }

    @Test
    @DisplayName("SEM-6: pins of a zombie room and of a room unknown to WSC are skipped")
    void pinsOfZombieAndUnknownRoomsAreSkipped() {
      assertEquals(7, chats.count("SELECT count(*) FROM chats.message_pin"));
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.message_pin WHERE room_id IN (?, ?)",
              ZOMBIE_ROOM,
              ORPHAN_ARCHIVE_ROOM));
    }

    @Test
    @DisplayName("SEM-V2: a source without pin_message imports everything else and no pin")
    void sourceWithoutPinMessageTableImportsWithoutPins() {
      ImportedChats withoutPins = runWithMutatedPinMessage("DROP TABLE pin_message");

      assertAll(
          () -> assertEquals(0, withoutPins.count("SELECT count(*) FROM chats.message_pin")),
          () ->
              assertTrue(withoutPins.message(GROUP_PIN_ON_TEXT_ROOM, R6457_TEXT_MAM).isPresent()));
    }

    @Test
    @DisplayName("SEM-V2: pin rows with blank stanza id, garbage room or blank pinner are skipped")
    void oddPinRowsAreSkippedWithoutAbortingTheImport() {
      ImportedChats odd =
          runWithMutatedPinMessage(
              "UPDATE pin_message SET stanza_id = '' WHERE room_id LIKE '7e7d6c0a%'",
              "UPDATE pin_message SET room_id = 'garbage' WHERE room_id LIKE 'f0cab8a6%'",
              "UPDATE pin_message SET pinned_by = '' WHERE room_id LIKE '99b17723%'");

      assertAll(
          () ->
              assertEquals(
                  messageId(
                      GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM),
                  odd.pin(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM).orElseThrow().messageId()),
          () ->
              assertEquals(
                  messageId(GROUP_PIN_ON_TEXT_ROOM, R6457_TEXT_MAM),
                  odd.pin(GROUP_PIN_ON_TEXT_ROOM).orElseThrow().messageId()),
          () -> assertTrue(odd.pin(GROUP_PIN_ON_REPLY_ROOM).isEmpty()),
          () -> assertTrue(odd.pin(GROUP_PIN_ON_FORWARD_ROOM).isEmpty()),
          () -> assertTrue(odd.pin(ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM).isEmpty()));
    }

    private ImportedChats runWithMutatedPinMessage(String... statements) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      String source = database.newSourceDatabase();
      database.execute(source, statements);
      ImportRun mutated =
          MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
      assertTrue(mutated.succeeded(), mutated::failureText);
      return mutated.chats();
    }
  }

  @Nested
  @DisplayName("SEM-7 / APP-12: MEMBER_ADDED and MEMBER_REMOVED events")
  class MemberEvents {

    @Test
    @DisplayName("SEM-7: each memberAdded row is its own MEMBER_ADDED event, never coalesced")
    void memberAddedIsOneEventPerRow() {
      SystemEvent first = event(GROUP_PIN_ON_FORWARD_ROOM, F0CA_MEMBER_ADDED_1_MAM, MEMBER_ADDED);
      SystemEvent second = event(GROUP_PIN_ON_FORWARD_ROOM, F0CA_MEMBER_ADDED_2_MAM, MEMBER_ADDED);

      assertAll(
          () -> assertEquals(Set.of("addedByUserId", "addedUserIds"), keys(first.json())),
          () -> assertEquals(USER_E7A2, first.json().get("addedByUserId").asText()),
          () -> assertEquals(1, first.json().get("addedUserIds").size()),
          () -> assertEquals(USER_1E49, first.json().get("addedUserIds").get(0).asText()),
          () -> assertEquals(USER_E7A2, second.json().get("addedByUserId").asText()),
          () -> assertEquals(1, second.json().get("addedUserIds").size()),
          () -> assertEquals(USER_B26C, second.json().get("addedUserIds").get(0).asText()),
          () -> assertEquals(ExpectedTimes.mamMillis(F0CA_MEMBER_ADDED_1_MAM), first.createdAt()),
          () -> assertEquals(ExpectedTimes.mamMillis(F0CA_MEMBER_ADDED_2_MAM), second.createdAt()));
    }

    @Test
    @DisplayName("SEM-7: a re-added member gets a second MEMBER_ADDED event")
    void reAddedMemberGetsSecondEvent() {
      SystemEvent initial =
          event(GROUP_PROMOTED_OWNER_ROOM, R7BE2_MEMBER_ADDED_2_MAM, MEMBER_ADDED);
      SystemEvent readded = event(GROUP_PROMOTED_OWNER_ROOM, R7BE2_RE_ADDED_MAM, MEMBER_ADDED);

      assertAll(
          () -> assertEquals(USER_4F82, initial.json().get("addedUserIds").get(0).asText()),
          () -> assertEquals(USER_4F82, readded.json().get("addedUserIds").get(0).asText()),
          () -> assertEquals(USER_3B60, readded.json().get("addedByUserId").asText()),
          () -> assertEquals(ExpectedTimes.mamMillis(R7BE2_RE_ADDED_MAM), readded.createdAt()));
    }

    @Test
    @DisplayName("SEM-7: a burst of 14 memberAdded rows gives 14 MEMBER_ADDED events")
    void memberAddedBurstGivesOneEventPerRow() {
      assertEquals(14, countTypeInRoom(GROUP_BURST_ROOM, MEMBER_ADDED));
    }

    @Test
    @DisplayName(
        "SEM-7: removedByUserId is the MAM row sender (first owner), not the promoted owner")
    void memberRemovedKeepsRowSenderAsRemovedBy() {
      SystemEvent first =
          event(GROUP_PROMOTED_OWNER_ROOM, R7BE2_REMOVED_BY_PROMOTED_OWNER_1_MAM, MEMBER_REMOVED);
      SystemEvent second =
          event(GROUP_PROMOTED_OWNER_ROOM, R7BE2_REMOVED_BY_PROMOTED_OWNER_2_MAM, MEMBER_REMOVED);

      assertAll(
          () -> assertEquals(Set.of("removedByUserId", "removedUserId"), keys(first.json())),
          () -> assertEquals(USER_3B60, first.json().get("removedByUserId").asText()),
          () -> assertEquals(USER_231E, first.json().get("removedUserId").asText()),
          () -> assertEquals(USER_3B60, second.json().get("removedByUserId").asText()),
          () -> assertEquals(USER_4F82, second.json().get("removedUserId").asText()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(R7BE2_REMOVED_BY_PROMOTED_OWNER_1_MAM),
                  first.createdAt()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(R7BE2_REMOVED_BY_PROMOTED_OWNER_2_MAM),
                  second.createdAt()));
    }

    @Test
    @DisplayName("SEM-7: MEMBER_REMOVED of a member removed by the owner in a plain group")
    void memberRemovedInPlainGroup() {
      SystemEvent removed =
          event(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_REMOVED_MAM, MEMBER_REMOVED);
      SystemEvent first = event(GROUP_OWNER_REMOVES_ALL_ROOM, ADE1_REMOVED_1_MAM, MEMBER_REMOVED);
      SystemEvent second = event(GROUP_OWNER_REMOVES_ALL_ROOM, ADE1_REMOVED_2_MAM, MEMBER_REMOVED);

      assertAll(
          () -> assertEquals(USER_556F, removed.json().get("removedByUserId").asText()),
          () -> assertEquals(USER_BA2C, removed.json().get("removedUserId").asText()),
          () -> assertEquals(USER_5853, first.json().get("removedByUserId").asText()),
          () -> assertEquals(USER_3C0B, first.json().get("removedUserId").asText()),
          () -> assertEquals(USER_5853, second.json().get("removedByUserId").asText()),
          () -> assertEquals(USER_4D84, second.json().get("removedUserId").asText()));
    }

    @Test
    @DisplayName("SEM-7: five simultaneous removals give five MEMBER_REMOVED events")
    void burstOfRemovalsGivesFiveEvents() {
      long[] mams = {
        R8A10_REMOVED_1_MAM,
        R8A10_REMOVED_2_MAM,
        R8A10_REMOVED_3_MAM,
        R8A10_REMOVED_4_MAM,
        R8A10_REMOVED_5_MAM
      };
      String[] removed = {USER_B26C, USER_184D, USER_5C89, USER_8817, USER_1F46};
      assertEquals(5, countTypeInRoom(GROUP_BURST_ROOM, MEMBER_REMOVED));
      for (int i = 0; i < mams.length; i++) {
        SystemEvent event = event(GROUP_BURST_ROOM, mams[i], MEMBER_REMOVED);
        assertEquals(USER_8D00, event.json().get("removedByUserId").asText());
        assertEquals(removed[i], event.json().get("removedUserId").asText());
      }
    }

    @Test
    @DisplayName("SEM-V1 / C1: memberRemoved in the TEMPORARY room is a MEMBER_REMOVED event")
    void memberRemovedInTemporaryRoomIsImported() {
      assertTrue(chats.message(TEMPORARY_ROOM, TEMP_TEXT_AFTER_CLEAR_MAM).isPresent());
      SystemEvent removed = event(TEMPORARY_ROOM, TEMP_MEMBER_REMOVED_MAM, MEMBER_REMOVED);
      assertAll(
          () -> assertEquals(1, countTypeInRoom(TEMPORARY_ROOM, ROOM_CREATED)),
          () -> assertEquals(1, countTypeInRoom(TEMPORARY_ROOM, MEMBER_REMOVED)),
          () ->
              assertEquals(
                  Set.of("removedByUserId", "removedUserId"),
                  keys(removed.json()),
                  removed.content()),
          () -> assertEquals(removed.json().get("removedByUserId").asText(), removed.actorId()),
          () -> assertFalse(chats.message(TEMPORARY_ROOM, TEMP_MEMBER_REMOVED_MAM).isPresent()));
    }

    @Test
    @DisplayName("SEM-7 / SEM-V1 / C1: 81 MEMBER_ADDED and 17 MEMBER_REMOVED events in total")
    void memberEventTotals() {
      assertAll(
          () -> assertEquals(81, countType(MEMBER_ADDED)),
          () -> assertEquals(17, countType(MEMBER_REMOVED)));
    }

    @Test
    @DisplayName("TL10: ONE_TO_ONE rooms get no MEMBER_ADDED events, GROUP rooms keep theirs")
    void oneToOneRoomsHaveNoMemberAddedEvents() {
      String perType =
          "SELECT count(*) FROM chats.system_event e JOIN chats.room r ON r.id = e.room_id"
              + " WHERE e.type = ? AND r.type = ?";
      assertAll(
          () -> assertEquals(0, chats.count(perType, MEMBER_ADDED, "ONE_TO_ONE")),
          () -> assertTrue(chats.count(perType, MEMBER_ADDED, "GROUP") > 0),
          () ->
              assertNeitherMessageNorEvent(
                  ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM, R99B1_MEMBER_ADDED_MAM),
          () -> assertNeitherMessageNorEvent(ONE_TO_ONE_MEETING_ROOM, RCD09_MEMBER_ADDED_MAM));
    }

    @Test
    @DisplayName("TL10: ROOM_CREATED stays strictly before the first imported row of a 1:1 room")
    void roomCreatedPrecedesFirstImportedRowOfOneToOneRoom() {
      List<TimelineItem> timeline = chats.timeline(ONE_TO_ONE_PIN_ON_ATTACHMENT_ROOM);
      assertAll(
          () -> assertEquals(ROOM_CREATED, timeline.get(0).kind()),
          () -> assertTrue(timeline.get(0).createdAt().isBefore(timeline.get(1).createdAt())));
    }

    @Test
    @DisplayName("SEM-7: memberAdded and memberRemoved rows are never chat messages")
    void memberRowsAreNeverMessages() {
      assertTrue(
          chats
              .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM)
              .isPresent());
      assertAll(
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_ADDED_1_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_ADDED_2_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_ADDED_3_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_REMOVED_MAM)
                      .isPresent()));
    }
  }

  @Nested
  @DisplayName("SEM-7 / APP-12: MESSAGE_PINNED and MESSAGE_UNPINNED events")
  class PinEvents {

    @Test
    @DisplayName("SEM-7: a pin notification becomes MESSAGE_PINNED with the new id of the text")
    void pinNotificationOnTextBecomesPinnedEvent() {
      SystemEvent pinned =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_TEXT_NOTIFICATION_MAM,
              MESSAGE_PINNED);

      assertAll(
          () -> assertEquals(Set.of("pinnedBy", "messageId"), keys(pinned.json())),
          () -> assertEquals(USER_6FE7, pinned.json().get("pinnedBy").asText()),
          () ->
              assertEquals(
                  messageId(
                      GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM),
                  pinned.json().get("messageId").asText()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(AF0B_PINNED_TEXT_NOTIFICATION_MAM), pinned.createdAt()));
    }

    @Test
    @DisplayName("SEM-7: an unpin notification becomes MESSAGE_UNPINNED with unpinnedBy")
    void unpinNotificationBecomesUnpinnedEvent() {
      SystemEvent unpinned =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_UNPINNED_TEXT_NOTIFICATION_MAM,
              MESSAGE_UNPINNED);

      assertAll(
          () -> assertEquals(Set.of("unpinnedBy", "messageId"), keys(unpinned.json())),
          () -> assertEquals(USER_6FE7, unpinned.json().get("unpinnedBy").asText()),
          () ->
              assertEquals(
                  messageId(
                      GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_PINNED_THEN_UNPINNED_MAM),
                  unpinned.json().get("messageId").asText()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(AF0B_UNPINNED_TEXT_NOTIFICATION_MAM),
                  unpinned.createdAt()));
    }

    @Test
    @DisplayName("SEM-7: pin notifications on reply, attachment and later texts keep their targets")
    void pinNotificationsOnReplyAttachmentAndTextKeepTargets() {
      SystemEvent onAttachment =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_ATTACHMENT_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent onReply =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_REPLY_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent onLastText =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_LAST_TEXT_NOTIFICATION_MAM,
              MESSAGE_PINNED);

      assertAll(
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_ATTACHMENT_MAM),
                  onAttachment.json().get("messageId").asText()),
          () -> assertEquals(USER_EC31, onAttachment.json().get("pinnedBy").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_REPLY_MAM),
                  onReply.json().get("messageId").asText()),
          () -> assertEquals(USER_BA2C, onReply.json().get("pinnedBy").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_LAST_PINNED_MAM),
                  onLastText.json().get("messageId").asText()));
    }

    @Test
    @DisplayName("SEM-7: a pin notification on an edit row points to the ORIGINAL message")
    void pinNotificationOnEditRowPointsToOriginal() {
      SystemEvent onEditOfText =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_EDIT_ROW_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent onEditOfForward =
          event(
              GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM,
              A992_PINNED_EDIT_ROW_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent unpinOnEdit =
          event(
              GROUP_PIN_NOTIFICATIONS_ONLY_ROOM,
              R9552_UNPIN_EDIT_ROW_NOTIFICATION_MAM,
              MESSAGE_UNPINNED);

      assertAll(
          () ->
              assertEquals(
                  messageId(
                      GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TEXT_EDITED_BY_PINNED_EDIT_MAM),
                  onEditOfText.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM, A992_FORWARD_ORIGINAL_MAM),
                  onEditOfForward.json().get("messageId").asText()),
          () -> assertEquals(USER_6FE7, onEditOfForward.json().get("pinnedBy").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PIN_NOTIFICATIONS_ONLY_ROOM, R9552_TEXT_MAM),
                  unpinOnEdit.json().get("messageId").asText()),
          () -> assertEquals(USER_69E9, unpinOnEdit.json().get("unpinnedBy").asText()));
    }

    @Test
    @DisplayName(
        "SEM-7: pin notifications on tombstones are kept and point to the deleted messages")
    void pinNotificationsOnTombstonesPointToDeletedMessages() {
      SystemEvent inAf0b =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_TOMBSTONE_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent unpinInAf0b =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_UNPINNED_TOMBSTONE_NOTIFICATION_MAM,
              MESSAGE_UNPINNED);
      SystemEvent inE0df =
          event(
              GROUP_PINNED_BY_NON_MEMBER_ROOM,
              E0DF_PIN_NOTIFICATION_TOMBSTONE_1_MAM,
              MESSAGE_PINNED);
      SystemEvent secondInE0df =
          event(
              GROUP_PINNED_BY_NON_MEMBER_ROOM,
              E0DF_PIN_NOTIFICATION_TOMBSTONE_2_MAM,
              MESSAGE_PINNED);

      assertAll(
          () ->
              assertTrue(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TOMBSTONE_MAM)
                      .orElseThrow()
                      .deleted()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TOMBSTONE_MAM),
                  inAf0b.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_TOMBSTONE_MAM),
                  unpinInAf0b.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_TOMBSTONE_PINNED_1_MAM),
                  inE0df.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_TOMBSTONE_PINNED_2_MAM),
                  secondInE0df.json().get("messageId").asText()),
          () -> assertEquals(USER_7D03, inE0df.json().get("pinnedBy").asText()));
    }

    @Test
    @DisplayName("SEM-7: pin notifications on reply, text and attachment of a crafted room")
    void pinNotificationsInCraftedRoomKeepTargets() {
      SystemEvent onReply =
          event(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_PIN_NOTIFICATION_REPLY_MAM, MESSAGE_PINNED);
      SystemEvent onText =
          event(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_PIN_NOTIFICATION_TEXT_MAM, MESSAGE_PINNED);
      SystemEvent onAttachment =
          event(
              GROUP_PINNED_BY_NON_MEMBER_ROOM,
              E0DF_PIN_NOTIFICATION_ATTACHMENT_MAM,
              MESSAGE_PINNED);
      SystemEvent unpin =
          event(
              GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_UNPIN_NOTIFICATION_TEXT_MAM, MESSAGE_UNPINNED);

      assertAll(
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_REPLY_MAM),
                  onReply.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_TEXT_MAM),
                  onText.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_ATTACHMENT_MAM),
                  onAttachment.json().get("messageId").asText()),
          () ->
              assertEquals(
                  messageId(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_TEXT_MAM),
                  unpin.json().get("messageId").asText()),
          () -> assertEquals(USER_7D03, unpin.json().get("unpinnedBy").asText()));
    }

    @Test
    @DisplayName(
        "SEM-7: pin notifications on a cross-room target or on an affiliation row are skipped")
    void pinNotificationsOnCrossRoomOrAffiliationTargetsAreSkipped() {
      event(GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_PIN_NOTIFICATION_REPLY_MAM, MESSAGE_PINNED);
      assertAll(
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_PIN_NOTIFICATION_CROSS_ROOM_MAM),
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_NON_MEMBER_ROOM, E0DF_PIN_NOTIFICATION_AFFILIATION_TARGET_MAM));
    }

    @Test
    @DisplayName("SEM-7: 30 MESSAGE_PINNED and 8 MESSAGE_UNPINNED events in total")
    void pinEventTotals() {
      assertAll(
          () -> assertEquals(30, countType(MESSAGE_PINNED)),
          () -> assertEquals(8, countType(MESSAGE_UNPINNED)));
    }

    @Test
    @DisplayName("SEM-7: pin notifications are never chat messages")
    void pinNotificationsAreNeverMessages() {
      event(
          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
          AF0B_PINNED_FIRST_TEXT_AGAIN_NOTIFICATION_MAM,
          MESSAGE_PINNED);
      assertAll(
          () ->
              assertFalse(
                  chats
                      .message(
                          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_PINNED_TEXT_NOTIFICATION_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(
                          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
                          AF0B_UNPINNED_FIRST_TEXT_NOTIFICATION_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(
                          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
                          AF0B_PINNED_FIRST_TEXT_NOTIFICATION_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_PINNED_2_NOTIFICATION_MAM)
                      .isPresent()),
          () ->
              assertFalse(
                  chats
                      .message(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_PINNED_3_NOTIFICATION_MAM)
                      .isPresent()));
    }
  }

  @Nested
  @DisplayName("SEM-7 / gen-D: rows that are never system events nor messages")
  class SkippedRows {

    @Test
    @DisplayName("SEM-7: muc_light affiliation rows are never messages nor system events")
    void affiliationRowsAreSkipped() {
      assertEquals(1, countTypeInRoom(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, ROOM_CREATED));
      assertAll(
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_AFFILIATION_1_MAM),
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_AFFILIATION_2_MAM),
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_AFFILIATION_3_MAM),
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_AFFILIATION_REMOVAL_MAM));
    }

    @Test
    @DisplayName("SEM-7 / C2: name, description and picture change rows are room events")
    void roomConfigurationRowsAreEvents() {
      SystemEvent name = event(GROUP_CONFIG_ROWS_ROOM, F3F0_NAME_CHANGED_MAM, ROOM_NAME_CHANGED);
      SystemEvent description =
          event(GROUP_CONFIG_ROWS_ROOM, F3F0_DESCRIPTION_CHANGED_MAM, ROOM_DESCRIPTION_CHANGED);
      SystemEvent emptyDescription =
          event(GROUP_CONFIG_ROWS_ROOM, F3F0_EMPTY_DESCRIPTION_MAM, ROOM_DESCRIPTION_CHANGED);
      SystemEvent updated =
          event(GROUP_CONFIG_ROWS_ROOM, F3F0_PICTURE_UPDATED_MAM, ROOM_PICTURE_UPDATED);
      SystemEvent deleted =
          event(GROUP_CONFIG_ROWS_ROOM, F3F0_PICTURE_DELETED_MAM, ROOM_PICTURE_DELETED);
      assertAll(
          () -> assertEquals(Set.of("changedBy", "name"), keys(name.json())),
          () -> assertEquals("L1 oremips", name.json().get("name").asText()),
          () -> assertEquals(USER_1E49, name.json().get("changedBy").asText()),
          () -> assertEquals(USER_1E49, name.actorId()),
          () -> assertEquals(Set.of("changedBy", "description"), keys(description.json())),
          () -> assertEquals("lor emipsumdolo", description.json().get("description").asText()),
          () -> assertEquals("", emptyDescription.json().get("description").asText()),
          () -> assertEquals(Set.of("updatedBy"), keys(updated.json())),
          () -> assertEquals(USER_1E49, updated.json().get("updatedBy").asText()),
          () -> assertEquals(USER_1E49, updated.actorId()),
          () -> assertEquals(Set.of("deletedBy"), keys(deleted.json())),
          () -> assertEquals(USER_1E49, deleted.json().get("deletedBy").asText()),
          () -> assertTrue(chats.message(GROUP_CONFIG_ROWS_ROOM, F3F0_NAME_CHANGED_MAM).isEmpty()));
    }

    @Test
    @DisplayName("C2: 5 name, 5 description, 7 picture updated and 3 picture deleted events")
    void roomConfigurationTotals() {
      assertAll(
          () -> assertEquals(5, countType(ROOM_NAME_CHANGED)),
          () -> assertEquals(5, countType(ROOM_DESCRIPTION_CHANGED)),
          () -> assertEquals(7, countType(ROOM_PICTURE_UPDATED)),
          () -> assertEquals(3, countType(ROOM_PICTURE_DELETED)));
    }

    @Test
    @DisplayName("SEM-7 / C3: the roomHistoryCleared row of the TEMPORARY room is a clear event")
    void historyClearedRowIsAnEvent() {
      assertTrue(chats.message(TEMPORARY_ROOM, TEMP_TEXT_AFTER_CLEAR_MAM).isPresent());
      SystemEvent cleared = event(TEMPORARY_ROOM, TEMP_HISTORY_CLEARED_MAM, ROOM_HISTORY_CLEARED);
      assertAll(
          () -> assertEquals(Set.of("clearedBy"), keys(cleared.json())),
          () -> assertEquals(USER_69E9, cleared.json().get("clearedBy").asText()),
          () -> assertEquals(USER_69E9, cleared.actorId()),
          () -> assertEquals(1, countType(ROOM_HISTORY_CLEARED)),
          () -> assertFalse(chats.message(TEMPORARY_ROOM, TEMP_HISTORY_CLEARED_MAM).isPresent()));
    }

    @Test
    @DisplayName("SEM-7: meetingDeclined rows of a GROUP room are skipped")
    void meetingDeclinedInGroupRoomIsSkipped() {
      assertEquals(2, countTypeInRoom(GROUP_MEETING_DECLINED_ROOM, MEMBER_ADDED));
      assertAll(
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_MEETING_DECLINED_ROOM, EEEC_MEETING_DECLINED_1_MAM),
          () ->
              assertNeitherMessageNorEvent(
                  GROUP_MEETING_DECLINED_ROOM, EEEC_MEETING_DECLINED_2_MAM));
    }
  }

  @Nested
  @DisplayName("APP-12: system_event contract (types, exact content keys, canonical ids)")
  class EventContract {

    @Test
    @DisplayName("APP-12: only the ten dump-backed types exist, 192 events in total")
    void onlyKnownTypesAndExactTotal() {
      assertAll(
          () -> assertEquals(192, chats.count("SELECT count(*) FROM chats.system_event")),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE type NOT IN"
                          + " (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                      ROOM_CREATED,
                      MEMBER_ADDED,
                      MEMBER_REMOVED,
                      MESSAGE_PINNED,
                      MESSAGE_UNPINNED,
                      ROOM_NAME_CHANGED,
                      ROOM_DESCRIPTION_CHANGED,
                      ROOM_PICTURE_UPDATED,
                      ROOM_PICTURE_DELETED,
                      ROOM_HISTORY_CLEARED)));
    }

    @Test
    @DisplayName(
        "TL3: the dump has no 1:1 call rows and its GROUP meetingDeclined rows are skipped, so no"
            + " MEETING_* event exists (MeetingLinesIT covers the 1:1 call lines)")
    void noMeetingEventsInTheDump() {
      assertEquals(
          0, chats.count("SELECT count(*) FROM chats.system_event WHERE type LIKE 'MEETING\\_%'"));
    }

    @Test
    @DisplayName("APP-12: MEMBER_ADDED content has exactly the native keys and canonical user ids")
    void memberAddedContentIsNative() throws Exception {
      List<String> contents = contentsOf(MEMBER_ADDED);
      assertEquals(81, contents.size());
      for (String content : contents) {
        JsonNode json = JSON.readTree(content);
        assertEquals(Set.of("addedByUserId", "addedUserIds"), keys(json), content);
        assertTrue(CANONICAL_UUID.matcher(json.get("addedByUserId").asText()).matches(), content);
        assertTrue(json.get("addedUserIds").isArray(), content);
        assertEquals(1, json.get("addedUserIds").size(), content);
        assertTrue(
            CANONICAL_UUID.matcher(json.get("addedUserIds").get(0).asText()).matches(), content);
      }
    }

    @Test
    @DisplayName(
        "APP-12: MEMBER_REMOVED content has exactly the native keys and canonical user ids")
    void memberRemovedContentIsNative() throws Exception {
      List<String> contents = contentsOf(MEMBER_REMOVED);
      assertEquals(17, contents.size());
      for (String content : contents) {
        JsonNode json = JSON.readTree(content);
        assertEquals(Set.of("removedByUserId", "removedUserId"), keys(json), content);
        assertTrue(CANONICAL_UUID.matcher(json.get("removedByUserId").asText()).matches(), content);
        assertTrue(CANONICAL_UUID.matcher(json.get("removedUserId").asText()).matches(), content);
      }
    }

    @Test
    @DisplayName(
        "APP-12: pin and unpin content has exactly the native keys, ids of same-room messages")
    void pinContentIsNative() throws Exception {
      assertPinContent(MESSAGE_PINNED, "pinnedBy", 30);
      assertPinContent(MESSAGE_UNPINNED, "unpinnedBy", 8);
    }

    @Test
    @DisplayName("APP-12: ROOM_CREATED content has exactly creatorId, a canonical user id")
    void roomCreatedContentIsNative() throws Exception {
      List<String> contents = contentsOf(ROOM_CREATED);
      assertEquals(IMPORTED_ROOMS, contents.size());
      for (String content : contents) {
        JsonNode json = JSON.readTree(content);
        assertEquals(Set.of("creatorId"), keys(json), content);
        assertTrue(CANONICAL_UUID.matcher(json.get("creatorId").asText()).matches(), content);
      }
    }

    private void assertPinContent(String type, String actorKey, int expected) throws Exception {
      List<String> contents = contentsOf(type);
      assertEquals(expected, contents.size());
      for (String content : contents) {
        JsonNode json = JSON.readTree(content);
        assertEquals(Set.of(actorKey, "messageId"), keys(json), content);
        assertTrue(CANONICAL_UUID.matcher(json.get(actorKey).asText()).matches(), content);
        assertTrue(CANONICAL_UUID.matcher(json.get("messageId").asText()).matches(), content);
      }
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.system_event e WHERE e.type = ? AND NOT EXISTS (SELECT 1"
                  + " FROM chats.message m WHERE m.id = (e.content::json ->> 'messageId') AND"
                  + " m.room_id = e.room_id)",
              type));
    }

    private List<String> contentsOf(String type) {
      return chats.query(
          "SELECT content FROM chats.system_event WHERE type = ? ORDER BY id",
          rs -> rs.getString(1),
          type);
    }
  }

  @Nested
  @DisplayName("ACTOR: every imported system event carries the user who produced it")
  class ActorContract {

    private static final String ACTOR_KEY_SQL =
        "CASE type WHEN 'ROOM_CREATED' THEN 'creatorId' WHEN 'MEMBER_ADDED' THEN 'addedByUserId'"
            + " WHEN 'MEMBER_REMOVED' THEN 'removedByUserId' WHEN 'MESSAGE_PINNED' THEN 'pinnedBy'"
            + " WHEN 'MESSAGE_UNPINNED' THEN 'unpinnedBy' WHEN 'ROOM_NAME_CHANGED' THEN"
            + " 'changedBy' WHEN 'ROOM_DESCRIPTION_CHANGED' THEN 'changedBy' WHEN"
            + " 'ROOM_PICTURE_UPDATED' THEN 'updatedBy' WHEN 'ROOM_PICTURE_DELETED' THEN"
            + " 'deletedBy' WHEN 'ROOM_HISTORY_CLEARED' THEN 'clearedBy' END";

    private String mamSender(long mam) throws Exception {
      try (java.sql.Connection source =
              MongooseImportDatabase.get().connect(MongooseImportDatabase.SOURCE_DATABASE);
          java.sql.PreparedStatement query =
              source.prepareStatement(
                  "SELECT s.user_name FROM mam_muc_message m JOIN mam_server_user s ON s.id ="
                      + " m.sender_id WHERE m.id = ? AND s.server = 'carbonio'")) {
        query.setLong(1, mam);
        try (java.sql.ResultSet rs = query.executeQuery()) {
          assertTrue(rs.next(), "no MAM sender for " + mam);
          return rs.getString(1);
        }
      }
    }

    @Test
    @DisplayName("ACTOR: all 192 events have a canonical, non-null actor and no event lacks one")
    void everyEventHasACanonicalActor() {
      assertAll(
          () -> assertEquals(192, chats.count("SELECT count(*) FROM chats.system_event")),
          () ->
              assertEquals(
                  0, chats.count("SELECT count(*) FROM chats.system_event WHERE actor_id IS NULL")),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE actor_id !~ "
                          + CANONICAL_UUID_SQL)));
    }

    @Test
    @DisplayName("ACTOR: the actor equals the content actor key for every event, per type")
    void actorMatchesContentKey() {
      assertAll(
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE actor_id IS DISTINCT FROM"
                          + " (content::json ->> "
                          + ACTOR_KEY_SQL
                          + ")")),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE (content::json ->> "
                          + ACTOR_KEY_SQL
                          + ") IS NULL")));
    }

    @Test
    @DisplayName("ACTOR: every non-creation actor is a real carbonio user of the MongooseIM source")
    void actorsAreRealSourceUsers() throws Exception {
      List<String> actors =
          chats.query(
              "SELECT DISTINCT actor_id FROM chats.system_event WHERE type <> 'ROOM_CREATED'",
              rs -> rs.getString(1));
      assertFalse(actors.isEmpty());
      try (java.sql.Connection source =
              MongooseImportDatabase.get().connect(MongooseImportDatabase.SOURCE_DATABASE);
          java.sql.PreparedStatement query =
              source.prepareStatement(
                  "SELECT count(*) FROM mam_server_user WHERE server = 'carbonio' AND user_name ="
                      + " ?")) {
        for (String actor : actors) {
          query.setString(1, actor);
          try (java.sql.ResultSet rs = query.executeQuery()) {
            rs.next();
            assertEquals(1, rs.getLong(1), actor);
          }
        }
      }
    }

    @Test
    @DisplayName("ACTOR: ROOM_CREATED actor is the creatorId, an owner of the room")
    void roomCreatedActorIsAnOwnerOfTheRoom() {
      assertAll(
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event e WHERE e.type = 'ROOM_CREATED' AND"
                          + " NOT EXISTS (SELECT 1 FROM chats.subscription s WHERE s.room_id ="
                          + " e.room_id AND s.user_id = e.actor_id AND s.owner)")),
          () ->
              assertEquals(
                  USER_B26C,
                  chats.systemEvents(ONE_TO_ONE_CREATOR_ROOM).stream()
                      .filter(e -> ROOM_CREATED.equals(e.type()))
                      .findFirst()
                      .orElseThrow()
                      .actorId()));
    }

    @Test
    @DisplayName("ACTOR: known MEMBER_ADDED, MEMBER_REMOVED, pin and unpin rows use the MAM sender")
    void knownRowsUseTheMamSender() throws Exception {
      SystemEvent added =
          event(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_ADDED_1_MAM, MEMBER_ADDED);
      SystemEvent removed =
          event(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, AF0B_MEMBER_REMOVED_MAM, MEMBER_REMOVED);
      SystemEvent pinned =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_PINNED_TEXT_NOTIFICATION_MAM,
              MESSAGE_PINNED);
      SystemEvent unpinned =
          event(
              GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
              AF0B_UNPINNED_TEXT_NOTIFICATION_MAM,
              MESSAGE_UNPINNED);

      assertAll(
          () -> assertEquals(mamSender(AF0B_MEMBER_ADDED_1_MAM), added.actorId()),
          () -> assertEquals(mamSender(AF0B_MEMBER_REMOVED_MAM), removed.actorId()),
          () -> assertEquals(USER_556F, removed.actorId()),
          () -> assertEquals(mamSender(AF0B_PINNED_TEXT_NOTIFICATION_MAM), pinned.actorId()),
          () -> assertEquals(USER_6FE7, pinned.actorId()),
          () -> assertEquals(mamSender(AF0B_UNPINNED_TEXT_NOTIFICATION_MAM), unpinned.actorId()),
          () -> assertEquals(USER_6FE7, unpinned.actorId()));
    }
  }

  @Nested
  @DisplayName("APP-12: one synthetic ROOM_CREATED per imported room, first in its timeline")
  class RoomCreated {

    @Test
    @DisplayName("APP-12: every imported room has exactly one ROOM_CREATED, nothing else does")
    void oneRoomCreatedPerImportedRoom() {
      assertAll(
          () ->
              assertEquals(
                  IMPORTED_ROOMS,
                  chats.count("SELECT count(DISTINCT room_id) FROM chats.subscription")),
          () -> assertEquals(IMPORTED_ROOMS, countType(ROOM_CREATED)),
          () ->
              assertEquals(
                  IMPORTED_ROOMS,
                  chats.count(
                      "SELECT count(DISTINCT room_id) FROM chats.system_event WHERE type = ?",
                      ROOM_CREATED)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.system_event WHERE type = ? AND room_id NOT IN"
                          + " (SELECT room_id FROM chats.subscription)",
                      ROOM_CREATED)));
    }

    @Test
    @DisplayName("APP-12: the zombie room (no subscription) gets no system event")
    void zombieRoomHasNoEvents() {
      assertEquals(IMPORTED_ROOMS, countType(ROOM_CREATED));
      assertEquals(
          0, chats.count("SELECT count(*) FROM chats.system_event WHERE room_id = ?", ZOMBIE_ROOM));
    }

    @Test
    @DisplayName("APP-12: ROOM_CREATED is the first timeline item of every imported room")
    void roomCreatedIsFirstTimelineItemOfEveryRoom() {
      List<String> rooms =
          chats.query(
              "SELECT DISTINCT room_id FROM chats.subscription ORDER BY room_id",
              rs -> rs.getString(1));
      assertEquals(IMPORTED_ROOMS, rooms.size());
      for (String room : rooms) {
        List<TimelineItem> timeline = chats.timeline(room);
        assertFalse(timeline.isEmpty(), room);
        assertEquals(ROOM_CREATED, timeline.get(0).kind(), room);
      }
    }

    @Test
    @DisplayName(
        "APP-12: creatorId is the current owner who joined first (earliest joined_at, then user"
            + " id)")
    void creatorIdIsTheEarliestCurrentOwner() {
      assertAll(
          () -> assertCreator(GROUP_PROMOTED_OWNER_ROOM, USER_4B86),
          () -> assertCreator(GROUP_PINNED_BY_REMOVED_MEMBER_ROOM, USER_556F),
          () -> assertCreator(GROUP_PIN_ON_FORWARD_ROOM, USER_E7A2),
          () -> assertCreator(GROUP_PIN_ON_REPLY_ROOM, USER_1E49),
          () -> assertCreator(ONE_TO_ONE_CREATOR_ROOM, USER_B26C));
    }

    @Test
    @DisplayName(
        "APP-12 / C1 / C3: the TEMPORARY room has ROOM_CREATED, the clear and the member removal")
    void temporaryRoomEvents() {
      List<String> types = chats.systemEvents(TEMPORARY_ROOM).stream().map(e -> e.type()).toList();
      assertEquals(Set.of(ROOM_CREATED, ROOM_HISTORY_CLEARED, MEMBER_REMOVED), Set.copyOf(types));
      assertEquals(3, types.size());
      assertEquals(ROOM_CREATED, types.get(0));
    }

    private void assertCreator(String room, String creator) {
      List<SystemEvent> created =
          chats.systemEvents(room).stream().filter(e -> ROOM_CREATED.equals(e.type())).toList();
      assertEquals(1, created.size(), room);
      assertEquals(creator, created.get(0).json().get("creatorId").asText(), room);
    }
  }

  @Nested
  @DisplayName("APP-12: system events interleave with messages in strictly increasing time")
  class Interleaving {

    @Test
    @DisplayName("APP-12: every imported timeline has strictly increasing created_at")
    void everyTimelineIsStrictlyIncreasing() {
      List<String> rooms =
          chats.query(
              "SELECT DISTINCT room_id FROM chats.subscription ORDER BY room_id",
              rs -> rs.getString(1));
      assertEquals(IMPORTED_ROOMS, rooms.size());
      for (String room : rooms) {
        List<TimelineItem> timeline = chats.timeline(room);
        assertFalse(timeline.isEmpty(), room);
        for (int i = 1; i < timeline.size(); i++) {
          assertTrue(
              timeline.get(i - 1).createdAt().isBefore(timeline.get(i).createdAt()),
              room + " item " + i + ": " + timeline.get(i - 1) + " then " + timeline.get(i));
        }
      }
    }

    @Test
    @DisplayName(
        "SEM-7: a group room timeline interleaves messages, pins and member events by MAM order")
    void groupRoomTimelineFollowsMamOrder() {
      List<String> expected = new ArrayList<>();
      String r = GROUP_PINNED_BY_REMOVED_MEMBER_ROOM;
      long[] events = {
        AF0B_MEMBER_ADDED_1_MAM, AF0B_MEMBER_ADDED_2_MAM, AF0B_MEMBER_ADDED_3_MAM,
      };
      for (long mam : events) {
        expected.add(eventId(r, mam));
      }
      for (long mam :
          new long[] {
            AF0B_TEXT_PINNED_BY_REMOVED_MEMBER_MAM,
            AF0B_TEXT_PINNED_THEN_UNPINNED_MAM,
            AF0B_TOMBSTONE_MAM,
            AF0B_TEXT_EDITED_BY_PINNED_EDIT_MAM,
            AF0B_TEXT_LAST_PINNED_MAM,
            AF0B_ATTACHMENT_MAM,
            AF0B_REPLY_MAM
          }) {
        expected.add(messageId(r, mam));
      }
      for (long mam :
          new long[] {
            AF0B_PINNED_TEXT_NOTIFICATION_MAM,
            AF0B_PINNED_2_NOTIFICATION_MAM,
            AF0B_UNPINNED_TEXT_NOTIFICATION_MAM,
            AF0B_PINNED_3_NOTIFICATION_MAM,
            AF0B_PINNED_EDIT_ROW_NOTIFICATION_MAM,
            AF0B_PINNED_ATTACHMENT_NOTIFICATION_MAM,
            AF0B_PINNED_REPLY_NOTIFICATION_MAM,
            AF0B_PINNED_TOMBSTONE_NOTIFICATION_MAM,
            AF0B_UNPINNED_TOMBSTONE_NOTIFICATION_MAM,
            AF0B_PINNED_TOMBSTONE_AGAIN_NOTIFICATION_MAM,
            AF0B_PINNED_FIRST_TEXT_NOTIFICATION_MAM,
            AF0B_PINNED_FIRST_TEXT_AGAIN_NOTIFICATION_MAM,
            AF0B_UNPINNED_FIRST_TEXT_NOTIFICATION_MAM,
            AF0B_PINNED_LAST_TEXT_NOTIFICATION_MAM,
            AF0B_MEMBER_REMOVED_MAM
          }) {
        expected.add(eventId(r, mam));
      }

      List<TimelineItem> timeline = chats.timeline(r);
      assertEquals(ROOM_CREATED, timeline.get(0).kind());
      assertEquals(expected, ids(timeline).subList(1, timeline.size()));
    }

    @Test
    @DisplayName(
        "SEM-7: the promoted-owner room timeline interleaves removals, re-add and messages")
    void promotedOwnerRoomTimelineFollowsMamOrder() {
      String r = GROUP_PROMOTED_OWNER_ROOM;
      List<String> expected =
          List.of(
              eventId(r, R7BE2_MEMBER_ADDED_1_MAM),
              eventId(r, R7BE2_MEMBER_ADDED_2_MAM),
              eventId(r, R7BE2_MEMBER_ADDED_3_MAM),
              messageId(r, R7BE2_TEXT_1_MAM),
              messageId(r, R7BE2_TEXT_2_MAM),
              eventId(r, R7BE2_REMOVED_BY_PROMOTED_OWNER_1_MAM),
              messageId(r, R7BE2_TEXT_3_MAM),
              eventId(r, R7BE2_REMOVED_BY_PROMOTED_OWNER_2_MAM),
              eventId(r, R7BE2_RE_ADDED_MAM),
              messageId(r, R7BE2_TOMBSTONE_MAM),
              messageId(r, R7BE2_TEXT_4_MAM));

      List<TimelineItem> timeline = chats.timeline(r);
      assertEquals(ROOM_CREATED, timeline.get(0).kind());
      assertEquals(expected, ids(timeline).subList(1, timeline.size()));
    }

    @Test
    @DisplayName("APP-12: MAM-derived events are stamped with the MAM instant at ms precision")
    void eventsAreStampedWithTheMamInstant() {
      assertAll(
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(AF0B_MEMBER_REMOVED_MAM),
                  event(
                          GROUP_PINNED_BY_REMOVED_MEMBER_ROOM,
                          AF0B_MEMBER_REMOVED_MAM,
                          MEMBER_REMOVED)
                      .createdAt()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(A992_PINNED_EDIT_ROW_NOTIFICATION_MAM),
                  event(
                          GROUP_PIN_ON_EDIT_OF_FORWARD_ROOM,
                          A992_PINNED_EDIT_ROW_NOTIFICATION_MAM,
                          MESSAGE_PINNED)
                      .createdAt()),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(E0DF_UNPIN_NOTIFICATION_TEXT_MAM),
                  event(
                          GROUP_PINNED_BY_NON_MEMBER_ROOM,
                          E0DF_UNPIN_NOTIFICATION_TEXT_MAM,
                          MESSAGE_UNPINNED)
                      .createdAt()));
    }
  }
}
