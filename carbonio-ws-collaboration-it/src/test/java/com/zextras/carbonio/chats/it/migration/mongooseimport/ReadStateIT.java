// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.Message;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.ReadMarker;
import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: read state, last seen and contacts")
class ReadStateIT {

  private static final String TEXT_MARKER_ROOM = "026d176c-f9e9-44e6-a488-7565d7ae4eef";
  private static final String EMPTY_BODY_MARKER_ROOM = "132fe05c-2df1-4ce8-9738-85188aca781b";
  private static final String DANGLING_MARKER_ROOM = "158eebe3-11e6-4b25-9148-d34a500621ed";
  private static final String TEMPORARY_ROOM = "332d2b24-6fbc-45de-a6d3-9e0b16b69aea";
  private static final String TWO_USER_ONE_TO_ONE_ROOM = "61a47ae3-8c95-47f5-8ce8-2b8c22b46402";
  private static final String EDITED_MARKER_ROOM = "6457d489-8eeb-46f9-abf4-d14fb63ddc65";
  private static final String TOMBSTONE_ROOM = "7be2cb4f-4ce4-4530-b473-2df640007959";
  private static final String SPECIAL_CHARACTERS_ROOM = "9552b98c-cdf5-484e-9cfa-df1e2e7d8229";
  private static final String ONE_TO_ONE_MARKER_ROOM = "95bb865d-edbd-4450-85a5-9e8aca66ca8f";
  private static final String MEMBER_ADDED_ONE_TO_ONE_ROOM_A =
      "9688feea-bf86-4f63-b08d-a45d72f59287";
  private static final String REPLY_ONE_TO_ONE_ROOM = "99b17723-465e-4f87-bb1c-429687075b0d";
  private static final String MEMBER_ADDED_ONE_TO_ONE_ROOM_B =
      "cd093b9c-3434-4d5a-bd19-502d3b442faa";
  private static final String ATTACHMENT_ROOM_D1 = "d1d43aa7-32d2-49ce-a035-ac5ab4eeeffd";
  private static final String ATTACHMENT_ROOM_E0 = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final String DUPLICATE_CLIENT_ID_ROOM = "4df5be7a-ec3c-43f8-93ef-aae54ffdec78";
  private static final String FORWARD_ROOM = "f0cab8a6-2991-4ccb-a59b-49ada7879299";
  private static final String UPPERCASE_MARKER_ROOM = "fa0dfad7-7583-4b95-9bd4-a38e7431f1d9";
  private static final String UNIMPORTED_ROOM_WITH_MARKER_A =
      "1d5706c9-4038-4200-8ea9-8d311752eea9";
  private static final String UNIMPORTED_ROOM_WITH_MARKER_B =
      "58f2fef8-2314-44b7-991f-039aeeae6349";
  private static final String SYNTHETIC_MARKER_ROOM = "8a10332b-0a46-4a7f-8f1a-db54f0dabf02";

  private static final String USER_TEXT_MARKER = "a793945e-d600-5362-8ecf-bb6b1d88dbc3";
  private static final String USER_SECOND_TEXT_MARKER = "ba2c72b5-0128-5656-806c-8a163b58450e";
  private static final String USER_EARLIER_TEXT_MARKER = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";
  private static final String USER_FASTENING_MARKER = "231e8c22-87e0-5817-8960-c0e71b070652";
  private static final String USER_EMPTY_BODY_MARKER = "4b86ebf8-3973-57a3-b5e8-aff96036c8c4";
  private static final String USER_AFFILIATION_MARKER = "2fffefd7-dbe9-5a42-be01-5c95b0d9e4eb";
  private static final String USER_DANGLING_TEXT_MARKER = "556fe629-f88e-5d3e-bc5e-c54915c7adfa";
  private static final String USER_DEAD_BEEF_MARKER = "42ad6d39-3dfc-55de-a8db-f01680df2db3";
  private static final String USER_LONG_ID_MARKER = "831528cf-1a70-5bac-af91-2ad8ae4e239c";
  private static final String USER_EMPTY_ID_MARKER = "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8";
  private static final String USER_TEMPORARY_MARKER = "69e94bcc-f52d-550d-bee1-b40dc0368d0f";
  private static final String USER_TOMBSTONE_MARKER = "4f82bc10-f121-51ed-aed7-6738a8552323";
  private static final String USER_ONE_TO_ONE_MARKER = "2fffefd7-dbe9-5a42-be01-5c95b0d9e4eb";
  private static final String USER_FORWARD_MARKER = "b26c4bc8-1334-53a0-a372-becd06eb598b";
  private static final String USER_MEMBER_ADDED_MARKER_A = "1f9f9cc6-eac5-552d-b31b-f382633a1ad7";
  private static final String USER_MEMBER_ADDED_MARKER_B = "ffebb6fc-6c9b-58cf-86bb-6df35a3a30f8";
  private static final String USER_UNIMPORTED_ROOM_MARKER = "a0f0e228-9e1e-5aef-b2ac-1616214742c8";
  private static final String USER_ALL_READ_GROUP = "ac9fa7fc-0c1c-50ee-95ac-d027bce21970";
  private static final String USER_ALL_READ_ONE_TO_ONE = "d58d2bd7-5dd3-546b-b49f-60fde8a84e4e";
  private static final String USER_TWO_USER_ROOM_SECOND = "d7f47520-8d8f-5609-bb75-f7fc23f33ded";
  private static final String USER_EDITED_ROOM_FIRST = "6fe7e7cd-1ec7-517d-9902-c4b83886fe3f";
  private static final String USER_EDITED_ROOM_SECOND = "ec31dfc9-bc07-5474-86f2-ffa20548dbe4";
  private static final String USER_ATTACHMENT_D1_FIRST = "04920d70-5db0-55bf-b094-75cb05023d64";
  private static final String USER_ATTACHMENT_D1_SECOND = "3b6045dc-cb8c-5f87-8158-3c1a7b277a94";
  private static final String USER_ATTACHMENT_E0 = "97c353af-4374-5ff3-a3eb-3056c0b4dba9";

  private static final String USER_WITHOUT_MARKER_WHO_POSTED =
      "95e96aab-bbe9-5ef7-86a8-5d096f361932";
  private static final String USER_FORMER_MEMBER = "3b6045dc-cb8c-5f87-8158-3c1a7b277a94";

  private static final String USER_CASE_ONLY = "184dbd20-51ed-5451-ae7f-7a13e3995c8d";
  private static final String USER_STANZA_ID = "1f4645c9-5446-519d-81e0-2a9ee24ef783";
  private static final String USER_RECEIVED_TYPE = "8817c09b-3e70-5e64-95c4-dbc5567a4165";
  private static final String USER_ACKNOWLEDGED_TYPE = "907d4ce3-d89c-47b8-83b4-a1cb8ef4cb35";
  private static final String USER_PRIVATE_MESSAGE = "de7dbd2d-5822-4aae-a1e0-fedb164e664d";
  private static final String USER_NON_SUBSCRIBER = "04920d70-5db0-55bf-b094-75cb05023d64";
  private static final String USER_BEHIND = "a8ea4b6d-982e-5666-95bb-f1e6ae637742";
  private static final String USER_DUPLICATE_THREE_ROWS = "8f9408ed-9a1e-4449-a649-4e87e44f9106";
  private static final String USER_DUPLICATE_TWO_ROWS = "1e6e335f-223e-5f62-bc8d-dded8d0e753c";
  private static final String USER_ON_EDIT_ROW = "272d081d-2ce9-4b23-8a25-7dcf110a1ab7";
  private static final String USER_ON_REACTION_ROW = "e4d36ad6-3be9-4e80-a028-0a1702d42f46";
  private static final String USER_ON_RETRACT_ROW = "25bce295-3793-416e-83a2-2b5e7f98e22c";
  private static final String USER_AT_FIRST_DUPLICATE = "c8c8c8c8-0001-4a5b-8c6d-7e8f9a0b1c2d";
  private static final String USER_AT_SECOND_DUPLICATE = "c8c8c8c8-0002-4a5b-8c6d-7e8f9a0b1c2d";
  private static final String USER_BEFORE_EVERY_DUPLICATE = "c8c8c8c8-0003-4a5b-8c6d-7e8f9a0b1c2d";
  private static final String USER_ON_PRE_CLEAR_ROW = "2fb3d2bb-bd2e-4c80-bbbd-ca0842523b14";
  private static final String PRIVATE_MESSAGE_PEER = "5c89638f-9a69-5c9b-8fa6-b4b64e10351e";

