// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
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
import java.util.List;
import java.util.UUID;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class SearchIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final MessageRepository messageRepository;

  private static UUID meId;
  private static UUID otherId;
  private static String meToken;

  private static final OffsetDateTime T0 =
      OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);

  private Room room;
  private String roomId;

  public SearchIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      MessageRepository messageRepository) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.messageRepository = messageRepository;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
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
            UUID.fromString(roomId), RoomTypeDto.GROUP, "search-test-room", List.of(meId, otherId));
  }

  private String message(String text, int second) {
    String id = UUID.randomUUID().toString();
    messageRepository.insert(
        Message.create().id(id).roomId(roomId).senderId(otherId.toString()).text(text));
    integrationTestUtils.setMessageTime(id, T0.plusSeconds(second));
    return id;
  }

  private MockHttpResponse search(String q) throws Exception {
    return dispatcher.get(
        String.format(
            "/rooms/%s/messages/search?q=%s", roomId, URLEncoder.encode(q, StandardCharsets.UTF_8)),
        meToken);
  }

  private List<String> found(String q) throws Exception {
    MockHttpResponse response = search(q);
    assertEquals(200, response.getStatus());
    List<String> ids = new java.util.ArrayList<>();
    for (JsonNode m : objectMapper.readTree(response.getContentAsString()).get("messages")) {
      ids.add(m.get("id").asText());
    }
    return ids;
  }

  @Test
  @DisplayName("Words are ANDed in any order")
  void wordsAnyOrder() throws Exception {
    String hit = message("meeting about the budget", 1);
    message("meeting about the roadmap", 2);

    assertEquals(List.of(hit), found("budget meeting"));
  }

  @Test
  @DisplayName("Punctuation in the query and in the text is insignificant")
  void punctuationInsensitive() throws Exception {
    String hit = message("hello, world", 1);

    assertEquals(List.of(hit), found("hello world"));
    assertEquals(List.of(hit), found("hello-world"));
    assertEquals(List.of(hit), found("hello, world!"));
  }

  @Test
  @DisplayName("Matching is case-insensitive")
  void caseInsensitive() throws Exception {
    String hit = message("Ciao Mondo", 1);

    assertEquals(List.of(hit), found("CIAO mondo"));
  }

  @Test
  @DisplayName("Accented and non-Latin words are kept")
  void unicodeWords() throws Exception {
    String accented = message("perché no", 1);
    String cyrillic = message("привет мир", 2);

    assertEquals(List.of(accented), found("perché"));
    assertEquals(List.of(cyrillic), found("Привет"));
  }

  @Test
  @DisplayName("A percent sign is dropped from the query and never acts as a wildcard")
  void percentIsDropped() throws Exception {
    String thousand = message("1000", 1);
    String hundred = message("go 100", 2);
    message("go fifty", 3);

    assertEquals(List.of(hundred, thousand), found("100%"));
    assertEquals(List.of(hundred), found("go 100%"));
    assertEquals(List.of(), found("%"));
  }

  @Test
  @DisplayName("Apostrophes split query words and match the stored text")
  void apostropheSplits() throws Exception {
    String hit = message("è l'ultimo giorno", 1);
    message("tutto bene", 2);

    assertEquals(List.of(hit), found("l'ultimo"));
  }

  @Test
  @DisplayName("An email address is searchable as its parts")
  void emailAddress() throws Exception {
    String hit = message("write to mario@zextras.com please", 1);
    message("write to luigi@zextras.com please", 2);

    assertEquals(List.of(hit), found("mario@zextras.com"));
  }

  @Test
  @DisplayName("An underscore is a literal character, not a single-character wildcard")
  void underscoreIsNotWildcard() throws Exception {
    message("axb", 1);
    String hit = message("a_b", 2);

    assertEquals(List.of(hit), found("a_b"));
  }

  @Test
  @DisplayName("A query without words returns an empty page with 200")
  void noWords() throws Exception {
    message("hello", 1);

    assertEquals(List.of(), found(""));
    assertEquals(List.of(), found("   "));
    assertEquals(List.of(), found(",.;:-?!"));
  }

  @Test
  @DisplayName("Search runs on the current text of edited messages")
  void editedText() throws Exception {
    String id = message("old wording", 1);
    integrationTestUtils.editMessageText(id, "new wording");

    assertEquals(List.of(), found("old"));
    assertEquals(List.of(id), found("new wording"));
  }

  @Test
  @DisplayName("Results stay newest first and hidden history is not searched")
  void clearedHistoryFloor() throws Exception {
    message("needle one", 1);
    message("needle two", 2);
    String visible = message("needle three", 3);
    integrationTestUtils.setRoomUserSettings(
        RoomUserSettings.create(room, meId.toString()).clearedAt(T0.plusSeconds(2)));

    assertEquals(List.of(visible), found("needle"));
  }

  private List<String> foundPage(String query) throws Exception {
    MockHttpResponse response =
        dispatcher.get(String.format("/rooms/%s/messages/search?%s", roomId, query), meToken);
    assertEquals(200, response.getStatus());
    List<String> ids = new java.util.ArrayList<>();
    for (JsonNode m : objectMapper.readTree(response.getContentAsString()).get("messages")) {
      ids.add(m.get("id").asText());
    }
    return ids;
  }

  private String messageAt(String text, OffsetDateTime at) {
    String id = UUID.randomUUID().toString();
    messageRepository.insert(
        Message.create().id(id).roomId(roomId).senderId(otherId.toString()).text(text));
    integrationTestUtils.setMessageTime(id, at);
    return id;
  }

  @Test
  @DisplayName("Search before with beforeId pages through messages sharing one timestamp")
  void beforeIdPagesThroughTiedTimestamps() throws Exception {
    OffsetDateTime tieAt = T0.plusSeconds(10).plusNanos(123_456_000);
    messageAt("needle first", T0.plusSeconds(1));
    for (int i = 0; i < 5; i++) {
      messageAt("needle tied " + i, tieAt);
    }
    messageAt("needle last", T0.plusSeconds(20));
    List<String> full = foundPage("q=needle&limit=50");
    assertEquals(7, full.size());

    List<String> walked = new java.util.ArrayList<>(foundPage("q=needle&limit=3"));
    assertEquals(full.subList(0, 3), walked);
    for (int guard = 0; guard < 5 && walked.size() < full.size(); guard++) {
      String lastId = walked.get(walked.size() - 1);
      OffsetDateTime lastAt = messageRepository.getById(lastId).orElseThrow().getCreatedAt();
      walked.addAll(
          foundPage(
              String.format(
                  "q=needle&limit=3&before=%s&beforeId=%s",
                  URLEncoder.encode(lastAt.toString(), StandardCharsets.UTF_8), lastId)));
    }
    assertEquals(full, walked);
  }

  @Test
  @DisplayName("Search before without beforeId keeps the strict timestamp semantics")
  void beforeWithoutIdIsStrict() throws Exception {
    OffsetDateTime tieAt = T0.plusSeconds(10);
    String first = messageAt("needle first", T0.plusSeconds(1));
    for (int i = 0; i < 3; i++) {
      messageAt("needle tied " + i, tieAt);
    }
    assertEquals(
        List.of(first),
        foundPage(
            "q=needle&limit=50&before="
                + URLEncoder.encode(tieAt.toString(), StandardCharsets.UTF_8)));
  }
}
