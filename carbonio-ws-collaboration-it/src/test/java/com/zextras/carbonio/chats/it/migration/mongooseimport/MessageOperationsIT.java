// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: edits, reactions, deletions and forwards")
class MessageOperationsIT {

  private static final String ROOM_WEB_OPS = "5f6b4438-fdb8-48c1-a866-ff3505b8649a";
  private static final String ROOM_MOBILE_OPS = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String ROOM_DELETES_AND_REACTIONS = "026d176c-f9e9-44e6-a488-7565d7ae4eef";
  private static final String ROOM_EDGE_CASES = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String ROOM_REMOVED_MEMBER = "7be2cb4f-4ce4-4530-b473-2df640007959";
  private static final String ROOM_TEMPORARY = "332d2b24-6fbc-45de-a6d3-9e0b16b69aea";
  private static final String ROOM_FORWARD_LITERAL_ESCAPE = "f0cab8a6-2991-4ccb-a59b-49ada7879299";
  private static final String ROOM_FORWARDS_ONE = "06877759-759d-48a8-bbff-854a7784179e";
  private static final String ROOM_FORWARDS_TWO = "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String ROOM_FORWARD_THIRD_HOP = "d39be5dc-cb00-4ad4-9ae3-03f4f0894b99";
  private static final String ROOM_FORWARDS_CRAFTED = "4df5be7a-ec3c-43f8-93ef-aae54ffdec78";
  private static final String ROOM_ATTACHMENTS = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String ROOM_SMOKE = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";

  private static final String USER_2FFFEFD7 = "2fffefd7-dbe9-5a42-be01-5c95b0d9e4eb";
  private static final String USER_831528CF = "831528cf-1a70-5bac-af91-2ad8ae4e239c";
  private static final String USER_EC31DFC9 = "ec31dfc9-bc07-5474-86f2-ffa20548dbe4";
  private static final String USER_BEA9390A = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";
  private static final String USER_BA2C72B5 = "ba2c72b5-0128-5656-806c-8a163b58450e";
  private static final String USER_6FE7E7CD = "6fe7e7cd-1ec7-517d-9902-c4b83886fe3f";
  private static final String USER_231E8C22 = "231e8c22-87e0-5817-8960-c0e71b070652";
  private static final String USER_4F82BC10 = "4f82bc10-f121-51ed-aed7-6738a8552323";
  private static final String USER_95E96AAB = "95e96aab-bbe9-5ef7-86a8-5d096f361932";
  private static final String USER_A793945E = "a793945e-d600-5362-8ecf-bb6b1d88dbc3";
  private static final String USER_97C353AF = "97c353af-4374-5ff3-a3eb-3056c0b4dba9";
  private static final String USER_7D7EBBE1 = "7d7ebbe1-c1c7-513f-bae9-24131c672d1d";
  private static final String USER_7D03B0C9 = "7d03b0c9-cd6e-5392-9186-1b9646f07fef";
  private static final String USER_F65A7FDC = "f65a7fdc-542e-53bf-9722-5e580cdcca42";
  private static final String USER_D7F47520 = "d7f47520-8d8f-5609-bb75-f7fc23f33ded";
  private static final String USER_04920D70 = "04920d70-5db0-55bf-b094-75cb05023d64";
  private static final String USER_1E49BBA9 = "1e49bba9-89c2-5a09-8eeb-efe969b5389d";
  private static final String USER_E7A245BA = "e7a245ba-f1e6-5f32-ba77-90798e7f219b";
  private static final String USER_BE7B38AB = "be7b38ab-c669-5de9-b8d5-baf31ed4ad06";

  private static final long WEB_CHAIN_ORIGINAL_MAM = 458596077475982849L;
  private static final long WEB_CHAIN_EDIT_1_MAM = 458596077718326273L;
  private static final long WEB_CHAIN_EDIT_2_MAM = 458596077959380225L;
  private static final long WEB_CHAIN_EDIT_3_MAM = 458596078200317697L;
  private static final long WEB_REPLY_ORIGINAL_MAM = 458596074922999297L;
  private static final long WEB_REPLY_EDIT_1_MAM = 458596078921887233L;
  private static final long WEB_REPLY_EDIT_2_MAM = 458596079161724161L;
  private static final long WEB_NON_AUTHOR_EDIT_TARGET_MAM = 458596074199930625L;
  private static final long WEB_NON_AUTHOR_EDIT_MAM = 458596079402752257L;
  private static final long WEB_DELETED_TARGET_MAM = 458596079643774465L;
  private static final long WEB_EDIT_AFTER_DELETE_MAM = 458596080125883905L;
  private static final long WEB_EMPTY_EDIT_TARGET_MAM = 458596074441024001L;
  private static final long WEB_EMPTY_EDIT_MAM = 458596080607432705L;
  private static final long WEB_CLIENT_ID_EDIT_MAM = 458596080366824449L;
  private static final long WEB_UNDECODABLE_TARGET_EDIT_MAM = 458596080846161409L;
  private static final long WEB_WRONG_PARENT_TARGET_MAM = 458596075643364865L;
  private static final long WEB_WRONG_PARENT_EDIT_MAM = 458596081087137281L;
  private static final long ANDROID_EDIT_ORIGINAL_MAM = 458596164097944321L;
  private static final long ANDROID_EDIT_1_MAM = 458596164819702017L;
  private static final long ANDROID_EDIT_2_MAM = 458596165060672257L;
  private static final long IOS_EDIT_ORIGINAL_MAM = 458596166264453377L;
  private static final long IOS_EDIT_MAM = 458596166746436353L;
  private static final long WEB_EDIT_OF_IOS_MESSAGE_MAM = 458596169630819841L;
  private static final long RN_EDIT_ORIGINAL_MAM = 458596167950717441L;
  private static final long RN_EDIT_MAM = 458596168432282625L;
  private static final long LEGACY_EDIT_ORIGINAL_MAM = 458596163856904961L;
  private static final long LEGACY_EDIT_WITHOUT_PARENT_MAM = 458596170594682881L;
  private static final long FORWARD_EDITED_ORIGINAL_MAM = 458596133384694529L;
  private static final long FORWARD_EDIT_MAM = 458596140811828481L;
  private static final long REMOVED_MEMBER_ORIGINAL_MAM = 458615894569365505L;
  private static final long REMOVED_MEMBER_EDIT_MAM = 458615894836019713L;
  private static final long CAPTION_EDIT_ORIGINAL_MAM = 458596909685854465L;
  private static final long CAPTION_EDIT_1_MAM = 458596913913901569L;
  private static final long CAPTION_EDIT_2_MAM = 458596914309697281L;
  private static final long CAPTION_ADDED_ORIGINAL_MAM = 458596908626787841L;
  private static final long CAPTION_ADDED_EDIT_MAM = 458596914397243137L;
  private static final long EDGE_TOMBSTONE_WITH_EDITS_MAM = 458596907231357953L;
  private static final long EDGE_UNKNOWN_TARGET_EDIT_MAM = 458596921384113665L;
  private static final long EDGE_OTHER_ROOM_EDIT_MAM = 458596921487783425L;
  private static final long EDGE_SYSTEM_ROW_EDIT_MAM = 458596921587450625L;
  private static final long OTHER_ROOM_TARGET_MAM = 458596908393360641L;
  private static final long EDGE_OTHER_ROOM_REACTION_MAM = 458596934563488513L;
  private static final long EDGE_OTHER_ROOM_RETRACT_MAM = 458596924008476417L;

