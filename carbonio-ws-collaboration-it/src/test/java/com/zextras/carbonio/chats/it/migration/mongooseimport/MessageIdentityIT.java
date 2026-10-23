// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Message;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: message identity and references")
class MessageIdentityIT {

  private static final Pattern CANONICAL_V4 =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

  private static final String WEB_ROOM = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String ANDROID_ROOM = "61a47ae3-8c95-47f5-8ce8-2b8c22b46402";
  private static final String MOBILE_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String TOMBSTONE_EDIT_ROOM = "5f6b4438-fdb8-48c1-a866-ff3505b8649a";
  private static final String FORWARD_ROOM = "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String EDGE_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String EDGE_OTHER_ROOM = "4df5be7a-ec3c-43f8-93ef-aae54ffdec78";
  private static final String ATTACHMENT_REPLY_ROOM = "ec84f534-92bd-4d88-923f-ab5f8e027526";
  private static final String ATTACHMENT_DUPLICATE_ROOM = "e6ff78bf-33d5-48e6-9db2-aa313e20b33e";
  private static final String ATTACHMENT_TARGET_ROOM = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String IOS_ONE_TO_ONE_ROOM = "fa0dfad7-7583-4b95-9bd4-a38e7431f1d9";
  private static final String BACKEND_ATTACHMENT_ROOM = "9688feea-bf86-4f63-b08d-a45d72f59287";
  private static final String PIN_ROOM = "af0b223e-0c55-4dff-82cc-a69149c758b0";

  private static final long WEB_TEXT_MAM = 458595356030646529L;
  private static final String WEB_TEXT_CLIENT_ID = "dfee0ad0-cf3d-4e0d-94bd-028c64625a77";
  private static final long ANDROID_TEXT_MAM = 458595986177208833L;
  private static final String ANDROID_TEXT_CLIENT_ID = "9dff9e16-655e-4638-9b03-58817ee0324c";
  private static final long RN_TEXT_MAM = 458596168191258369L;
  private static final String RN_TEXT_CLIENT_ID = "8e4a1323-3e89-4bc8-ab00-294b9e27e4dc";

  private static final long IOS_TEXT_MAM = 458596166264453377L;
  private static final String IOS_TEXT_CLIENT_ID = "CA2AD8C5-FF32-4735-ACB8-424B37746A52";
  private static final long IOS_ONE_TO_ONE_TEXT_MAM = 458595960606606337L;
  private static final String IOS_ONE_TO_ONE_TEXT_CLIENT_ID =
      "48539D7F-4D6E-4780-8C25-B593F7641A59";
  private static final long IOS_ATTACHMENT_MAM = 458596201721417985L;
  private static final String IOS_ATTACHMENT_CLIENT_ID = "8EB2D491-A41D-4011-A8FC-3544F9667ECF";
  private static final long IOS_TOMBSTONE_MAM = 458596167469571073L;
  private static final String IOS_TOMBSTONE_CLIENT_ID = "5B01A36C-A7D8-47B1-9D81-8B94347AB734";

  private static final long BACKEND_FORWARD_MAM = 458596133384694529L;
  private static final long BACKEND_ATTACHMENT_MAM = 456005033677711873L;
  private static final long BACKEND_TOMBSTONE_MAM = 458596134044895233L;
  private static final long BACKEND_MEMBER_ADDED_MAM = 458596073786111489L;
  private static final long BACKEND_MEMBER_REMOVED_MAM = 458596081390031617L;

  private static final long PIN_V1_MAM = 458595361294167553L;
  private static final long PIN_V1_ON_EDIT_MAM = 458595361459130369L;
  private static final long UNPIN_LOCALHOST_ON_EDIT_MAM = 458595361730871553L;
  private static final long PIN_ROOM_PINNED_MAM = 458596113273188865L;
  private static final long PIN_ROOM_UNPINNED_MAM = 458596114144042497L;
  private static final long PIN_TARGET_MAM = 458595355979477505L;
  private static final long PIN_ROOM_PINNED_TARGET_MAM = 458596110979159553L;
  private static final long PIN_ROOM_UNPINNED_TARGET_MAM = 458596111218734849L;

  private static final long NON_UUID_STANZA_LOOKALIKE_MAM = 458596912817732865L;
  private static final long NON_UUID_DASHED_NUMBERS_MAM = 458596913217681409L;
  private static final long NON_UUID_ABC_MAM = 458596913716004865L;
  private static final long NON_UUID_CYRILLIC_EMOJI_MAM = 458596913816851969L;
  private static final long NON_UUID_SLASHES_MAM = 458596914016171777L;
  private static final long NON_UUID_SEND_IQ_SUFFIX_MAM = 458596914115822081L;
  private static final long NON_UUID_PATH_TRAVERSAL_MAM = 458596914215452417L;
  private static final long NON_UUID_TAB_AND_LINE_FEED_MAM = 458596914315024897L;
  private static final long NON_UUID_ATTACHMENT_MAM = 458596926361368577L;
  private static final long NON_UUID_ATTACHMENT_OTHER_ROOM_MAM = 458597020134006529L;

  private static final long LENGTH_64_MAM = 458596915685807617L;
  private static final long LENGTH_65_MAM = 458596915913443073L;
  private static final long LENGTH_83_ROOM_AT_LOCALHOST_MAM = 458596913015708417L;
  private static final long LENGTH_100_MAM = 458596916142238977L;
  private static final long LENGTH_300_MAM = 458596916371206145L;
  private static final long LENGTH_1000_MAM = 458596916600105985L;
  private static final long LENGTH_3000_MAM = 458596916829001217L;
  private static final long LENGTH_70000_MAM = 458596917068846337L;
  private static final long LENGTH_100_ATTACHMENT_MAM = 458596926410625537L;

