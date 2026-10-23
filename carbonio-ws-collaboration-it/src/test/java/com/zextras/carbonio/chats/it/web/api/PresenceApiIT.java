// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.api.PresenceApi;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@ApiIntegrationTest
class PresenceApiIT {

  private static final String URL = "/presence/batch";

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final WsPresenceRepository wsPresenceRepository;

  public PresenceApiIT(
      PresenceApi presenceApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      WsPresenceRepository wsPresenceRepository) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.wsPresenceRepository = wsPresenceRepository;
    this.dispatcher.getRegistry().addSingletonResource(presenceApi);
  }

  private MockHttpResponse requestPresenceOf(int users) throws Exception {
    String body =
        objectMapper.writeValueAsString(
            Map.of("userIds", IntStream.range(0, users).mapToObj(i -> UUID.randomUUID()).toList()));
    return dispatcher.post(
        URL, body, MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken());
  }

  @Test
  @DisplayName("Up to 100 user ids are accepted")
  void getPresenceBatch_100Users_returnsAll() throws Exception {
    MockHttpResponse response = requestPresenceOf(100);

    assertEquals(200, response.getStatus());
    JsonNode result = objectMapper.readTree(response.getContentAsString());
    assertEquals(100, result.size());
  }

  @Test
  @DisplayName("More than 100 user ids are rejected with 400")
  void getPresenceBatch_101Users_returnsBadRequest() throws Exception {
    MockHttpResponse response = requestPresenceOf(101);

    assertEquals(400, response.getStatus());
  }

  @Test
  @DisplayName("Online and last activity are both returned, also for an online user")
  void getPresenceBatch_onlineUser_returnsLastActivityToo() throws Exception {
    String user = UUID.randomUUID().toString();
    OffsetDateTime lastActivity = wsPresenceRepository.upsertLastSeen(user);
    wsPresenceRepository.insertAndCountOnline(UUID.randomUUID().toString(), user, "svc");

    MockHttpResponse response =
        dispatcher.post(
            URL,
            objectMapper.writeValueAsString(Map.of("userIds", List.of(user))),
            MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken());

    JsonNode presence = objectMapper.readTree(response.getContentAsString()).get(0);
    assertTrue(presence.get("online").asBoolean());
    assertTrue(lastActivity.isEqual(OffsetDateTime.parse(presence.get("lastActivity").asText())));
  }
}