  private static final String COPY_OF_SECOND_TEXT_MARKER = "4e9d5086-71be-4f02-8119-846f18ae1be1";
  private static final String COPY_OF_EARLIER_TEXT_MARKER = "9389918d-9c64-4a9a-b04d-f3bd0c0f3f2b";
  private static final String COPY_OF_REPLY_MARKER = "0687a2d3-1b7f-49a0-bc10-8b3f59619764";
  private static final String COPY_OF_TWO_USER_ROOM_SECOND_MARKER =
      "8b6d102d-2b75-4430-9fbb-c28f90a3bbcf";
  private static final String COPY_OF_EDITED_ROOM_FIRST_MARKER =
      "bde46e1f-8fb3-4bdd-8d07-90e1c47d891f";
  private static final String COPY_OF_EDITED_ROOM_SECOND_MARKER =
      "3a6694ff-c3de-49a4-966f-079580227fb4";
  private static final String COPY_OF_SPECIAL_CHARACTERS_MARKER =
      "ed6f2169-bb68-4643-a4c9-bcbadcfc24f4";
  private static final String COPY_OF_ATTACHMENT_D1_FIRST_MARKER =
      "ba98b107-d1dd-40d9-ae49-23d815f9441c";
  private static final String COPY_OF_ATTACHMENT_D1_SECOND_MARKER =
      "b5ee6ef7-5b9f-4bec-8a0c-dea88d232e23";
  private static final String COPY_OF_ATTACHMENT_D1_THIRD_MARKER =
      "53c57668-f346-4fbc-9d71-740345097586";
  private static final String COPY_OF_ATTACHMENT_E0_MARKER = "7cb05cfe-d0ae-4892-a1f4-27d3fdf87389";
  private static final String COPY_OF_DANGLING_TEXT_MARKER = "a5154cfd-eff5-4a24-896d-379281cec532";
  private static final String COPY_OF_AFFILIATION_MARKER = "3c130526-c991-436e-b8eb-7f2bb53e9cc6";
  private static final String COPY_OF_MEMBER_ADDED_MARKER_A =
      "b27b2675-a872-4c16-8b69-02eff6493a48";
  private static final String COPY_OF_MEMBER_ADDED_MARKER_B =
      "d2142b9a-820e-4fc6-a9e5-79fc9888d20f";
  private static final String COPY_OF_DEAD_BEEF_MARKER = "1d743ccd-e2ef-43f8-aaa6-c5ef49c8fcf3";
  private static final String COPY_OF_LONG_ID_MARKER = "98620900-9f9f-428e-bff6-c90d7f10e5ab";
  private static final String COPY_OF_EMPTY_ID_MARKER = "6f924f31-a97f-4a56-9cf3-c55c7ca1ab11";

  private static final long TEXT_MARKER_MAM = 458596104129542913L;
  private static final long EARLIER_TEXT_MARKER_MAM = 458596093446679297L;
  private static final long FASTENING_USER_TARGET_MAM = 458596048105158913L;
  private static final long EMPTY_BODY_ROW_MAM = 458596047264831489L;
  private static final long AFFILIATIONS_ROW_MAM = 458596937490769665L;
  private static final long DANGLING_ROOM_TEXT_MAM = 458596943211889409L;
  private static final long TEMPORARY_AFTER_CLEAR_TEXT_MAM = 458615893454507265L;
  private static final long TWO_USER_ROOM_FIRST_MAM = 458595995663630337L;
  private static final long TWO_USER_ROOM_SECOND_MAM = 458595994839588353L;
  private static final long EDITED_ROOM_FIRST_MAM = 458596167950717441L;
  private static final long EDITED_ROOM_SECOND_MAM = 458596163856904961L;
  private static final long TOMBSTONE_MAM = 458615896594573569L;
  private static final long SPECIAL_CHARACTERS_MAM = 458595355979477505L;
  private static final long ONE_TO_ONE_TEXT_MAM = 458596941744330241L;
  private static final String GROUP_MEMBER_ADDED_ROOM = "f0cab8a6-2991-4ccb-a59b-49ada7879299";
  private static final String GROUP_MEMBER_ADDED_CLIENT_ID = "8a300f3912a13d05";
  private static final String USER_ON_GROUP_MEMBER_ADDED_ROW =
      "5d0ab7c4-3e1f-5a2b-9c6d-7e8f9a0b1c2d";
  private static final long GROUP_MEMBER_ADDED_ROW_MAM = 458615899281732865L;
  private static final long MEMBER_ADDED_ROW_A_MAM = 456003398209645313L;
  private static final long REPLY_ONE_TO_ONE_MAM = 458596161647531265L;
  private static final long MEMBER_ADDED_ROW_B_MAM = 455227107284563969L;
  private static final long ATTACHMENT_D1_FIRST_MAM = 458596909061548289L;
  private static final long ATTACHMENT_D1_SECOND_MAM = 458596909901494529L;
  private static final long ATTACHMENT_D1_THIRD_MAM = 458596910507354369L;
  private static final long ATTACHMENT_E0_MAM = 458596909203672833L;
  private static final long FORWARD_ROW_MAM = 458615900807871233L;
  private static final long UPPERCASE_TEXT_MAM = 458595961492611585L;
  private static final long FIRST_SYNTHETIC_TEXT_MAM = 458597075442481153L;
  private static final long SECOND_SYNTHETIC_TEXT_MAM = 458597075442481409L;
  private static final long DUPLICATE_THREE_ROWS_FIRST_MAM = 458596910664776705L;
  private static final long DUPLICATE_THREE_ROWS_SECOND_MAM = 458596910764379393L;
  private static final long DUPLICATE_THREE_ROWS_LAST_MAM = 458596910865479937L;
  private static final long DUPLICATE_TWO_ROWS_FIRST_MAM = 458596911476750081L;
  private static final long DUPLICATE_TWO_ROWS_LAST_MAM = 458596911790366465L;
  private static final long EDIT_ROW_MAM = 458596922489575937L;
  private static final long REACTION_ROW_MAM = 458596165301699329L;
  private static final long RETRACT_REQUEST_ROW_MAM = 458596095370874113L;
  private static final long PRE_CLEAR_TEXT_MAM = 458615892177532929L;
  private static final long ATTACHMENT_D1_FIRST_USER_OWN_LAST_MAM = 458596951603375105L;
  private static final long SPECIAL_CHARACTERS_OWN_REACTION_MAM = 458595356064303105L;
  private static final long REPLY_ROOM_OWN_PIN_NOTIFICATION_MAM = 458596163195546625L;
  private static final long DEAD_BEEF_USER_OWN_LAST_MAM = 458596944363171329L;
  private static final long FORMER_MEMBER_OWN_MEMBER_ADDED_MAM = 458615896044660481L;