  private static final long EMPTY_ID_FIRST_MAM = 458596913316400385L;
  private static final long EMPTY_ID_SECOND_MAM = 458596913417056513L;
  private static final long BLANK_ID_MAM = 458596913516589057L;
  private static final long MISSING_ID_ATTRIBUTE_MAM = 458596913916429569L;

  private static final long DUPLICATE_SAME_SENDER_FIRST_MAM = 458596910664776705L;
  private static final long DUPLICATE_SAME_SENDER_SECOND_MAM = 458596910764379393L;
  private static final long DUPLICATE_SAME_SENDER_THIRD_MAM = 458596910865479937L;
  private static final long DUPLICATE_OTHER_SENDER_FIRST_MAM = 458596910965024513L;
  private static final long DUPLICATE_OTHER_SENDER_SECOND_MAM = 458596911276023041L;
  private static final long DUPLICATE_OTHER_ROOM_FIRST_ROOM_MAM = 458596911377067521L;
  private static final long DUPLICATE_OTHER_ROOM_SECOND_ROOM_MAM = 458596911476750081L;
  private static final long DUPLICATE_OTHER_ROOM_SECOND_ROOM_OTHER_SENDER_MAM = 458596911790366465L;
  private static final long ATTACHMENT_DUPLICATE_FIRST_MAM = 458596926458562561L;
  private static final long ATTACHMENT_DUPLICATE_SECOND_MAM = 458596926505754881L;
  private static final long ATTACHMENT_DUPLICATE_THIRD_MAM = 458596926601204993L;
  private static final long ATTACHMENT_DUPLICATE_ACROSS_ROOMS_FIRST_MAM = 458597019542200577L;
  private static final long ATTACHMENT_DUPLICATE_ACROSS_ROOMS_SECOND_MAM = 458597019738341377L;
  private static final long ATTACHMENT_DUPLICATE_ACROSS_ROOMS_OTHER_ROOM_MAM = 458597023449622529L;
  private static final long TOMBSTONE_SHARED_ID_TOMBSTONE_MAM = 458596906494726913L;
  private static final long TOMBSTONE_SHARED_ID_TEXT_MAM = 458596913118003201L;
  private static final long AFFILIATION_SHARED_ID_AFFILIATION_MAM = 458596905518453761L;
  private static final long AFFILIATION_SHARED_ID_TEXT_MAM = 458596912916569857L;

  private static final long LEGACY_REPLACE_FIRST_MAM = 458596170111340801L;
  private static final long LEGACY_REPLACE_SECOND_MAM = 458596922489575937L;
  private static final long LEGACY_REPLACE_THIRD_MAM = 458596922590568705L;
  private static final long LEGACY_RETRACT_FIRST_MAM = 458596170352381185L;
  private static final long LEGACY_RETRACT_SECOND_MAM = 458596917667859969L;
  private static final long LEGACY_RETRACT_THIRD_MAM = 458596924908881153L;
  private static final long MOBILE_ROOM_ANCHOR_MAM = 458596169871606273L;
  private static final long EDGE_ROOM_ANCHOR_MAM = 458596906805728513L;

  private static final long WEB_REPLY_MAM = 458595356030646529L;
  private static final long WEB_REPLY_TARGET_MAM = 458595355979477505L;
  private static final long ANDROID_REPLY_MAM = 458595986177208833L;
  private static final long ANDROID_REPLY_TARGET_MAM = 458595985723873025L;
  private static final long ANDROID_SECOND_REPLY_MAM = 458595994228325889L;
  private static final long ANDROID_SECOND_REPLY_TARGET_MAM = 458595985868840193L;
  private static final long RN_REPLY_MAM = 458596168191258369L;
  private static final long RN_REPLY_TARGET_MAM = 458596163856904961L;
  private static final long IOS_REPLY_BEFORE_BODY_MAM = 458596166505477633L;
  private static final long IOS_REPLY_BEFORE_BODY_TARGET_MAM = 458596164097944321L;
  private static final long IOS_SECOND_REPLY_BEFORE_BODY_MAM = 458596169871606273L;
  private static final long IOS_SECOND_REPLY_BEFORE_BODY_TARGET_MAM = 458596167950717441L;
  private static final long ANDROID_REPLY_TO_ATTACHMENT_MAM = 458596076208896769L;
  private static final long ANDROID_REPLY_ATTACHMENT_TARGET_MAM = 458596075942288385L;
  private static final long IOS_REPLY_TO_ATTACHMENT_MAM = 458596172124248833L;
  private static final long IOS_REPLY_ATTACHMENT_TARGET_MAM = 458596170883761153L;
  private static final long REPLY_TO_FORWARD_MAM = 458596140572131841L;
  private static final long REPLY_TO_FORWARD_TARGET_MAM = 458596133384694529L;
  private static final long REPLY_TO_TOMBSTONE_MAM = 458596077236215809L;
  private static final long REPLY_TO_TOMBSTONE_TARGET_MAM = 458596076756919041L;
  private static final long SECOND_REPLY_TO_TOMBSTONE_MAM = 458596919815387905L;
  private static final long SECOND_REPLY_TO_TOMBSTONE_TARGET_MAM = 458596906904507393L;
  private static final long REPLY_TO_EDIT_ROW_MAM = 458596078681000961L;
  private static final long REPLY_TO_EDIT_ROW_EDIT_MAM = 458596078200317697L;
  private static final long REPLY_TO_EDIT_ROW_ORIGINAL_MAM = 458596077475982849L;
  private static final long REPLY_WRONG_NAMESPACE_MAM = 458596920927591681L;
  private static final long REPLY_NESTED_IN_FORWARD_MAM = 458596933371849729L;
  private static final long REPLY_GHOST_STANZA_ID_MAM = 458596918685765633L;
  private static final long REPLY_GHOST_STANZA_ID_SECOND_MAM = 458597019147780353L;
  private static final long REPLY_ZZZ_STANZA_ID_MAM = 458596929172156673L;
  private static final long REPLY_NOT_A_STANZA_ID_MAM = 458596918786743809L;
  private static final long REPLY_WITHOUT_ID_MAM = 458596918889091585L;
  private static final long REPLY_EMPTY_ID_MAM = 458596918988678657L;
  private static final long REPLY_TO_AFFILIATION_ROW_MAM = 458596919402156289L;
  private static final long REPLY_TO_RETRACT_REQUEST_MAM = 458596920215340545L;
  private static final long RETRACT_REQUEST_ROW_MAM = 458596908102407937L;
  private static final long REPLY_TO_OTHER_ROOM_MAM = 458596919302474497L;
  private static final long REPLY_TO_OTHER_ROOM_TARGET_MAM = 458596908393360641L;
  private static final long ATTACHMENT_REPLY_TO_OTHER_ROOM_MAM = 458596929223660801L;
  private static final long ATTACHMENT_REPLY_OTHER_ROOM_TARGET_MAM = 458596908382690049L;
  private static final long SELF_REPLY_MAM = 458596920628715521L;
  private static final long REPLY_TO_CLIENT_ID_LOOKALIKE_MAM = 458596920729720065L;
  private static final long REPLY_TO_CLIENT_ID_LOOKALIKE_TARGET_MAM = 458596906494726913L;

