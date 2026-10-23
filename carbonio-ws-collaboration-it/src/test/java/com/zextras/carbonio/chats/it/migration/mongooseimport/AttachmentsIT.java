// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@DisplayName("V2_0_1 import of the real MongooseIM dump: attachments, files and captions")
class AttachmentsIT {

  private static final String HISTORICAL_ROOM = "9688feea-bf86-4f63-b08d-a45d72f59287";
  private static final String ESCAPED_CAPTION_ROOM = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String IOS_AND_WEB_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String CAPTION_SHAPES_ROOM = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String BROKEN_EDGES_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String SHARED_CLIENT_ID_ROOM = "e6ff78bf-33d5-48e6-9db2-aa313e20b33e";
  private static final String SECOND_SHARED_CLIENT_ID_ROOM = "4df5be7a-ec3c-43f8-93ef-aae54ffdec78";
  private static final String REPLY_ATTACHMENT_ROOM = "5f6b4438-fdb8-48c1-a866-ff3505b8649a";
  private static final String REPLY_CAPTION_ROOM = "ec84f534-92bd-4d88-923f-ab5f8e027526";
  private static final String FORWARD_ROOM_ONE = "06877759-759d-48a8-bbff-854a7784179e";
  private static final String FORWARD_ROOM_TWO = "a992e390-3e44-4536-aed5-a5e13572d046";
  private static final String FORWARD_ROOM_THREE = "b4ee4ff8-9d81-4f3c-909d-0832d2fb9ca4";
  private static final String AVATAR_ROOM = "b199be0b-bd3d-46ba-bb4c-41eb5873efbc";
  private static final String ZOMBIE_ROOM = "80e4de67-3d6c-4c15-8876-e6255ac0c1d5";
  private static final String ROOM_ABSENT_FROM_WSC = "6171aad4-52b2-47a5-9d2b-83d556a3e6f9";

  private static final long HISTORICAL_NO_KEYS_MAM = 456005033677711873L;
  private static final String HISTORICAL_NO_KEYS_FILE = "c677e644-297e-4b70-8f51-6a8bf2142924";
  private static final long HISTORICAL_STANZA_ONLY_MAM = 456177124404531969L;
  private static final String HISTORICAL_STANZA_ONLY_FILE = "856393b2-9eb5-4789-aea1-0c954547abdc";
  private static final long HISTORICAL_CLIENT_ID_MAM = 456165541949905153L;
  private static final String HISTORICAL_CLIENT_ID_FILE = "47f4bdb9-b5cc-4b5d-bbce-557a1730fe18";
  private static final String HISTORICAL_CLIENT_ID = "0fbb0354-27fb-4718-821e-1e589ba64b0b";

  private static final long ESCAPED_CAPTION_MAM = 458595356415351553L;
  private static final String ESCAPED_CAPTION_FILE = "0049899e-f3c9-4d46-8746-d5e486960d6f";
  private static final long WEB_HEADER_CAPTION_MAM = 458596170883761153L;
  private static final String WEB_HEADER_CAPTION_FILE = "634f270a-414a-4c5f-87ec-9ac7eb7b22d4";
  private static final String WEB_HEADER_CLIENT_ID = "376eb087-78ac-4eac-b0f8-78bea4b2739c";
  private static final long IOS_UPPERCASE_ID_MAM = 458596201721417985L;
  private static final String IOS_UPPERCASE_ID_FILE = "4fadf90b-7278-4757-ab99-27ad944c6162";
  private static final String IOS_UPPERCASE_CLIENT_ID = "8EB2D491-A41D-4011-A8FC-3544F9667ECF";
  private static final long HEADER_NEWLINE_CAPTION_MAM = 458596202042710017L;
  private static final long ESCAPED_NEWLINE_CAPTION_MAM = 458596202360620545L;
  private static final long NON_UUID_CLIENT_ID_MAM = 458597020134006529L;
  private static final String NON_UUID_CLIENT_ID_FILE = "bb05a9f4-af20-496a-90de-dfb93ee2bfe6";
  private static final long SECOND_NON_UUID_CLIENT_ID_MAM = 458596926361368577L;
  private static final String SECOND_NON_UUID_CLIENT_ID_FILE =
      "9fded963-78d3-42a8-aaeb-e79225b04b6f";
  private static final long NULL_MESSAGE_ID_MAM = 458597019934691841L;
  private static final String NULL_MESSAGE_ID_FILE = "953813a0-115e-42bb-9623-09af6d378b29";
  private static final long SECOND_NULL_MESSAGE_ID_MAM = 458596926316316161L;
  private static final String SECOND_NULL_MESSAGE_ID_FILE = "7d8c885e-af0e-4575-9698-6236cbeb4e60";
  private static final long SHARED_ID_FIRST_MAM = 458596926458562561L;
  private static final String SHARED_ID_FIRST_FILE = "b8aa91a6-1579-4836-9019-d01f1229cceb";
  private static final long SHARED_ID_SECOND_MAM = 458596926505754881L;
  private static final String SHARED_ID_SECOND_FILE = "f05c574e-30c5-4d1e-b929-b35a37801d38";
  private static final long SHARED_ID_THIRD_MAM = 458596926601204993L;
  private static final String SHARED_ID_THIRD_FILE = "6dd370c7-6d9c-46fd-be31-76363da8d413";
  private static final long SHARED_ID_BROKEN_FIRST_MAM = 458597019542200577L;
  private static final String SHARED_ID_BROKEN_FIRST_FILE = "71aeba68-e3b0-4f76-bb9c-b69a786087f2";
  private static final long SHARED_ID_BROKEN_SECOND_MAM = 458597019738341377L;
  private static final String SHARED_ID_BROKEN_SECOND_FILE = "4f1af0b2-cb76-405b-9ecc-6924f2bece84";
  private static final long SHARED_ID_OTHER_ROOM_MAM = 458597023449622529L;
  private static final String SHARED_ID_OTHER_ROOM_FILE = "76552c40-6547-4c5d-a5e8-8c7a67581fe3";
  private static final long REPLY_ATTACHMENT_MAM = 458596076490113025L;
  private static final String REPLY_ATTACHMENT_FILE = "069635b6-13ed-43d5-8e5c-0f0c63bbf09a";
  private static final long REPLY_CAPTION_MAM = 458596928978884865L;
  private static final String REPLY_CAPTION_FILE = "4acc5504-01a5-468e-b2bb-0833dc373e06";