  private static final long REACTION_TARGET_MIXED_MAM = 458596093205538049L;
  private static final long REACTION_TARGET_SECOND_MAM = 458596093446679297L;
  private static final long RN_REACTION_TARGET_MAM = 458596167950717441L;
  private static final long RN_REACTION_MAM = 458596168674616065L;
  private static final long ANDROID_REACTION_TARGET_MAM = 458596163856904961L;
  private static final long ANDROID_REACTION_ADD_MAM = 458596165301699329L;
  private static final long ANDROID_REACTION_REMOVE_MAM = 458596165542676737L;
  private static final long IOS_REACTION_ADD_MAM = 458596166987473921L;
  private static final long IOS_REACTION_REMOVE_MAM = 458596167228461569L;
  private static final long EDGE_REACTION_TARGET_MAM = 458596906805728513L;
  private static final long EDGE_LONG_REACTION_MAM = 458596936072893185L;
  private static final long EDGE_ANDROID_REACTION_MAM = 458596936471453441L;
  private static final long EDGE_IOS_REACTION_MAM = 458596936672130561L;
  private static final long EDGE_WEB_REACTION_MAM = 458596936772160769L;
  private static final long SMOKE_REACTION_TARGET_MAM = 458595355979477505L;
  private static final long SMOKE_REACTION_MAM = 458595356064303105L;
  private static final long REACTION_ON_REACTION_ROW_MAM = 458596101618393089L;
  private static final long REMOVED_MEMBER_REACTION_TARGET_MAM = 458615894558619137L;
  private static final long REMOVED_MEMBER_REACTION_MAM = 458615894846742785L;
  private static final long REMOVED_MEMBER_MESSAGE_REACTION_MAM = 458615894861697281L;
  private static final long TEMPORARY_PURGED_TARGET_REACTION_MAM = 458615892168619521L;
  private static final long TEMPORARY_POST_CLEAR_TEXT_MAM = 458615893127340289L;

  private static final long TOMBSTONE_TEXT_MAM = 458596092965907713L;
  private static final long TOMBSTONE_IOS_UPPERCASE_MAM = 458596167469571073L;
  private static final long TOMBSTONE_FORWARD_MAM = 458596134044895233L;
  private static final long TOMBSTONE_XEP_0424_MAM = 458596917767526145L;
  private static final long TOMBSTONE_DOUBLE_RETRACT_MAM = 458596094169649153L;
  private static final long DOUBLE_RETRACT_FIRST_MAM = 458596096573140225L;
  private static final long DOUBLE_RETRACT_SECOND_MAM = 458596096812800513L;
  private static final long NON_AUTHOR_RETRACT_TARGET_MAM = 458596093688102145L;
  private static final long NON_AUTHOR_RETRACT_MAM = 458596095370874113L;
  private static final long OWNER_RETRACT_TARGET_MAM = 458596094650321409L;
  private static final long OWNER_RETRACT_MAM = 458596095610364673L;
  private static final long EDGE_NON_AUTHOR_RETRACT_MAM = 458596923907052033L;
  private static final long CLIENT_ID_RETRACT_TARGET_MAM = 458596094409292801L;
  private static final long CLIENT_ID_RETRACT_MAM = 458596097292150017L;
  private static final long LEGACY_XEP_0424_RETRACT_MAM = 458596170352381185L;
  private static final long EDGE_CLIENT_ID_RETRACT_MAM = 458596924908881153L;
  private static final long EDGE_CLIENT_ID_COLLISION_VICTIM_MAM = 458596912817732865L;
  private static final long EDGE_RETRACT_OF_UNKNOWN_TARGET_MAM = 458596923803232257L;
  private static final long EDGE_RETRACT_OF_SYSTEM_ROW_MAM = 458596924110624513L;
  private static final long EDGE_APPLY_TO_EDIT_AND_RETRACT_MAM = 458596934162225153L;
  private static final long EDGE_APPLY_TO_UNKNOWN_CHILD_MAM = 458596934263197185L;