  private static final String FIRST_SYNTHETIC_TEXT_CLIENT_ID =
      "295b6218-92ba-4927-9d51-d7d4381b024b";
  private static final String FIRST_SYNTHETIC_TEXT_CLIENT_ID_UPPER =
      "295B6218-92BA-4927-9D51-D7D4381B024B";
  private static final String SECOND_SYNTHETIC_TEXT_CLIENT_ID =
      "5ba447dc-ac90-4bf9-913a-e58b32684d0c";
  private static final String SECOND_SYNTHETIC_TEXT_STANZA_ID = "CNA3JH0CGE81";
  private static final String DUPLICATE_THREE_ROWS_CLIENT_ID =
      "4ddba31f-4d76-4fe9-b6c3-0d8c0c3db8dd";
  private static final String DUPLICATE_TWO_ROWS_CLIENT_ID = "77f0a888-bc44-4200-8a46-936d37358c4c";
  private static final String EDIT_ROW_CLIENT_ID = "26ba2c6c-7ccd-4ca6-986b-ee1e787e71b1";
  private static final String REACTION_ROW_CLIENT_ID = "c68d2e81-fdde-4cae-bb5c-c1ff8ef05d07";
  private static final String RETRACT_REQUEST_ROW_CLIENT_ID =
      "888d6081-e2fe-4075-8ea9-00cd26235380";
  private static final String PRE_CLEAR_TEXT_CLIENT_ID = "f5c994de-7cd5-4e12-9a46-15e06a374aa7";
  private static final long SYNTHETIC_MARKER_MICROS = 1791500000123456L;
  private static final int SOURCE_MARKER_ROWS = 30;
  private static final int IMPORTED_MARKER_ROWS = 24;
  private static final int OWN_ROW_PAIRS = 92;
  private static final int OWN_ROW_ONLY_PAIRS = 69;
  private static final int UNANCHORED_ROWS = 19;
  private static final int VALID_SYNTHETIC_MARKER_ROWS = 25;

  private static ImportRun run;
  private static ImportedChats chats;
  private static ImportRun syntheticRun;
  private static String syntheticSource;
  private static ImportRun emptyMarkersRun;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  private static synchronized ImportRun syntheticRun() {
    if (syntheticRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      syntheticSource = database.newSourceDatabase();
      String mucRoom = SYNTHETIC_MARKER_ROOM + "@muclight.carbonio";
      database.execute(
          syntheticSource,
          marker(USER_CASE_ONLY, mucRoom, "D", FIRST_SYNTHETIC_TEXT_CLIENT_ID_UPPER),
          marker(USER_STANZA_ID, mucRoom, "D", SECOND_SYNTHETIC_TEXT_STANZA_ID),
          marker(USER_RECEIVED_TYPE, mucRoom, "R", SECOND_SYNTHETIC_TEXT_CLIENT_ID),
          marker(USER_ACKNOWLEDGED_TYPE, mucRoom, "A", SECOND_SYNTHETIC_TEXT_CLIENT_ID),
          marker(
              USER_PRIVATE_MESSAGE,
              PRIVATE_MESSAGE_PEER + "@carbonio",
              "D",
              SECOND_SYNTHETIC_TEXT_CLIENT_ID),
          marker(USER_NON_SUBSCRIBER, mucRoom, "D", SECOND_SYNTHETIC_TEXT_CLIENT_ID),
          marker(USER_BEHIND, mucRoom, "D", FIRST_SYNTHETIC_TEXT_CLIENT_ID),
          marker(
              USER_DUPLICATE_THREE_ROWS,
              ATTACHMENT_ROOM_E0 + "@muclight.carbonio",
              "D",
              DUPLICATE_THREE_ROWS_CLIENT_ID),
          marker(
              USER_DUPLICATE_TWO_ROWS,
              DUPLICATE_CLIENT_ID_ROOM + "@muclight.carbonio",
              "D",
              DUPLICATE_TWO_ROWS_CLIENT_ID),
          markerAt(
              USER_AT_FIRST_DUPLICATE,
              DUPLICATE_CLIENT_ID_ROOM + "@muclight.carbonio",
              DUPLICATE_TWO_ROWS_CLIENT_ID,
              DUPLICATE_TWO_ROWS_FIRST_MAM >> 8),
          markerAt(
              USER_AT_SECOND_DUPLICATE,
              ATTACHMENT_ROOM_E0 + "@muclight.carbonio",
              DUPLICATE_THREE_ROWS_CLIENT_ID,
              DUPLICATE_THREE_ROWS_SECOND_MAM >> 8),
          markerAt(
              USER_BEFORE_EVERY_DUPLICATE,
              ATTACHMENT_ROOM_E0 + "@muclight.carbonio",
              DUPLICATE_THREE_ROWS_CLIENT_ID,
              (DUPLICATE_THREE_ROWS_FIRST_MAM >> 8) - 1),
          marker(
              USER_ON_EDIT_ROW, ATTACHMENT_ROOM_E0 + "@muclight.carbonio", "D", EDIT_ROW_CLIENT_ID),
          marker(
              USER_ON_REACTION_ROW,
              EDITED_MARKER_ROOM + "@muclight.carbonio",
              "D",
              REACTION_ROW_CLIENT_ID),
          marker(
              USER_ON_RETRACT_ROW,
              TEXT_MARKER_ROOM + "@muclight.carbonio",
              "D",
              RETRACT_REQUEST_ROW_CLIENT_ID),
          marker(
              USER_ON_PRE_CLEAR_ROW,
              TEMPORARY_ROOM + "@muclight.carbonio",
              "D",
              PRE_CLEAR_TEXT_CLIENT_ID),
          marker(
              USER_ON_GROUP_MEMBER_ADDED_ROW,
              GROUP_MEMBER_ADDED_ROOM + "@muclight.carbonio",
              "D",
              GROUP_MEMBER_ADDED_CLIENT_ID),
          copyMarker(USER_SECOND_TEXT_MARKER, TEXT_MARKER_ROOM, COPY_OF_SECOND_TEXT_MARKER),
          copyMarker(USER_EARLIER_TEXT_MARKER, TEXT_MARKER_ROOM, COPY_OF_EARLIER_TEXT_MARKER),
          copyMarker(USER_TEXT_MARKER, REPLY_ONE_TO_ONE_ROOM, COPY_OF_REPLY_MARKER),
          copyMarker(
              USER_TWO_USER_ROOM_SECOND,
              TWO_USER_ONE_TO_ONE_ROOM,
              COPY_OF_TWO_USER_ROOM_SECOND_MARKER),
          copyMarker(USER_EDITED_ROOM_FIRST, EDITED_MARKER_ROOM, COPY_OF_EDITED_ROOM_FIRST_MARKER),
          copyMarker(
              USER_EDITED_ROOM_SECOND, EDITED_MARKER_ROOM, COPY_OF_EDITED_ROOM_SECOND_MARKER),
          copyMarker(
              USER_TWO_USER_ROOM_SECOND,
              SPECIAL_CHARACTERS_ROOM,
              COPY_OF_SPECIAL_CHARACTERS_MARKER),
          copyMarker(
              USER_ATTACHMENT_D1_FIRST, ATTACHMENT_ROOM_D1, COPY_OF_ATTACHMENT_D1_FIRST_MARKER),
          copyMarker(
              USER_ATTACHMENT_D1_SECOND, ATTACHMENT_ROOM_D1, COPY_OF_ATTACHMENT_D1_SECOND_MARKER),
          copyMarker(
              USER_TWO_USER_ROOM_SECOND, ATTACHMENT_ROOM_D1, COPY_OF_ATTACHMENT_D1_THIRD_MARKER),
          copyMarker(USER_ATTACHMENT_E0, ATTACHMENT_ROOM_E0, COPY_OF_ATTACHMENT_E0_MARKER),
          copyMarker(USER_DANGLING_TEXT_MARKER, DANGLING_MARKER_ROOM, COPY_OF_DANGLING_TEXT_MARKER),
          copyMarker(USER_AFFILIATION_MARKER, DANGLING_MARKER_ROOM, COPY_OF_AFFILIATION_MARKER),
          copyMarker(
              USER_MEMBER_ADDED_MARKER_A,
              MEMBER_ADDED_ONE_TO_ONE_ROOM_A,
              COPY_OF_MEMBER_ADDED_MARKER_A),
          copyMarker(
              USER_MEMBER_ADDED_MARKER_B,
              MEMBER_ADDED_ONE_TO_ONE_ROOM_B,
              COPY_OF_MEMBER_ADDED_MARKER_B),
          copyMarker(USER_DEAD_BEEF_MARKER, DANGLING_MARKER_ROOM, COPY_OF_DEAD_BEEF_MARKER),
          copyMarker(USER_LONG_ID_MARKER, DANGLING_MARKER_ROOM, COPY_OF_LONG_ID_MARKER),
          copyMarker(USER_EMPTY_ID_MARKER, EDITED_MARKER_ROOM, COPY_OF_EMPTY_ID_MARKER));
      syntheticRun =
          MongooseImport.run(
              database.newTargetDatabase(), database.credentialsFor(syntheticSource));
    }
    return syntheticRun;
  }