  private static final long SURROGATE_CAPTION_MAM = 458597018163639297L;
  private static final long COMBINING_CAPTION_MAM = 458596909061548289L;
  private static final long WHITESPACE_CAPTION_MAM = 458596911697547009L;
  private static final long ESCAPED_BACKSLASH_CAPTION_MAM = 458597017763690753L;
  private static final long DOUBLE_ESCAPED_BACKSLASH_CAPTION_MAM = 458596911500752385L;
  private static final long CONTROL_CHARS_CAPTION_MAM = 458596912658557441L;
  private static final long MARKUP_LOOKALIKE_CAPTION_MAM = 458596912855165441L;
  private static final long IOS_FIVE_HEX_CAPTION_MAM = 458597021767810049L;
  private static final long TYPED_ESCAPE_HEADER_CAPTION_MAM = 458596926087755777L;
  private static final long TYPED_ESCAPE_IOS_HEADER_CAPTION_MAM = 458596926134895617L;
  private static final long EMPTY_UPPERCASE_ID_CAPTION_MAM = 458597022373363201L;

  private static final long AREA_LARGE_HISTORICAL_MAM = HISTORICAL_NO_KEYS_MAM;
  private static final long AREA_ZERO_MAM = 458596912460196865L;
  private static final String AREA_ZERO_FILE = "1bfe6226-3f33-4ae3-b733-d34ea7d978d7";
  private static final long AREA_HUGE_MAM = 458597020695635713L;
  private static final String AREA_HUGE_FILE = "740676a3-7fbf-4084-aa7b-a2a440479db0";
  private static final long AREA_SQUARE_MAM = ESCAPED_CAPTION_MAM;
  private static final long AREA_SMALL_MAM = 458596908382690049L;
  private static final String AREA_SMALL_FILE = "25e9e21b-5f8b-42ef-9863-d3e71c866048";
  private static final long AREA_WIDE_MAM = 458596908626787841L;
  private static final String AREA_WIDE_FILE = "109e5e34-75eb-490a-9797-cd9be5fd57a8";
  private static final long AREA_TALL_MAM = 458596908841123073L;
  private static final String AREA_TALL_FILE = "a1fd1b2a-c6c9-4d03-8a95-9ff7f94811d9";
  private static final String NO_AREA_PDF_FILE = "59c2df6b-8407-477c-a1ab-a5b1dd1e1e8a";

  private static final long WEB_RETRACTED_ATTACHMENT_MAM = 458596936993164289L;
  private static final String WEB_RETRACTED_ATTACHMENT_FILE =
      "a9ee15de-03a3-4146-bf43-a92aad7bf5a9";
  private static final long IOS_RETRACTED_ATTACHMENT_MAM = 458596171219829505L;
  private static final String IOS_RETRACTED_ATTACHMENT_FILE =
      "e9dc8c37-773a-4b9b-8ea2-d0a7079adafe";
  private static final String IOS_RETRACTED_ATTACHMENT_CLIENT_ID =
      "0C9E611B-46A8-46DB-ACED-5A2547B9E562";
  private static final long UPLOAD_RETRACTED_ATTACHMENT_MAM = 458597018757274881L;
  private static final String UPLOAD_RETRACTED_ATTACHMENT_FILE =
      "9d9c43e0-c7fe-425d-8935-128d0de7da3a";

  private static final String DANGLING_MESSAGE_ID_FILE = "869fde65-a79e-4011-9aff-33e06f82efc4";
  private static final String DANGLING_MESSAGE_ID = "8d18e613-4ac5-4adb-a832-c6439c33c8c5";
  private static final String ZOMBIE_ROOM_FILE = "93f00efb-6190-4b82-8951-c671db3b55ca";
  private static final String ROOM_AVATAR_FILE = "ad4bcc46-c14b-44c7-8f1f-6d74f9f64bb9";

  private static final long FILE_ROW_COUNT = 253L;
  private static final String UNTOUCHED_FILE_COLUMNS_DIGEST = "2fdb2e1a20a9e1b86860991c96c72879";

  private record Ref(String roomId, long mamId, String fileId) {}

