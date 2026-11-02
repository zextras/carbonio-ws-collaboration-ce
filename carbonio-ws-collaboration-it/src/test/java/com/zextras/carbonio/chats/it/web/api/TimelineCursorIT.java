// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class TimelineCursorIT {

  private record Stored(String id, OffsetDateTime at) {}

  private static final OffsetDateTime BASE =
      OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 123_000_000, ZoneOffset.UTC);

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;

  private UUID userId;
  private String token;
  private UUID roomId;
  private List<Stored> expectedNewestFirst;

  public TimelineCursorIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeEach
  void setUp() {
    userId = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
    token = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "cursor-room", List.of(userId));

    List<Stored> stored = new ArrayList<>();
    stored.add(saveMessage(BASE.plusNanos(100_000)));
    stored.add(saveMessage(BASE.plusNanos(200_000)));
    stored.add(saveEvent(BASE.plusNanos(250_000)));
    stored.add(saveMessage(BASE.plusNanos(300_000)));
    expectedNewestFirst = newestFirst(stored);
  }

  private static List<Stored> newestFirst(List<Stored> stored) {
    return stored.stream()
        .sorted(
            Comparator.comparing(Stored::at)
                .thenComparing(Stored::id, Comparator.naturalOrder())
                .reversed())
        .toList();
  }

  private UUID freshRoom() {
    roomId = UUID.randomUUID();
    integrationTestUtils.generateAndSaveRoom(
        roomId, RoomTypeDto.GROUP, "cursor-room", List.of(userId));
    return roomId;
  }

  private List<String> timelineIds(String query) throws Exception {
    var response = dispatcher.get(String.format("/rooms/%s/timeline%s", roomId, query), token);
    assertEquals(200, response.getStatus());
    JsonNode root = objectMapper.readTree(response.getContentAsString());
    List<String> ids = new ArrayList<>();
    root.get("items").forEach(item -> ids.add(itemId(item)));
    return ids;
  }

  private Stored saveMessage(OffsetDateTime at) {
    String id = UUID.randomUUID().toString();
    messageRepository.insert(
        Message.create()
            .id(id)
            .roomId(roomId.toString())
            .senderId(userId.toString())
            .text("text-" + id));
    integrationTestUtils.setMessageTime(id, at);
    return new Stored(id, at);
  }

  private Stored saveEvent(OffsetDateTime at) {
    String id = UUID.randomUUID().toString();
    systemEventRepository.insert(
        SystemEvent.create()
            .id(id)
            .roomId(roomId.toString())
            .type("CURSOR_TEST")
            .actorId(userId.toString())
            .content("{}"));
    integrationTestUtils.setSystemEventTime(id, at);
    return new Stored(id, at);
  }

  private static String enc(OffsetDateTime t) {
    return URLEncoder.encode(t.toString(), StandardCharsets.UTF_8);
  }

  private static String itemId(JsonNode item) {
    return item.has("message") && !item.get("message").isNull()
        ? item.get("message").get("id").asText()
        : item.get("systemEvent").get("id").asText();
  }

  private List<String> walk(UnaryOperator<OffsetDateTime> clientPrecision) throws Exception {
    List<String> walked = new ArrayList<>();
    String query = "?limit=1";
    for (int guard = 0; guard < expectedNewestFirst.size() + 3; guard++) {
      var response = dispatcher.get(String.format("/rooms/%s/timeline%s", roomId, query), token);
      assertEquals(200, response.getStatus());
      JsonNode root = objectMapper.readTree(response.getContentAsString());
      if (root.get("items").isEmpty()) {
        return walked;
      }
      JsonNode oldest = root.get("items").get(0);
      walked.add(itemId(oldest));
      if (!root.get("hasMoreBefore").asBoolean()) {
        return walked;
      }
      OffsetDateTime createdAt = OffsetDateTime.parse(oldest.get("createdAt").asText());
      query =
          String.format(
              "?limit=1&before=%s&beforeId=%s",
              enc(clientPrecision.apply(createdAt)), itemId(oldest));
    }
    fail("walk did not terminate, collected " + walked);
    return walked;
  }

  @Test
  @DisplayName("Paging with before truncated to milliseconds returns every item once")
  void pagingWithMillisecondBefore() throws Exception {
    assertEquals(
        expectedNewestFirst.stream().map(Stored::id).toList(),
        walk(t -> t.truncatedTo(ChronoUnit.MILLIS)));
  }

  @Test
  @DisplayName("Paging with microsecond before returns every item once")
  void pagingWithMicrosecondBefore() throws Exception {
    assertEquals(expectedNewestFirst.stream().map(Stored::id).toList(), walk(t -> t));
  }

  @Test
  @DisplayName("Paging with a message and a system event at the same microsecond is stable")
  void pagingWithSameMicrosecondTie() throws Exception {
    freshRoom();
    List<Stored> stored = new ArrayList<>();
    stored.add(saveMessage(BASE.plusNanos(100_000)));
    stored.add(saveMessage(BASE.plusNanos(500_000)));
    stored.add(saveEvent(BASE.plusNanos(500_000)));
    stored.add(saveMessage(BASE.plusNanos(900_000)));
    expectedNewestFirst = newestFirst(stored);
    List<String> expected = expectedNewestFirst.stream().map(Stored::id).toList();
    assertEquals(expected, walk(t -> t.truncatedTo(ChronoUnit.MILLIS)));
    assertEquals(expected, walk(t -> t));
  }

  @Test
  @DisplayName("A beforeId belonging to another room falls back to the client before")
  void beforeIdOfAnotherRoomFallsBackToBefore() throws Exception {
    String foreignId = saveMessage(BASE.plusNanos(10_000_000)).id();
    List<Stored> stored = new ArrayList<>();
    freshRoom();
    stored.add(saveMessage(BASE.plusNanos(100_000)));
    stored.add(saveMessage(BASE.plusNanos(200_000)));
    stored.add(saveMessage(BASE.plusNanos(300_000)));
    assertEquals(
        List.of(stored.get(0).id(), stored.get(1).id()),
        timelineIds("?limit=10&before=" + enc(BASE.plusNanos(250_000)) + "&beforeId=" + foreignId));
  }

  private void assertAroundHalvesDisjoint(OffsetDateTime around, List<String> expected)
      throws Exception {
    assertEquals(expected, timelineIds("?limit=10&around=" + enc(around)));
  }

  @Test
  @DisplayName("Around keeps the halves disjoint for a millisecond or microsecond target")
  void aroundHalvesAreDisjoint() throws Exception {
    freshRoom();
    Stored earlier = saveMessage(BASE.minusNanos(1_000_000));
    Stored first = saveMessage(BASE.plusNanos(100_000));
    Stored middle = saveEvent(BASE.plusNanos(400_000));
    Stored last = saveMessage(BASE.plusNanos(700_000));
    Stored later = saveMessage(BASE.plusNanos(2_000_000));
    List<String> expected = List.of(earlier.id(), first.id(), middle.id(), last.id(), later.id());
    assertAroundHalvesDisjoint(middle.at().truncatedTo(ChronoUnit.MILLIS), expected);
    assertAroundHalvesDisjoint(middle.at(), expected);
  }
}
