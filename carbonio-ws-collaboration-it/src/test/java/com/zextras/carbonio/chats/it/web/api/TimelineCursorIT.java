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
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
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

  private List<Stored> saveTieGroup(OffsetDateTime at) {
    List<Stored> tie = new ArrayList<>();
    tie.add(saveMessage(at));
    tie.add(saveMessage(at));
    tie.add(saveEvent(at));
    tie.add(saveMessage(at));
    tie.add(saveEvent(at));
    return tie;
  }

  @Test
  @DisplayName("After with afterId pages through tied timestamps without skips or repeats")
  void afterIdPagesThroughTiedTimestamps() throws Exception {
    freshRoom();
    OffsetDateTime tieAt = BASE.plusNanos(500_000);
    Stored earlier = saveMessage(BASE.plusNanos(100_000));
    saveTieGroup(tieAt);
    Stored later = saveMessage(BASE.plusNanos(900_000));
    List<String> full = timelineIds("?limit=50&after=" + enc(BASE));
    assertEquals(7, full.size());

    List<String> walked = new ArrayList<>();
    String query = "?limit=2&after=" + enc(BASE);
    for (int guard = 0; guard < 10; guard++) {
      var response = dispatcher.get(String.format("/rooms/%s/timeline%s", roomId, query), token);
      assertEquals(200, response.getStatus());
      JsonNode root = objectMapper.readTree(response.getContentAsString());
      JsonNode items = root.get("items");
      items.forEach(item -> walked.add(itemId(item)));
      if (!root.get("hasMoreAfter").asBoolean()) {
        break;
      }
      JsonNode last = items.get(items.size() - 1);
      query =
          String.format(
              "?limit=2&after=%s&afterId=%s",
              enc(
                  OffsetDateTime.parse(last.get("createdAt").asText())
                      .truncatedTo(ChronoUnit.MILLIS)),
              itemId(last));
    }
    assertEquals(full, walked);
    assertEquals(earlier.id(), full.get(0));
    assertEquals(later.id(), full.get(6));
  }

  @Test
  @DisplayName("After without afterId keeps the strict timestamp semantics")
  void afterWithoutIdIsStrict() throws Exception {
    freshRoom();
    OffsetDateTime tieAt = BASE.plusNanos(500_000);
    saveMessage(BASE.plusNanos(100_000));
    saveTieGroup(tieAt);
    Stored later = saveMessage(BASE.plusNanos(900_000));
    assertEquals(List.of(later.id()), timelineIds("?limit=50&after=" + enc(tieAt)));
  }

  @Test
  @DisplayName("Around with aroundId centres on the item inside a tied timestamp")
  void aroundIdCentresInsideTiedTimestamp() throws Exception {
    freshRoom();
    OffsetDateTime tieAt = BASE.plusNanos(500_000);
    saveMessage(BASE.plusNanos(100_000));
    saveTieGroup(tieAt);
    saveMessage(BASE.plusNanos(900_000));
    List<String> full = timelineIds("?limit=50&after=" + enc(BASE));
    assertEquals(7, full.size());

    int k = 3;
    assertEquals(
        full.subList(k - 2, k + 2),
        timelineIds("?limit=4&around=" + enc(tieAt) + "&aroundId=" + full.get(k)));
    k = 2;
    assertEquals(
        full.subList(k - 2, k + 2),
        timelineIds("?limit=4&around=" + enc(tieAt) + "&aroundId=" + full.get(k)));
  }

  @Test
  @DisplayName("Around without aroundId keeps the timestamp split")
  void aroundWithoutIdKeepsTimestampSplit() throws Exception {
    freshRoom();
    OffsetDateTime tieAt = BASE.plusNanos(500_000);
    saveMessage(BASE.plusNanos(100_000));
    saveTieGroup(tieAt);
    Stored later = saveMessage(BASE.plusNanos(1_500_000));
    List<String> full = timelineIds("?limit=50&after=" + enc(BASE));

    assertEquals(
        List.of(full.get(4), full.get(5), later.id()),
        timelineIds("?limit=4&around=" + enc(tieAt)));
  }

  @Test
  @DisplayName("An afterId or aroundId of another room falls back to the client timestamp")
  void foreignAfterAndAroundIdFallBack() throws Exception {
    String foreignId = saveMessage(BASE.plusNanos(10_000_000)).id();
    freshRoom();
    Stored a = saveMessage(BASE.plusNanos(100_000));
    Stored b = saveMessage(BASE.plusNanos(200_000));
    Stored c = saveMessage(BASE.plusNanos(300_000));
    assertEquals(
        List.of(b.id(), c.id()),
        timelineIds("?limit=10&after=" + enc(BASE.plusNanos(150_000)) + "&afterId=" + foreignId));
    assertEquals(
        timelineIds("?limit=10&around=" + enc(BASE.plusNanos(250_000))),
        timelineIds("?limit=10&around=" + enc(BASE.plusNanos(250_000)) + "&aroundId=" + foreignId));
    assertEquals(3, timelineIds("?limit=10&around=" + enc(BASE.plusNanos(250_000))).size());
    assertEquals(a.id(), timelineIds("?limit=10&around=" + enc(BASE.plusNanos(250_000))).get(0));
  }

  private Stored[] belowFloorRoom() {
    roomId = UUID.randomUUID();
    Room room =
        integrationTestUtils.generateAndSaveRoom(
            roomId, RoomTypeDto.GROUP, "cursor-room", List.of(userId));
    Stored a = saveMessage(BASE.plusNanos(100_000));
    Stored b = saveMessage(BASE.plusNanos(200_000));
    Stored c = saveMessage(BASE.plusNanos(300_000));
    Stored d = saveMessage(BASE.plusNanos(400_000));
    integrationTestUtils.setRoomUserSettings(
        RoomUserSettings.create(room, userId.toString()).clearedAt(BASE.plusNanos(250_000)));
    return new Stored[] {a, b, c, d};
  }

  @Test
  @DisplayName("A beforeId at or below the caller's floor behaves like an unknown id")
  void beforeIdBelowFloorIsUnknown() throws Exception {
    Stored[] s = belowFloorRoom();
    String query = "?limit=10&before=" + enc(BASE.plusNanos(350_000)) + "&beforeId=";
    List<String> viaUnknown = timelineIds(query + UUID.randomUUID());
    assertEquals(List.of(s[2].id()), viaUnknown);
    assertEquals(viaUnknown, timelineIds(query + s[1].id()));
    assertEquals(viaUnknown, timelineIds(query + s[0].id()));
  }

  @Test
  @DisplayName("An afterId at or below the caller's floor behaves like an unknown id")
  void afterIdBelowFloorIsUnknown() throws Exception {
    Stored[] s = belowFloorRoom();
    String query = "?limit=10&after=" + enc(BASE.plusNanos(350_000)) + "&afterId=";
    List<String> viaUnknown = timelineIds(query + UUID.randomUUID());
    assertEquals(List.of(s[3].id()), viaUnknown);
    assertEquals(viaUnknown, timelineIds(query + s[1].id()));
    assertEquals(viaUnknown, timelineIds(query + s[0].id()));
  }

  @Test
  @DisplayName("An aroundId at or below the caller's floor behaves like an unknown id")
  void aroundIdBelowFloorIsUnknown() throws Exception {
    Stored[] s = belowFloorRoom();
    String query = "?limit=2&around=" + enc(BASE.plusNanos(350_000)) + "&aroundId=";
    List<String> viaUnknown = timelineIds(query + UUID.randomUUID());
    assertEquals(List.of(s[3].id()), viaUnknown);
    assertEquals(viaUnknown, timelineIds(query + s[1].id()));
    assertEquals(viaUnknown, timelineIds(query + s[0].id()));
  }

  @Test
  @DisplayName("An afterId unknown to the room keeps the composite comparison with the timestamp")
  void unknownAfterIdIsComposite() throws Exception {
    freshRoom();
    OffsetDateTime tieAt = BASE.plusNanos(500_000);
    Stored first = saveMessage(tieAt);
    Stored second = saveMessage(tieAt);
    List<Stored> tie = newestFirst(List.of(first, second)).reversed();
    String lowId = "00000000-0000-0000-0000-000000000000";
    String highId = "ffffffff-ffff-ffff-ffff-ffffffffffff";
    assertEquals(
        tie.stream().map(Stored::id).toList(),
        timelineIds("?limit=10&after=" + enc(tieAt) + "&afterId=" + lowId));
    assertEquals(List.of(), timelineIds("?limit=10&after=" + enc(tieAt) + "&afterId=" + highId));
  }
}