  private static final List<Ref> FORWARDED_COPIES =
      List.of(
          new Ref(FORWARD_ROOM_ONE, 458596136851200513L, "7d0ff309-c5cb-48cd-8c22-0deaa67a6ed6"),
          new Ref(FORWARD_ROOM_TWO, 458596137576214273L, "b3419e49-5747-4bbd-b381-90061f4d20ea"),
          new Ref(FORWARD_ROOM_TWO, 458596138290045697L, "e51989da-1880-42b4-ab26-7bdb86289e08"),
          new Ref(FORWARD_ROOM_THREE, 458596929760653569L, "9f1c9c65-e14a-43bf-aaf4-397356b1560a"),
          new Ref(FORWARD_ROOM_THREE, 458596931459926017L, "d5cfe90b-aa12-48a3-a45b-f532c2a82d71"),
          new Ref(FORWARD_ROOM_THREE, 458596931897707265L, "c242d95d-6795-4a4f-ac6c-019a7fb0783a"),
          new Ref(FORWARD_ROOM_THREE, 458596932333171201L, "b8b73018-92b3-4d8c-bd09-a00b1394fe20"),
          new Ref(FORWARD_ROOM_THREE, 458596932767670017L, "6f387e2d-8c67-46d2-a04f-2a61086f3b32"),
          new Ref(FORWARD_ROOM_THREE, 458596938501819393L, "619e3644-5771-4c64-bcb3-b3a7265f8e22"),
          new Ref(CAPTION_SHAPES_ROOM, 458596935318918401L, "be3c1791-7085-4744-81d2-a12c0a56cd66"),
          new Ref(REPLY_CAPTION_ROOM, 458596931026141953L, "c232723a-1a34-46e3-8f61-1834c3c6c9f6"));

