// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import io.ebean.SqlRow;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class ReactionTimesIT {

  private static final String UP = "👍";
  private static final String HEART = "❤️";

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final Channel channel;
  private final Database database;

  private static UUID aUserId;
  private static UUID bUserId;
  private static String aToken;
  private static String bToken;
  private static String outsiderToken;

  private UUID roomId;
  private String messageId;
  private String queueB;

  public ReactionTimesIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      Channel channel,
      Database database) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.channel = channel;
    this.database = database;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeAll
  static void initAll() {
    aUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    aToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    bUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    bToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
    outsiderToken = MockedAccount.getAccount(MockedAccountType.LINUS_VAN_PELT).getToken();
  }

  @BeforeEach
  void setUp() throws Exception {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "reaction-times-room", List.of(aUserId, bUserId));
    messageId = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveMessage(messageId, roomId, aUserId, "react to me");
    channel.exchangeDeclare(bUserId.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    queueB = channel.queueDeclare().getQueue();
    channel.queueBind(queueB, bUserId.toString(), "user-events");
  }

  private String path(String message) {
    return String.format("/rooms/%s/messages/%s/reaction", roomId, message);
  }

  private int put(String message, String reaction, String token) throws Exception {
    return dispatcher
        .put(path(message), objectMapper.writeValueAsString(Map.of("reaction", reaction)), token)
        .getStatus();
  }

  private int delete(String message, String token) throws Exception {
    return dispatcher.delete(path(message), token).getStatus();
  }

  private List<JsonNode> reactionEvents() throws Exception {
    Thread.sleep(500);
    List<JsonNode> events = new ArrayList<>();
    GetResponse response;
    while ((response = channel.basicGet(queueB, true)) != null) {
      JsonNode event =
          objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
      assertFalse(event.has("sentDate"), "sentDate must not be sent: " + event);
      if ("ReactionChanged".equals(event.get("type").asText())) {
        events.add(event);
      }
    }
    return events;
  }

  private JsonNode onlyEvent() throws Exception {
    List<JsonNode> events = reactionEvents();
    assertEquals(1, events.size(), "expected exactly one ReactionChanged: " + events);
    return events.get(0);
  }

  private OffsetDateTime storedUpdatedAt() {
    SqlRow row =
        database
            .sqlQuery(
                "SELECT updated_at FROM chats.message_reaction WHERE message_id = :m AND user_id"
                    + " = :u")
            .setParameter("m", messageId)
            .setParameter("u", aUserId.toString())
            .findOne();
    return row.getTimestamp("updated_at").toInstant().atOffset(java.time.ZoneOffset.UTC);
  }

  private long rowCount() {
    return database
        .sqlQuery("SELECT count(*) AS c FROM chats.message_reaction WHERE message_id = :m")
        .setParameter("m", messageId)
        .findOne()
        .getLong("c");
  }

  private JsonNode timelineReactionUser() throws Exception {
    JsonNode timeline =
        objectMapper.readTree(
            dispatcher
                .get(String.format("/rooms/%s/timeline", roomId), aToken)
                .getContentAsString());
    for (JsonNode item : timeline.get("items")) {
      JsonNode message = item.get("message");
      if (message != null && messageId.equals(message.get("id").asText())) {
        JsonNode group = message.get("reactions").get(0);
        assertEquals(1, group.get("users").size());
        return group.get("users").get(0);
      }
    }
    throw new AssertionError("message not in timeline");
  }

  private static boolean same(OffsetDateTime expected, JsonNode actual) {
    return expected.isEqual(OffsetDateTime.parse(actual.asText()));
  }

  @Test
  @DisplayName("PUT of a new reaction sends one added whose updatedAt is the stored value")
  void added() throws Exception {
    assertEquals(204, put(messageId, UP, aToken));

    JsonNode event = onlyEvent();
    OffsetDateTime stored = storedUpdatedAt();
    assertEquals("added", event.get("operation").asText());
    assertEquals(UP, event.get("reaction").asText());
    assertEquals(aUserId.toString(), event.get("userId").asText());
    assertFalse(event.has("previousReaction"));
    assertFalse(event.has("deletedAt"));
    assertTrue(same(stored, event.get("updatedAt")), event + " != " + stored);
    JsonNode user = timelineReactionUser();
    assertEquals(aUserId.toString(), user.get("userId").asText());
    assertTrue(same(stored, user.get("updatedAt")));
  }

  @Test
  @DisplayName("PUT of a different emoji sends one updated with previousReaction and a later time")
  void updated() throws Exception {
    assertEquals(204, put(messageId, UP, aToken));
    OffsetDateTime first = storedUpdatedAt();
    reactionEvents();

    assertEquals(204, put(messageId, HEART, aToken));

    JsonNode event = onlyEvent();
    OffsetDateTime stored = storedUpdatedAt();
    assertEquals("updated", event.get("operation").asText());
    assertEquals(HEART, event.get("reaction").asText());
    assertEquals(UP, event.get("previousReaction").asText());
    assertFalse(event.has("deletedAt"));
    assertTrue(same(stored, event.get("updatedAt")));
    assertTrue(stored.isAfter(first));
    assertTrue(same(stored, timelineReactionUser().get("updatedAt")));
    assertEquals(1, rowCount());
  }

  @Test
  @DisplayName("PUT of the same emoji is a 204 without event and keeps updated_at")
  void same() throws Exception {
    assertEquals(204, put(messageId, UP, aToken));
    OffsetDateTime first = storedUpdatedAt();
    reactionEvents();

    assertEquals(204, put(messageId, UP, aToken));

    assertTrue(reactionEvents().isEmpty());
    assertTrue(first.isEqual(storedUpdatedAt()));
  }

  @Test
  @DisplayName("DELETE sends one removed with deletedAt after updatedAt and leaves no row")
  void removed() throws Exception {
    assertEquals(204, put(messageId, UP, aToken));
    OffsetDateTime first = storedUpdatedAt();
    reactionEvents();

    assertEquals(204, delete(messageId, aToken));

    JsonNode event = onlyEvent();
    assertEquals("removed", event.get("operation").asText());
    assertEquals(UP, event.get("reaction").asText());
    assertFalse(event.has("updatedAt"));
    assertFalse(event.has("previousReaction"));
    assertTrue(OffsetDateTime.parse(event.get("deletedAt").asText()).isAfter(first));
    assertEquals(0, rowCount());
  }

  @Test
  @DisplayName("DELETE without a reaction is a 204 without event")
  void removeNothing() throws Exception {
    assertEquals(204, delete(messageId, aToken));

    assertTrue(reactionEvents().isEmpty());
    assertEquals(0, rowCount());
  }

  @Test
  @DisplayName("An unknown message is 404 on PUT and DELETE")
  void unknownMessage() throws Exception {
    String unknown = UUID.randomUUID().toString();

    assertEquals(404, put(unknown, UP, aToken));
    assertEquals(404, delete(unknown, aToken));
    assertTrue(reactionEvents().isEmpty());
  }

  @Test
  @DisplayName("A blank or 33-character reaction is 400 and stores nothing")
  void invalidReaction() throws Exception {
    assertEquals(400, put(messageId, "", aToken));
    assertEquals(400, put(messageId, "   ", aToken));
    assertEquals(400, put(messageId, "a".repeat(33), aToken));
    assertEquals(400, dispatcher.put(path(messageId), "{}", aToken).getStatus());

    assertEquals(0, rowCount());
    assertTrue(reactionEvents().isEmpty());
  }

  @Test
  @DisplayName("A non-member is 403 on PUT and DELETE")
  void nonMember() throws Exception {
    assertEquals(403, put(messageId, UP, outsiderToken));
    assertEquals(403, delete(messageId, outsiderToken));

    assertEquals(0, rowCount());
  }

  @Test
  @DisplayName("PUT on a deleted message is 404")
  void deletedMessage() throws Exception {
    assertEquals(
        204,
        dispatcher
            .delete(String.format("/rooms/%s/messages/%s", roomId, messageId), aToken)
            .getStatus());

    assertEquals(404, put(messageId, UP, bToken));
    assertEquals(0, rowCount());
  }
}