  private static synchronized ImportRun emptyMarkersRun() {
    if (emptyMarkersRun == null) {
      MongooseImportDatabase database = MongooseImportDatabase.get();
      String source = database.newSourceDatabase();
      database.execute(source, "DELETE FROM smart_markers");
      emptyMarkersRun =
          MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
    }
    return emptyMarkersRun;
  }

  private static String marker(String user, String toJid, String type, String msgId) {
    return markerAt(user, toJid, type, msgId, SYNTHETIC_MARKER_MICROS);
  }

  private static String markerAt(String user, String toJid, String msgId, long micros) {
    return markerAt(user, toJid, "D", msgId, micros);
  }

  private static String markerAt(
      String user, String toJid, String type, String msgId, long micros) {
    return "INSERT INTO smart_markers (lserver, luser, to_jid, thread, type, msg_id, timestamp)"
        + " VALUES ('carbonio', '"
        + user
        + "', '"
        + toJid
        + "', '', '"
        + type
        + "', '"
        + msgId
        + "', "
        + micros
        + ")";
  }

  private static String copyMarker(String user, String room, String copyUser) {
    return "INSERT INTO smart_markers (lserver, luser, to_jid, thread, type, msg_id, timestamp)"
        + " SELECT lserver, '"
        + copyUser
        + "', to_jid, thread, type, msg_id, timestamp FROM smart_markers WHERE luser = '"
        + user
        + "' AND to_jid = '"
        + room
        + "@muclight.carbonio'";
  }

  private static long ownRowsOf(ImportedChats imported, String user, String room) {
    return imported.count(
        "SELECT (SELECT count(*) FROM chats.message WHERE room_id = ? AND sender_id = ?)"
            + " + (SELECT count(*) FROM chats.system_event WHERE room_id = ? AND actor_id = ?)",
        room,
        user,
        room,
        user);
  }

  private static long sourceCount(String sourceDatabase, String sql, String... parameters) {
    try (Connection connection = MongooseImportDatabase.get().connect(sourceDatabase);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setString(i + 1, parameters[i]);
      }
      try (ResultSet rs = statement.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Cannot query " + sourceDatabase, e);
    }
  }

  private static long sourceMarkers(String sourceDatabase, String user, String room) {
    return sourceCount(
        sourceDatabase,
        "SELECT count(*) FROM smart_markers WHERE luser = ? AND to_jid LIKE ?",
        user,
        room + "@%");
  }

  private static long messagesOf(ImportedChats imported, String room) {
    return imported.count("SELECT count(*) FROM chats.message WHERE room_id = ?", room);
  }

  private static long violations(String sql) {
    assertTrue(chats.count("SELECT count(*) FROM chats.read_marker") > 0);
    return chats.count(sql);
  }

  private static ReadMarker readMarker(ImportedChats imported, String user, String room) {
    return imported
        .readMarker(user, room)
        .orElseThrow(() -> new AssertionError("No read_marker row for " + user + " in " + room));
  }

  private static Message message(ImportedChats imported, String room, long mam) {
    return imported
        .message(room, mam)
        .orElseThrow(() -> new AssertionError("Message not imported: " + room + " / " + mam));
  }

  private static void assertOnMessage(ImportedChats imported, String user, String room, long mam) {
    Message target = message(imported, room, mam);
    ReadMarker marker = readMarker(imported, user, room);
    assertEquals(target.createdAt(), marker.itemCreatedAt());
  }

  private static void assertOnSystemEvent(
      ImportedChats imported, String user, String room, long mam, String type) {
    SystemEvent event =
        imported
            .systemEvent(room, mam)
            .orElseThrow(() -> new AssertionError("System event not imported: " + mam));
    assertEquals(type, event.type());
    assertTrue(imported.message(room, mam).isEmpty());
    ReadMarker marker = readMarker(imported, user, room);
    assertEquals(event.createdAt(), marker.itemCreatedAt());
  }

  private static void assertOnUnimportedRow(
      ImportedChats imported, String user, String room, long mam) {
    assertAll(
        () -> assertTrue(messagesOf(imported, room) > 0),
        () -> assertTrue(imported.message(room, mam).isEmpty()),
        () -> assertTrue(imported.systemEvent(room, mam).isEmpty()));
    ReadMarker marker = readMarker(imported, user, room);
    assertEquals(ExpectedTimes.mamMillis(mam), marker.itemCreatedAt());
  }

  private static void assertNoRow(
      ImportRun importRun, String sourceDatabase, String user, String room) {
    assertAll(
        () -> assertTrue(importRun.succeeded(), importRun::failureText),
        () -> assertTrue(messagesOf(importRun.chats(), room) > 0),
        () -> assertEquals(1, sourceMarkers(sourceDatabase, user, room)),
        () -> assertTrue(importRun.chats().readMarker(user, room).isEmpty()));
  }

  @Nested
  @DisplayName("READ-D1: type D markers on rows imported as messages")
  class MarkersOnMessages {

    @Test
    @DisplayName("READ-D1: a marker on a text message takes the created_at of that message")
    void markerOnTextMessage() {
      assertOnMessage(chats, USER_TEXT_MARKER, TEXT_MARKER_ROOM, TEXT_MARKER_MAM);
    }

    @Test
    @DisplayName("READ-D1 / C4: a marker on an empty-body message takes the created_at of it")
    void markerOnEmptyBodyMessage() {
      assertOnMessage(chats, USER_EMPTY_BODY_MARKER, EMPTY_BODY_MARKER_ROOM, EMPTY_BODY_ROW_MAM);
    }

    @Test
    @DisplayName("READ-D1: [synthetic copies of real markers] different users get one row each")
    void severalUsersInTheSameRoom() {
      ImportedChats synthetic = syntheticRun().chats();
      assertOnMessage(synthetic, USER_TEXT_MARKER, TEXT_MARKER_ROOM, TEXT_MARKER_MAM);
      assertOnMessage(synthetic, COPY_OF_SECOND_TEXT_MARKER, TEXT_MARKER_ROOM, TEXT_MARKER_MAM);
      assertOnMessage(
          synthetic, COPY_OF_EARLIER_TEXT_MARKER, TEXT_MARKER_ROOM, EARLIER_TEXT_MARKER_MAM);
    }

    @Test
    @DisplayName("READ-D1 / TIME-6: a marker keeps the full microsecond precision of the message")
    void markerKeepsMicrosecondPrecision() {
      assertOnMessage(
          chats, USER_FASTENING_MARKER, EMPTY_BODY_MARKER_ROOM, FASTENING_USER_TARGET_MAM);
    }

    @Test
    @DisplayName("READ-D1: a marker on a message of a 1:1 room")
    void markerOnOneToOneMessage() {
      assertOnMessage(chats, USER_ONE_TO_ONE_MARKER, ONE_TO_ONE_MARKER_ROOM, ONE_TO_ONE_TEXT_MAM);
    }

    @Test
    @DisplayName("READ-D1: [synthetic copy of a real marker] a marker on a reply message of a 1:1")
    void markerOnReplyMessage() {
      assertOnMessage(
          syntheticRun().chats(),
          COPY_OF_REPLY_MARKER,
          REPLY_ONE_TO_ONE_ROOM,
          REPLY_ONE_TO_ONE_MAM);
    }