  private static final List<Ref> MISSING_FILE_ATTACHMENTS =
      List.of(
          new Ref(IOS_AND_WEB_ROOM, 458596171535397121L, "b93d1c4e-f70c-4df0-8139-87d408429792"),
          new Ref(CAPTION_SHAPES_ROOM, 458596936950493185L, "2d94d22d-06d8-4c72-b807-d91bdc29e7b8"),
          new Ref(CAPTION_SHAPES_ROOM, 458596937209546241L, "e7047089-85c6-4d45-b8ca-1628f666be4c"),
          new Ref(CAPTION_SHAPES_ROOM, 458596937376612865L, "be1cf2d9-dce4-4340-b51d-59c2eb1f937f"),
          new Ref(CAPTION_SHAPES_ROOM, 458596937419788801L, "37fae53b-6ff4-4773-9c65-1f9be8e99817"),
          new Ref(CAPTION_SHAPES_ROOM, 458596937466224641L, "625904ea-ca88-402b-a864-4eacb3d1d9a9"),
          new Ref(CAPTION_SHAPES_ROOM, 458596951435313409L, "261df26b-29f1-4087-beb7-f7d6c32c50d6"),
          new Ref(CAPTION_SHAPES_ROOM, 458596951477679361L, "8d94070c-9a0f-4faf-9ce3-19de463d31fe"),
          new Ref(BROKEN_EDGES_ROOM, 458597018562995201L, "34aea4d9-790d-4e99-b685-d377abf7d2f2"),
          new Ref(
              SHARED_CLIENT_ID_ROOM, 458596926410625537L, "f08c61c9-e862-4944-9c22-17a038db652e"));

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
        .orElseThrow(() -> new AssertionError("message not imported: " + roomId + "/" + mamId));
  }

  private static ImportedChats.File file(String fileId) {
    return chats.file(fileId).orElseThrow(() -> new AssertionError("file row missing: " + fileId));
  }

  private static void assertImportRan() {
    assertTrue(run.succeeded(), run::failureText);
    assertTrue(chats.count("SELECT count(*) FROM chats.message") > 0);
  }

  private static void assertLinked(String roomId, long mamId, String fileId) {
    ImportedChats.Message message = message(roomId, mamId);
    ImportedChats.File file = file(fileId);
    assertAll(
        () -> assertEquals(fileId, message.attachmentId()),
        () -> assertEquals(message.id(), file.messageId()),
        () -> assertEquals(roomId, file.roomId()),
        () -> assertEquals(false, message.deleted()));
  }

  private static void assertCaption(String roomId, long mamId, String fileId, String caption) {
    ImportedChats.Message message = message(roomId, mamId);
    assertAll(
        () -> assertEquals(fileId, message.attachmentId()),
        () -> assertEquals(caption, message.text()));
  }

  @Nested
  @DisplayName("ID-5: attachment_id and file_metadata.chat_message_id come from the attachment-id")
  class FileLinks {

    @Test
    @DisplayName("ID-5: a multipart upload row becomes a message linked both ways to its file")
    void multipartUploadIsLinkedBothWays() {
      assertLinked(ESCAPED_CAPTION_ROOM, ESCAPED_CAPTION_MAM, ESCAPED_CAPTION_FILE);
    }

    @Test
    @DisplayName("ID-5: the new id goes to chat_message_id, the old client id stays in message_id")
    void clientIdIsKeptNextToTheNewMessageId() {
      assertLinked(IOS_AND_WEB_ROOM, WEB_HEADER_CAPTION_MAM, WEB_HEADER_CAPTION_FILE);
      assertEquals(WEB_HEADER_CLIENT_ID, file(WEB_HEADER_CAPTION_FILE).legacyMessageId());
      assertEquals(
          ExpectedIds.message(IOS_AND_WEB_ROOM, WEB_HEADER_CAPTION_MAM),
          file(WEB_HEADER_CAPTION_FILE).messageId());
    }

    @Test
    @DisplayName("ID-5: an iOS uppercase client id on the file is kept, the link is the new id")
    void iosUppercaseClientIdIsKept() {
      assertLinked(IOS_AND_WEB_ROOM, IOS_UPPERCASE_ID_MAM, IOS_UPPERCASE_ID_FILE);
      assertEquals(IOS_UPPERCASE_CLIENT_ID, file(IOS_UPPERCASE_ID_FILE).legacyMessageId());
    }

    @Test
    @DisplayName("ID-5: files whose message_id was a non-UUID client id are linked to the new id")
    void nonUuidClientIdFilesAreLinked() {
      assertAll(
          () -> assertLinked(BROKEN_EDGES_ROOM, NON_UUID_CLIENT_ID_MAM, NON_UUID_CLIENT_ID_FILE),
          () ->
              assertLinked(
                  SHARED_CLIENT_ID_ROOM,
                  SECOND_NON_UUID_CLIENT_ID_MAM,
                  SECOND_NON_UUID_CLIENT_ID_FILE));
    }

    @Test
    @DisplayName("ID-5: files whose message_id was NULL but have a row are linked")
    void nullMessageIdFilesAreLinked() {
      assertAll(
          () -> assertLinked(BROKEN_EDGES_ROOM, NULL_MESSAGE_ID_MAM, NULL_MESSAGE_ID_FILE),
          () ->
              assertLinked(
                  SHARED_CLIENT_ID_ROOM, SECOND_NULL_MESSAGE_ID_MAM, SECOND_NULL_MESSAGE_ID_FILE));
    }

    @Test
    @DisplayName("ID-5/C71: attachments sharing one client id each link to their own message")
    void attachmentsSharingAClientIdLinkSeparately() {
      assertAll(
          () -> assertLinked(SHARED_CLIENT_ID_ROOM, SHARED_ID_FIRST_MAM, SHARED_ID_FIRST_FILE),
          () -> assertLinked(SHARED_CLIENT_ID_ROOM, SHARED_ID_SECOND_MAM, SHARED_ID_SECOND_FILE),
          () -> assertLinked(SHARED_CLIENT_ID_ROOM, SHARED_ID_THIRD_MAM, SHARED_ID_THIRD_FILE),
          () ->
              assertLinked(
                  BROKEN_EDGES_ROOM, SHARED_ID_BROKEN_FIRST_MAM, SHARED_ID_BROKEN_FIRST_FILE),
          () ->
              assertLinked(
                  BROKEN_EDGES_ROOM, SHARED_ID_BROKEN_SECOND_MAM, SHARED_ID_BROKEN_SECOND_FILE),
          () ->
              assertLinked(
                  SECOND_SHARED_CLIENT_ID_ROOM,
                  SHARED_ID_OTHER_ROOM_MAM,
                  SHARED_ID_OTHER_ROOM_FILE));
      assertEquals(
          3,
          chats.count(
              "SELECT count(DISTINCT chat_message_id) FROM chats.file_metadata"
                  + " WHERE id IN (?, ?, ?)",
              SHARED_ID_FIRST_FILE,
              SHARED_ID_SECOND_FILE,
              SHARED_ID_THIRD_FILE));
    }

    @Test
    @DisplayName("ID-5: a reply that carries an attachment is linked to its file")
    void replyWithAttachmentIsLinked() {
      assertLinked(REPLY_ATTACHMENT_ROOM, REPLY_ATTACHMENT_MAM, REPLY_ATTACHMENT_FILE);
    }

    @Test
    @DisplayName("ID-5/RG5.1-9: historical files with no message_id and no stanza_id are linked")
    void historicalFileWithoutAnyKeyIsLinked() {
      assertLinked(HISTORICAL_ROOM, HISTORICAL_NO_KEYS_MAM, HISTORICAL_NO_KEYS_FILE);
    }

    @Test
    @DisplayName(
        "ID-5/RG5.1-22: historical files with a NULL message_id and a stanza_id are linked")
    void historicalFileWithStanzaIdOnlyIsLinked() {
      assertLinked(HISTORICAL_ROOM, HISTORICAL_STANZA_ONLY_MAM, HISTORICAL_STANZA_ONLY_FILE);
    }

    @Test
    @DisplayName("ID-5: a historical file that already held a client id keeps it and is linked")
    void historicalFileWithClientIdIsLinked() {
      assertLinked(HISTORICAL_ROOM, HISTORICAL_CLIENT_ID_MAM, HISTORICAL_CLIENT_ID_FILE);
      assertEquals(HISTORICAL_CLIENT_ID, file(HISTORICAL_CLIENT_ID_FILE).legacyMessageId());
    }

    @Test
    @DisplayName("ID-5/RG5.1-22: all 145 historical attachments are linked both ways")
    void everyHistoricalAttachmentIsLinked() {
      assertAll(
          () ->
              assertEquals(
                  145,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE room_id = ? AND attachment_id IS"
                          + " NOT NULL",
                      HISTORICAL_ROOM)),
          () ->
              assertEquals(
                  145,
                  chats.count(
                      "SELECT count(*) FROM chats.file_metadata f JOIN chats.message m ON m.id ="
                          + " f.chat_message_id AND m.attachment_id = f.id WHERE f.room_id = ?",
                      HISTORICAL_ROOM)));
    }

    @Test
    @DisplayName(
        "ID-5: 236 linked uploads plus 3 retracted attachments make 239 linked pairs outside"
            + " forwards")
    void totalLinkedPairsMatchTheDump() {
      List<String> forwardedFiles = FORWARDED_COPIES.stream().map(Ref::fileId).toList();
      String placeholders = String.join(", ", forwardedFiles.stream().map(f -> "?").toList());
      assertEquals(
          239,
          chats.count(
              "SELECT count(*) FROM chats.file_metadata f JOIN chats.message m ON m.id ="
                  + " f.chat_message_id AND m.attachment_id = f.id WHERE f.id NOT IN ("
                  + placeholders
                  + ")",
              forwardedFiles.toArray()));
    }

    @Test
    @DisplayName("ID-5: no attachment_id is claimed by two messages")
    void noFileIsClaimedTwice() {
      assertImportRan();
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM (SELECT attachment_id FROM chats.message WHERE attachment_id"
                  + " IS NOT NULL GROUP BY attachment_id HAVING count(*) > 1) d"));
    }

    @Test
    @DisplayName("ID-5: no file_metadata.chat_message_id names a message that does not exist")
    void noFileKeepsADanglingMessageId() {
      assertImportRan();
      assertEquals(
          0,
          chats.count(
              "SELECT count(*) FROM chats.file_metadata f WHERE f.chat_message_id IS NOT NULL"
                  + " AND NOT EXISTS (SELECT 1 FROM chats.message m WHERE m.id ="
                  + " f.chat_message_id)"));
    }
  }

  @Nested
  @DisplayName("ID-V1: retracted attachments keep their file linked to the deleted message")
  class RetractedAttachments {

    private void assertDeletedAndLinked(String roomId, long mamId, String fileId) {
      ImportedChats.Message message = message(roomId, mamId);
      ImportedChats.File file = file(fileId);
      assertAll(
          () -> assertEquals(true, message.deleted()),
          () -> assertEquals("", message.text()),
          () -> assertEquals(fileId, message.attachmentId()),
          () -> assertEquals(message.id(), file.messageId()),
          () -> assertEquals(roomId, file.roomId()));
    }

    @Test
    @DisplayName("ID-V1/SEM-9: a web retracted attachment links its file to the deleted message")
    void webRetractedAttachmentIsLinked() {
      assertDeletedAndLinked(
          CAPTION_SHAPES_ROOM, WEB_RETRACTED_ATTACHMENT_MAM, WEB_RETRACTED_ATTACHMENT_FILE);
    }

    @Test
    @DisplayName("ID-V1/C151: an iOS retracted attachment links its file despite the client id")
    void iosRetractedAttachmentIsLinked() {
      assertDeletedAndLinked(
          IOS_AND_WEB_ROOM, IOS_RETRACTED_ATTACHMENT_MAM, IOS_RETRACTED_ATTACHMENT_FILE);
      assertEquals(
          IOS_RETRACTED_ATTACHMENT_CLIENT_ID,
          file(IOS_RETRACTED_ATTACHMENT_FILE).legacyMessageId());
    }

    @Test
    @DisplayName("ID-V1/C70: a retracted upload with a stanza id only keeps its file linked")
    void retractedUploadIsLinked() {
      assertDeletedAndLinked(
          BROKEN_EDGES_ROOM, UPLOAD_RETRACTED_ATTACHMENT_MAM, UPLOAD_RETRACTED_ATTACHMENT_FILE);
    }

    @Test
    @DisplayName("ID-V1: the files of retracted attachments are never deleted")
    void retractedAttachmentFilesAreKept() {
      assertAll(
          () -> assertNotNull(file(WEB_RETRACTED_ATTACHMENT_FILE)),
          () -> assertNotNull(file(IOS_RETRACTED_ATTACHMENT_FILE)),
          () -> assertNotNull(file(UPLOAD_RETRACTED_ATTACHMENT_FILE)));
    }
  }

  @Nested
  @DisplayName("SEM-9: forwards, missing files and gallery-only files")
  class FilesWithoutAOneToOneMessage {

    @Test
    @DisplayName("SEM-9/RG5.1-10: every forwarded attachment row is imported as a forward")
    void forwardedAttachmentsAreImportedAsForwards() {
      List<Executable> checks = new ArrayList<>();
      for (Ref ref : FORWARDED_COPIES) {
        checks.add(
            () -> {
              ImportedChats.Message message = message(ref.roomId(), ref.mamId());
              assertEquals(true, message.forwarded(), "forwarded " + ref.mamId());
              assertEquals(ref.fileId(), message.attachmentId(), "attachment " + ref.mamId());
            });
      }
      assertAll(checks);
    }

    @Test
    @DisplayName("SEM-9/RG5.1-10: a forward copy file is linked to the forward message")
    void forwardedCopyFilesAreLinkedToTheForward() {
      List<Executable> checks = new ArrayList<>();
      for (Ref ref : FORWARDED_COPIES) {
        checks.add(
            () -> {
              ImportedChats.Message message = message(ref.roomId(), ref.mamId());
              assertEquals(message.id(), file(ref.fileId()).messageId(), "file " + ref.fileId());
            });
      }
      assertAll(checks);
    }

    @Test
    @DisplayName(
        "SEM-9/O18/C6: an attachment whose file is gone becomes a deleted placeholder with"
            + " deleted_at = created_at")
    void missingFileAttachmentsBecomeDeletedPlaceholders() {
      List<Executable> checks = new ArrayList<>();
      for (Ref ref : MISSING_FILE_ATTACHMENTS) {
        checks.add(
            () -> {
              ImportedChats.Message message = message(ref.roomId(), ref.mamId());
              assertEquals("", message.text(), "text " + ref.mamId());
              assertEquals(true, message.deleted(), "deleted " + ref.mamId());
              assertNull(message.attachmentId(), "attachment " + ref.mamId());
              assertEquals(message.createdAt(), message.deletedAt(), "deleted_at " + ref.mamId());
            });
      }
      assertAll(checks);
    }

    @Test
    @DisplayName("SEM-9: attachment rows of a room absent from WSC are not imported")
    void attachmentRowsOfARoomAbsentFromWscAreNotImported() {
      assertImportRan();
      assertAll(
          () ->
              assertTrue(
                  chats.message(ROOM_ABSENT_FROM_WSC, 458596947235737601L).isEmpty(), "first row"),
          () ->
              assertTrue(
                  chats.message(ROOM_ABSENT_FROM_WSC, 458596947280222977L).isEmpty(),
                  "second row"));
    }

    @Test
    @DisplayName("SEM-9/C70: a file whose message_id names no archive row ends gallery-only")
    void danglingMessageIdIsNulled() {
      assertImportRan();
      ImportedChats.File file = file(DANGLING_MESSAGE_ID_FILE);
      assertAll(
          () -> assertNull(file.messageId()),
          () -> assertEquals(DANGLING_MESSAGE_ID, file.legacyMessageId()),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE attachment_id = ?",
                      DANGLING_MESSAGE_ID_FILE)));
    }

    @Test
    @DisplayName("SEM-9/C70: a file of a zombie room that named a message ends gallery-only")
    void zombieRoomFileIsNulled() {
      assertImportRan();
      ImportedChats.File file = file(ZOMBIE_ROOM_FILE);
      assertAll(
          () -> assertNull(file.messageId()),
          () -> assertEquals(ZOMBIE_ROOM, file.roomId()),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE room_id = ?", ZOMBIE_ROOM)));
    }

    @Test
    @DisplayName("SEM-9: a room avatar file is untouched by the attachment linking")
    void roomAvatarFileIsUntouched() {
      assertImportRan();
      ImportedChats.File file = file(ROOM_AVATAR_FILE);
      assertAll(
          () -> assertNull(file.messageId()),
          () -> assertNull(file.area()),
          () -> assertEquals(AVATAR_ROOM, file.roomId()),
          () ->
              assertEquals(
                  "ROOM_AVATAR",
                  chats
                      .query(
                          "SELECT type FROM chats.file_metadata WHERE id = ?",
                          rs -> rs.getString(1),
                          ROOM_AVATAR_FILE)
                      .get(0)));
    }

    @Test
    @DisplayName("SEM-9: the import never deletes a file row")
    void noFileRowIsDeleted() {
      assertImportRan();
      assertAll(
          () ->
              assertEquals(FILE_ROW_COUNT, chats.count("SELECT count(*) FROM chats.file_metadata")),
          () ->
              assertEquals(
                  252,
                  chats.count(
                      "SELECT count(*) FROM chats.file_metadata WHERE type = 'ATTACHMENT'")),
          () ->
              assertEquals(
                  1,
                  chats.count(
                      "SELECT count(*) FROM chats.file_metadata WHERE type = 'ROOM_AVATAR'")));
    }

    @Test
    @DisplayName("SEM-9: names, sizes, types, owners and rooms of every file are untouched")
    void fileColumnsOtherThanTheLinkAreUntouched() {
      assertImportRan();
      List<String> digest =
          chats.query(
              "SELECT md5(string_agg(concat_ws('|', id, name, original_size, mime_type, type,"
                  + " user_id, coalesce(room_id, '~')), E'\\n' ORDER BY id COLLATE \"C\")) FROM"
                  + " chats.file_metadata",
              rs -> rs.getString(1));
      assertEquals(List.of(UNTOUCHED_FILE_COLUMNS_DIGEST), digest);
    }

    @Test
    @DisplayName("SEM-9: unicode, bidi, quote and emoji file names are kept as stored")
    void weirdFileNamesAreKept() {
      assertImportRan();
      assertAll(
          () ->
              assertEquals(
                  "lorem\u202eips.umd", file("047bd71a-bd08-44bf-b8ef-3782cd4e1518").name()),
          () ->
              assertEquals(
                  "\uD83D\uDE00\uD83D\uDE80.lor",
                  file("5758e21b-bfe2-4b75-b839-8a578deeafd4").name()),
          () ->
              assertEquals("lorem2 \"i\".psu", file("1fef2953-1a10-400c-8ad9-233dfd8ef619").name()),
          () ->
              assertEquals("lorem ips\u00e8u \uD83D\uDE00.mdo", file(ESCAPED_CAPTION_FILE).name()),
          () ->
              assertEquals(
                  "lor \u00e9\u1f60" + "0.lor", file(IOS_RETRACTED_ATTACHMENT_FILE).name()),
          () -> assertEquals("l\u00e9or \uD83D\uDE00.emi", file(ZOMBIE_ROOM_FILE).name()));
    }
  }

  @Nested
  @DisplayName("SEM-10: the area is copied into file_metadata.area")
  class Area {

    @Test
    @DisplayName("SEM-10/C148: a large historical area is copied")
    void largeHistoricalAreaIsCopied() {
      assertLinked(HISTORICAL_ROOM, AREA_LARGE_HISTORICAL_MAM, HISTORICAL_NO_KEYS_FILE);
      assertEquals("2237x2237", file(HISTORICAL_NO_KEYS_FILE).area());
    }

    @Test
    @DisplayName("SEM-10: a 1x1 area is copied")
    void tinyAreaIsCopied() {
      assertLinked(ESCAPED_CAPTION_ROOM, AREA_SQUARE_MAM, ESCAPED_CAPTION_FILE);
      assertEquals("1x1", file(ESCAPED_CAPTION_FILE).area());
    }

    @Test
    @DisplayName("SEM-10: a 0x0 area is copied as stored")
    void zeroAreaIsCopiedAsStored() {
      assertLinked(CAPTION_SHAPES_ROOM, AREA_ZERO_MAM, AREA_ZERO_FILE);
      assertEquals("0x0", file(AREA_ZERO_FILE).area());
    }

    @Test
    @DisplayName("SEM-10: a 99999x99999 area is copied as stored")
    void hugeAreaIsCopiedAsStored() {
      assertLinked(BROKEN_EDGES_ROOM, AREA_HUGE_MAM, AREA_HUGE_FILE);
      assertEquals("99999x99999", file(AREA_HUGE_FILE).area());
    }

    @Test
    @DisplayName("SEM-10: portrait, landscape and thumbnail sized areas are copied")
    void ordinaryAreasAreCopied() {
      assertAll(
          () -> assertLinked(CAPTION_SHAPES_ROOM, AREA_SMALL_MAM, AREA_SMALL_FILE),
          () -> assertEquals("64x48", file(AREA_SMALL_FILE).area()),
          () -> assertLinked(CAPTION_SHAPES_ROOM, AREA_WIDE_MAM, AREA_WIDE_FILE),
          () -> assertEquals("1600x400", file(AREA_WIDE_FILE).area()),
          () -> assertLinked(CAPTION_SHAPES_ROOM, AREA_TALL_MAM, AREA_TALL_FILE),
          () -> assertEquals("300x1200", file(AREA_TALL_FILE).area()));
    }

    @Test
    @DisplayName("SEM-10: a linked attachment without an area keeps a NULL area")
    void attachmentWithoutAreaStaysNull() {
      assertLinked(HISTORICAL_ROOM, 456005044045356801L, NO_AREA_PDF_FILE);
      assertNull(file(NO_AREA_PDF_FILE).area());
    }

    @Test
    @DisplayName("SEM-10: forward copies, gallery-only files and retracted files have no area")
    void filesWithoutAnAreaInTheArchiveStayNull() {
      assertImportRan();
      List<Executable> checks = new ArrayList<>();
      for (Ref ref : FORWARDED_COPIES) {
        checks.add(() -> assertNull(file(ref.fileId()).area(), ref.fileId()));
      }
      checks.add(() -> assertNull(file(DANGLING_MESSAGE_ID_FILE).area()));
      checks.add(() -> assertNull(file(IOS_RETRACTED_ATTACHMENT_FILE).area()));
      assertAll(checks);
    }

    @Test
    @DisplayName("SEM-10: exactly the 29 files with an area in the archive carry one")
    void onlyFilesWithAnArchiveAreaCarryOne() {
      assertImportRan();
      assertEquals(
          29, chats.count("SELECT count(*) FROM chats.file_metadata WHERE area IS NOT NULL"));
    }
  }

  @Nested
  @DisplayName("TEXT-3: attachment captions")
  class Captions {

    @Test
    @DisplayName("TEXT-3: a fully escaped multipart caption is decoded, surrogate pair included")
    void escapedMultipartCaptionIsDecoded() {
      assertCaption(
          ESCAPED_CAPTION_ROOM,
          ESCAPED_CAPTION_MAM,
          ESCAPED_CAPTION_FILE,
          "lorem ipsu \u00e9 \uD83D\uDE00");
    }

    @Test
    @DisplayName(
        "TEXT-3/C04: an escaped caption decodes newline, quotes, angle brackets and ampersand")
    void escapedCaptionWithPunctuationIsDecoded() {
      assertCaption(
          IOS_AND_WEB_ROOM,
          ESCAPED_NEWLINE_CAPTION_MAM,
          "d5cf4650-f2f2-4c81-9a5c-50f7d9429961",
          "lore1\nmips2 \"umdolo\" <rsi> & tam");
    }

    @Test
    @DisplayName("TEXT-3/C06: two escaped surrogate pairs decode to two emoji")
    void escapedSurrogatePairsAreCombined() {
      assertCaption(
          BROKEN_EDGES_ROOM,
          SURROGATE_CAPTION_MAM,
          "f7938668-95a5-436e-8b84-4f88834bd7ab",
          "\uD83D\uDE00\uD83D\uDE80");
    }

    @Test
    @DisplayName("TEXT-3: combining marks, CJK and a ZWJ family sequence are decoded unchanged")
    void escapedCaptionKeepsCombiningAndZwjSequences() {
      assertCaption(
          CAPTION_SHAPES_ROOM,
          COMBINING_CAPTION_MAM,
          "5dbeb074-e343-43f2-9e44-83f634b6bcb5",
          "l\u0301 orem\u00e8 \u2014 \u2615 \u65e5\u672c\u8a9e"
              + " \uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67");
    }

    @Test
    @DisplayName("TEXT-3: an escaped whitespace-only caption keeps its three spaces")
    void escapedWhitespaceCaptionIsKept() {
      assertCaption(
          CAPTION_SHAPES_ROOM,
          WHITESPACE_CAPTION_MAM,
          "311c26ae-d58d-4fa1-84c5-d6a4e3354bc1",
          "   ");
    }

    @Test
    @DisplayName("TEXT-3/C10: an escaped backslash is not rescanned as the start of a new escape")
    void escapedBackslashIsNotRescanned() {
      assertAll(
          () ->
              assertCaption(
                  BROKEN_EDGES_ROOM,
                  ESCAPED_BACKSLASH_CAPTION_MAM,
                  "5fc24cd1-c4e2-420d-84b5-167d6fa1c7a4",
                  "L:\\orem\\u0041 lor \\"),
          () ->
              assertCaption(
                  CAPTION_SHAPES_ROOM,
                  DOUBLE_ESCAPED_BACKSLASH_CAPTION_MAM,
                  "02e605d9-3ba8-4391-8147-b261ce40ed02",
                  "loremip \\u0041 lor \\u00e8 lor E:\\mipsu\\mdo lor \\\\u0041"));
    }

    @Test
    @DisplayName("TEXT-3/TEXT-5: escaped XML-illegal control characters are dropped, DEL is kept")
    void escapedControlCharactersAreSanitized() {
      assertCaption(
          CAPTION_SHAPES_ROOM,
          CONTROL_CHARS_CAPTION_MAM,
          "7a5fae46-d9c8-4164-bd17-96c19eb2f523",
          "lore mip-sum dol\u007f ors");
    }

    @Test
    @DisplayName("TEXT-3: escaped markup lookalikes are decoded once and never unescaped again")
    void escapedMarkupLookalikesStayLiteral() {
      assertCaption(
          CAPTION_SHAPES_ROOM,
          MARKUP_LOOKALIKE_CAPTION_MAM,
          "6ead19cf-d040-45b6-82e8-f621020c6051",
          "]]> <![LOREM[i]]> &lt; &#x1F600; </lore></mipsumd>");
    }

    @Test
    @DisplayName("TEXT-3: a header-path caption is kept verbatim")
    void headerPathCaptionIsKept() {
      assertCaption(
          IOS_AND_WEB_ROOM,
          WEB_HEADER_CAPTION_MAM,
          WEB_HEADER_CAPTION_FILE,
          "loremip sumd \u00e9 \uD83D\uDE00");
    }

    @Test
    @DisplayName(
        "TEXT-3: a header-path caption keeps newline, quotes, angle brackets and ampersand")
    void headerPathCaptionKeepsPunctuation() {
      assertCaption(
          IOS_AND_WEB_ROOM,
          HEADER_NEWLINE_CAPTION_MAM,
          "1fef2953-1a10-400c-8ad9-233dfd8ef619",
          "lore1\nmips2 'loremi' <lor> & lor");
    }

    @Test
    @DisplayName("TEXT-4: an iOS corrupted 5-hex emoji in a caption is kept as stored")
    void iosFiveHexCaptionIsKept() {
      assertAll(
          () ->
              assertCaption(
                  IOS_AND_WEB_ROOM,
                  IOS_UPPERCASE_ID_MAM,
                  IOS_UPPERCASE_ID_FILE,
                  "lor emip2 \u1f60" + "0 \u00e9"),
          () ->
              assertCaption(
                  BROKEN_EDGES_ROOM,
                  IOS_FIVE_HEX_CAPTION_MAM,
                  "0267430a-b0d1-4bda-8fe0-5a6c01f6884e",
                  "lor emip \u00e8\u1f60" + "0"));
    }

    @Test
    @DisplayName("TEXT-3: a typed backslash-u in a header-path caption is decoded like any caption")
    void typedEscapeInHeaderCaptionIsDecoded() {
      assertAll(
          () ->
              assertCaption(
                  SHARED_CLIENT_ID_ROOM,
                  TYPED_ESCAPE_HEADER_CAPTION_MAM,
                  "8c3f4fed-cccd-4264-9951-8edc2028745b",
                  "loremi psumdol \u00e8 \uD83D\uDE00 'l' & <l> A"),
          () ->
              assertCaption(
                  SHARED_CLIENT_ID_ROOM,
                  TYPED_ESCAPE_IOS_HEADER_CAPTION_MAM,
                  "2d263411-6449-4419-b3a9-1d4e3b45483a",
                  "loremi psumdol \u00e8 \u1f60" + "0 'l' & <l> A"));
    }

    @Test
    @DisplayName("TEXT-3: an attachment without a caption gets an empty text")
    void noCaptionGivesEmptyText() {
      assertAll(
          () -> assertCaption(HISTORICAL_ROOM, HISTORICAL_NO_KEYS_MAM, HISTORICAL_NO_KEYS_FILE, ""),
          () ->
              assertCaption(
                  BROKEN_EDGES_ROOM,
                  EMPTY_UPPERCASE_ID_CAPTION_MAM,
                  "be0d3812-5faa-48aa-abce-0362b7f70393",
                  ""),
          () ->
              assertCaption(
                  REPLY_ATTACHMENT_ROOM, REPLY_ATTACHMENT_MAM, REPLY_ATTACHMENT_FILE, ""));
    }

    @Test
    @DisplayName("TEXT-3: all 145 historical attachments without a caption are imported with ''")
    void historicalAttachmentsHaveEmptyText() {
      assertAll(
          () ->
              assertEquals(
                  145,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE room_id = ? AND attachment_id IS"
                          + " NOT NULL AND text = ''",
                      HISTORICAL_ROOM)),
          () ->
              assertEquals(
                  145,
                  chats.count(
                      "SELECT count(*) FROM chats.message WHERE room_id = ? AND attachment_id IS"
                          + " NOT NULL",
                      HISTORICAL_ROOM)));
    }

    @Test
    @DisplayName("TEXT-3: a reply with an escaped caption decodes the caption")
    void replyCaptionIsDecoded() {
      assertCaption(REPLY_CAPTION_ROOM, REPLY_CAPTION_MAM, REPLY_CAPTION_FILE, "lore");
    }

    @Test
    @DisplayName(
        "TEXT-3: only the two captions with an escaped backslash still hold a backslash-u sequence")
    void noCaptionKeepsAnUndecodedEscape() {
      assertImportRan();
      assertEquals(
          2,
          chats.count(
              "SELECT count(*) FROM chats.message WHERE attachment_id IS NOT NULL AND text ~ ?",
              "\\\\u[0-9a-fA-F]{4}"));
    }
  }
}
