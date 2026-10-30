// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.Parameter.param;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.utils.StringFormatUtils;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.tools.StorageMockServer;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.it.utils.MockedFiles;
import com.zextras.carbonio.chats.it.utils.MockedFiles.FileMock;
import com.zextras.carbonio.chats.it.utils.MockedFiles.MockedFileType;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.JsonBody;

@ApiIntegrationTest
class SystemLineEventsIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final SystemEventRepository systemEventRepository;
  private final Channel channel;
  private final StorageMockServer storageMockServer;

  private static UUID aUserId;
  private static UUID bUserId;
  private static UUID cUserId;
  private static UUID dUserId;
  private static UUID eUserId;
  private static String aToken;
  private static String bToken;

  private UUID roomId;
  private String messageId;
  private String queueA;
  private String queueB;

  public SystemLineEventsIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      SystemEventRepository systemEventRepository,
      Channel channel,
      StorageMockServer storageMockServer) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.systemEventRepository = systemEventRepository;
    this.channel = channel;
    this.storageMockServer = storageMockServer;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeAll
  static void initAll() {
    aUserId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    aToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    bUserId = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
    bToken = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getToken();
    cUserId = MockedAccount.getAccount(MockedAccountType.LINUS_VAN_PELT).getUUID();
    dUserId = MockedAccount.getAccount(MockedAccountType.PEPERITA_PATTY).getUUID();
    eUserId = MockedAccount.getAccount(MockedAccountType.MARCIE_JOHNSON).getUUID();
  }

  @BeforeEach
  void setUp() throws Exception {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "system-line-room", List.of(aUserId, bUserId, cUserId));
    messageId = UUID.randomUUID().toString();
    integrationTestUtils.generateAndSaveMessage(messageId, roomId, aUserId, "pin me");
    queueA = bindQueue(aUserId);
    queueB = bindQueue(bUserId);
  }

  private String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  private JsonNode nextEvent(String queue, String type) throws Exception {
    for (int i = 0; i < 50; i++) {
      GetResponse response = channel.basicGet(queue, true);
      if (response != null) {
        JsonNode event =
            objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
        if (type.equals(event.get("type").asText())) {
          return event;
        }
      } else {
        Thread.sleep(100);
      }
    }
    throw new AssertionError("no " + type + " event received on " + queue);
  }

  private SystemEvent persisted(String id) {
    return systemEventRepository
        .getById(id)
        .orElseThrow(() -> new AssertionError("system event not persisted: " + id));
  }

  private void assertCreatedAt(SystemEvent row, JsonNode event, String field) {
    assertTrue(event.hasNonNull(field), field + " missing in " + event);
    assertTrue(
        row.getCreatedAt().isEqual(OffsetDateTime.parse(event.get(field).asText())),
        field + " " + event.get(field).asText() + " != persisted " + row.getCreatedAt());
  }

  private MockHttpResponse markRead(String itemId, String token) throws Exception {
    return dispatcher.put(
        String.format("/rooms/%s/read", roomId),
        objectMapper.writeValueAsString(Map.of("itemId", itemId)),
        token);
  }

  private void addMembers(List<UUID> users) throws Exception {
    List<MemberToInsertDto> body =
        users.stream()
            .map(u -> MemberToInsertDto.create().userId(u).historyCleared(false))
            .toList();
    assertEquals(
        201,
        dispatcher
            .post(
                String.format("/rooms/%s/members", roomId),
                objectMapper.writeValueAsString(body),
                aToken)
            .getStatus());
  }

  @Test
  @DisplayName("MessagePinned carries pinnedBy and the id of the persisted MESSAGE_PINNED row")
  void pinned() throws Exception {
    assertEquals(
        204,
        dispatcher
            .put(String.format("/rooms/%s/messages/%s/pin", roomId, messageId), null, aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "MessagePinned");
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("MESSAGE_PINNED", row.getType());
    assertEquals(roomId.toString(), row.getRoomId());
    assertEquals(row.getActorId(), event.get("pinnedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");
    assertEquals(aUserId.toString(), row.getActorId());
    assertEquals(204, markRead(row.getId(), bToken).getStatus());
  }

  @Test
  @DisplayName("MessageUnpinned carries unpinnedBy and the id of the persisted row")
  void unpinned() throws Exception {
    dispatcher.put(String.format("/rooms/%s/messages/%s/pin", roomId, messageId), null, aToken);

    assertEquals(
        204,
        dispatcher
            .delete(String.format("/rooms/%s/messages/%s/pin", roomId, messageId), bToken)
            .getStatus());

    JsonNode event = nextEvent(queueA, "MessageUnpinned");
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("MESSAGE_UNPINNED", row.getType());
    assertEquals(row.getActorId(), event.get("unpinnedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");
    assertEquals(bUserId.toString(), row.getActorId());
    assertEquals(204, markRead(row.getId(), aToken).getStatus());
  }

  @Test
  @DisplayName("Bulk add writes one MEMBER_ADDED row per member and every member gets every event")
  void bulkAdded() throws Exception {
    String queueE = bindQueue(eUserId);

    addMembers(List.of(dUserId, eUserId));

    JsonNode first = nextEvent(queueA, "RoomMemberAdded");
    JsonNode second = nextEvent(queueA, "RoomMemberAdded");
    assertEquals(
        List.of(dUserId.toString(), eUserId.toString()),
        List.of(first.get("userId").asText(), second.get("userId").asText()));
    SystemEvent firstRow = persisted(first.get("systemEventId").asText());
    SystemEvent secondRow = persisted(second.get("systemEventId").asText());
    assertNotEquals(firstRow.getId(), secondRow.getId());
    for (SystemEvent row : List.of(firstRow, secondRow)) {
      assertEquals("MEMBER_ADDED", row.getType());
      assertEquals(aUserId.toString(), row.getActorId());
      assertEquals(1, content(row).get("addedUserIds").size());
    }
    assertEquals(dUserId.toString(), content(firstRow).get("addedUserIds").get(0).asText());
    assertEquals(eUserId.toString(), content(secondRow).get("addedUserIds").get(0).asText());
    assertEquals(
        1L, Duration.between(firstRow.getCreatedAt(), secondRow.getCreatedAt()).toMillis());
    assertCreatedAt(firstRow, first, "systemEventCreatedAt");
    assertCreatedAt(secondRow, second, "systemEventCreatedAt");
    assertEquals(aUserId.toString(), first.get("addedBy").asText());
    assertEquals(aUserId.toString(), second.get("addedBy").asText());

    assertEquals(dUserId.toString(), nextEvent(queueE, "RoomMemberAdded").get("userId").asText());
    assertEquals(eUserId.toString(), nextEvent(queueE, "RoomMemberAdded").get("userId").asText());
  }

  @Test
  @DisplayName("A bulk add listing the same user twice adds it once, with one row and one event")
  void bulkAddedDuplicate() throws Exception {
    addMembers(List.of(dUserId, dUserId));

    assertEquals(dUserId.toString(), nextEvent(queueA, "RoomMemberAdded").get("userId").asText());
    assertEquals(
        1L,
        systemEventRepository.findByRoomId(roomId.toString()).stream()
            .filter(e -> "MEMBER_ADDED".equals(e.getType()))
            .count());
  }

  @Test
  @DisplayName("RoomMemberRemoved carries the remover and the id of the persisted row")
  void removed() throws Exception {
    assertEquals(
        204,
        dispatcher
            .delete(String.format("/rooms/%s/members/%s", roomId, cUserId), aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "RoomMemberRemoved");
    assertEquals(cUserId.toString(), event.get("userId").asText());
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("MEMBER_REMOVED", row.getType());
    assertEquals(aUserId.toString(), row.getActorId());
    assertEquals(row.getActorId(), event.get("removedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");
  }

  @Test
  @DisplayName("On self-leave removedBy is the leaving user")
  void selfLeave() throws Exception {
    assertEquals(
        204,
        dispatcher
            .delete(String.format("/rooms/%s/members/%s", roomId, bUserId), bToken)
            .getStatus());

    JsonNode event = nextEvent(queueA, "RoomMemberRemoved");
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertNotNull(row);
    assertEquals(bUserId.toString(), event.get("userId").asText());
    assertEquals(bUserId.toString(), event.get("removedBy").asText());
    assertEquals(row.getActorId(), event.get("removedBy").asText());
    assertFalse(event.get("systemEventId").isNull());
    assertCreatedAt(row, event, "systemEventCreatedAt");
  }

  @Test
  @DisplayName(
      "Reading a live system line puts the read marker exactly at its systemEventCreatedAt")
  void readMarkerLandsOnLiveLineTime() throws Exception {
    assertEquals(
        204,
        dispatcher
            .delete(String.format("/rooms/%s/members/%s", roomId, cUserId), aToken)
            .getStatus());
    JsonNode removed = nextEvent(queueB, "RoomMemberRemoved");

    assertEquals(204, markRead(removed.get("systemEventId").asText(), bToken).getStatus());

    JsonNode read = nextEvent(queueB, "ReadUpdated");
    assertTrue(
        OffsetDateTime.parse(read.get("itemCreatedAt").asText())
            .isEqual(OffsetDateTime.parse(removed.get("systemEventCreatedAt").asText())),
        "marker " + read.get("itemCreatedAt") + " != line " + removed.get("systemEventCreatedAt"));
  }

  private JsonNode content(SystemEvent row) throws Exception {
    return objectMapper.readTree(row.getContent());
  }

  private List<JsonNode> timelineSystemEvents(UUID room, String token) throws Exception {
    MockHttpResponse response = dispatcher.get(String.format("/rooms/%s/timeline", room), token);
    assertEquals(200, response.getStatus());
    List<JsonNode> events = new ArrayList<>();
    objectMapper
        .readTree(response.getContentAsString())
        .get("items")
        .forEach(
            item -> {
              if (item.hasNonNull("systemEvent")) {
                events.add(item.get("systemEvent"));
              }
            });
    return events;
  }

  private JsonNode timelineEvent(UUID room, String token, String id) throws Exception {
    return timelineSystemEvents(room, token).stream()
        .filter(e -> id.equals(e.get("id").asText()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("system event not in timeline: " + id));
  }

  private String updateBody(String name, String description) throws Exception {
    Map<String, String> body = new LinkedHashMap<>();
    if (name != null) {
      body.put("name", name);
    }
    if (description != null) {
      body.put("description", description);
    }
    return objectMapper.writeValueAsString(body);
  }

  @Test
  @DisplayName("RoomUpdated with name and description carries updatedBy and both persisted rows")
  void roomUpdatedNameAndDescription() throws Exception {
    assertEquals(
        200,
        dispatcher
            .put(
                String.format("/rooms/%s", roomId),
                updateBody("renamed", "new description"),
                aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "RoomUpdated");
    assertEquals(aUserId.toString(), event.get("updatedBy").asText());
    SystemEvent nameRow = persisted(event.get("nameSystemEventId").asText());
    SystemEvent descriptionRow = persisted(event.get("descriptionSystemEventId").asText());
    assertEquals("ROOM_NAME_CHANGED", nameRow.getType());
    assertEquals("ROOM_DESCRIPTION_CHANGED", descriptionRow.getType());
    assertEquals(roomId.toString(), nameRow.getRoomId());
    assertEquals(aUserId.toString(), nameRow.getActorId());
    assertEquals(aUserId.toString(), descriptionRow.getActorId());
    assertEquals(aUserId.toString(), content(nameRow).get("changedBy").asText());
    assertEquals("renamed", content(nameRow).get("name").asText());
    assertEquals(aUserId.toString(), content(descriptionRow).get("changedBy").asText());
    assertEquals("new description", content(descriptionRow).get("description").asText());
    assertEquals(
        1L, Duration.between(nameRow.getCreatedAt(), descriptionRow.getCreatedAt()).toMillis());
    assertCreatedAt(nameRow, event, "nameSystemEventCreatedAt");
    assertCreatedAt(descriptionRow, event, "descriptionSystemEventCreatedAt");

    JsonNode timelineName = timelineEvent(roomId, bToken, nameRow.getId());
    assertEquals("ROOM_NAME_CHANGED", timelineName.get("type").asText());
    assertEquals(aUserId.toString(), timelineName.get("actorId").asText());
    assertEquals("renamed", timelineName.get("content").get("name").asText());
    JsonNode timelineDescription = timelineEvent(roomId, bToken, descriptionRow.getId());
    assertEquals("ROOM_DESCRIPTION_CHANGED", timelineDescription.get("type").asText());
    assertEquals("new description", timelineDescription.get("content").get("description").asText());
    assertEquals(204, markRead(descriptionRow.getId(), bToken).getStatus());
  }

  @Test
  @DisplayName(
      "RoomUpdated for a description cleared to empty carries only descriptionSystemEventId")
  void roomUpdatedDescriptionCleared() throws Exception {
    dispatcher.put(String.format("/rooms/%s", roomId), updateBody(null, "something"), aToken);
    nextEvent(queueB, "RoomUpdated");

    assertEquals(
        200,
        dispatcher
            .put(String.format("/rooms/%s", roomId), updateBody(null, ""), aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "RoomUpdated");
    assertFalse(event.has("nameSystemEventId"));
    SystemEvent row = persisted(event.get("descriptionSystemEventId").asText());
    assertEquals("ROOM_DESCRIPTION_CHANGED", row.getType());
    assertEquals("", content(row).get("description").asText());
    assertCreatedAt(row, event, "descriptionSystemEventCreatedAt");
    assertFalse(event.has("nameSystemEventCreatedAt"));
    assertEquals(aUserId.toString(), event.get("updatedBy").asText());
  }

  @Test
  @DisplayName("An update that changes nothing writes no system event and sends no RoomUpdated")
  void roomUpdatedNothingChanged() throws Exception {
    dispatcher.put(String.format("/rooms/%s", roomId), updateBody("same-name", null), aToken);
    nextEvent(queueB, "RoomUpdated");
    int before = timelineSystemEvents(roomId, aToken).size();

    assertEquals(
        200,
        dispatcher
            .put(String.format("/rooms/%s", roomId), updateBody("same-name", null), aToken)
            .getStatus());

    assertEquals(before, timelineSystemEvents(roomId, aToken).size());
  }

  @Test
  @DisplayName("RoomPictureChanged carries updatedBy and the id of the persisted row")
  void pictureUpdated() throws Exception {
    FileMock image = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
    storageMockServer
        .when(
            request()
                .withMethod("PUT")
                .withPath("/upload")
                .withQueryStringParameter(param("type", "chats")))
        .respond(
            HttpResponse.response()
                .withStatusCode(201)
                .withBody(
                    JsonBody.json(
                        new StorageMockServer.UploadResponse()
                            .digest("digest")
                            .digestAlgorithm("digest-algorithm")
                            .size(image.getSize()))));

    assertEquals(
        204,
        dispatcher
            .put(
                String.format("/rooms/%s/picture", roomId),
                image.getFileBytes(),
                Map.of(
                    "Content-Type",
                    "application/octet-stream",
                    "fileName",
                    StringFormatUtils.encodeToUtf8("peanuts.jpg"),
                    "mimeType",
                    image.getMimeType(),
                    "Content-Length",
                    String.valueOf(image.getSize())),
                aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "RoomPictureChanged");
    assertEquals(aUserId.toString(), event.get("updatedBy").asText());
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("ROOM_PICTURE_UPDATED", row.getType());
    assertEquals(roomId.toString(), row.getRoomId());
    assertEquals(aUserId.toString(), row.getActorId());
    assertEquals(aUserId.toString(), content(row).get("updatedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");
    JsonNode timeline = timelineEvent(roomId, bToken, row.getId());
    assertEquals("ROOM_PICTURE_UPDATED", timeline.get("type").asText());
    assertEquals(aUserId.toString(), timeline.get("content").get("updatedBy").asText());
  }

  @Test
  @DisplayName("RoomPictureDeleted carries deletedBy and the id of the persisted row")
  void pictureDeleted() throws Exception {
    FileMock image = MockedFiles.get(MockedFileType.PEANUTS_IMAGE);
    integrationTestUtils.generateAndSaveFileMetadata(
        image, FileMetadataType.ROOM_AVATAR, aUserId, roomId);
    storageMockServer.mockDelete(image.getId(), true);

    assertEquals(
        204, dispatcher.delete(String.format("/rooms/%s/picture", roomId), aToken).getStatus());

    JsonNode event = nextEvent(queueB, "RoomPictureDeleted");
    assertEquals(aUserId.toString(), event.get("deletedBy").asText());
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("ROOM_PICTURE_DELETED", row.getType());
    assertEquals(aUserId.toString(), row.getActorId());
    assertEquals(aUserId.toString(), content(row).get("deletedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");
    JsonNode timeline = timelineEvent(roomId, bToken, row.getId());
    assertEquals("ROOM_PICTURE_DELETED", timeline.get("type").asText());
    assertEquals(aUserId.toString(), timeline.get("content").get("deletedBy").asText());
  }

  @Test
  @DisplayName("Clearing a TEMPORARY room persists ROOM_HISTORY_CLEARED after the purge")
  void temporaryHistoryCleared() throws Exception {
    UUID temporaryRoomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        temporaryRoomId, RoomTypeDto.TEMPORARY, "temporary", List.of(aUserId, bUserId));
    integrationTestUtils.generateAndSaveMessage(
        UUID.randomUUID().toString(), temporaryRoomId, aUserId, "to be purged");
    systemEventRepository.insert(
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(temporaryRoomId.toString())
            .type("MEMBER_ADDED")
            .actorId(aUserId.toString())
            .content("{}"));

    assertEquals(
        200,
        dispatcher
            .put(String.format("/rooms/%s/clear", temporaryRoomId), null, aToken)
            .getStatus());

    JsonNode event = nextEvent(queueB, "RoomHistoryCleared");
    assertEquals(aUserId.toString(), event.get("clearedBy").asText());
    SystemEvent row = persisted(event.get("systemEventId").asText());
    assertEquals("ROOM_HISTORY_CLEARED", row.getType());
    assertEquals(temporaryRoomId.toString(), row.getRoomId());
    assertEquals(aUserId.toString(), row.getActorId());
    assertEquals(aUserId.toString(), content(row).get("clearedBy").asText());
    assertCreatedAt(row, event, "systemEventCreatedAt");

    List<JsonNode> remaining = timelineSystemEvents(temporaryRoomId, bToken);
    assertEquals(1, remaining.size());
    assertEquals(row.getId(), remaining.get(0).get("id").asText());
    assertEquals("ROOM_HISTORY_CLEARED", remaining.get(0).get("type").asText());
  }

  @Test
  @DisplayName("The per-user clear of a GROUP room has no clearedBy, no systemEventId and no row")
  void groupHistoryClearedForUser() throws Exception {

    assertEquals(
        200, dispatcher.put(String.format("/rooms/%s/clear", roomId), null, bToken).getStatus());

    JsonNode event = nextEvent(queueB, "RoomHistoryCleared");
    assertFalse(event.has("clearedBy"));
    assertFalse(event.has("systemEventId"));
    assertFalse(event.has("systemEventCreatedAt"));
    assertTrue(
        timelineSystemEvents(roomId, aToken).stream()
            .noneMatch(e -> "ROOM_HISTORY_CLEARED".equals(e.get("type").asText())));
  }
}
