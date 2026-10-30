// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only
package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class PinReactionApiIT {
  private static final String THUMBS_UP = "👍";

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final SystemEventRepository systemEventRepository;
  private final ChatMessageService chatMessageService;
  private final Channel channel;
  private static UUID aUserId;
  private static String aToken;
  private static UUID bUserId;
  private static String bToken;
  private UUID roomId;
  private String first;
  private String second;
  private String queueB;

  public PinReactionApiIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      SystemEventRepository systemEventRepository,
      ChatMessageService chatMessageService,
      Channel channel) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.systemEventRepository = systemEventRepository;
    this.chatMessageService = chatMessageService;
    this.channel = channel;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeAll
  static void initAll() {
    aUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    aToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    bUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    bToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
  }

  @BeforeEach
  void setUp() throws Exception {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "pin-reaction-room", List.of(aUserId, bUserId));
    first = saveMessage("first");
    second = saveMessage("second");
    queueB = bindQueue(bUserId);
  }

  private String saveMessage(String text) {
    String id = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveMessage(id, roomId, aUserId, text);
    return id;
  }

  private String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  private JsonNode next(String queue) throws Exception {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline) {
      GetResponse response = channel.basicGet(queue, true);
      if (response != null) {
        return objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
      }
      Thread.onSpinWait();
    }
    throw new AssertionError("no WS event received on " + queue);
  }

  private JsonNode nextOfType(String queue, String type) throws Exception {
    JsonNode event = next(queue);
    while (!type.equals(event.get("type").asText())) {
      event = next(queue);
    }
    return event;
  }

  private int pin(String messageId, String token) throws Exception {
    return dispatcher
        .put(String.format("/rooms/%s/messages/%s/pin", roomId, messageId), null, token)
        .getStatus();
  }

  private int unpin(String messageId, String token) throws Exception {
    return dispatcher
        .delete(String.format("/rooms/%s/messages/%s/pin", roomId, messageId), token)
        .getStatus();
  }

  private int deleteMessage(String messageId, String token) throws Exception {
    return dispatcher
        .delete(String.format("/rooms/%s/messages/%s", roomId, messageId), token)
        .getStatus();
  }

  private String reactionPath(UUID room, String messageId, String reaction) {
    return String.format(
        "/rooms/%s/messages/%s/reactions/%s",
        room, messageId, URLEncoder.encode(reaction, StandardCharsets.UTF_8));
  }

  private int react(UUID room, String messageId, String reaction, String token) throws Exception {
    return dispatcher.post(reactionPath(room, messageId, reaction), token).getStatus();
  }

  private int unreact(UUID room, String messageId, String reaction, String token) throws Exception {
    return dispatcher.delete(reactionPath(room, messageId, reaction), token).getStatus();
  }

  private List<String> pinnedIds() throws Exception {
    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/pin", roomId), aToken);
    assertEquals(200, response.getStatus());
    List<String> ids = new ArrayList<>();
    objectMapper
        .readTree(response.getContentAsString())
        .forEach(pinned -> ids.add(pinned.get("messageId").asText()));
    return ids;
  }

  private List<SystemEvent> rows(String type) {
    return systemEventRepository.findByRoomId(roomId.toString()).stream()
        .filter(e -> type.equals(e.getType()))
        .toList();
  }

  @Nested
  @DisplayName("Unpin")
  class Unpin {

    @Test
    @DisplayName("of a message that is not the room pin is a 204 no-op: no row, no event")
    void unpinNotPinned() throws Exception {
      assertEquals(204, pin(first, aToken));
      nextOfType(queueB, "MessagePinned");

      assertEquals(204, unpin(second, bToken));
      assertEquals(204, unpin(UUID.randomUUID().toString(), bToken));
      assertTrue(rows("MESSAGE_UNPINNED").isEmpty());
      assertEquals(List.of(first), pinnedIds());
      assertEquals(204, react(roomId, first, THUMBS_UP, aToken));
      assertEquals("ReactionChanged", next(queueB).get("type").asText());
    }

    @Test
    @DisplayName("of the room pin writes MESSAGE_UNPINNED and sends MessageUnpinned")
    void unpinPinned() throws Exception {
      assertEquals(204, pin(first, aToken));
      nextOfType(queueB, "MessagePinned");

      assertEquals(204, unpin(first, bToken));

      JsonNode event = next(queueB);
      assertEquals("MessageUnpinned", event.get("type").asText());
      assertEquals(bUserId.toString(), event.get("unpinnedBy").asText());
      List<SystemEvent> unpinRows = rows("MESSAGE_UNPINNED");
      assertEquals(1, unpinRows.size());
      assertEquals(unpinRows.get(0).getId(), event.get("systemEventId").asText());
      assertTrue(pinnedIds().isEmpty());
    }
  }

  @Nested
  @DisplayName("Pin")
  class Pin {

    @Test
    @DisplayName("of a deleted message is 404 and writes neither a pin nor a MESSAGE_PINNED row")
    void pinDeleted() throws Exception {
      assertEquals(204, deleteMessage(first, aToken));

      assertEquals(404, pin(first, bToken));

      assertTrue(pinnedIds().isEmpty());
      assertTrue(rows("MESSAGE_PINNED").isEmpty());
    }

    @Test
    @DisplayName("of a second message replaces the pin and announces the new one")
    void pinReplaces() throws Exception {
      assertEquals(204, pin(first, aToken));
      assertEquals(204, pin(second, bToken));

      assertEquals(first, nextOfType(queueB, "MessagePinned").get("messageId").asText());
      JsonNode replaced = nextOfType(queueB, "MessagePinned");
      assertEquals(second, replaced.get("messageId").asText());
      assertEquals(bUserId.toString(), replaced.get("pinnedBy").asText());
      assertEquals("second", replaced.get("message").get("text").asText());
      assertEquals(List.of(second), pinnedIds());
    }
  }

  @Nested
  @DisplayName("Deleting the pinned message")
  class DeletePinned {

    @Test
    @DisplayName("unpins it, writes MESSAGE_UNPINNED by the deleter, sends MessageUnpinned last")
    void deletePinned() throws Exception {
      assertEquals(204, pin(first, bToken));
      nextOfType(queueB, "MessagePinned");

      assertEquals(204, deleteMessage(first, aToken));

      JsonNode deleted = next(queueB);
      assertEquals("MessageDeleted", deleted.get("type").asText());
      assertEquals(first, deleted.get("messageId").asText());
      JsonNode unpinned = next(queueB);
      assertEquals("MessageUnpinned", unpinned.get("type").asText());
      assertEquals(first, unpinned.get("messageId").asText());
      assertEquals(aUserId.toString(), unpinned.get("unpinnedBy").asText());
      SystemEvent row =
          systemEventRepository
              .getById(unpinned.get("systemEventId").asText())
              .orElseThrow(() -> new AssertionError("MESSAGE_UNPINNED row not persisted"));
      assertEquals("MESSAGE_UNPINNED", row.getType());
      assertEquals(roomId.toString(), row.getRoomId());
      assertEquals(aUserId.toString(), row.getActorId());
      JsonNode content = objectMapper.readTree(row.getContent());
      assertEquals(aUserId.toString(), content.get("unpinnedBy").asText());
      assertEquals(first, content.get("messageId").asText());
      assertTrue(
          row.getCreatedAt()
              .isEqual(OffsetDateTime.parse(unpinned.get("systemEventCreatedAt").asText())));
      assertTrue(pinnedIds().isEmpty());
    }

    @Test
    @DisplayName("Deleting another message keeps the pin and sends no MessageUnpinned")
    void deleteOther() throws Exception {
      assertEquals(204, pin(first, bToken));
      nextOfType(queueB, "MessagePinned");

      assertEquals(204, deleteMessage(second, aToken));

      assertEquals("MessageDeleted", next(queueB).get("type").asText());
      assertEquals(204, react(roomId, first, THUMBS_UP, aToken));
      assertEquals("ReactionChanged", next(queueB).get("type").asText());
      assertTrue(rows("MESSAGE_UNPINNED").isEmpty());
      assertEquals(List.of(first), pinnedIds());
    }
  }

  @Nested
  @DisplayName("Reactions")
  class Reactions {

    @Test
    @DisplayName("Adding a reaction to a deleted message is 404 and stores nothing")
    void reactDeleted() throws Exception {
      assertEquals(204, deleteMessage(first, aToken));

      assertEquals(404, react(roomId, first, THUMBS_UP, bToken));

      assertTrue(chatMessageService.getReactions(first).isEmpty());
    }

    @Test
    @DisplayName("Removing through a room the message is not in is 404 and keeps the reaction")
    void unreactOtherRoom() throws Exception {
      UUID otherRoomId = UUID.randomUUID();
      integrationTestUtils.generateAndSaveRoom(
          otherRoomId, RoomTypeDto.GROUP, "other-room", List.of(aUserId, bUserId));
      assertEquals(204, react(roomId, first, THUMBS_UP, bToken));

      assertEquals(404, unreact(otherRoomId, first, THUMBS_UP, bToken));

      assertEquals(1, chatMessageService.getReactions(first).size());
    }

    @Test
    @DisplayName("Removing ignores the emoji in the path")
    void unreactIgnoresEmoji() throws Exception {
      assertEquals(204, react(roomId, first, THUMBS_UP, bToken));

      assertEquals(204, unreact(roomId, first, "❤️", bToken));

      assertTrue(chatMessageService.getReactions(first).isEmpty());
    }
  }
}
