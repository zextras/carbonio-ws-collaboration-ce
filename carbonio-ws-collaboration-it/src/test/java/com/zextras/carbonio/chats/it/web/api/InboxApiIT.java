// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.InboxApi;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class InboxApiIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final Database database;
  private final AppClock clock;
  private final WsPresenceRepository wsPresenceRepository;

  private static UUID meId;
  private static UUID otherId;
  private static String meToken;

  private static final OffsetDateTime T0 =
      OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
  private static final OffsetDateTime T1 = T0.plusSeconds(10);
  private static final OffsetDateTime T2 = T0.plusSeconds(20);
  private static final OffsetDateTime T3 = T0.plusSeconds(30);
  private static final OffsetDateTime T4 = T0.plusSeconds(40);
  private static final OffsetDateTime T5 = T0.plusSeconds(50);

  private Room room;
  private String roomId;

  public InboxApiIT(
      RoomsApi roomsApi,
      InboxApi inboxApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository,
      Database database,
      Clock clock,
      WsPresenceRepository wsPresenceRepository) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.database = database;
    this.clock = (AppClock) clock;
    this.wsPresenceRepository = wsPresenceRepository;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.dispatcher.getRegistry().addSingletonResource(inboxApi);
  }

  @BeforeAll
  static void initAll() {
    meId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    meToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    otherId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
  }

  @BeforeEach
  void setUp() {
    roomId = UUID.randomUUID().toString();
    room =
        integrationTestUtils.generateAndSaveRoom(
            UUID.fromString(roomId), RoomTypeDto.GROUP, "inbox-test-room", List.of(meId, otherId));
    setJoinedAt(meId, T0.minusDays(1));
  }

  private void setJoinedAt(UUID userId, OffsetDateTime joinedAt) {
    Subscription subscription =
        database.find(Subscription.class, new SubscriptionId(roomId, userId.toString()));
    database.update(subscription.joinedAt(joinedAt));
  }

  private void clearHistoryAt(OffsetDateTime clearedAt) {
    RoomUserSettings settings =
        integrationTestUtils
            .getRoomUserSettings(UUID.fromString(roomId), meId)
            .orElseGet(() -> RoomUserSettings.create(room, meId.toString()));
    integrationTestUtils.setRoomUserSettings(settings.clearedAt(clearedAt));
  }

  private Message message(String id, UUID sender, OffsetDateTime at, boolean deleted, String file) {
    clock.fixTimeAt(at.toInstant());
    try {
      return messageRepository.insert(
          Message.create()
              .id(id)
              .roomId(roomId)
              .senderId(sender.toString())
              .text("text-" + id)
              .deleted(deleted)
              .attachmentId(file));
    } finally {
      clock.removeFixTime();
    }
  }

  private String message(UUID sender, OffsetDateTime at) {
    String id = UUID.randomUUID().toString();
    message(id, sender, at, false, null);
    return id;
  }

  private String deletedMessage(UUID sender, OffsetDateTime at) {
    String id = UUID.randomUUID().toString();
    message(id, sender, at, true, null);
    return id;
  }

  private String event(String id, UUID actor, OffsetDateTime at) {
    systemEventRepository.insert(
        SystemEvent.create()
            .id(id)
            .roomId(roomId)
            .type("MESSAGE_PINNED")
            .actorId(actor.toString())
            .content("{\"k\":\"v\"}")
            .createdAt(at));
    return id;
  }

  private String event(UUID actor, OffsetDateTime at) {
    return event(UUID.randomUUID().toString(), actor, at);
  }

  private void bulkMessages(UUID sender, int count) {
    database
        .sqlUpdate(
            "INSERT INTO chats.message (id, room_id, sender_id, text, created_at, updated_at)"
                + " SELECT :roomId || '-m-' || CAST(g AS text), :roomId, :sender, 'bulk',"
                + " TIMESTAMP '2026-06-01 10:00:10' + g * interval '1 second',"
                + " TIMESTAMP '2026-06-01 10:00:10' + g * interval '1 second'"
                + " FROM generate_series(1, :count) g")
        .setParameter("roomId", roomId)
        .setParameter("sender", sender.toString())
        .setParameter("count", count)
        .execute();
  }

  private void bulkEvents(UUID actor, int count) {
    database
        .sqlUpdate(
            "INSERT INTO chats.system_event (id, room_id, type, actor_id, content, created_at)"
                + " SELECT :roomId || '-e-' || CAST(g AS text), :roomId, 'MESSAGE_PINNED',"
                + " :actor, '{}', TIMESTAMP '2026-06-01 10:00:10' + g * interval '1 second'"
                + " FROM generate_series(1, :count) g")
        .setParameter("roomId", roomId)
        .setParameter("actor", actor.toString())
        .setParameter("count", count)
        .execute();
  }

  private String reply(UUID sender, OffsetDateTime at, String replyToId) {
    String id = UUID.randomUUID().toString();
    clock.fixTimeAt(at.toInstant());
    try {
      messageRepository.insert(
          Message.create()
              .id(id)
              .roomId(roomId)
              .senderId(sender.toString())
              .text("reply-" + id)
              .replyToId(replyToId));
    } finally {
      clock.removeFixTime();
    }
    return id;
  }

  private void markRead(String itemId) throws Exception {
    MockHttpResponse response =
        dispatcher.put(
            String.format("/rooms/%s/read", roomId),
            objectMapper.writeValueAsString(Map.of("itemId", itemId)),
            MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken());
    assertEquals(204, response.getStatus());
  }

  private JsonNode conversation() throws Exception {
    MockHttpResponse response = dispatcher.get("/inbox", meToken);
    assertEquals(200, response.getStatus());
    for (JsonNode c : objectMapper.readTree(response.getContentAsString()).get("conversations")) {
      if (roomId.equals(c.get("room").get("id").asText())) {
        return c;
      }
    }
    throw new AssertionError("room must be in the inbox");
  }

  private int unread() throws Exception {
    return conversation().get("unreadCount").asInt();
  }

  @Nested
  @DisplayName("lastItem")
  class LastItem {

    @Test
    @DisplayName("A newer message wins over an older event, with attachment metadata")
    void messageNewerThanEvent() throws Exception {
      UUID fileId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveFileMetadata(
          fileId,
          "photo.png",
          "image/png",
          FileMetadataType.ATTACHMENT,
          42L,
          otherId,
          UUID.fromString(roomId));
      event(otherId, T1);
      String messageId = UUID.randomUUID().toString();
      message(messageId, otherId, T2, false, fileId.toString());

      JsonNode c = conversation();

      JsonNode item = c.get("lastItem");
      assertEquals("message", item.get("itemType").asText());
      assertEquals(
          T2.toInstant(), OffsetDateTime.parse(item.get("createdAt").asText()).toInstant());
      JsonNode msg = item.get("message");
      assertEquals(messageId, msg.get("id").asText());
      assertEquals(roomId, msg.get("roomId").asText());
      assertEquals(otherId.toString(), msg.get("senderId").asText());
      assertEquals("photo.png", msg.get("attachment").get("name").asText());
      assertEquals("image/png", msg.get("attachment").get("mimeType").asText());
      assertEquals(42, msg.get("attachment").get("size").asInt());
      assertFalse(item.has("systemEvent"));
      assertFalse(c.has("lastMessage"));
    }

    @Test
    @DisplayName("A newer event wins over an older message")
    void eventNewerThanMessage() throws Exception {
      String messageId = message(otherId, T1);
      String eventId = event(meId, T2);

      JsonNode c = conversation();

      JsonNode item = c.get("lastItem");
      assertEquals("system_event", item.get("itemType").asText());
      assertEquals(
          T2.toInstant(), OffsetDateTime.parse(item.get("createdAt").asText()).toInstant());
      JsonNode ev = item.get("systemEvent");
      assertEquals(eventId, ev.get("id").asText());
      assertEquals(roomId, ev.get("roomId").asText());
      assertEquals("MESSAGE_PINNED", ev.get("type").asText());
      assertEquals(meId.toString(), ev.get("actorId").asText());
      assertEquals("v", ev.get("content").get("k").asText());
      assertFalse(item.has("message"));
      assertFalse(c.has("lastMessage"));
    }

    @Test
    @DisplayName("On equal timestamps the greater id wins, like the timeline ordering")
    void tieBrokenById() throws Exception {
      message("00000000-0000-0000-0000-00000000000a", otherId, T1, false, null);
      event("00000000-0000-0000-0000-00000000000b", otherId, T1);

      assertEquals("system_event", conversation().get("lastItem").get("itemType").asText());
    }

    @Test
    @DisplayName("On equal timestamps the message wins when its id is greater")
    void tieBrokenByIdMessageWins() throws Exception {
      event("00000000-0000-0000-0000-00000000000a", otherId, T1);
      message("00000000-0000-0000-0000-00000000000b", otherId, T1, false, null);

      assertEquals("message", conversation().get("lastItem").get("itemType").asText());
    }

    @Test
    @DisplayName("A last reply carries its visible original as repliedMessage")
    void replyCarriesRepliedMessage() throws Exception {
      String original = message(otherId, T1);
      String reply = reply(meId, T2, original);

      JsonNode msg = conversation().get("lastItem").get("message");

      assertEquals(reply, msg.get("id").asText());
      assertEquals(original, msg.get("replyToId").asText());
      assertEquals(original, msg.get("repliedMessage").get("id").asText());
      assertEquals("text-" + original, msg.get("repliedMessage").get("text").asText());
      assertFalse(msg.get("repliedMessage").has("repliedMessage"));
    }

    @Test
    @DisplayName("An original hidden by cleared history is not exploded")
    void hiddenOriginalNotExploded() throws Exception {
      String original = message(otherId, T1);
      String reply = reply(meId, T3, original);
      clearHistoryAt(T2);

      JsonNode msg = conversation().get("lastItem").get("message");

      assertEquals(reply, msg.get("id").asText());
      assertFalse(msg.has("replyToId"));
      assertFalse(msg.has("repliedMessage"));
    }

    @Test
    @DisplayName("An original in another room is not exploded")
    void originalInOtherRoomNotExploded() throws Exception {
      String otherRoomId = UUID.randomUUID().toString();
      integrationTestUtils.generateAndSaveRoom(
          UUID.fromString(otherRoomId), RoomTypeDto.GROUP, "other", List.of(meId, otherId));
      String foreign = UUID.randomUUID().toString();
      integrationTestUtils.generateAndSaveMessage(
          foreign, UUID.fromString(otherRoomId), otherId, "foreign");
      reply(meId, T2, foreign);

      JsonNode msg = conversation().get("lastItem").get("message");

      assertFalse(msg.has("replyToId"));
      assertFalse(msg.has("repliedMessage"));
    }

    @Test
    @DisplayName("The last message carries its reactions grouped by emoji")
    void lastItemCarriesReactions() throws Exception {
      String messageId = message(otherId, T1);
      database
          .sqlUpdate(
              "INSERT INTO chats.message_reaction (message_id, user_id, reaction) VALUES (:m, :u1,"
                  + " 'up'), (:m, :u2, 'up'), (:m, :u2, 'up') ON CONFLICT DO NOTHING")
          .setParameter("m", messageId)
          .setParameter("u1", meId.toString())
          .setParameter("u2", otherId.toString())
          .execute();

      JsonNode reactions = conversation().get("lastItem").get("message").get("reactions");

      assertEquals(1, reactions.size());
      assertEquals("up", reactions.get(0).get("reaction").asText());
      assertEquals(2, reactions.get(0).get("userIds").size());
    }

    @Test
    @DisplayName("A room without items has no lastItem")
    void emptyRoom() throws Exception {
      JsonNode c = conversation();

      assertFalse(c.has("lastItem"));
      assertEquals(0, c.get("unreadCount").asInt());
    }

    @Test
    @DisplayName("Items created up to cleared_at are not visible (strict)")
    void respectsClearedAt() throws Exception {
      message(otherId, T1);
      event(otherId, T2);

      clearHistoryAt(T1);
      assertEquals("system_event", conversation().get("lastItem").get("itemType").asText());

      clearHistoryAt(T2);
      JsonNode c = conversation();
      assertFalse(c.has("lastItem"));
    }

    @Test
    @DisplayName("A deleted last message is skipped")
    void deletedLastMessageSkipped() throws Exception {
      String kept = message(otherId, T1);
      deletedMessage(otherId, T2);

      JsonNode c = conversation();

      assertEquals(kept, c.get("lastItem").get("message").get("id").asText());
    }
  }

  @Nested
  @DisplayName("unreadCount")
  class Unread {

    @Test
    @DisplayName("Counts others' messages and events, excludes own, from joined_at without marker")
    void excludesOwnItems() throws Exception {
      message(otherId, T1);
      message(otherId, T2);
      message(meId, T3);
      event(otherId, T4);
      event(meId, T5);

      assertEquals(3, unread());
    }

    @Test
    @DisplayName("Only items after the marker count")
    void afterMarker() throws Exception {
      message(otherId, T1);
      String second = message(otherId, T2);
      message(otherId, T3);
      event(otherId, T4);

      markRead(second);

      assertEquals(2, unread());
    }

    @Test
    @DisplayName("Items created up to joined_at do not count")
    void respectsJoinedAt() throws Exception {
      message(otherId, T1);
      message(otherId, T2);
      event(otherId, T2.minusSeconds(1));
      message(otherId, T3);
      setJoinedAt(meId, T2);

      assertEquals(1, unread());
    }

    @Test
    @DisplayName("Items created up to cleared_at do not count")
    void respectsClearedAt() throws Exception {
      message(otherId, T1);
      event(otherId, T2);
      message(otherId, T3);
      clearHistoryAt(T2);

      assertEquals(1, unread());
    }

    @Test
    @DisplayName("Deleted messages do not count")
    void excludesDeleted() throws Exception {
      message(otherId, T1);
      deletedMessage(otherId, T2);

      assertEquals(1, unread());
    }

    @Test
    @DisplayName("Count is capped at 1000 for messages")
    void cappedMessages() throws Exception {
      bulkMessages(otherId, 1005);

      assertEquals(1000, unread());
    }

    @Test
    @DisplayName("Count is capped at 1000 across messages and events")
    void cappedMixed() throws Exception {
      bulkMessages(otherId, 600);
      bulkEvents(otherId, 600);

      assertEquals(1000, unread());
    }

    @Test
    @DisplayName("Count is capped at 1000 for events only")
    void cappedEventsOnly() throws Exception {
      bulkEvents(otherId, 1005);

      assertEquals(1000, unread());
    }

    @Test
    @DisplayName("A subscription with NULL joined_at, no marker and no clear counts everything")
    void nullJoinedAtCountsEverything() throws Exception {
      database
          .sqlUpdate("UPDATE chats.subscription SET joined_at = NULL WHERE room_id = :roomId")
          .setParameter("roomId", roomId)
          .execute();
      message(otherId, T1);
      event(otherId, T2);
      message(meId, T3);

      assertEquals(2, unread());
    }

    @Test
    @DisplayName("A NULL joined_at still caps the count")
    void nullJoinedAtCapped() throws Exception {
      database
          .sqlUpdate("UPDATE chats.subscription SET joined_at = NULL WHERE room_id = :roomId")
          .setParameter("roomId", roomId)
          .execute();
      bulkMessages(otherId, 1005);

      assertEquals(1000, unread());
    }
  }

  @Nested
  @DisplayName("Room creation lines")
  class CreationLines {

    @Test
    @DisplayName("A group created through the API is unread-free for every member")
    void creationLinesAreNotUnread() throws Exception {
      MockedAccount.MockUserProfile third =
          MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT);
      clock.removeFixTime();
      MockHttpResponse created =
          dispatcher.post(
              "/rooms",
              objectMapper.writeValueAsString(
                  Map.of(
                      "name",
                      "created-via-api",
                      "type",
                      "GROUP",
                      "members",
                      List.of(
                          Map.of("userId", otherId.toString(), "owner", false),
                          Map.of("userId", third.getUUID().toString(), "owner", false)))),
              meToken);
      assertEquals(201, created.getStatus(), created.getContentAsString());
      String newRoomId = objectMapper.readTree(created.getContentAsString()).get("id").asText();

      for (String token :
          List.of(
              meToken,
              MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken(),
              third.getToken())) {
        MockHttpResponse inbox = dispatcher.get("/inbox", token);
        assertEquals(200, inbox.getStatus());
        JsonNode conversation = null;
        for (JsonNode c : objectMapper.readTree(inbox.getContentAsString()).get("conversations")) {
          if (newRoomId.equals(c.get("room").get("id").asText())) {
            conversation = c;
          }
        }
        assertNotNull(conversation);
        assertEquals(0, conversation.get("unreadCount").asInt());

        MockHttpResponse timeline = dispatcher.get("/rooms/" + newRoomId + "/timeline", token);
        assertEquals(200, timeline.getStatus());
        assertEquals(3, objectMapper.readTree(timeline.getContentAsString()).get("items").size());
      }
    }
  }

  @Nested
  @DisplayName("members presence")
  class MembersPresence {

    @Test
    @DisplayName("Online and last activity are both returned, also for an online member")
    void onlineMember_returnsLastActivityToo() throws Exception {
      OffsetDateTime lastActivity = wsPresenceRepository.upsertLastSeen(otherId.toString());
      wsPresenceRepository.insertAndCountOnline(
          UUID.randomUUID().toString(), otherId.toString(), "svc");

      JsonNode other = null;
      for (JsonNode member : conversation().get("room").get("members")) {
        if (otherId.toString().equals(member.get("userId").asText())) {
          other = member;
        }
      }

      assertTrue(other.get("online").asBoolean());
      assertTrue(lastActivity.isEqual(OffsetDateTime.parse(other.get("lastActivity").asText())));
    }
  }
}