  private static final long CLIENT_ID_REPLY_MOBILE_MAM = 458596164578619905L;
  private static final long CLIENT_ID_REPLY_MOBILE_TARGET_MAM = 458596163856904961L;
  private static final long CLIENT_ID_REPLY_ATTACHMENT_MAM = 458596929272633345L;
  private static final long CLIENT_ID_REPLY_ATTACHMENT_TARGET_MAM = 458596928844845057L;
  private static final long CLIENT_ID_REPLY_EDGE_MAM = 458596919501694977L;
  private static final long CLIENT_ID_REPLY_EDGE_TARGET_MAM = 458596906805728513L;

  private static ImportRun run;
  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  private static Message message(String roomId, long mamId) {
    return chats
        .message(roomId, mamId)
        .orElseThrow(
            () ->
                new AssertionError(
                    "message not imported: room " + roomId + " mam " + mamId + " " + importNote()));
  }

  private static SystemEvent systemEvent(String roomId, long mamId) {
    return chats
        .systemEvent(roomId, mamId)
        .orElseThrow(
            () ->
                new AssertionError(
                    "system event not imported: room "
                        + roomId
                        + " mam "
                        + mamId
                        + " "
                        + importNote()));
  }

  private static String importNote() {
    return run.succeeded() ? "" : "(import failed: " + run.failureText() + ")";
  }

  private static void assertOwnMessageId(String roomId, long mamId) {
    Message message = message(roomId, mamId);
    assertAll(
        () -> assertEquals(ExpectedIds.message(roomId, mamId), message.id()),
        () -> assertEquals(roomId, message.roomId()),
        () -> assertTrue(CANONICAL_V4.matcher(message.id()).matches(), message.id()));
  }

  private static void assertOwnMessageId(String roomId, long mamId, String clientId) {
    Message message = message(roomId, mamId);
    assertAll(
        () -> assertOwnMessageId(roomId, mamId),
        () -> assertNotEquals(clientId, message.id()),
        () -> assertNotEquals(clientId.toLowerCase(), message.id()));
  }

  private static void assertOwnSystemEventId(String roomId, long mamId) {
    SystemEvent event = systemEvent(roomId, mamId);
    assertAll(
        () -> assertEquals(ExpectedIds.systemEvent(roomId, mamId), event.id()),
        () -> assertEquals(roomId, event.roomId()),
        () -> assertTrue(CANONICAL_V4.matcher(event.id()).matches(), event.id()));
  }

  private static void assertRepliesTo(String roomId, long referrerMam, long targetMam) {
    Message referrer = message(roomId, referrerMam);
    Message target = message(roomId, targetMam);
    assertEquals(ExpectedIds.message(roomId, targetMam), target.id());
    assertEquals(target.id(), referrer.replyToId());
  }

  private static void assertImportedWithoutReply(String roomId, long referrerMam) {
    Message referrer = message(roomId, referrerMam);
    assertNull(referrer.replyToId());
  }

  private static void assertNeverImported(String roomId, long anchorMam, long mamId) {
    message(roomId, anchorMam);
    assertAll(
        () -> assertTrue(chats.message(roomId, mamId).isEmpty(), "imported as message"),
        () -> assertTrue(chats.systemEvent(roomId, mamId).isEmpty(), "imported as system event"));
  }

  @Nested
  @DisplayName("ID-1: every imported id is the minted id of its MAM row, never the client id")
  class MintedIds {

    @Test
    @DisplayName("ID-1: web lowercase uuid client id is replaced by the expected id")
    void webLowercaseUuid() {
      assertOwnMessageId(WEB_ROOM, WEB_TEXT_MAM, WEB_TEXT_CLIENT_ID);
    }