  private static final long FORWARD_TEXT_MAM = 458596134729373953L;
  private static final long FORWARD_SELF_MAM = 458596134732684289L;
  private static final long FORWARD_TWO_HOPS_MAM = 458596141793010433L;
  private static final long FORWARD_THREE_HOPS_MAM = 458596142455475201L;
  private static final long FORWARD_OF_DELETED_SOURCE_MAM = 458596139194256641L;
  private static final long FORWARD_SELF_OF_DELETED_SOURCE_MAM = 458596160756915201L;
  private static final long FORWARD_LITERAL_ESCAPE_MAM = 458615900807871233L;
  private static final long FORWARD_DELAY_1999_MAM = 458597035470329857L;
  private static final long FORWARD_WITH_DESCRIPTION_MAM = 458596933270881025L;
  private static final long FORWARD_WITH_NESTED_REPLY_MAM = 458596933371849729L;
  private static final long FORWARD_EMPTY_BAD_FROM_MAM = 458596146893932289L;
  private static final long FORWARD_EMPTY_NO_FROM_MAM = 458597035481061121L;
  private static final long FORWARD_EMPTY_NESTED_RETRACT_MAM = 458596933661348609L;

  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    chats = MongooseImport.ofDump().chats();
  }

  private static ImportedChats.Message message(String roomId, long mamId) {
    return chats
        .message(roomId, mamId)
        .orElseThrow(() -> new AssertionError("message not imported: " + roomId + "/" + mamId));
  }

  private static void assertNotMessage(String roomId, long mamId) {
    assertFalse(chats.messages(roomId).isEmpty(), "room has no imported message: " + roomId);
    assertTrue(chats.message(roomId, mamId).isEmpty(), "row became a message: " + mamId);
  }

  private static void assertFolded(
      ImportedChats.Message message, String senderId, String text, boolean edited) {
    assertAll(
        () -> assertEquals(senderId, message.senderId()),
        () -> assertEquals(text, message.text()),
        () -> assertEquals(edited, message.edited()),
        () -> assertFalse(message.deleted()));
  }

  private static void assertDeletedTombstone(ImportedChats.Message message, String senderId) {
    assertAll(
        () -> assertEquals(senderId, message.senderId()),
        () -> assertTrue(message.deleted()),
        () -> assertEquals("", message.text()),
        () -> assertFalse(message.edited()),
        () -> assertNull(message.replyToId()),
        () -> assertNull(message.attachmentId()),
        () -> assertFalse(message.forwarded()),
        () -> assertNull(message.forwardedSenderId()),
        () -> assertNull(message.forwardedAt()));
  }

  private static Map<String, String> reactionsOn(String roomId, long targetMam) {
    String messageId = message(roomId, targetMam).id();
    Map<String, String> byUser = new LinkedHashMap<>();
    chats.reactions(messageId).forEach(r -> byUser.put(r.userId(), r.reaction()));
    return byUser;
  }

  private static LocalDateTime millis(LocalDateTime value) {
    return value.truncatedTo(ChronoUnit.MILLIS);
  }

  @Nested
  @DisplayName("SEM-1: fastening, retract-request and unknown-payload rows are never messages")
  class RowClassification {

    @Test
    @DisplayName(
        "SEM-1: an apply-to holding both edit and retract is a retract request, never an edit")
    void applyToWithEditAndRetractNeverEdits() {
      ImportedChats.Message target = message(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM);

      assertNotMessage(ROOM_EDGE_CASES, EDGE_APPLY_TO_EDIT_AND_RETRACT_MAM);
      assertFolded(target, USER_97C353AF, "loremips U2 md O", false);
    }

    @Test
    @DisplayName("SEM-1: an apply-to without edit, reaction or retract child is skipped")
    void applyToWithUnknownChildIsSkipped() {
      ImportedChats.Message target = message(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM);

      assertNotMessage(ROOM_EDGE_CASES, EDGE_APPLY_TO_UNKNOWN_CHILD_MAM);
      assertFolded(target, USER_97C353AF, "loremips U2 md O", false);
    }

    @Test
    @DisplayName(
        "SEM-1: edit, reaction and retract-request rows of every client shape are never messages")
    void fasteningRowsOfEveryShapeAreNeverMessages() {
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_CHAIN_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, ANDROID_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, IOS_EDIT_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, RN_EDIT_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, LEGACY_EDIT_WITHOUT_PARENT_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, RN_REACTION_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, ANDROID_REACTION_ADD_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, IOS_REACTION_ADD_MAM),
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, DOUBLE_RETRACT_FIRST_MAM),
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, REACTION_ON_REACTION_ROW_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, LEGACY_XEP_0424_RETRACT_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_APPLY_TO_UNKNOWN_CHILD_MAM));
    }
  }

  @Nested
  @DisplayName("SEM-4: edits are folded into the original message")
  class EditFolding {

    @Test
    @DisplayName("SEM-4.q: a chain of web edits with parent-id leaves the last edit text")
    void webEditChainWithParentIdKeepsLastEdit() {
      assertFolded(
          message(ROOM_WEB_OPS, WEB_CHAIN_ORIGINAL_MAM),
          USER_2FFFEFD7,
          "L1 oremip sumd o3 lorsi",
          true);
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_CHAIN_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_CHAIN_EDIT_2_MAM),
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_CHAIN_EDIT_3_MAM));
    }

    @Test
    @DisplayName("SEM-4.p: the edit with the highest MAM id wins, whatever the parent-id says")
    void editChainWinnerIsTheLastByMamId() {
      assertFolded(
          message(ROOM_WEB_OPS, WEB_REPLY_ORIGINAL_MAM),
          USER_2FFFEFD7,
          "lorem ip 141 (sumdol orsit)",
          true);
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_REPLY_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_REPLY_EDIT_2_MAM));
    }

    @Test
    @DisplayName(
        "SEM-4.u: an edit with a parent-id pointing to another message is applied to the apply-to"
            + " target")
    void wrongParentIdIsIgnored() {
      assertFolded(
          message(ROOM_WEB_OPS, WEB_WRONG_PARENT_TARGET_MAM),
          USER_831528CF,
          "lore mipsu mdolor",
          true);
      assertNotMessage(ROOM_WEB_OPS, WEB_WRONG_PARENT_EDIT_MAM);
    }

    @Test
    @DisplayName("SEM-4.s: Android edits without parent-id are folded, last one wins")
    void androidEditsWithoutParentIdAreFolded() {
      assertFolded(
          message(ROOM_MOBILE_OPS, ANDROID_EDIT_ORIGINAL_MAM),
          USER_EC31DFC9,
          "L8 oremips umdo LORSIT ametc",
          true);
      assertAll(
          () -> assertNotMessage(ROOM_MOBILE_OPS, ANDROID_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, ANDROID_EDIT_2_MAM));
    }

    @Test
    @DisplayName(
        "SEM-4.r: an iOS edit is folded and a later non-author web edit of the same message is"
            + " dropped")
    void iosEditIsFoldedAndLaterNonAuthorEditDropped() {
      assertFolded(
          message(ROOM_MOBILE_OPS, IOS_EDIT_ORIGINAL_MAM),
          USER_BEA9390A,
          "L8 ore mips UMDOLO",
          true);
      assertAll(
          () -> assertNotMessage(ROOM_MOBILE_OPS, IOS_EDIT_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, WEB_EDIT_OF_IOS_MESSAGE_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?", "lor emips umd olo")));
    }

    @Test
    @DisplayName("SEM-4.t: a React Native edit is folded")
    void reactNativeEditIsFolded() {
      assertFolded(
          message(ROOM_MOBILE_OPS, RN_EDIT_ORIGINAL_MAM), USER_BA2C72B5, "L8 or emip SUMDOL", true);
      assertNotMessage(ROOM_MOBILE_OPS, RN_EDIT_MAM);
    }

    @Test
    @DisplayName("SEM-4.u: a legacy web edit without parent-id is folded")
    void legacyEditWithoutParentIdIsFolded() {
      assertFolded(
          message(ROOM_MOBILE_OPS, LEGACY_EDIT_ORIGINAL_MAM),
          USER_6FE7E7CD,
          "loremi psumdolor sita",
          true);
      assertNotMessage(ROOM_MOBILE_OPS, LEGACY_EDIT_WITHOUT_PARENT_MAM);
    }

    @Test
    @DisplayName(
        "SEM-4.c: an edit of a forwarded message is folded and the message stays forwarded")
    void editOfForwardedMessageKeepsForwardedFlag() {
      ImportedChats.Message message = message(ROOM_FORWARDS_TWO, FORWARD_EDITED_ORIGINAL_MAM);

      assertFolded(message, USER_BA2C72B5, "L6 oremipsum dolors it ametconse", true);
      assertTrue(message.forwarded());
      assertNotMessage(ROOM_FORWARDS_TWO, FORWARD_EDIT_MAM);
    }

    @Test
    @DisplayName("SEM-4.d/4.e: attachment captions are replaced or added by edits")
    void attachmentCaptionEditsAreFolded() {
      assertAll(
          () ->
              assertFolded(
                  message(ROOM_ATTACHMENTS, CAPTION_EDIT_ORIGINAL_MAM),
                  USER_04920D70,
                  "loremip S3 / 2026 (umdolorsi tamet)",
                  true),
          () ->
              assertFolded(
                  message(ROOM_ATTACHMENTS, CAPTION_ADDED_ORIGINAL_MAM),
                  USER_D7F47520,
                  "lorem i psumdol orsit",
                  true),
          () -> assertNotMessage(ROOM_ATTACHMENTS, CAPTION_EDIT_1_MAM),
          () -> assertNotMessage(ROOM_ATTACHMENTS, CAPTION_EDIT_2_MAM),
          () -> assertNotMessage(ROOM_ATTACHMENTS, CAPTION_ADDED_EDIT_MAM));
    }

    @Test
    @DisplayName(
        "SEM-4.w2: an author edit by a member who was later removed from the room is applied")
    void authorEditOfLaterRemovedMemberIsApplied() {
      assertFolded(
          message(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_ORIGINAL_MAM),
          USER_231E8C22,
          "loremip su mdol orsitam etcons ectetu",
          true);
      assertNotMessage(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_EDIT_MAM);
    }

    @Test
    @DisplayName("SEM-4.h: an edit by a non-author is dropped and the original keeps its text")
    void nonAuthorEditIsDropped() {
      assertFolded(
          message(ROOM_WEB_OPS, WEB_NON_AUTHOR_EDIT_TARGET_MAM),
          USER_831528CF,
          "L1 orem ipsumdo lors 141: itame tcon",
          false);
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_NON_AUTHOR_EDIT_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?", "LOREMIPS um 143")));
    }

    @Test
    @DisplayName("SEM-4.j: an edit sent after the delete never resurrects the text")
    void editAfterDeleteDoesNotResurrectText() {
      ImportedChats.Message deleted = message(ROOM_WEB_OPS, WEB_DELETED_TARGET_MAM);

      assertDeletedTombstone(deleted, USER_831528CF);
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_EDIT_AFTER_DELETE_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?",
                      "lore mipsu mdolor (sitame)")));
    }

    @Test
    @DisplayName("SEM-4.i: edits sent before the delete never resurrect the deleted text")
    void editsSentBeforeTheDeleteDoNotResurrectText() {
      ImportedChats.Message deleted = message(ROOM_EDGE_CASES, EDGE_TOMBSTONE_WITH_EDITS_MAM);
      List<String> editBodies =
          List.of(
              "L4 oremip sumdo",
              "L4 oremip sumdol orsitametco",
              "L4 oremi psumdo",
              "L4 oremips umdo lo rsitam",
              "L4 ore mips",
              "L4 oremip sum dolo rs itamet",
              "lore mi psumdolo",
              "lore+mipsu",
              "L4 orem ipsum dolorsi");

      assertDeletedTombstone(deleted, USER_7D7EBBE1);
      editBodies.forEach(
          body ->
              assertEquals(
                  0,
                  chats.count("SELECT count(*) FROM chats.message WHERE text = ?", body),
                  "edit text resurfaced: " + body));
    }

    @Test
    @DisplayName("SEM-4.k / C5: an empty-body edit of a text message is applied")
    void emptyBodyEditIsApplied() {
      assertFolded(message(ROOM_WEB_OPS, WEB_EMPTY_EDIT_TARGET_MAM), USER_2FFFEFD7, "", true);
      assertNotMessage(ROOM_WEB_OPS, WEB_EMPTY_EDIT_MAM);
    }

    @Test
    @DisplayName("SEM-4.n: an edit addressed by client id is dropped")
    void editAddressedByClientIdIsDropped() {
      assertFolded(message(ROOM_WEB_OPS, WEB_EMPTY_EDIT_TARGET_MAM), USER_2FFFEFD7, "", true);
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_CLIENT_ID_EDIT_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?", "lore mi psumdo lo")));
    }

    @Test
    @DisplayName("SEM-4.m: an edit of an unknown or undecodable target is dropped")
    void editOfUnknownTargetIsDropped() {
      assertAll(
          () -> assertNotMessage(ROOM_WEB_OPS, WEB_UNDECODABLE_TARGET_EDIT_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_UNKNOWN_TARGET_EDIT_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?", "lore mi psumd")));
    }

    @Test
    @DisplayName("SEM-4.o: an edit of a row in another room or of a system row is dropped")
    void editOfOtherRoomOrSystemRowIsDropped() {
      ImportedChats.Message otherRoomTarget = message(ROOM_FORWARDS_CRAFTED, OTHER_ROOM_TARGET_MAM);

      assertFolded(otherRoomTarget, USER_BE7B38AB, "loremips U2 M1 do 124", false);
      assertAll(
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_OTHER_ROOM_EDIT_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_SYSTEM_ROW_EDIT_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE text = ?", "lore mipsu mdol")),
          () ->
              assertEquals(
                  0, chats.count("SELECT count(*) FROM chats.message WHERE text = ?", "lore mip")));
    }
  }

  @Nested
  @DisplayName("SEM-5: reactions are folded into chats.message_reaction")
  class ReactionFolding {

    @Test
    @DisplayName(
        "SEM-5.a/5.e/5.f: web reactions keep one row per user, the last wins, an empty body removes"
            + " it")
    void lastReactionPerUserWinsAndEmptyBodyRemoves() {
      Map<String, String> expected =
          Map.of(
              USER_95E96AAB, "🎉",
              USER_A793945E, "👍🏽",
              USER_BEA9390A, "👨‍👩‍👧‍👦");

      assertEquals(expected, reactionsOn(ROOM_DELETES_AND_REACTIONS, REACTION_TARGET_MIXED_MAM));
    }

    @Test
    @DisplayName(
        "SEM-5.h/5.q: several users on one target, duplicates and text reactions fold to the last"
            + " row")
    void severalUsersAndDuplicatesFoldToLastRow() {
      Map<String, String> expected =
          Map.of(
              USER_BA2C72B5, "👍",
              USER_BEA9390A, "👍",
              USER_A793945E, "👍👍👍🔥",
              USER_95E96AAB, "🔥");

      assertEquals(expected, reactionsOn(ROOM_DELETES_AND_REACTIONS, REACTION_TARGET_SECOND_MAM));
    }

    @Test
    @DisplayName("SEM-5.l: a ZWJ family reaction is stored intact")
    void zwjFamilyReactionIsStoredIntact() {
      String reaction =
          reactionsOn(ROOM_DELETES_AND_REACTIONS, REACTION_TARGET_MIXED_MAM).get(USER_BEA9390A);

      assertNotNull(reaction);
      assertEquals(7, reaction.codePointCount(0, reaction.length()));
    }

    @Test
    @DisplayName("SEM-5.a: the web smoke reaction lands on its target")
    void webReactionLandsOnTarget() {
      assertEquals(Map.of(USER_D7F47520, "👍"), reactionsOn(ROOM_SMOKE, SMOKE_REACTION_TARGET_MAM));
      assertNotMessage(ROOM_SMOKE, SMOKE_REACTION_MAM);
    }

    @Test
    @DisplayName(
        "SEM-5.d: a React Native reaction with the external nested in the reaction is recognised")
    void reactNativeNestedExternalReactionIsRecognised() {
      assertEquals(
          Map.of(USER_BA2C72B5, "🔥"), reactionsOn(ROOM_MOBILE_OPS, RN_REACTION_TARGET_MAM));
      assertNotMessage(ROOM_MOBILE_OPS, RN_REACTION_MAM);
    }

    @Test
    @DisplayName(
        "SEM-5.b: an iOS reaction (external name=reaction) is recognised and the last iOS row wins")
    void iosReactionIsRecognised() {
      Map<String, String> reactions = reactionsOn(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM);

      assertEquals("🚀", reactions.get(USER_F65A7FDC));
      assertNotMessage(ROOM_EDGE_CASES, EDGE_IOS_REACTION_MAM);
    }

    @Test
    @DisplayName(
        "SEM-5.c: Android reactions without external are recognised, including the removal")
    void androidReactionsAreRecognised() {
      Map<String, String> edgeReactions = reactionsOn(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM);
      Map<String, String> mobileReactions =
          reactionsOn(ROOM_MOBILE_OPS, ANDROID_REACTION_TARGET_MAM);

      assertAll(
          () -> assertEquals("👍", edgeReactions.get(USER_97C353AF)),
          () -> assertFalse(edgeReactions.containsKey(USER_7D7EBBE1)),
          () -> assertFalse(mobileReactions.containsKey(USER_EC31DFC9)),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_ANDROID_REACTION_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, ANDROID_REACTION_REMOVE_MAM));
    }

    @Test
    @DisplayName("SEM-5.f: an iOS removal row deletes the earlier iOS reaction")
    void iosRemovalDeletesTheReaction() {
      Map<String, String> reactions = reactionsOn(ROOM_MOBILE_OPS, ANDROID_REACTION_TARGET_MAM);

      assertFalse(reactions.containsKey(USER_BEA9390A));
      assertAll(
          () -> assertNotMessage(ROOM_MOBILE_OPS, IOS_REACTION_ADD_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, IOS_REACTION_REMOVE_MAM));
    }

    @Test
    @DisplayName("SEM-5.a: every client shape folds into the same final state of one target")
    void everyShapeFoldsIntoTheSameTarget() {
      Map<String, String> expected =
          Map.of(
              USER_F65A7FDC, "🚀",
              USER_97C353AF, "👍",
              USER_7D03B0C9, "🥳");

      assertEquals(expected, reactionsOn(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM));
      assertNotMessage(ROOM_EDGE_CASES, EDGE_WEB_REACTION_MAM);
    }

    @Test
    @DisplayName(
        "SEM-5.k: a reaction longer than 32 characters is dropped and never becomes a message")
    void tooLongReactionIsDropped() {
      assertNotMessage(ROOM_EDGE_CASES, EDGE_LONG_REACTION_MAM);
      assertAll(
          () ->
              assertEquals(
                  "👍", reactionsOn(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM).get(USER_97C353AF)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message_reaction WHERE char_length(reaction) >"
                          + " 32")));
    }

    @Test
    @DisplayName("SEM-5.i: reactions on deleted messages are dropped")
    void reactionsOnDeletedTargetsAreDropped() {
      long reactedTombstoneByBea9390a = 458596095850036993L;
      long reactedTombstoneBy95e96aab = 458596094890013697L;
      long reactedEdgeTombstoneBy97c353af = 458596906904507393L;
      long reactedEdgeTombstoneByF65a7fdc = 458596907900401153L;
      long reactedEdgeTombstoneBy7d7ebbe1 = 458596908001372673L;

      assertAll(
          () -> assertTrue(message(ROOM_DELETES_AND_REACTIONS, TOMBSTONE_TEXT_MAM).deleted()),
          () ->
              assertTrue(
                  chats
                      .reactions(message(ROOM_DELETES_AND_REACTIONS, TOMBSTONE_TEXT_MAM).id())
                      .isEmpty()),
          () ->
              assertTrue(
                  chats
                      .reactions(
                          message(ROOM_DELETES_AND_REACTIONS, reactedTombstoneByBea9390a).id())
                      .isEmpty()),
          () ->
              assertTrue(
                  chats
                      .reactions(
                          message(ROOM_DELETES_AND_REACTIONS, reactedTombstoneBy95e96aab).id())
                      .isEmpty()),
          () ->
              assertTrue(
                  chats
                      .reactions(message(ROOM_EDGE_CASES, reactedEdgeTombstoneBy97c353af).id())
                      .isEmpty()),
          () ->
              assertTrue(
                  chats
                      .reactions(message(ROOM_EDGE_CASES, reactedEdgeTombstoneByF65a7fdc).id())
                      .isEmpty()),
          () ->
              assertTrue(
                  chats
                      .reactions(message(ROOM_EDGE_CASES, reactedEdgeTombstoneBy7d7ebbe1).id())
                      .isEmpty()),
          () -> assertTrue(chats.count("SELECT count(*) FROM chats.message_reaction") > 0),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message_reaction r JOIN chats.message m ON m.id ="
                          + " r.message_id WHERE m.deleted")));
    }

    @Test
    @DisplayName("SEM-5.j: a reaction addressed to a message of another room is dropped")
    void reactionOnOtherRoomTargetIsDropped() {
      Map<String, String> reactions = reactionsOn(ROOM_FORWARDS_CRAFTED, OTHER_ROOM_TARGET_MAM);

      assertFalse(reactions.containsKey(USER_F65A7FDC));
      assertNotMessage(ROOM_EDGE_CASES, EDGE_OTHER_ROOM_REACTION_MAM);
    }

    @Test
    @DisplayName(
        "SEM-5.o: reactions of a member later removed, and on a removed member's message, are kept")
    void reactionsOfLaterRemovedMemberAreKept() {
      assertAll(
          () ->
              assertEquals(
                  Map.of(USER_231E8C22, "😀"),
                  reactionsOn(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_REACTION_TARGET_MAM)),
          () ->
              assertEquals(
                  Map.of(USER_4F82BC10, "❤️"),
                  reactionsOn(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_ORIGINAL_MAM)),
          () -> assertNotMessage(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_REACTION_MAM),
          () -> assertNotMessage(ROOM_REMOVED_MEMBER, REMOVED_MEMBER_MESSAGE_REACTION_MAM));
    }

    @Test
    @DisplayName("SEM-5.j: a reaction on a purged target of the TEMPORARY room is dropped")
    void reactionOnPurgedTargetOfTemporaryRoomIsDropped() {
      ImportedChats.Message afterClear = message(ROOM_TEMPORARY, TEMPORARY_POST_CLEAR_TEXT_MAM);

      assertEquals("lorem ipsumd olors", afterClear.text());
      assertAll(
          () -> assertNotMessage(ROOM_TEMPORARY, TEMPORARY_PURGED_TARGET_REACTION_MAM),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message_reaction r JOIN chats.message m ON m.id ="
                          + " r.message_id WHERE m.room_id = ?",
                      ROOM_TEMPORARY)));
    }
  }

  @Nested
  @DisplayName("SEM-3: tombstones delete, retract requests never do")
  class Deletions {

    @Test
    @DisplayName("SEM-3.a: a tombstone becomes a deleted message with empty text and no edit flag")
    void tombstoneBecomesDeletedMessage() {
      assertDeletedTombstone(
          message(ROOM_DELETES_AND_REACTIONS, TOMBSTONE_TEXT_MAM), USER_BA2C72B5);
    }

    @Test
    @DisplayName("SEM-3.b: a tombstone with an uppercase iOS client id is imported as deleted")
    void iosUppercaseTombstoneIsDeleted() {
      assertDeletedTombstone(message(ROOM_MOBILE_OPS, TOMBSTONE_IOS_UPPERCASE_MAM), USER_BEA9390A);
    }

    @Test
    @DisplayName("SEM-3.c: a tombstone of a forward is a plain deleted message")
    void forwardTombstoneIsDeleted() {
      assertDeletedTombstone(message(ROOM_FORWARDS_TWO, TOMBSTONE_FORWARD_MAM), USER_BA2C72B5);
    }

    @Test
    @DisplayName("SEM-3.h: a tombstone in the XEP-0424 namespace is imported as deleted")
    void xep0424TombstoneIsDeleted() {
      assertDeletedTombstone(message(ROOM_EDGE_CASES, TOMBSTONE_XEP_0424_MAM), USER_97C353AF);
    }

    @Test
    @DisplayName("SEM-3.i: deleted_at carries the retracted stamp")
    void deletedAtCarriesRetractedStamp() {
      ImportedChats.Message deleted = message(ROOM_WEB_OPS, WEB_DELETED_TARGET_MAM);

      assertEquals(LocalDateTime.parse("2026-10-07T16:35:37"), millis(deleted.deletedAt()));
    }

    @Test
    @DisplayName("SEM-3.q: a double retract leaves one deleted message and no retract message")
    void doubleRetractLeavesOneDeletedMessage() {
      assertDeletedTombstone(
          message(ROOM_DELETES_AND_REACTIONS, TOMBSTONE_DOUBLE_RETRACT_MAM), USER_BA2C72B5);
      assertAll(
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, DOUBLE_RETRACT_FIRST_MAM),
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, DOUBLE_RETRACT_SECOND_MAM));
    }

    @Test
    @DisplayName("SEM-3.l: a retract request by a non-author leaves the target intact")
    void nonAuthorRetractLeavesTargetIntact() {
      assertAll(
          () ->
              assertFolded(
                  message(ROOM_DELETES_AND_REACTIONS, NON_AUTHOR_RETRACT_TARGET_MAM),
                  USER_BA2C72B5,
                  "L2 ore M ip 145 su md olorsitam et con-sectet",
                  false),
          () ->
              assertFolded(
                  message(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM),
                  USER_97C353AF,
                  "loremips U2 md O",
                  false),
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, NON_AUTHOR_RETRACT_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_NON_AUTHOR_RETRACT_MAM));
    }

    @Test
    @DisplayName("SEM-3.m: a retract request by the room owner leaves the target intact")
    void ownerRetractLeavesTargetIntact() {
      assertFolded(
          message(ROOM_DELETES_AND_REACTIONS, OWNER_RETRACT_TARGET_MAM),
          USER_95E96AAB,
          "L2 ore M ip 147 sumdolors it amet conse",
          false);
      assertNotMessage(ROOM_DELETES_AND_REACTIONS, OWNER_RETRACT_MAM);
    }

    @Test
    @DisplayName("SEM-3.o/3.p: retract requests addressed by client id delete nothing")
    void clientIdAddressedRetractsDeleteNothing() {
      assertAll(
          () ->
              assertFolded(
                  message(ROOM_DELETES_AND_REACTIONS, CLIENT_ID_RETRACT_TARGET_MAM),
                  USER_BEA9390A,
                  "L2 ore M ip 146 sumdolo rs itamet co",
                  false),
          () ->
              assertFolded(
                  message(ROOM_MOBILE_OPS, LEGACY_EDIT_ORIGINAL_MAM),
                  USER_6FE7E7CD,
                  "loremi psumdolor sita",
                  true),
          () ->
              assertFolded(
                  message(ROOM_EDGE_CASES, EDGE_REACTION_TARGET_MAM),
                  USER_97C353AF,
                  "loremips U2 md O",
                  false),
          () -> assertNotMessage(ROOM_DELETES_AND_REACTIONS, CLIENT_ID_RETRACT_MAM),
          () -> assertNotMessage(ROOM_MOBILE_OPS, LEGACY_XEP_0424_RETRACT_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_CLIENT_ID_RETRACT_MAM));
    }

    @Test
    @DisplayName("SEM-3.o: a message whose client id equals a retracted stanza-id stays live")
    void clientIdCollidingWithRetractedStanzaIdStaysLive() {
      assertFolded(
          message(ROOM_EDGE_CASES, EDGE_CLIENT_ID_COLLISION_VICTIM_MAM),
          USER_7D7EBBE1,
          "lo remips um do L1 orsita-me",
          false);
    }

    @Test
    @DisplayName(
        "SEM-3.n/3.s: retract requests of unknown targets or system rows are never messages")
    void retractRequestsOfUnknownTargetsAreNeverMessages() {
      assertAll(
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_RETRACT_OF_UNKNOWN_TARGET_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_RETRACT_OF_SYSTEM_ROW_MAM),
          () -> assertNotMessage(ROOM_EDGE_CASES, EDGE_OTHER_ROOM_RETRACT_MAM));
    }

    @Test
    @DisplayName(
        "SEM-3.n: a retract request addressed to a message of another room leaves it intact")
    void otherRoomRetractLeavesTargetIntact() {
      assertFolded(
          message(ROOM_FORWARDS_CRAFTED, OTHER_ROOM_TARGET_MAM),
          USER_BE7B38AB,
          "loremips U2 M1 do 124",
          false);
    }
  }

  @Nested
  @DisplayName("SEM-8: forwards are imported from the inner message")
  class Forwards {

    @Test
    @DisplayName(
        "SEM-8.a: a backend forward takes the inner body, the inner author and the delay stamp")
    void forwardTakesInnerBodyAuthorAndDelay() {
      ImportedChats.Message forward = message(ROOM_FORWARDS_ONE, FORWARD_TEXT_MAM);

      assertAll(
          () -> assertEquals(USER_BEA9390A, forward.senderId()),
          () -> assertEquals("L5 orem ip sumdolo (rsita) 😀 & <l>", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(USER_BA2C72B5, forward.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T16:38:56.534"), millis(forward.forwardedAt())),
          () -> assertFalse(forward.edited()),
          () -> assertFalse(forward.deleted()));
    }

    @Test
    @DisplayName("SEM-8.e: a self-forward is forwarded and names the forwarder as original sender")
    void selfForwardIsForwarded() {
      ImportedChats.Message forward = message(ROOM_FORWARDS_ONE, FORWARD_SELF_MAM);

      assertAll(
          () -> assertEquals(USER_BEA9390A, forward.senderId()),
          () -> assertEquals("L5 oremip sumd ol orsitam et consect etur", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(USER_BEA9390A, forward.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T16:38:57.475"), millis(forward.forwardedAt())));
    }

    @Test
    @DisplayName("SEM-8.c: a two-hop forward keeps the previous forwarder as original sender")
    void twoHopForwardKeepsPreviousForwarder() {
      ImportedChats.Message forward = message(ROOM_FORWARDS_ONE, FORWARD_TWO_HOPS_MAM);

      assertAll(
          () -> assertEquals(USER_6FE7E7CD, forward.senderId()),
          () -> assertEquals("L5 orem IPSUM dolo", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(USER_EC31DFC9, forward.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T16:39:13.891"), millis(forward.forwardedAt())));
    }

    @Test
    @DisplayName("SEM-8.d: a three-hop forward keeps the previous forwarder as original sender")
    void threeHopForwardKeepsPreviousForwarder() {
      ImportedChats.Message forward = message(ROOM_FORWARD_THIRD_HOP, FORWARD_THREE_HOPS_MAM);

      assertAll(
          () -> assertEquals(USER_EC31DFC9, forward.senderId()),
          () -> assertEquals("L5 orem IPSUM dolo", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(USER_6FE7E7CD, forward.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T16:39:38.878"), millis(forward.forwardedAt())));
    }

    @Test
    @DisplayName("SEM-8.h: forwards of a deleted source keep their content")
    void forwardsOfDeletedSourceKeepContent() {
      ImportedChats.Message byOtherUser = message(ROOM_FORWARDS_TWO, FORWARD_OF_DELETED_SOURCE_MAM);
      ImportedChats.Message bySelf = message(ROOM_FORWARDS_ONE, FORWARD_SELF_OF_DELETED_SOURCE_MAM);

      assertAll(
          () -> assertEquals("L5 orem ipsu mdol orsitam etcon sectetu", byOtherUser.text()),
          () -> assertTrue(byOtherUser.forwarded()),
          () -> assertFalse(byOtherUser.deleted()),
          () -> assertEquals(USER_95E96AAB, byOtherUser.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-07T16:39:02.173"),
                  millis(byOtherUser.forwardedAt())),
          () -> assertEquals("L5 oremipsum dolo rsitame tc onsect (etura)", bySelf.text()),
          () -> assertTrue(bySelf.forwarded()),
          () -> assertFalse(bySelf.deleted()),
          () -> assertEquals(USER_BA2C72B5, bySelf.forwardedSenderId()));
    }

    @Test
    @DisplayName("SEM-8.s: a forward of text with a literal backslash-u sequence is decoded")
    void forwardInnerBodyIsUnicodeDecoded() {
      ImportedChats.Message forward =
          message(ROOM_FORWARD_LITERAL_ESCAPE, FORWARD_LITERAL_ESCAPE_MAM);

      assertAll(
          () -> assertEquals(USER_E7A245BA, forward.senderId()),
          () -> assertEquals("loreè lorem ipsumdolo A", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(USER_1E49BBA9, forward.forwardedSenderId()),
          () ->
              assertEquals(
                  LocalDateTime.parse("2026-10-08T14:06:00.790"), millis(forward.forwardedAt())));
    }

    @Test
    @DisplayName(
        "SEM-8.o: a forward keeps its original delay stamp even when it is implausible (1999)")
    void forwardDelayIsCopiedAsStored() {
      ImportedChats.Message forward = message(ROOM_FORWARDS_CRAFTED, FORWARD_DELAY_1999_MAM);

      assertAll(
          () -> assertEquals("loremi psumdol orsi", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertEquals(LocalDateTime.parse("1999-01-01T00:00:00"), forward.forwardedAt()));
    }

    @Test
    @DisplayName("SEM-8.j: the outer description of a forward is dropped")
    void forwardOuterDescriptionIsDropped() {
      ImportedChats.Message forward = message(ROOM_EDGE_CASES, FORWARD_WITH_DESCRIPTION_MAM);

      assertAll(
          () -> assertEquals(USER_7D03B0C9, forward.senderId()),
          () -> assertEquals("lorem ipsu", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertFalse(forward.text().contains("loremipsum")));
    }

    @Test
    @DisplayName("SEM-8.g: a nested reply in the forwarded message is ignored")
    void forwardNestedReplyIsIgnored() {
      ImportedChats.Message forward = message(ROOM_EDGE_CASES, FORWARD_WITH_NESTED_REPLY_MAM);

      assertAll(
          () -> assertEquals("lorem ipsum", forward.text()),
          () -> assertTrue(forward.forwarded()),
          () -> assertNull(forward.replyToId()));
    }

    @Test
    @DisplayName("SEM-8.l / C4: forwards with an empty inner body are imported with empty text")
    void forwardsWithEmptyInnerBodyAreImported() {
      ImportedChats.Message badFrom = message(ROOM_FORWARDS_TWO, FORWARD_EMPTY_BAD_FROM_MAM);
      ImportedChats.Message noFrom = message(ROOM_FORWARDS_CRAFTED, FORWARD_EMPTY_NO_FROM_MAM);
      assertAll(
          () -> assertTrue(badFrom.forwarded()),
          () -> assertEquals("", badFrom.text()),
          () -> assertTrue(noFrom.forwarded()),
          () -> assertEquals("", noFrom.text()));
    }

    @Test
    @DisplayName("SEM-8.l: a forward whose inner message has no body is not imported")
    void forwardsWithoutInnerBodyAreSkipped() {
      assertNotMessage(ROOM_EDGE_CASES, FORWARD_EMPTY_NESTED_RETRACT_MAM);
    }
  }
}
