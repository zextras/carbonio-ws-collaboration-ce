// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.InboxApi;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.config.AppClock;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class ReadMarkerIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final ReadMarkerRepository readRepository;
  private final AppClock clock;
  private final Database database;
  private final Channel channel;

  private static UUID senderUserId;
  private static UUID readerUserId;
  private static String senderToken;
  private static String readerToken;

  public ReadMarkerIT(
      RoomsApi roomsApi,
      InboxApi inboxApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository,
      ReadMarkerRepository readRepository,
      Database database,
      Clock clock,
      Channel channel) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.readRepository = readRepository;
    this.database = database;
    this.channel = channel;
    this.clock = (AppClock) clock;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
    this.dispatcher.getRegistry().addSingletonResource(inboxApi);
  }

  @BeforeAll
  static void initAll() {
    senderUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    senderToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    readerUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    readerToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
  }

  private static final OffsetDateTime T1 =
      OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
  private static final OffsetDateTime T2 = T1.plusSeconds(10);
  private static final OffsetDateTime T3 = T2.plusSeconds(10);

  private String roomId;
  private String msgId;
  private String eventId;

  @BeforeEach
  void setUp() {
    roomId = UUID.randomUUID().toString();
    msgId = UUID.randomUUID().toString();
    eventId = UUID.randomUUID().toString();

    integrationTestUtils.generateAndSaveRoom(
        UUID.fromString(roomId),
        RoomTypeDto.GROUP,
        "read-marker-test-room",
        List.of(senderUserId, readerUserId));
    database
        .sqlUpdate("UPDATE chats.subscription SET joined_at = :joinedAt WHERE room_id = :roomId")
        .setParameter("joinedAt", T1.minusDays(1))
        .setParameter("roomId", roomId)
        .execute();

    clock.fixTimeAt(T1.toInstant());
    integrationTestUtils.generateAndSaveMessage(msgId, UUID.fromString(roomId), senderUserId, "hi");
    clock.removeFixTime();

    systemEventRepository.insert(
        SystemEvent.create()
            .id(eventId)
            .roomId(roomId)
            .type("MESSAGE_PINNED")
            .actorId(senderUserId.toString())
            .content("{}")
            .createdAt(T2));
  }

  private String url(String rId) {
    return String.format("/rooms/%s/read", rId);
  }

  private String body(String id) throws Exception {
    return objectMapper.writeValueAsString(java.util.Map.of("itemId", id));
  }

  private MockHttpResponse markRead(String rId, String itemId, String token) throws Exception {
    return dispatcher.put(url(rId), body(itemId), token);
  }

  @Nested
  @DisplayName("markRead with a text message id")
  class TextMessageMark {

    @Test
    @DisplayName("Returns 204 and advances itemCreatedAt to the message timestamp")
    void textMessage_returns204AndSetsItemCreatedAt() throws Exception {
      MockHttpResponse response = markRead(roomId, msgId, readerToken);

      assertEquals(204, response.getStatus());
      Optional<ReadMarker> marker = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      assertNotNull(marker.get().getItemCreatedAt());
      assertEquals(T1.toInstant(), marker.get().getItemCreatedAt().toInstant());
    }

    @Test
    @DisplayName("Sender's own message does not count as unread")
    void textMessage_senderHasZeroUnread() throws Exception {
      assertEquals(
          2L,
          messageRepository
              .getUnreadCountsByUserId(readerUserId.toString(), 1000)
              .getOrDefault(roomId, 0L));
      markRead(roomId, msgId, senderToken);

      Optional<ReadMarker> marker = readRepository.get(senderUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      long unread =
          messageRepository
              .getUnreadCountsByUserId(senderUserId.toString(), 1000)
              .getOrDefault(roomId, 0L);
      assertEquals(0, unread);
    }
  }

  @Nested
  @DisplayName("markRead with a system-event id")
  class SystemEventMark {

    @Test
    @DisplayName("Returns 204 (not 404) when the id belongs to a system event")
    void systemEvent_returns204NotFound() throws Exception {
      MockHttpResponse response = markRead(roomId, eventId, readerToken);

      assertEquals(204, response.getStatus());
    }

    @Test
    @DisplayName("itemCreatedAt advances to the system event timestamp")
    void systemEvent_itemCreatedAtMovesForward() throws Exception {
      markRead(roomId, eventId, readerToken);

      Optional<ReadMarker> marker = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      assertEquals(T2.toInstant(), marker.get().getItemCreatedAt().toInstant());
    }

    @Test
    @DisplayName(
        "Unread count reaches 0 after marking trailing system-event when prior text is read")
    void systemEvent_unreadCountZero() throws Exception {
      assertEquals(
          2L,
          messageRepository
              .getUnreadCountsByUserId(readerUserId.toString(), 1000)
              .getOrDefault(roomId, 0L));
      markRead(roomId, eventId, readerToken);

      Optional<ReadMarker> marker = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      long unread =
          messageRepository
              .getUnreadCountsByUserId(readerUserId.toString(), 1000)
              .getOrDefault(roomId, 0L);
      assertEquals(0, unread);
    }

    @Test
    @DisplayName("Marking system-event after a text-message mark advances to the event timestamp")
    void systemEvent_afterTextMark_advances() throws Exception {
      markRead(roomId, msgId, readerToken);
      markRead(roomId, eventId, readerToken);

      Optional<ReadMarker> marker = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      assertEquals(T2.toInstant(), marker.get().getItemCreatedAt().toInstant());
    }
  }

  @Nested
  @DisplayName("monotonic guard")
  class MonotonicGuard {

    @Test
    @DisplayName("Older mark does not regress itemCreatedAt")
    void olderMark_doesNotRegress() throws Exception {
      markRead(roomId, eventId, readerToken);

      Optional<ReadMarker> afterEvent = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(afterEvent.isPresent());
      OffsetDateTime storedAfterEvent = afterEvent.get().getItemCreatedAt();

      markRead(roomId, msgId, readerToken);

      Optional<ReadMarker> afterOlderMsg = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(afterOlderMsg.isPresent());
      assertEquals(
          storedAfterEvent.toInstant(), afterOlderMsg.get().getItemCreatedAt().toInstant());
    }
  }

  @Nested
  @DisplayName("markRead unknown id")
  class UnknownId {

    @Test
    @DisplayName("Returns 404 when the id is neither a message nor a system event")
    void unknownId_returns404() throws Exception {
      String unknownId = UUID.randomUUID().toString();
      MockHttpResponse response = markRead(roomId, unknownId, readerToken);

      assertEquals(404, response.getStatus());
    }
  }

  @Nested
  @DisplayName("validation, visibility and broadcast")
  class ValidationAndBroadcast {

    @Test
    @DisplayName("Returns 400 when itemId is missing")
    void missingItemId_returns400() throws Exception {
      assertEquals(400, dispatcher.put(url(roomId), "{}", readerToken).getStatus());
      assertEquals(400, dispatcher.put(url(roomId), null, readerToken).getStatus());
      assertTrue(readRepository.get(readerUserId.toString(), roomId).isEmpty());
    }

    @Test
    @DisplayName("Returns 400 when itemId is blank")
    void blankItemId_returns400() throws Exception {
      assertEquals(400, markRead(roomId, " ", readerToken).getStatus());
    }

    @Test
    @DisplayName("Returns 403 for a non-member and writes nothing")
    void nonMember_returns403() throws Exception {
      MockedAccount.MockUserProfile outsider =
          MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT);

      assertEquals(403, markRead(roomId, msgId, outsider.getToken()).getStatus());
      assertTrue(readRepository.get(outsider.getUUID().toString(), roomId).isEmpty());
    }

    @Test
    @DisplayName("Returns 404 for an unknown room")
    void unknownRoom_returns404() throws Exception {
      assertEquals(404, markRead(UUID.randomUUID().toString(), msgId, readerToken).getStatus());
    }

    @Test
    @DisplayName("Returns 404 when the item is hidden by the caller's cleared history")
    void hiddenItem_returns404() throws Exception {
      database
          .sqlUpdate(
              "INSERT INTO chats.room_user_settings (room_id, user_id, cleared_at, created_at,"
                  + " updated_at) VALUES (:roomId, :userId, :clearedAt, :clearedAt, :clearedAt)")
          .setParameter("roomId", roomId)
          .setParameter("userId", readerUserId.toString())
          .setParameter("clearedAt", T1)
          .execute();

      assertEquals(404, markRead(roomId, msgId, readerToken).getStatus());
      assertEquals(204, markRead(roomId, eventId, readerToken).getStatus());
      assertEquals(
          T2.toInstant(),
          readRepository
              .get(readerUserId.toString(), roomId)
              .orElseThrow()
              .getItemCreatedAt()
              .toInstant());
    }

    @Test
    @DisplayName("Broadcasts ReadUpdated with the stored value to every room member")
    void broadcastsStoredValue() throws Exception {
      String readerQueue = bindQueue(readerUserId);
      String senderQueue = bindQueue(senderUserId);

      markRead(roomId, eventId, readerToken);

      for (String queue : List.of(readerQueue, senderQueue)) {
        JsonNode event = nextEvent(queue);
        assertEquals("ReadUpdated", event.get("type").asText());
        assertEquals(roomId, event.get("roomId").asText());
        assertEquals(readerUserId.toString(), event.get("userId").asText());
        assertEquals(
            T2.toInstant(), OffsetDateTime.parse(event.get("itemCreatedAt").asText()).toInstant());
      }
    }

    @Test
    @DisplayName("Re-marking the same item writes nothing and broadcasts nothing")
    void sameItem_noWriteNoBroadcast() throws Exception {
      String readerQueue = bindQueue(readerUserId);
      String senderQueue = bindQueue(senderUserId);
      assertEquals(204, markRead(roomId, eventId, readerToken).getStatus());
      nextEvent(readerQueue);
      nextEvent(senderQueue);
      String rowVersion = markerRowVersion();

      assertEquals(204, markRead(roomId, eventId, readerToken).getStatus());

      assertEquals(rowVersion, markerRowVersion());
      assertNoEvent(readerQueue);
      assertNoEvent(senderQueue);
    }

    @Test
    @DisplayName("Marking an older item writes nothing and broadcasts nothing")
    void olderItem_noWriteNoBroadcast() throws Exception {
      String readerQueue = bindQueue(readerUserId);
      String senderQueue = bindQueue(senderUserId);
      assertEquals(204, markRead(roomId, eventId, readerToken).getStatus());
      nextEvent(readerQueue);
      nextEvent(senderQueue);
      String rowVersion = markerRowVersion();

      assertEquals(204, markRead(roomId, msgId, readerToken).getStatus());

      assertEquals(rowVersion, markerRowVersion());
      assertNoEvent(readerQueue);
      assertNoEvent(senderQueue);
    }

    private String markerRowVersion() {
      return database
          .sqlQuery(
              "SELECT xmin::text AS v FROM chats.read_marker WHERE user_id = :userId AND room_id ="
                  + " :roomId")
          .setParameter("userId", readerUserId.toString())
          .setParameter("roomId", roomId)
          .findOne()
          .getString("v");
    }

    private void assertNoEvent(String queue) throws Exception {
      for (int i = 0; i < 10; i++) {
        GetResponse response = channel.basicGet(queue, true);
        if (response != null) {
          throw new AssertionError(
              "unexpected WS event on "
                  + queue
                  + ": "
                  + new String(response.getBody(), StandardCharsets.UTF_8));
        }
        Thread.sleep(100);
      }
    }

    private String bindQueue(UUID user) throws Exception {
      channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
      String queue = channel.queueDeclare().getQueue();
      channel.queueBind(queue, user.toString(), "user-events");
      return queue;
    }

    private JsonNode nextEvent(String queue) throws Exception {
      for (int i = 0; i < 50; i++) {
        GetResponse response = channel.basicGet(queue, true);
        if (response != null) {
          return objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
        }
        Thread.sleep(100);
      }
      throw new AssertionError("no WS event received on " + queue);
    }
  }

  @Nested
  @DisplayName("GET /rooms/{roomId}/read wire shape")
  class GetRoomReadMarkersWire {

    @Test
    @DisplayName("Response contains userId and itemCreatedAt; no messageId or readAt fields")
    void textMark_wireShapeHasOnlyUserIdAndItemCreatedAt() throws Exception {
      markRead(roomId, msgId, readerToken);

      MockHttpResponse response =
          dispatcher.get(String.format("/rooms/%s/read", roomId), readerToken);

      assertEquals(200, response.getStatus());
      com.fasterxml.jackson.databind.JsonNode body =
          objectMapper.readTree(response.getContentAsString());
      assertTrue(body.isArray());
      com.fasterxml.jackson.databind.JsonNode readerMarker =
          java.util.stream.StreamSupport.stream(body.spliterator(), false)
              .filter(n -> readerUserId.toString().equals(n.path("userId").asText()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("No marker for readerUserId"));
      assertTrue(readerMarker.has("itemCreatedAt"), "itemCreatedAt must be present");
      assertNotNull(readerMarker.get("itemCreatedAt").asText());
      assertTrue(!readerMarker.has("messageId"), "messageId must not be present");
      assertTrue(!readerMarker.has("readAt"), "readAt must not be present");
    }

    @Test
    @DisplayName(
        "System-event mark exposes itemCreatedAt equal to the event timestamp, no messageId")
    void systemEventMark_wireItemCreatedAtMatchesEventTimestamp() throws Exception {
      markRead(roomId, eventId, readerToken);

      MockHttpResponse response =
          dispatcher.get(String.format("/rooms/%s/read", roomId), readerToken);

      assertEquals(200, response.getStatus());
      com.fasterxml.jackson.databind.JsonNode body =
          objectMapper.readTree(response.getContentAsString());
      assertTrue(body.isArray());
      com.fasterxml.jackson.databind.JsonNode readerMarker =
          java.util.stream.StreamSupport.stream(body.spliterator(), false)
              .filter(n -> readerUserId.toString().equals(n.path("userId").asText()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("No marker for readerUserId"));
      assertTrue(readerMarker.has("itemCreatedAt"), "itemCreatedAt must be present");
      assertTrue(!readerMarker.has("messageId"), "messageId must not be present");
      OffsetDateTime returned = OffsetDateTime.parse(readerMarker.get("itemCreatedAt").asText());
      assertEquals(
          T2.toInstant(), returned.toInstant(), "itemCreatedAt must be the system event time");
    }
  }

  @Nested
  @DisplayName("marker writes")
  class MarkerWrites {

    private String sendText(String rId, String token) throws Exception {
      MockHttpResponse response =
          dispatcher.post(
              String.format("/rooms/%s/messages", rId),
              objectMapper.writeValueAsString(Map.of("text", "hello")),
              token);
      assertEquals(201, response.getStatus());
      return objectMapper.readTree(response.getContentAsString()).get("id").asText();
    }

    @Test
    @DisplayName("Sending a message never creates a marker")
    void sendMessage_doesNotCreateMarker() throws Exception {
      sendText(roomId, readerToken);
      sendText(roomId, senderToken);

      assertTrue(readRepository.get(readerUserId.toString(), roomId).isEmpty());
      assertTrue(readRepository.get(senderUserId.toString(), roomId).isEmpty());
    }

    @Test
    @DisplayName("Forwarding a message never creates a marker")
    void forwardMessage_doesNotCreateMarker() throws Exception {
      String targetRoomId = UUID.randomUUID().toString();
      integrationTestUtils.generateAndSaveRoom(
          UUID.fromString(targetRoomId),
          RoomTypeDto.GROUP,
          "read-marker-forward-target",
          List.of(senderUserId, readerUserId));

      MockHttpResponse response =
          dispatcher.post(
              String.format("/rooms/%s/messages/%s/forward", roomId, msgId),
              objectMapper.writeValueAsString(Map.of("toRoomId", targetRoomId)),
              readerToken);

      assertEquals(201, response.getStatus());
      assertTrue(readRepository.get(readerUserId.toString(), targetRoomId).isEmpty());
      assertTrue(readRepository.get(readerUserId.toString(), roomId).isEmpty());
    }

    @Test
    @DisplayName("Returns 404 and writes nothing when the item belongs to another room")
    void markRead_itemOfAnotherRoom_returns404() throws Exception {
      String otherRoomId = UUID.randomUUID().toString();
      String otherMsgId = UUID.randomUUID().toString();
      integrationTestUtils.generateAndSaveRoom(
          UUID.fromString(otherRoomId),
          RoomTypeDto.GROUP,
          "read-marker-other-room",
          List.of(senderUserId, readerUserId));
      integrationTestUtils.generateAndSaveMessage(
          otherMsgId, UUID.fromString(otherRoomId), senderUserId, "elsewhere");

      MockHttpResponse response = markRead(roomId, otherMsgId, readerToken);

      assertEquals(404, response.getStatus());
      assertTrue(readRepository.get(readerUserId.toString(), roomId).isEmpty());
    }

    @Test
    @DisplayName("Marking an older item keeps the existing marker")
    void markRead_olderItem_keepsMarker() throws Exception {
      markRead(roomId, eventId, readerToken);
      markRead(roomId, msgId, readerToken);

      Optional<ReadMarker> marker = readRepository.get(readerUserId.toString(), roomId);
      assertTrue(marker.isPresent());
      assertEquals(T2.toInstant(), marker.get().getItemCreatedAt().toInstant());
    }

    @Test
    @DisplayName("GET /inbox returns 200 and omits members who never read")
    void getInbox_memberWithoutMarker_returns200() throws Exception {
      sendText(roomId, senderToken);
      sendText(roomId, readerToken);
      markRead(roomId, msgId, senderToken);

      MockHttpResponse response = dispatcher.get("/inbox", senderToken);

      assertEquals(200, response.getStatus());
      JsonNode conversation = null;
      for (JsonNode c : objectMapper.readTree(response.getContentAsString()).get("conversations")) {
        if (roomId.equals(c.get("room").get("id").asText())) {
          conversation = c;
        }
      }
      assertNotNull(conversation, "room must be in the inbox");
      for (JsonNode marker : conversation.get("markers")) {
        assertNotEquals(readerUserId.toString(), marker.get("userId").asText());
      }
      assertEquals(1, conversation.get("markers").size());
    }
  }
}