    @Test
    @DisplayName("ID-1: Android lowercase uuid client id is replaced by the expected id")
    void androidLowercaseUuid() {
      assertOwnMessageId(ANDROID_ROOM, ANDROID_TEXT_MAM, ANDROID_TEXT_CLIENT_ID);
    }

    @Test
    @DisplayName("ID-1: RN lowercase uuid client id is replaced by the expected id")
    void rnLowercaseUuid() {
      assertOwnMessageId(MOBILE_ROOM, RN_TEXT_MAM, RN_TEXT_CLIENT_ID);
    }

    @Test
    @DisplayName("ID-1: iOS UPPERCASE uuid text messages get the expected lowercase v4 id")
    void iosUppercaseText() {
      assertAll(
          () -> assertOwnMessageId(MOBILE_ROOM, IOS_TEXT_MAM, IOS_TEXT_CLIENT_ID),
          () ->
              assertOwnMessageId(
                  IOS_ONE_TO_ONE_ROOM, IOS_ONE_TO_ONE_TEXT_MAM, IOS_ONE_TO_ONE_TEXT_CLIENT_ID));
    }

    @Test
    @DisplayName("ID-1: iOS UPPERCASE uuid attachment gets the expected id")
    void iosUppercaseAttachment() {
      assertOwnMessageId(MOBILE_ROOM, IOS_ATTACHMENT_MAM, IOS_ATTACHMENT_CLIENT_ID);
    }

    @Test
    @DisplayName("ID-1: iOS UPPERCASE uuid tombstone keeps the expected id and is deleted")
    void iosUppercaseTombstone() {
      assertOwnMessageId(MOBILE_ROOM, IOS_TOMBSTONE_MAM, IOS_TOMBSTONE_CLIENT_ID);
      assertTrue(message(MOBILE_ROOM, IOS_TOMBSTONE_MAM).deleted());
    }

    @Test
    @DisplayName("ID-1: backend 16-hex forward gets the expected id")
    void backendHexForward() {
      assertOwnMessageId(FORWARD_ROOM, BACKEND_FORWARD_MAM, "bd7608c2cedfa4ea");
    }

    @Test
    @DisplayName("ID-1: backend 16-hex attachment without messageId gets the expected id")
    void backendHexAttachment() {
      assertOwnMessageId(BACKEND_ATTACHMENT_ROOM, BACKEND_ATTACHMENT_MAM, "e5a64ccd89d274df");
    }

    @Test
    @DisplayName("ID-1: backend 16-hex tombstone gets the expected id and is deleted")
    void backendHexTombstone() {
      assertOwnMessageId(FORWARD_ROOM, BACKEND_TOMBSTONE_MAM, "aa44c574a8c8fac8");
      assertTrue(message(FORWARD_ROOM, BACKEND_TOMBSTONE_MAM).deleted());
    }

    @Test
    @DisplayName("ID-1: backend 16-hex memberAdded and memberRemoved get the expected event ids")
    void backendHexMemberEvents() {
      assertAll(
          () -> assertOwnSystemEventId(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_ADDED_MAM),
          () -> assertOwnSystemEventId(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_REMOVED_MAM),
          () ->
              assertEquals(
                  "MEMBER_ADDED",
                  systemEvent(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_ADDED_MAM).type()),
          () ->
              assertEquals(
                  "MEMBER_REMOVED",
                  systemEvent(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_REMOVED_MAM).type()),
          () ->
              assertTrue(
                  chats.message(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_ADDED_MAM).isEmpty()
                      && chats.message(TOMBSTONE_EDIT_ROOM, BACKEND_MEMBER_REMOVED_MAM).isEmpty()));
    }

    @Test
    @DisplayName("ID-1: uuid v1 pin notifications get the expected event ids")
    void uuidV1PinNotifications() {
      assertAll(
          () -> assertOwnSystemEventId(WEB_ROOM, PIN_V1_MAM),
          () -> assertOwnSystemEventId(PIN_ROOM, PIN_ROOM_PINNED_MAM),
          () -> assertEquals("MESSAGE_PINNED", systemEvent(WEB_ROOM, PIN_V1_MAM).type()),
          () -> assertEquals("MESSAGE_PINNED", systemEvent(PIN_ROOM, PIN_ROOM_PINNED_MAM).type()));
    }

    @Test
    @DisplayName("ID-1: room@localhost unpin notifications get the expected event ids")
    void roomAtLocalhostUnpinNotifications() {
      assertAll(
          () -> assertOwnSystemEventId(WEB_ROOM, UNPIN_LOCALHOST_ON_EDIT_MAM),
          () -> assertOwnSystemEventId(PIN_ROOM, PIN_ROOM_UNPINNED_MAM),
          () ->
              assertEquals(
                  "MESSAGE_UNPINNED", systemEvent(WEB_ROOM, UNPIN_LOCALHOST_ON_EDIT_MAM).type()),
          () ->
              assertEquals(
                  "MESSAGE_UNPINNED", systemEvent(PIN_ROOM, PIN_ROOM_UNPINNED_MAM).type()));
    }

