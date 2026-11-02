// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
    expectedNewestFirst =
        stored.stream()
            .sorted(
                Comparator.comparing(Stored::at)
                    .thenComparing(Stored::id, Comparator.naturalOrder())
                    .reversed())
            .toList();
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
    assertFalse(true, "walk did not terminate, collected " + walked);
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
}