    @Test
    @DisplayName(
        "READ-D1: [real marker and synthetic copy] two markers of the same 1:1 room point to"
            + " their own messages")
    void twoMarkersInTheSameOneToOneRoom() {
      ImportedChats synthetic = syntheticRun().chats();
      assertOnMessage(
          synthetic, USER_ALL_READ_GROUP, TWO_USER_ONE_TO_ONE_ROOM, TWO_USER_ROOM_FIRST_MAM);
      assertOnMessage(
          synthetic,
          COPY_OF_TWO_USER_ROOM_SECOND_MARKER,
          TWO_USER_ONE_TO_ONE_ROOM,
          TWO_USER_ROOM_SECOND_MAM);
    }

    @Test
    @DisplayName("READ-D1: a marker on a row after the roomHistoryCleared of a TEMPORARY room")
    void markerAfterTemporaryClear() {
      assertOnMessage(chats, USER_TEMPORARY_MARKER, TEMPORARY_ROOM, TEMPORARY_AFTER_CLEAR_TEXT_MAM);
    }

    @Test
    @DisplayName("READ-D1: a marker on an uppercase iOS client id matches it exactly")
    void markerOnUppercaseClientId() {
      assertOnMessage(chats, USER_TEMPORARY_MARKER, UPPERCASE_MARKER_ROOM, UPPERCASE_TEXT_MAM);
    }

    @Test
    @DisplayName("READ-D1: a marker on a tombstone takes the created_at of the deleted message")
    void markerOnTombstone() {
      assertTrue(message(chats, TOMBSTONE_ROOM, TOMBSTONE_MAM).deleted());
      assertOnMessage(chats, USER_TOMBSTONE_MARKER, TOMBSTONE_ROOM, TOMBSTONE_MAM);
    }

    @Test
    @DisplayName("READ-D1: a marker on a forward row takes the created_at of the forwarded message")
    void markerOnForward() {
      assertTrue(message(chats, FORWARD_ROOM, FORWARD_ROW_MAM).forwarded());
      assertOnMessage(chats, USER_FORWARD_MARKER, FORWARD_ROOM, FORWARD_ROW_MAM);
    }

    @Test
    @DisplayName(
        "READ-D1: [synthetic copies of real markers] a marker on the original of an edited"
            + " message takes the created_at of that message")
    void markerOnEditedOriginal() {
      ImportedChats synthetic = syntheticRun().chats();
      assertTrue(message(synthetic, EDITED_MARKER_ROOM, EDITED_ROOM_FIRST_MAM).edited());
      assertOnMessage(
          synthetic, COPY_OF_EDITED_ROOM_FIRST_MARKER, EDITED_MARKER_ROOM, EDITED_ROOM_FIRST_MAM);
      assertOnMessage(
          synthetic, COPY_OF_EDITED_ROOM_SECOND_MARKER, EDITED_MARKER_ROOM, EDITED_ROOM_SECOND_MAM);
    }

    @Test
    @DisplayName(
        "READ-D1: [synthetic copy of a real marker] a marker on a message with special characters")
    void markerOnSpecialCharactersMessage() {
      assertOnMessage(
          syntheticRun().chats(),
          COPY_OF_SPECIAL_CHARACTERS_MARKER,
          SPECIAL_CHARACTERS_ROOM,
          SPECIAL_CHARACTERS_MAM);
    }

    @Test
    @DisplayName(
        "READ-D1: [synthetic copies of real markers] markers on attachment rows point to the"
            + " attachment messages")
    void markersOnAttachmentRows() {
      ImportedChats synthetic = syntheticRun().chats();
      assertAll(
          () ->
              assertOnMessage(
                  synthetic,
                  COPY_OF_ATTACHMENT_D1_FIRST_MARKER,
                  ATTACHMENT_ROOM_D1,
                  ATTACHMENT_D1_FIRST_MAM),
          () ->
              assertOnMessage(
                  synthetic,
                  COPY_OF_ATTACHMENT_D1_SECOND_MARKER,
                  ATTACHMENT_ROOM_D1,
                  ATTACHMENT_D1_SECOND_MAM),
          () ->
              assertOnMessage(
                  synthetic,
                  COPY_OF_ATTACHMENT_D1_THIRD_MARKER,
                  ATTACHMENT_ROOM_D1,
                  ATTACHMENT_D1_THIRD_MAM),
          () ->
              assertOnMessage(
                  synthetic, COPY_OF_ATTACHMENT_E0_MARKER, ATTACHMENT_ROOM_E0, ATTACHMENT_E0_MAM));
    }

    @Test
    @DisplayName(
        "READ-D1: [synthetic copy of a real marker] a marker on a text row of a room that also"
            + " has affiliation rows")
    void markerOnTextRowOfRoomWithAffiliationRows() {
      assertOnMessage(
          syntheticRun().chats(),
          COPY_OF_DANGLING_TEXT_MARKER,
          DANGLING_MARKER_ROOM,
          DANGLING_ROOM_TEXT_MAM);
    }
  }

  @Nested
  @DisplayName("READ-D2: markers on rows imported as system events")
  class MarkersOnSystemEvents {

    @Test
    @DisplayName(
        "READ-D2: [synthetic marker on a GROUP memberAdded row] no message and the event"
            + " created_at")
    void markerOnGroupMemberAddedRow() {
      assertOnSystemEvent(
          syntheticRun().chats(),
          USER_ON_GROUP_MEMBER_ADDED_ROW,
          GROUP_MEMBER_ADDED_ROOM,
          GROUP_MEMBER_ADDED_ROW_MAM,
          "MEMBER_ADDED");
    }
  }

  @Nested
  @DisplayName("READ-D3: markers on rows that are not imported keep the MAM row time")
  class MarkersOnUnimportedRows {

    @Test
    @DisplayName(
        "READ-D3: [synthetic copy of a real marker] a marker on a 1:1 memberAdded row has no"
            + " event and the MAM time")
    void markerOnOneToOneMemberAddedRowA() {
      assertOnUnimportedRow(
          syntheticRun().chats(),
          COPY_OF_MEMBER_ADDED_MARKER_A,
          MEMBER_ADDED_ONE_TO_ONE_ROOM_A,
          MEMBER_ADDED_ROW_A_MAM);
    }

    @Test
    @DisplayName(
        "READ-D3: [synthetic copy of a real marker] a marker on the memberAdded row that opens"
            + " another 1:1 room")
    void markerOnOneToOneMemberAddedRowB() {
      assertOnUnimportedRow(
          syntheticRun().chats(),
          COPY_OF_MEMBER_ADDED_MARKER_B,
          MEMBER_ADDED_ONE_TO_ONE_ROOM_B,
          MEMBER_ADDED_ROW_B_MAM);
    }

    @Test
    @DisplayName(
        "READ-D3: [synthetic copy of a real marker] a marker on an affiliation row has no"
            + " message and the MAM time")
    void markerOnAffiliationRow() {
      assertOnUnimportedRow(
          syntheticRun().chats(),
          COPY_OF_AFFILIATION_MARKER,
          DANGLING_MARKER_ROOM,
          AFFILIATIONS_ROW_MAM);
    }

    @Test
    @DisplayName("READ-D3: [synthetic marker on a real edit row] no message and the MAM time")
    void markerOnEditRow() {
      assertOnUnimportedRow(
          syntheticRun().chats(), USER_ON_EDIT_ROW, ATTACHMENT_ROOM_E0, EDIT_ROW_MAM);
    }

    @Test
    @DisplayName("READ-D3: [synthetic marker on a real reaction row] no message and the MAM time")
    void markerOnReactionRow() {
      assertOnUnimportedRow(
          syntheticRun().chats(), USER_ON_REACTION_ROW, EDITED_MARKER_ROOM, REACTION_ROW_MAM);
    }

    @Test
    @DisplayName("READ-D3: [synthetic marker on a real retract-request row] no message, MAM time")
    void markerOnRetractRequestRow() {
      assertOnUnimportedRow(
          syntheticRun().chats(), USER_ON_RETRACT_ROW, TEXT_MARKER_ROOM, RETRACT_REQUEST_ROW_MAM);
    }