    @Test
    @DisplayName("ID-1: client ids that are not uuids at all get the expected ids")
    void nonUuidClientIds() {
      assertAll(
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_STANZA_LOOKALIKE_MAM, "CNA3EJLBC4O1"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_DASHED_NUMBERS_MAM, "1791-394192-259343"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_ABC_MAM, "abc"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_CYRILLIC_EMOJI_MAM, "идентификатор-😀-é"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_SLASHES_MAM, "a/b@c/d"),
          () ->
              assertOwnMessageId(
                  EDGE_ROOM,
                  NON_UUID_SEND_IQ_SUFFIX_MAM,
                  "444fc7da-52cf-479b-ae18-2c5ea5225159:sendIQ"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_PATH_TRAVERSAL_MAM, "..\\..\\x"));
    }

    @Test
    @DisplayName("ID-1: a client id with raw TAB and LF gets the expected id")
    void clientIdWithTabAndLineFeed() {
      assertOwnMessageId(EDGE_ROOM, NON_UUID_TAB_AND_LINE_FEED_MAM, "a\nb\tc");
    }

    @Test
    @DisplayName("ID-1: attachments with the client id 'not-a-uuid' get the expected ids")
    void notAUuidAttachments() {
      assertAll(
          () ->
              assertOwnMessageId(ATTACHMENT_DUPLICATE_ROOM, NON_UUID_ATTACHMENT_MAM, "not-a-uuid"),
          () -> assertOwnMessageId(EDGE_ROOM, NON_UUID_ATTACHMENT_OTHER_ROOM_MAM, "not-a-uuid"));
    }

