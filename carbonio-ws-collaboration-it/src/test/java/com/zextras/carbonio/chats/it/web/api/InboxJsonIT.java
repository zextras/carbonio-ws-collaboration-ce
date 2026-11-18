// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zextras.carbonio.chats.api.InboxApi;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class InboxJsonIT {

  private static final UUID ME = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  private static final UUID CHARLIE =
      MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
  private static final UUID LUCY =
      MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();
  private static final String TOKEN = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();

  private static final UUID GROUP_ROOM = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  private static final UUID ONE_TO_ONE_ROOM =
      UUID.fromString("00000000-0000-0000-0000-0000000000a2");
  private static final UUID TEMPORARY_ROOM =
      UUID.fromString("00000000-0000-0000-0000-0000000000a3");
  private static final UUID CLEARED_ROOM = UUID.fromString("00000000-0000-0000-0000-0000000000a4");
  private static final UUID FILE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

  private static final OffsetDateTime T0 =
      OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils utils;
  private final MeetingTestUtils meetingUtils;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final Database database;
  private UUID meetingId;

  public InboxJsonIT(
      InboxApi inboxApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils utils,
      MeetingTestUtils meetingUtils,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository,
      Database database) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.utils = utils;
    this.meetingUtils = meetingUtils;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.database = database;
    this.dispatcher.getRegistry().addSingletonResource(inboxApi);
  }

  @Test
  @DisplayName("The /inbox JSON for a realistic fixture is stable")
  void inboxJson() throws Exception {
    buildFixture();

    MockHttpResponse response = dispatcher.get("/inbox", TOKEN);

    assertEquals(200, response.getStatus());
    JsonNode actual = normalize(objectMapper.readTree(response.getContentAsString()));
    JsonNode expected =
        normalize(objectMapper.readTree(EXPECTED.replace("MEETING_ID", meetingId.toString())));
    assertEquals(expected, actual, actual::toPrettyString);
  }

  private void buildFixture() {
    utils.generateAndSaveRoom(
        GROUP_ROOM,
        RoomTypeDto.GROUP,
        "Group A",
        "Group description",
        List.of(ME, CHARLIE, LUCY),
        List.of(ME, LUCY),
        List.of(ME),
        T0.minusDays(3));
    utils.generateAndSaveFileMetadata(
        FILE_ID, "photo.png", "image/png", FileMetadataType.ATTACHMENT, 42L, CHARLIE, GROUP_ROOM);

    meetingId = meetingUtils.generateAndSaveMeeting(GROUP_ROOM, List.of());
    sql(
        "UPDATE chats.room SET meeting_id = :meeting WHERE id = :room",
        "meeting",
        meetingId.toString(),
        "room",
        GROUP_ROOM.toString());
    String original = "00000000-0000-0000-0000-0000000000b1";
    insertMessage(GROUP_ROOM, original, LUCY, T0.plusSeconds(10), null, null);
    insertMessage(
        GROUP_ROOM,
        "00000000-0000-0000-0000-0000000000b2",
        CHARLIE,
        T0.plusSeconds(20),
        null,
        null);
    String last = "00000000-0000-0000-0000-0000000000b3";
    insertMessage(GROUP_ROOM, last, CHARLIE, T0.plusSeconds(30), original, FILE_ID.toString());
    sql(
        "UPDATE chats.message SET edited = true, updated_at = :at WHERE id = :id",
        "at",
        T0.plusSeconds(35),
        "id",
        last);
    sql(
        "INSERT INTO chats.message_reaction (message_id, user_id, reaction, updated_at) VALUES"
            + " (:m, :u1, 'up', :at), (:m, :u2, 'up', :at), (:m, :u3, 'heart', :at)",
        "m",
        last,
        "u1",
        ME.toString(),
        "u2",
        CHARLIE.toString(),
        "u3",
        LUCY.toString(),
        "at",
        T0.plusSeconds(40));
    marker(GROUP_ROOM, ME, T0.plusSeconds(10));
    marker(GROUP_ROOM, CHARLIE, T0.plusSeconds(30).plusNanos(500_000_000));
    marker(GROUP_ROOM, LUCY, T0);
    roomTimes(GROUP_ROOM, T0.minusDays(5), T0.minusDays(2));
    lastSeen(CHARLIE, T0.minusHours(2));
    online(CHARLIE);
    lastSeen(LUCY, T0.minusHours(1).plusNanos(123_456_000));

    utils.generateAndSaveRoom(ONE_TO_ONE_ROOM, RoomTypeDto.ONE_TO_ONE, null, List.of(ME, CHARLIE));
    String eventId = "00000000-0000-0000-0000-0000000000e1";
    systemEventRepository.insert(
        SystemEvent.create()
            .id(eventId)
            .roomId(ONE_TO_ONE_ROOM.toString())
            .type("MESSAGE_PINNED")
            .actorId(CHARLIE.toString())
            .content("{\"messageId\":\"x\"}"));
    utils.setSystemEventTime(eventId, T0.plusSeconds(5));
    insertMessage(
        ONE_TO_ONE_ROOM, "00000000-0000-0000-0000-0000000000b4", ME, T0.plusSeconds(2), null, null);
    roomTimes(ONE_TO_ONE_ROOM, T0.minusDays(4), T0.minusDays(4));

    utils.generateAndSaveRoom(
        TEMPORARY_ROOM,
        RoomTypeDto.TEMPORARY,
        "Temporary",
        List.of(ME, LUCY),
        List.of(ME),
        null,
        null);
    roomTimes(TEMPORARY_ROOM, T0.minusDays(6), T0.minusDays(6));

    utils.generateAndSaveRoom(
        CLEARED_ROOM, RoomTypeDto.GROUP, "Cleared", List.of(ME, CHARLIE), List.of(ME), null, null);
    insertMessage(
        CLEARED_ROOM,
        "00000000-0000-0000-0000-0000000000b5",
        CHARLIE,
        T0.plusSeconds(10),
        null,
        null);
    insertMessage(
        CLEARED_ROOM,
        "00000000-0000-0000-0000-0000000000b6",
        CHARLIE,
        T0.plusSeconds(50),
        null,
        null);
    sql(
        "INSERT INTO chats.room_user_settings (user_id, room_id, cleared_at, created_at,"
            + " updated_at) VALUES (:u, :r, :at, :at, :at)",
        "u",
        ME.toString(),
        "r",
        CLEARED_ROOM.toString(),
        "at",
        T0.plusSeconds(30));
    marker(CLEARED_ROOM, ME, T0.plusSeconds(5));
    marker(CLEARED_ROOM, CHARLIE, T0.plusSeconds(40).plusNanos(123_456_000));
    roomTimes(CLEARED_ROOM, T0.minusDays(7), T0.minusDays(7));
    sql("UPDATE chats.subscription SET joined_at = :at", "at", T0.minusDays(1));
  }

  private void insertMessage(
      UUID room, String id, UUID sender, OffsetDateTime at, String replyTo, String attachmentId) {
    messageRepository.insert(
        Message.create()
            .id(id)
            .roomId(room.toString())
            .senderId(sender.toString())
            .text("text-" + id.substring(id.length() - 2))
            .replyToId(replyTo)
            .attachmentId(attachmentId));
    utils.setMessageTime(id, at);
  }

  private void marker(UUID room, UUID user, OffsetDateTime at) {
    sql(
        "INSERT INTO chats.read_marker (user_id, room_id, item_created_at) VALUES (:u, :r, :at)",
        "u",
        user.toString(),
        "r",
        room.toString(),
        "at",
        at);
  }

  private void roomTimes(UUID room, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    sql(
        "UPDATE chats.room SET created_at = :c, updated_at = :u WHERE id = :r",
        "c",
        createdAt,
        "u",
        updatedAt,
        "r",
        room.toString());
  }

  private void lastSeen(UUID user, OffsetDateTime at) {
    sql(
        "INSERT INTO chats.user_last_seen (user_id, last_seen_at) VALUES (:u, :at)",
        "u",
        user.toString(),
        "at",
        at);
  }

  private void online(UUID user) {
    sql(
        "INSERT INTO chats.ws_session (session_id, user_id, service_id) VALUES (:s, :u, 'svc')",
        "s",
        UUID.randomUUID().toString(),
        "u",
        user.toString());
  }

  private void sql(String statement, Object... params) {
    var update = database.sqlUpdate(statement);
    for (int i = 0; i < params.length; i += 2) {
      update.setParameter((String) params[i], params[i + 1]);
    }
    update.execute();
  }

  private JsonNode normalize(JsonNode root) {
    ObjectNode copy = root.deepCopy();
    ArrayNode conversations = (ArrayNode) copy.get("conversations");
    List<JsonNode> sorted = new ArrayList<>();
    conversations.forEach(sorted::add);
    sorted.sort(Comparator.comparing(c -> c.get("roomId").asText()));
    conversations.removeAll();
    for (JsonNode conversation : sorted) {
      sortArray(conversation.get("room").get("members"), "userId");
      sortArray(conversation.get("markers"), "userId");
      conversations.add(conversation);
    }
    return copy;
  }

  private void sortArray(JsonNode node, String key) {
    if (node == null || !node.isArray()) {
      return;
    }
    ArrayNode array = (ArrayNode) node;
    List<JsonNode> items = new ArrayList<>();
    array.forEach(items::add);
    items.sort(Comparator.comparing(i -> i.get(key).asText()));
    array.removeAll();
    items.forEach(array::add);
  }

  private static final String EXPECTED =
      """
      {
        "conversations": [
          {
            "roomId": "00000000-0000-0000-0000-0000000000a1",
            "room": {
              "id": "00000000-0000-0000-0000-0000000000a1",
              "name": "Group A",
              "description": "Group description",
              "type": "group",
              "members": [
                {
                  "userId": "332a9527-3388-4207-be77-6d7e2978a723",
                  "owner": true,
                  "online": false
                },
                {
                  "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                  "owner": false,
                  "online": true,
                  "lastActivity": "2026-06-01T08:00:00Z"
                },
                {
                  "userId": "ea7b9b61-bef5-4cf4-80cb-19612c42593a",
                  "owner": true,
                  "online": false,
                  "lastActivity": "2026-06-01T09:00:00.123456Z"
                }
              ],
              "userSettings": {
                "muted": true
              },
              "meetingId": "MEETING_ID",
              "pictureUpdatedAt": "2026-05-29T10:00:00Z",
              "createdAt": "2026-05-27T10:00:00Z",
              "updatedAt": "2026-05-30T10:00:00Z"
            },
            "lastItem": {
              "itemType": "message",
              "createdAt": "2026-06-01T10:00:30Z",
              "message": {
                "id": "00000000-0000-0000-0000-0000000000b3",
                "roomId": "00000000-0000-0000-0000-0000000000a1",
                "senderId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                "text": "text-b3",
                "createdAt": "2026-06-01T10:00:30Z",
                "replyToId": "00000000-0000-0000-0000-0000000000b1",
                "editedInfo": {
                  "editedAt": "2026-06-01T10:00:35Z"
                },
                "attachment": {
                  "id": "00000000-0000-0000-0000-0000000000f1",
                  "name": "photo.png",
                  "mimeType": "image/png",
                  "size": 42
                },
                "reactions": [
                  {
                    "reaction": "up",
                    "users": [
                      {
                        "userId": "332a9527-3388-4207-be77-6d7e2978a723",
                        "updatedAt": "2026-06-01T10:00:40Z"
                      },
                      {
                        "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                        "updatedAt": "2026-06-01T10:00:40Z"
                      }
                    ]
                  },
                  {
                    "reaction": "heart",
                    "users": [
                      {
                        "userId": "ea7b9b61-bef5-4cf4-80cb-19612c42593a",
                        "updatedAt": "2026-06-01T10:00:40Z"
                      }
                    ]
                  }
                ],
                "repliedMessage": {
                  "id": "00000000-0000-0000-0000-0000000000b1",
                  "roomId": "00000000-0000-0000-0000-0000000000a1",
                  "senderId": "ea7b9b61-bef5-4cf4-80cb-19612c42593a",
                  "text": "text-b1",
                  "createdAt": "2026-06-01T10:00:10Z",
                  "reactions": []
                }
              }
            },
            "unreadCount": 2,
            "muted": true,
            "markers": [
              {
                "itemCreatedAt": "2026-06-01T10:00:10Z",
                "userId": "332a9527-3388-4207-be77-6d7e2978a723"
              },
              {
                "itemCreatedAt": "2026-06-01T10:00:30.5Z",
                "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45"
              },
              {
                "itemCreatedAt": "2026-06-01T10:00:00Z",
                "userId": "ea7b9b61-bef5-4cf4-80cb-19612c42593a"
              }
            ]
          },
          {
            "roomId": "00000000-0000-0000-0000-0000000000a2",
            "room": {
              "id": "00000000-0000-0000-0000-0000000000a2",
              "type": "one_to_one",
              "members": [
                {
                  "userId": "332a9527-3388-4207-be77-6d7e2978a723",
                  "owner": true,
                  "online": false
                },
                {
                  "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                  "owner": true,
                  "online": true,
                  "lastActivity": "2026-06-01T08:00:00Z"
                }
              ],
              "userSettings": {
                "muted": false
              },
              "createdAt": "2026-05-28T10:00:00Z",
              "updatedAt": "2026-05-28T10:00:00Z"
            },
            "lastItem": {
              "itemType": "system_event",
              "createdAt": "2026-06-01T10:00:05Z",
              "systemEvent": {
                "id": "00000000-0000-0000-0000-0000000000e1",
                "roomId": "00000000-0000-0000-0000-0000000000a2",
                "type": "MESSAGE_PINNED",
                "actorId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                "createdAt": "2026-06-01T10:00:05Z",
                "content": {
                  "messageId": "x"
                }
              }
            },
            "unreadCount": 1,
            "muted": false,
            "markers": []
          },
          {
            "roomId": "00000000-0000-0000-0000-0000000000a3",
            "room": {
              "id": "00000000-0000-0000-0000-0000000000a3",
              "name": "Temporary",
              "type": "temporary",
              "members": [
                {
                  "userId": "332a9527-3388-4207-be77-6d7e2978a723",
                  "owner": true,
                  "online": false
                },
                {
                  "userId": "ea7b9b61-bef5-4cf4-80cb-19612c42593a",
                  "owner": false,
                  "online": false,
                  "lastActivity": "2026-06-01T09:00:00.123456Z"
                }
              ],
              "userSettings": {
                "muted": false
              },
              "createdAt": "2026-05-26T10:00:00Z",
              "updatedAt": "2026-05-26T10:00:00Z"
            },
            "unreadCount": 0,
            "muted": false,
            "markers": []
          },
          {
            "roomId": "00000000-0000-0000-0000-0000000000a4",
            "room": {
              "id": "00000000-0000-0000-0000-0000000000a4",
              "name": "Cleared",
              "type": "group",
              "members": [
                {
                  "userId": "332a9527-3388-4207-be77-6d7e2978a723",
                  "owner": true,
                  "online": false
                },
                {
                  "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                  "owner": false,
                  "online": true,
                  "lastActivity": "2026-06-01T08:00:00Z"
                }
              ],
              "userSettings": {
                "muted": false,
                "clearedAt": "2026-06-01T10:00:30Z"
              },
              "createdAt": "2026-05-25T10:00:00Z",
              "updatedAt": "2026-05-25T10:00:00Z"
            },
            "lastItem": {
              "itemType": "message",
              "createdAt": "2026-06-01T10:00:50Z",
              "message": {
                "id": "00000000-0000-0000-0000-0000000000b6",
                "roomId": "00000000-0000-0000-0000-0000000000a4",
                "senderId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45",
                "text": "text-b6",
                "createdAt": "2026-06-01T10:00:50Z",
                "reactions": []
              }
            },
            "unreadCount": 1,
            "muted": false,
            "markers": [
              {
                "itemCreatedAt": "2026-06-01T10:00:30Z",
                "userId": "332a9527-3388-4207-be77-6d7e2978a723"
              },
              {
                "itemCreatedAt": "2026-06-01T10:00:40.123456Z",
                "userId": "82735f6d-4c6c-471e-99d9-4eef91b1ec45"
              }
            ]
          }
        ]
      }
      """;
}