    @Test
    @DisplayName("READ-D3: [synthetic marker on a skipped pre-clear row] no message, MAM time")
    void markerOnPreClearRow() {
      assertOnUnimportedRow(
          syntheticRun().chats(), USER_ON_PRE_CLEAR_ROW, TEMPORARY_ROOM, PRE_CLEAR_TEXT_MAM);
    }
  }

  @Nested
  @DisplayName("READ-D4: markers that produce no read_marker row")
  class IgnoredMarkers {

    @Test
    @DisplayName(
        "READ-D4: [synthetic copy of a real marker] a 00000000-dead-beef marker is dangling and"
            + " gets no row")
    void deadBeefMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, COPY_OF_DEAD_BEEF_MARKER, DANGLING_MARKER_ROOM);
    }

    @Test
    @DisplayName(
        "READ-D4: [synthetic copy of a real marker] a marker with a 100-character id gets no row")
    void hundredCharacterMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, COPY_OF_LONG_ID_MARKER, DANGLING_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D4: [synthetic copy of a real marker] a marker with an empty id gets no row")
    void emptyIdMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, COPY_OF_EMPTY_ID_MARKER, EDITED_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D4: markers of rooms that are not imported get no row")
    void markersOfRoomsNotImportedGetNoRow() {
      assertTrue(run.succeeded(), run::failureText);
      assertTrue(chats.count("SELECT count(*) FROM chats.message") > 0);
      assertAll(
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.room WHERE id IN (?, ?)",
                      UNIMPORTED_ROOM_WITH_MARKER_A,
                      UNIMPORTED_ROOM_WITH_MARKER_B)),
          () ->
              assertEquals(
                  1,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_UNIMPORTED_ROOM_MARKER,
                      UNIMPORTED_ROOM_WITH_MARKER_A)),
          () ->
              assertEquals(
                  1,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_MEMBER_ADDED_MARKER_A,
                      UNIMPORTED_ROOM_WITH_MARKER_B)),
          () ->
              assertEquals(
                  1,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_UNIMPORTED_ROOM_MARKER,
                      UNIMPORTED_ROOM_WITH_MARKER_B)),
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.read_marker WHERE room_id IN (?, ?)",
                      UNIMPORTED_ROOM_WITH_MARKER_A,
                      UNIMPORTED_ROOM_WITH_MARKER_B)));
    }

    @Test
    @DisplayName("READ-D6: a subscriber without any marker and without own rows gets no row")
    void subscriberWithoutMarkerGetsNoRow() {
      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () ->
              assertEquals(
                  1,
                  chats.count(
                      "SELECT count(*) FROM chats.subscription WHERE user_id = ? AND room_id = ?",
                      USER_ALL_READ_GROUP,
                      SYNTHETIC_MARKER_ROOM)),
          () -> assertTrue(messagesOf(chats, SYNTHETIC_MARKER_ROOM) > 0),
          () -> assertEquals(0, ownRowsOf(chats, USER_ALL_READ_GROUP, SYNTHETIC_MARKER_ROOM)),
          () ->
              assertEquals(
                  0,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_ALL_READ_GROUP,
                      SYNTHETIC_MARKER_ROOM)),
          () -> assertTrue(chats.readMarker(USER_ALL_READ_GROUP, SYNTHETIC_MARKER_ROOM).isEmpty()));
    }

    @Test
    @DisplayName(
        "READ-D4/D10: rows are the 24 resolvable type D markers of imported rooms plus the 69"
            + " users with own rows and no marker in a room")
    void onlyResolvableMarkersAndOwnRowsBecomeRows() {
      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () ->
              assertEquals(
                  SOURCE_MARKER_ROWS,
                  sourceCount(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      "SELECT count(*) FROM smart_markers")),
          () ->
              assertEquals(
                  IMPORTED_MARKER_ROWS + OWN_ROW_ONLY_PAIRS,
                  chats.count("SELECT count(*) FROM chats.read_marker")));
    }

    @Test
    @DisplayName("READ-D2: [synthetic] a marker whose id differs only by case gets no row")
    void caseOnlyDifferenceGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, USER_CASE_ONLY, SYNTHETIC_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D2: [synthetic] a marker carrying the stanza-id of a row gets no row")
    void stanzaIdMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, USER_STANZA_ID, SYNTHETIC_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D1: [synthetic] a type R marker with a valid id gets no row")
    void receivedTypeMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, USER_RECEIVED_TYPE, SYNTHETIC_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D1: [synthetic] a type A marker with a valid id gets no row")
    void acknowledgedTypeMarkerGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, USER_ACKNOWLEDGED_TYPE, SYNTHETIC_MARKER_ROOM);
    }

    @Test
    @DisplayName("READ-D1: [synthetic] a private-message marker (user@carbonio) gets no row")
    void privateMessageMarkerGetsNoRow() {
      ImportedChats synthetic = syntheticRun().chats();

      assertAll(
          () -> assertTrue(syntheticRun().succeeded(), () -> syntheticRun().failureText()),
          () ->
              assertEquals(
                  1, sourceMarkers(syntheticSource, USER_PRIVATE_MESSAGE, PRIVATE_MESSAGE_PEER)),
          () -> assertTrue(messagesOf(synthetic, SYNTHETIC_MARKER_ROOM) > 0),
          () ->
              assertEquals(
                  0,
                  synthetic.count(
                      "SELECT count(*) FROM chats.read_marker WHERE user_id = ?",
                      USER_PRIVATE_MESSAGE)));
    }

    @Test
    @DisplayName("READ-D4: [synthetic] the ignored shapes add no row beyond the valid ones")
    void ignoredShapesAddNoRows() {
      assertEquals(
          IMPORTED_MARKER_ROWS + OWN_ROW_ONLY_PAIRS + VALID_SYNTHETIC_MARKER_ROWS,
          syntheticRun().chats().count("SELECT count(*) FROM chats.read_marker"));
    }
  }

  @Nested
  @DisplayName("READ-D5: duplicate ids, markers behind own posts and non-subscribers")
  class SyntheticMarkerShapes {

    @Test
    @DisplayName("READ-D8: [synthetic] the import succeeds with every odd marker shape")
    void syntheticScenarioImports() {
      assertAll(
          () -> assertTrue(syntheticRun().succeeded(), () -> syntheticRun().failureText()),
          () ->
              assertEquals(
                  1,
                  syntheticRun()
                      .chats()
                      .count(
                          "SELECT count(*) FROM chats.flyway_schema_history WHERE version ="
                              + " '2.0.1' AND success")));
    }

    @Test
    @DisplayName("READ-D5: [synthetic marker on real duplicates] the highest MAM id of three wins")
    void highestMamIdOfThreeDuplicatesWins() {
      ImportedChats synthetic = syntheticRun().chats();
      Message first = message(synthetic, ATTACHMENT_ROOM_E0, DUPLICATE_THREE_ROWS_FIRST_MAM);
      Message second = message(synthetic, ATTACHMENT_ROOM_E0, DUPLICATE_THREE_ROWS_SECOND_MAM);
      Message last = message(synthetic, ATTACHMENT_ROOM_E0, DUPLICATE_THREE_ROWS_LAST_MAM);

      assertAll(
          () -> assertFalse(first.id().equals(last.id())),
          () -> assertFalse(second.id().equals(last.id())),
          () ->
              assertOnMessage(
                  synthetic,
                  USER_DUPLICATE_THREE_ROWS,
                  ATTACHMENT_ROOM_E0,
                  DUPLICATE_THREE_ROWS_LAST_MAM));
    }

    @Test
    @DisplayName("READ-D5: [synthetic marker on real duplicates] the highest MAM id of two wins")
    void highestMamIdOfTwoDuplicatesWins() {
      ImportedChats synthetic = syntheticRun().chats();
      Message first = message(synthetic, DUPLICATE_CLIENT_ID_ROOM, DUPLICATE_TWO_ROWS_FIRST_MAM);
      Message last = message(synthetic, DUPLICATE_CLIENT_ID_ROOM, DUPLICATE_TWO_ROWS_LAST_MAM);

      assertAll(
          () -> assertFalse(first.id().equals(last.id())),
          () ->
              assertOnMessage(
                  synthetic,
                  USER_DUPLICATE_TWO_ROWS,
                  DUPLICATE_CLIENT_ID_ROOM,
                  DUPLICATE_TWO_ROWS_LAST_MAM));
    }

    @Test
    @DisplayName(
        "READ-D5 / C8: [synthetic] a marker made when only the first duplicate existed resolves to"
            + " it")
    void markerAtTheFirstDuplicateResolvesToIt() {
      ImportedChats synthetic = syntheticRun().chats();
      assertOnMessage(
          synthetic,
          USER_AT_FIRST_DUPLICATE,
          DUPLICATE_CLIENT_ID_ROOM,
          DUPLICATE_TWO_ROWS_FIRST_MAM);
    }

    @Test
    @DisplayName(
        "READ-D5 / C8: [synthetic] a marker made at the second of three duplicates resolves to it")
    void markerAtTheSecondDuplicateResolvesToIt() {
      ImportedChats synthetic = syntheticRun().chats();
      assertOnMessage(
          synthetic, USER_AT_SECOND_DUPLICATE, ATTACHMENT_ROOM_E0, DUPLICATE_THREE_ROWS_SECOND_MAM);
    }

    @Test
    @DisplayName(
        "READ-D5 / C8: [synthetic] a marker made before every row with its client id gets no row")
    void markerBeforeEveryDuplicateGetsNoRow() {
      assertNoRow(syntheticRun(), syntheticSource, USER_BEFORE_EVERY_DUPLICATE, ATTACHMENT_ROOM_E0);
    }

    @Test
    @DisplayName(
        "READ-D10: [synthetic] a marker behind the user's own later post moves to that post")
    void markerBehindOwnPostMovesToIt() {
      ImportedChats synthetic = syntheticRun().chats();
      Message marked = message(synthetic, SYNTHETIC_MARKER_ROOM, FIRST_SYNTHETIC_TEXT_MAM);
      Message own = message(synthetic, SYNTHETIC_MARKER_ROOM, SECOND_SYNTHETIC_TEXT_MAM);

      assertAll(
          () -> assertEquals(USER_BEHIND, own.senderId()),
          () -> assertTrue(own.createdAt().isAfter(marked.createdAt())),
          () ->
              assertOnMessage(
                  synthetic, USER_BEHIND, SYNTHETIC_MARKER_ROOM, SECOND_SYNTHETIC_TEXT_MAM));
    }

    @Test
    @DisplayName(
        "READ-D7: [synthetic] the marker of a user that is not a subscriber is transcribed")
    void markerOfNonSubscriberIsTranscribed() {
      ImportedChats synthetic = syntheticRun().chats();

      assertAll(
          () ->
              assertEquals(
                  0,
                  synthetic.count(
                      "SELECT count(*) FROM chats.subscription WHERE user_id = ? AND room_id = ?",
                      USER_NON_SUBSCRIBER,
                      SYNTHETIC_MARKER_ROOM)),
          () ->
              assertOnMessage(
                  synthetic,
                  USER_NON_SUBSCRIBER,
                  SYNTHETIC_MARKER_ROOM,
                  SECOND_SYNTHETIC_TEXT_MAM));
    }
  }

  @Nested
  @DisplayName("READ-D10: the user's own rows count as read, as in MongooseIM's inbox")
  class OwnRowsCountAsRead {

    @Test
    @DisplayName("READ-D10: a marker behind the user's own later message moves to that message")
    void markerMovesToOwnLaterMessage() {
      Message own = message(chats, ATTACHMENT_ROOM_D1, ATTACHMENT_D1_FIRST_USER_OWN_LAST_MAM);

      assertAll(
          () -> assertEquals(USER_ATTACHMENT_D1_FIRST, own.senderId()),
          () ->
              assertTrue(
                  own.createdAt()
                      .isAfter(
                          message(chats, ATTACHMENT_ROOM_D1, ATTACHMENT_D1_FIRST_MAM).createdAt())),
          () ->
              assertEquals(
                  1,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_ATTACHMENT_D1_FIRST,
                      ATTACHMENT_ROOM_D1)),
          () ->
              assertOnMessage(
                  chats,
                  USER_ATTACHMENT_D1_FIRST,
                  ATTACHMENT_ROOM_D1,
                  ATTACHMENT_D1_FIRST_USER_OWN_LAST_MAM));
    }

    @Test
    @DisplayName("READ-D10: a later own row that is not imported (a reaction) gives its MAM time")
    void markerMovesToOwnLaterReactionRow() {
      assertOnUnimportedRow(
          chats,
          USER_TWO_USER_ROOM_SECOND,
          SPECIAL_CHARACTERS_ROOM,
          SPECIAL_CHARACTERS_OWN_REACTION_MAM);
    }

    @Test
    @DisplayName("READ-D10: a later own row imported as a system event gives the event time")
    void markerMovesToOwnLaterSystemEvent() {
      assertOnSystemEvent(
          chats,
          USER_TEXT_MARKER,
          REPLY_ONE_TO_ONE_ROOM,
          REPLY_ROOM_OWN_PIN_NOTIFICATION_MAM,
          "MESSAGE_PINNED");
    }

    @Test
    @DisplayName("READ-D10: a marker ahead of every own row of the user is kept")
    void markerAheadOfOwnRowsIsKept() {
      assertAll(
          () -> assertTrue(ownRowsOf(chats, USER_TEXT_MARKER, TEXT_MARKER_ROOM) > 0),
          () -> assertOnMessage(chats, USER_TEXT_MARKER, TEXT_MARKER_ROOM, TEXT_MARKER_MAM));
    }

    @Test
    @DisplayName("READ-D10: a subscriber without a marker who posted gets a row at the last post")
    void subscriberWithoutMarkerWhoPostedGetsRow() {
      assertAll(
          () ->
              assertEquals(
                  0,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_WITHOUT_MARKER_WHO_POSTED,
                      TEXT_MARKER_ROOM)),
          () ->
              assertEquals(
                  USER_WITHOUT_MARKER_WHO_POSTED,
                  message(chats, TEXT_MARKER_ROOM, TEXT_MARKER_MAM).senderId()),
          () ->
              assertOnMessage(
                  chats, USER_WITHOUT_MARKER_WHO_POSTED, TEXT_MARKER_ROOM, TEXT_MARKER_MAM));
    }

    @Test
    @DisplayName("READ-D10: a dangling marker gets no row of its own, the user's own rows still do")
    void danglingMarkerUserWithOwnRowsGetsOwnRowTime() {
      assertAll(
          () ->
              assertEquals(
                  1,
                  sourceMarkers(
                      MongooseImportDatabase.SOURCE_DATABASE,
                      USER_DEAD_BEEF_MARKER,
                      DANGLING_MARKER_ROOM)),
          () ->
              assertOnUnimportedRow(
                  chats, USER_DEAD_BEEF_MARKER, DANGLING_MARKER_ROOM, DEAD_BEEF_USER_OWN_LAST_MAM));
    }

    @Test
    @DisplayName("READ-D10: own rows of a user who is no longer a subscriber produce a row too")
    void formerMemberWithOwnRowsGetsRow() {
      assertAll(
          () ->
              assertEquals(
                  0,
                  chats.count(
                      "SELECT count(*) FROM chats.subscription WHERE user_id = ? AND room_id = ?",
                      USER_FORMER_MEMBER,
                      TOMBSTONE_ROOM)),
          () ->
              assertOnSystemEvent(
                  chats,
                  USER_FORMER_MEMBER,
                  TOMBSTONE_ROOM,
                  FORMER_MEMBER_OWN_MEMBER_ADDED_MAM,
                  "MEMBER_ADDED"));
    }
  }

  @Nested
  @DisplayName("READ-D8: the import never fails because of markers")
  class ImportNeverFailsBecauseOfMarkers {

    @Test
    @DisplayName("READ-D8: the import commits and no aborted-transaction error is ever raised")
    void importCommitsDespiteDanglingMarkers() {
      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () -> assertFalse(run.allOutput().contains("25P02")),
          () -> assertFalse(run.allOutput().contains("current transaction is aborted")),
          () ->
              assertEquals(
                  1,
                  chats.count(
                      "SELECT count(*) FROM chats.flyway_schema_history WHERE version = '2.0.1'"
                          + " AND success")));
    }

    @Test
    @DisplayName(
        "READ-D8/D10: an empty smart_markers table imports the archive; rows come only from own"
            + " rows")
    void emptyMarkersCreateRowsOnlyFromOwnRows() {
      ImportRun degenerate = emptyMarkersRun();

      assertAll(
          () -> assertTrue(degenerate.succeeded(), degenerate::failureText),
          () -> assertTrue(degenerate.chats().count("SELECT count(*) FROM chats.message") > 0),
          () ->
              assertEquals(
                  OWN_ROW_PAIRS,
                  degenerate.chats().count("SELECT count(*) FROM chats.read_marker")));
    }
  }

  @Nested
  @DisplayName("READ-D9: invariants of every imported read_marker row")
  class ReadRowInvariants {

    @Test
    @DisplayName("READ-D9: every read_marker row belongs to a room present in chats.room")
    void everyReadRowBelongsToAnImportedRoom() {
      assertEquals(
          0,
          violations(
              "SELECT count(*) FROM chats.read_marker mr WHERE NOT EXISTS (SELECT 1 FROM"
                  + " chats.room r WHERE r.id = mr.room_id)"));
    }

    @Test
    @DisplayName(
        "READ-D9: item_created_at is the created_at of a message or system event of the same room,"
            + " except for the 19 rows whose marker or last own row is not imported")
    void itemCreatedAtMatchesAnImportedItemTime() {
      assertEquals(
          UNANCHORED_ROWS,
          violations(
              "SELECT count(*) FROM chats.read_marker mr WHERE NOT EXISTS (SELECT 1 FROM"
                  + " chats.message m WHERE m.room_id = mr.room_id AND m.created_at ="
                  + " mr.item_created_at) AND NOT EXISTS (SELECT 1 FROM chats.system_event e"
                  + " WHERE e.room_id = mr.room_id AND e.created_at = mr.item_created_at)"));
    }

    @Test
    @DisplayName("READ-D9: user ids of read_marker rows are canonical lowercase UUIDs")
    void userIdsAreCanonical() {
      assertEquals(
          0,
          violations(
              "SELECT count(*) FROM chats.read_marker WHERE user_id !~"
                  + " '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'"));
    }
  }

  @Nested
  @DisplayName("READ-5 / APP-V1: last -> user_last_seen")
  class LastSeen {

    @Test
    @DisplayName("READ-5: every canonical user of the last table gets a user_last_seen row")
    void everyLastRowIsImported() {
      assertEquals(44, chats.count("SELECT count(*) FROM chats.user_last_seen"));
    }

    @Test
    @DisplayName("READ-5: an empty-state row keeps its seconds as UTC wall clock")
    void emptyStateRowIsImported() {
      assertEquals(
          Optional.of(LocalDateTime.parse("2026-02-28T11:30:43")),
          chats.lastSeen("feebaebf-9b1a-5b3b-9d80-8e6d1a4045c8"));
      assertEquals(
          Optional.of(LocalDateTime.parse("2026-10-07T15:31:16")),
          chats.lastSeen(USER_MEMBER_ADDED_MARKER_A));
    }

    @Test
    @DisplayName("READ-5: a stream_end row is imported, its state is dropped")
    void streamEndRowIsImported() {
      assertEquals(
          Optional.of(LocalDateTime.parse("2026-10-07T17:42:50")),
          chats.lastSeen(USER_ALL_READ_GROUP));
    }

    @Test
    @DisplayName("READ-5: a socket_error row is imported, its state is dropped")
    void socketErrorRowIsImported() {
      assertEquals(
          Optional.of(LocalDateTime.parse("2026-06-27T12:47:54")),
          chats.lastSeen(USER_UNIMPORTED_ROOM_MARKER));
    }

    @Test
    @DisplayName("READ-5: a socket_closed row is imported, its state is dropped")
    void socketClosedRowIsImported() {
      assertEquals(
          Optional.of(LocalDateTime.parse("2026-10-07T17:39:11")),
          chats.lastSeen("97c353af-4374-5ff3-a3eb-3056c0b4dba9"));
    }

    @Test
    @DisplayName("READ-5: a subscriber without a last row gets no user_last_seen row")
    void userWithoutLastRowGetsNone() {
      assertTrue(chats.count("SELECT count(*) FROM chats.user_last_seen") > 0);
      assertTrue(chats.lastSeen(USER_ALL_READ_ONE_TO_ONE).isEmpty());
    }
  }

  @Nested
  @DisplayName("READ-6 / APP-V1: user_contact derived from ONE_TO_ONE subscriptions")
  class Contacts {

    private static final String CONTACT_A = "1f9f9cc6-eac5-552d-b31b-f382633a1ad7";
    private static final String CONTACT_B = "d58d2bd7-5dd3-546b-b49f-60fde8a84e4e";

    @Test
    @DisplayName("READ-6: exactly the 20 directed pairs of the 10 ONE_TO_ONE rooms are contacts")
    void onlyOneToOnePairsAreContacts() {
      assertEquals(20, chats.count("SELECT count(*) FROM chats.user_contact"));
    }

    @Test
    @DisplayName("READ-6: a 1:1 room yields both directions with created_at = room created_at")
    void oneToOneRoomYieldsBothDirections() {
      LocalDateTime roomCreatedAt = LocalDateTime.parse("2026-06-12T11:21:14.310");

      assertAll(
          () ->
              assertEquals(
                  List.of(CONTACT_B, "ffebb6fc-6c9b-58cf-86bb-6df35a3a30f8"),
                  chats.contacts(CONTACT_A)),
          () -> assertEquals(List.of(CONTACT_A), chats.contacts(CONTACT_B)),
          () ->
              assertEquals(
                  List.of(roomCreatedAt, roomCreatedAt),
                  chats.query(
                      "SELECT created_at FROM chats.user_contact WHERE (user_id = ? AND"
                          + " contact_id = ?) OR (user_id = ? AND contact_id = ?) ORDER BY"
                          + " user_id",
                      rs -> rs.getObject(1, LocalDateTime.class),
                      CONTACT_A,
                      CONTACT_B,
                      CONTACT_B,
                      CONTACT_A)));
    }

    @Test
    @DisplayName("READ-6: roster B pairs without a 1:1 room are not contacts")
    void rosterPairsWithoutOneToOneRoomAreNotContacts() {
      assertEquals(
          List.of("a8ea4b6d-982e-5666-95bb-f1e6ae637742"),
          chats.contacts("04920d70-5db0-55bf-b094-75cb05023d64"));
      assertEquals(List.of(), chats.contacts(USER_FASTENING_MARKER));
    }

    @Test
    @DisplayName("READ-6: non-B roster rows (T, F, N) are not contacts")
    void nonBothRosterRowsAreNotContacts() {
      assertEquals(
          List.of("be7b38ab-c669-5de9-b8d5-baf31ed4ad06", "bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8"),
          chats.contacts(USER_TEXT_MARKER));
      assertEquals(
          List.of(USER_TEXT_MARKER), chats.contacts("bea9390a-0b2f-5ca0-a0a1-c7c26532d2f8"));
      assertEquals(List.of(USER_ONE_TO_ONE_MARKER), chats.contacts(USER_DEAD_BEEF_MARKER));
    }
  }
}