    @Test
    @DisplayName("ID-1: client ids longer than 64 chars (65 to 70000) get the expected 36-char ids")
    void clientIdsLongerThan64() {
      assertAll(
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_65_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_83_ROOM_AT_LOCALHOST_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_100_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_300_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_1000_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_3000_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, LENGTH_70000_MAM),
          () -> assertOwnMessageId(ATTACHMENT_DUPLICATE_ROOM, LENGTH_100_ATTACHMENT_MAM));
    }

    @Test
    @DisplayName("ID-1: the 64-char client id boundary gets the expected id")
    void clientIdOfExactly64() {
      assertOwnMessageId(EDGE_ROOM, LENGTH_64_MAM);
    }

    @Test
    @DisplayName("ID-1: empty, blank and missing client ids get the expected ids")
    void emptyClientIds() {
      assertAll(
          () -> assertOwnMessageId(EDGE_ROOM, EMPTY_ID_FIRST_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, EMPTY_ID_SECOND_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, BLANK_ID_MAM),
          () -> assertOwnMessageId(EDGE_ROOM, MISSING_ID_ATTRIBUTE_MAM));
    }

    @Test
    @DisplayName("ID-1: rows with empty client ids do not share one id")
    void emptyClientIdsAreDistinct() {
      List<String> ids =
          List.of(
              message(EDGE_ROOM, EMPTY_ID_FIRST_MAM).id(),
              message(EDGE_ROOM, EMPTY_ID_SECOND_MAM).id(),
              message(EDGE_ROOM, BLANK_ID_MAM).id(),
              message(EDGE_ROOM, MISSING_ID_ATTRIBUTE_MAM).id());
      assertEquals(4, ids.stream().distinct().count());
    }

    @Test
    @DisplayName("ID-1: legacy XEP-0308 replace rows are never imported as messages")
    void legacyReplaceRowsAreNeverMessages() {
      assertAll(
          () -> assertNeverImported(MOBILE_ROOM, MOBILE_ROOM_ANCHOR_MAM, LEGACY_REPLACE_FIRST_MAM),
          () -> assertNeverImported(EDGE_ROOM, EDGE_ROOM_ANCHOR_MAM, LEGACY_REPLACE_SECOND_MAM),
          () -> assertNeverImported(EDGE_ROOM, EDGE_ROOM_ANCHOR_MAM, LEGACY_REPLACE_THIRD_MAM));
    }

    @Test
    @DisplayName("ID-1: legacy XEP-0424 retract rows are never imported as messages")
    void legacyRetractRowsAreNeverMessages() {
      assertAll(
          () -> assertNeverImported(MOBILE_ROOM, MOBILE_ROOM_ANCHOR_MAM, LEGACY_RETRACT_FIRST_MAM),
          () -> assertNeverImported(EDGE_ROOM, EDGE_ROOM_ANCHOR_MAM, LEGACY_RETRACT_SECOND_MAM),
          () -> assertNeverImported(EDGE_ROOM, EDGE_ROOM_ANCHOR_MAM, LEGACY_RETRACT_THIRD_MAM));
    }
  }

  @Nested
  @DisplayName("ID-4: duplicate client ids keep one message per archived row")
  class DuplicateClientIds {

    private void assertDistinctOwnMessages(String roomId, long... mams) {
      List<String> ids = new ArrayList<>();
      for (long mam : mams) {
        assertOwnMessageId(roomId, mam);
        ids.add(message(roomId, mam).id());
      }
      assertEquals(mams.length, ids.stream().distinct().count());
    }

    @Test
    @DisplayName("ID-4: three copies of one client id from the same sender in one room all survive")
    void sameRoomSameSender() {
      assertDistinctOwnMessages(
          EDGE_ROOM,
          DUPLICATE_SAME_SENDER_FIRST_MAM,
          DUPLICATE_SAME_SENDER_SECOND_MAM,
          DUPLICATE_SAME_SENDER_THIRD_MAM);
    }

    @Test
    @DisplayName("ID-4: the same client id from two senders in one room gives two messages")
    void sameRoomOtherSender() {
      assertDistinctOwnMessages(
          EDGE_ROOM, DUPLICATE_OTHER_SENDER_FIRST_MAM, DUPLICATE_OTHER_SENDER_SECOND_MAM);
      assertNotEquals(
          message(EDGE_ROOM, DUPLICATE_OTHER_SENDER_FIRST_MAM).senderId(),
          message(EDGE_ROOM, DUPLICATE_OTHER_SENDER_SECOND_MAM).senderId());
    }

    @Test
    @DisplayName("ID-4: the same client id in two rooms gives one message per room")
    void otherRoom() {
      assertAll(
          () -> assertOwnMessageId(EDGE_ROOM, DUPLICATE_OTHER_ROOM_FIRST_ROOM_MAM),
          () -> assertOwnMessageId(EDGE_OTHER_ROOM, DUPLICATE_OTHER_ROOM_SECOND_ROOM_MAM),
          () ->
              assertOwnMessageId(
                  EDGE_OTHER_ROOM, DUPLICATE_OTHER_ROOM_SECOND_ROOM_OTHER_SENDER_MAM));
    }

    @Test
    @DisplayName("ID-4: duplicate attachment client ids in one room all survive")
    void attachmentCopiesSameRoom() {
      assertDistinctOwnMessages(
          ATTACHMENT_DUPLICATE_ROOM,
          ATTACHMENT_DUPLICATE_FIRST_MAM,
          ATTACHMENT_DUPLICATE_SECOND_MAM,
          ATTACHMENT_DUPLICATE_THIRD_MAM);
    }

    @Test
    @DisplayName("ID-4: duplicate attachment client ids across two rooms all survive")
    void attachmentCopiesAcrossRooms() {
      assertAll(
          () ->
              assertDistinctOwnMessages(
                  EDGE_ROOM,
                  ATTACHMENT_DUPLICATE_ACROSS_ROOMS_FIRST_MAM,
                  ATTACHMENT_DUPLICATE_ACROSS_ROOMS_SECOND_MAM),
          () ->
              assertOwnMessageId(
                  EDGE_OTHER_ROOM, ATTACHMENT_DUPLICATE_ACROSS_ROOMS_OTHER_ROOM_MAM));
    }

    @Test
    @DisplayName("ID-4: a text sharing its client id with a tombstone is a separate live message")
    void textSharingTombstoneClientId() {
      Message tombstone = message(EDGE_ROOM, TOMBSTONE_SHARED_ID_TOMBSTONE_MAM);
      Message text = message(EDGE_ROOM, TOMBSTONE_SHARED_ID_TEXT_MAM);
      assertAll(
          () -> assertNotEquals(tombstone.id(), text.id()),
          () ->
              assertEquals(
                  ExpectedIds.message(EDGE_ROOM, TOMBSTONE_SHARED_ID_TOMBSTONE_MAM),
                  tombstone.id()),
          () ->
              assertEquals(ExpectedIds.message(EDGE_ROOM, TOMBSTONE_SHARED_ID_TEXT_MAM), text.id()),
          () -> assertTrue(tombstone.deleted()),
          () -> assertFalse(text.deleted()));
    }

    @Test
    @DisplayName("ID-4: a text sharing its client id with an affiliation row is imported alone")
    void textSharingAffiliationClientId() {
      assertOwnMessageId(EDGE_ROOM, AFFILIATION_SHARED_ID_TEXT_MAM);
      assertAll(
          () ->
              assertTrue(chats.message(EDGE_ROOM, AFFILIATION_SHARED_ID_AFFILIATION_MAM).isEmpty()),
          () ->
              assertTrue(
                  chats.systemEvent(EDGE_ROOM, AFFILIATION_SHARED_ID_AFFILIATION_MAM).isEmpty()));
    }
  }

  @Nested
  @DisplayName("ID-2: stanza-id references resolve to the target's new id")
  class StanzaReferences {

    @Test
    @DisplayName("ID-2: web reply with a stray '}' in @to resolves to the target")
    void webReplyWithStrayBrace() {
      assertRepliesTo(WEB_ROOM, WEB_REPLY_MAM, WEB_REPLY_TARGET_MAM);
    }

    @Test
    @DisplayName("ID-2: RN reply with the sender jid in @to resolves to the target")
    void rnReply() {
      assertRepliesTo(MOBILE_ROOM, RN_REPLY_MAM, RN_REPLY_TARGET_MAM);
    }

    @Test
    @DisplayName("ID-2: Android reply without @to resolves to the target")
    void androidReplyWithoutTo() {
      assertAll(
          () -> assertRepliesTo(ANDROID_ROOM, ANDROID_REPLY_MAM, ANDROID_REPLY_TARGET_MAM),
          () ->
              assertRepliesTo(
                  ANDROID_ROOM, ANDROID_SECOND_REPLY_MAM, ANDROID_SECOND_REPLY_TARGET_MAM));
    }

    @Test
    @DisplayName("ID-2: iOS reply placed before the body resolves to the target")
    void iosReplyBeforeBody() {
      assertAll(
          () ->
              assertRepliesTo(
                  MOBILE_ROOM, IOS_REPLY_BEFORE_BODY_MAM, IOS_REPLY_BEFORE_BODY_TARGET_MAM),
          () ->
              assertRepliesTo(
                  MOBILE_ROOM,
                  IOS_SECOND_REPLY_BEFORE_BODY_MAM,
                  IOS_SECOND_REPLY_BEFORE_BODY_TARGET_MAM));
    }

    @Test
    @DisplayName("ID-2: replies to an attachment message resolve to the attachment")
    void replyToAttachment() {
      assertAll(
          () ->
              assertRepliesTo(
                  TOMBSTONE_EDIT_ROOM,
                  ANDROID_REPLY_TO_ATTACHMENT_MAM,
                  ANDROID_REPLY_ATTACHMENT_TARGET_MAM),
          () ->
              assertRepliesTo(
                  MOBILE_ROOM, IOS_REPLY_TO_ATTACHMENT_MAM, IOS_REPLY_ATTACHMENT_TARGET_MAM));
    }

    @Test
    @DisplayName("ID-2: reply to a forward resolves to the forward message")
    void replyToForward() {
      assertRepliesTo(FORWARD_ROOM, REPLY_TO_FORWARD_MAM, REPLY_TO_FORWARD_TARGET_MAM);
    }

    @Test
    @DisplayName("ID-2: reply to a tombstoned message resolves to the deleted message")
    void replyToTombstone() {
      assertAll(
          () ->
              assertRepliesTo(
                  TOMBSTONE_EDIT_ROOM, REPLY_TO_TOMBSTONE_MAM, REPLY_TO_TOMBSTONE_TARGET_MAM),
          () ->
              assertRepliesTo(
                  EDGE_ROOM, SECOND_REPLY_TO_TOMBSTONE_MAM, SECOND_REPLY_TO_TOMBSTONE_TARGET_MAM),
          () -> assertTrue(message(TOMBSTONE_EDIT_ROOM, REPLY_TO_TOMBSTONE_TARGET_MAM).deleted()),
          () -> assertTrue(message(EDGE_ROOM, SECOND_REPLY_TO_TOMBSTONE_TARGET_MAM).deleted()));
    }

    @Test
    @DisplayName("ID-2: reply to an edit row resolves to the original message")
    void replyToEditRow() {
      assertAll(
          () ->
              assertRepliesTo(
                  TOMBSTONE_EDIT_ROOM, REPLY_TO_EDIT_ROW_MAM, REPLY_TO_EDIT_ROW_ORIGINAL_MAM),
          () ->
              assertTrue(
                  chats.message(TOMBSTONE_EDIT_ROOM, REPLY_TO_EDIT_ROW_EDIT_MAM).isEmpty(),
                  "the edit row must not be a message"));
    }

    @Test
    @DisplayName("ID-2: reply to another room's stanza-id is imported with a NULL reply_to_id")
    void replyToOtherRoom() {
      assertAll(
          () -> message(EDGE_OTHER_ROOM, REPLY_TO_OTHER_ROOM_TARGET_MAM),
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_TO_OTHER_ROOM_MAM),
          () -> message(ATTACHMENT_TARGET_ROOM, ATTACHMENT_REPLY_OTHER_ROOM_TARGET_MAM),
          () ->
              assertImportedWithoutReply(
                  ATTACHMENT_REPLY_ROOM, ATTACHMENT_REPLY_TO_OTHER_ROOM_MAM));
    }

    @Test
    @DisplayName("ID-2: reply to a stanza-id that was never archived is imported with NULL")
    void replyToUnknownStanzaId() {
      assertAll(
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_GHOST_STANZA_ID_MAM),
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_GHOST_STANZA_ID_SECOND_MAM),
          () -> assertImportedWithoutReply(ATTACHMENT_REPLY_ROOM, REPLY_ZZZ_STANZA_ID_MAM));
    }

    @Test
    @DisplayName("ID-2: reply with an undecodable, empty or missing id is imported with NULL")
    void replyWithMalformedReference() {
      assertAll(
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_NOT_A_STANZA_ID_MAM),
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_WITHOUT_ID_MAM),
          () -> assertImportedWithoutReply(EDGE_ROOM, REPLY_EMPTY_ID_MAM));
    }

    @Test
    @DisplayName("ID-2: reply to an affiliation row (never a message) is imported with NULL")
    void replyToAffiliationRow() {
      assertImportedWithoutReply(EDGE_ROOM, REPLY_TO_AFFILIATION_ROW_MAM);
      assertTrue(chats.message(EDGE_ROOM, AFFILIATION_SHARED_ID_AFFILIATION_MAM).isEmpty());
    }

    @Test
    @DisplayName("ID-2: reply to a retract request row (never a message) is imported with NULL")
    void replyToRetractRequestRow() {
      assertImportedWithoutReply(EDGE_ROOM, REPLY_TO_RETRACT_REQUEST_MAM);
      assertTrue(chats.message(EDGE_ROOM, RETRACT_REQUEST_ROW_MAM).isEmpty());
    }

    @Test
    @DisplayName("ID-2: a reply element in a foreign namespace is ignored")
    void replyInWrongNamespace() {
      assertImportedWithoutReply(EDGE_ROOM, REPLY_WRONG_NAMESPACE_MAM);
    }

    @Test
    @DisplayName("ID-2: a reply nested inside a forwarded message is not the outer reply")
    void replyNestedInForward() {
      assertImportedWithoutReply(EDGE_ROOM, REPLY_NESTED_IN_FORWARD_MAM);
    }

    @Test
    @DisplayName("ID-2: a client id equal to a stanza-id does not capture the stanza-id references")
    void clientIdEqualToStanzaIdDoesNotConfuseReplies() {
      Message lookalike = message(EDGE_ROOM, NON_UUID_STANZA_LOOKALIKE_MAM);
      assertAll(
          () ->
              assertRepliesTo(
                  EDGE_ROOM,
                  REPLY_TO_CLIENT_ID_LOOKALIKE_MAM,
                  REPLY_TO_CLIENT_ID_LOOKALIKE_TARGET_MAM),
          () ->
              assertNotEquals(
                  lookalike.id(), message(EDGE_ROOM, REPLY_TO_CLIENT_ID_LOOKALIKE_MAM).replyToId()),
          () -> assertTrue(message(EDGE_ROOM, REPLY_TO_CLIENT_ID_LOOKALIKE_TARGET_MAM).deleted()));
    }

    @Test
    @DisplayName("ID-2: a reply to its own stanza-id or client id is imported with NULL")
    void selfReply() {
      assertImportedWithoutReply(EDGE_ROOM, SELF_REPLY_MAM);
    }
  }

  @Nested
  @DisplayName("ID-3 / C7: replies addressed by client id are normal messages without quote")
  class ClientIdReferences {

    @Test
    @DisplayName("ID-3 / C7: mobile text reply by client id has no reply_to_id")
    void textReplyByClientId() {
      message(MOBILE_ROOM, CLIENT_ID_REPLY_MOBILE_TARGET_MAM);
      assertImportedWithoutReply(MOBILE_ROOM, CLIENT_ID_REPLY_MOBILE_MAM);
    }

    @Test
    @DisplayName("ID-3 / C7: mobile attachment reply by client id has no reply_to_id")
    void attachmentReplyByClientId() {
      message(ATTACHMENT_REPLY_ROOM, CLIENT_ID_REPLY_ATTACHMENT_TARGET_MAM);
      assertImportedWithoutReply(ATTACHMENT_REPLY_ROOM, CLIENT_ID_REPLY_ATTACHMENT_MAM);
    }

    @Test
    @DisplayName("ID-3 / C7: reply by client id in the edge room has no reply_to_id")
    void replyByClientIdInEdgeRoom() {
      message(EDGE_ROOM, CLIENT_ID_REPLY_EDGE_TARGET_MAM);
      assertImportedWithoutReply(EDGE_ROOM, CLIENT_ID_REPLY_EDGE_MAM);
    }

    @Test
    @DisplayName("ID-3 / C7: a stanza-id reply to the same target keeps its quote")
    void stanzaIdReplyKeepsTheQuote() {
      assertAll(
          () -> assertNotNull(message(MOBILE_ROOM, RN_REPLY_MAM).replyToId()),
          () -> assertNull(message(MOBILE_ROOM, CLIENT_ID_REPLY_MOBILE_MAM).replyToId()));
    }
  }

  @Nested
  @DisplayName("ID-6: provenance without a per-message legacy map")
  class Provenance {

    @Test
    @DisplayName("ID-6: the import commits and creates no per-message legacy map table")
    void noPerMessageLegacyMap() {
      assertTrue(run.succeeded(), run::failureText);
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'chats' AND"
                  + " table_name LIKE '%legacy%map%'"));
    }

    @Test
    @DisplayName("ID-6: the import logs never contain client ids")
    void logsDoNotContainClientIds() {
      message(EDGE_ROOM, DUPLICATE_SAME_SENDER_FIRST_MAM);
      String output = run.allOutput();
      assertAll(
          () -> assertFalse(output.contains("4ddba31f-4d76-4fe9-b6c3-0d8c0c3db8dd")),
          () -> assertFalse(output.contains("dddddddd-0000-4000-8000-000000000001")),
          () -> assertFalse(output.contains(WEB_TEXT_CLIENT_ID)),
          () -> assertFalse(output.contains(IOS_TEXT_CLIENT_ID)));
    }
  }

  @Nested
  @DisplayName("ID-2: message ids embedded in pin notifications are minted ids")
  class EmbeddedMessageIds {

    private String messageIdOf(SystemEvent event) {
      return event.json().get("messageId").asText();
    }

    @Test
    @DisplayName("ID-2: messagePinned notification carries the target's new id")
    void pinnedNotificationCarriesNewId() {
      assertAll(
          () ->
              assertEquals(
                  ExpectedIds.message(WEB_ROOM, PIN_TARGET_MAM),
                  messageIdOf(systemEvent(WEB_ROOM, PIN_V1_MAM))),
          () ->
              assertEquals(
                  ExpectedIds.message(PIN_ROOM, PIN_ROOM_PINNED_TARGET_MAM),
                  messageIdOf(systemEvent(PIN_ROOM, PIN_ROOM_PINNED_MAM))),
          () -> message(WEB_ROOM, PIN_TARGET_MAM),
          () -> message(PIN_ROOM, PIN_ROOM_PINNED_TARGET_MAM));
    }

    @Test
    @DisplayName("ID-2: messageUnpinned notification carries the target's new id")
    void unpinnedNotificationCarriesNewId() {
      assertAll(
          () ->
              assertEquals(
                  ExpectedIds.message(PIN_ROOM, PIN_ROOM_UNPINNED_TARGET_MAM),
                  messageIdOf(systemEvent(PIN_ROOM, PIN_ROOM_UNPINNED_MAM))),
          () -> message(PIN_ROOM, PIN_ROOM_UNPINNED_TARGET_MAM));
    }

    @Test
    @DisplayName("ID-2: pin and unpin notifications addressed to an edit row carry the original id")
    void notificationsOnEditRowCarryOriginalId() {
      assertAll(
          () ->
              assertEquals(
                  ExpectedIds.message(WEB_ROOM, PIN_TARGET_MAM),
                  messageIdOf(systemEvent(WEB_ROOM, PIN_V1_ON_EDIT_MAM))),
          () ->
              assertEquals(
                  ExpectedIds.message(WEB_ROOM, PIN_TARGET_MAM),
                  messageIdOf(systemEvent(WEB_ROOM, UNPIN_LOCALHOST_ON_EDIT_MAM))));
    }
  }
}
