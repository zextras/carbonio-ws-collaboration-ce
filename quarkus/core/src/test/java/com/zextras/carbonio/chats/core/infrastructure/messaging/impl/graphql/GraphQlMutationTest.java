// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class GraphQlMutationTest {

  @Test
  @DisplayName("checkAuth returns a query string targeting the checkAuth operation")
  void checkAuth_returnsCorrectQueryString() {
    String query = GraphQlMutation.checkAuth();

    assertTrue(query.contains("checkAuth"));
    assertTrue(query.contains("authStatus"));
  }

  @Test
  @DisplayName("createRoom builds a muc_light mutation with mucDomain, roomId and ownerJid")
  void createRoom_buildsCorrectBody() {
    GraphQlBody body = GraphQlMutation.createRoom("muclight.carbonio", "room-1", "owner@carbonio");

    assertAll(
        () -> assertEquals("muc_light", body.getOperationName()),
        () -> assertTrue(body.getQuery().contains("muclight.carbonio")),
        () -> assertTrue(body.getQuery().contains("room-1")),
        () -> assertTrue(body.getQuery().contains("owner@carbonio")),
        () -> assertTrue(body.getQuery().contains("createRoom")),
        () -> assertTrue(body.getVariables().isEmpty()));
  }

  @Test
  @DisplayName("inviteUser builds a muc_light mutation with room, sender and recipient JIDs")
  void inviteUser_buildsCorrectBody() {
    GraphQlBody body =
        GraphQlMutation.inviteUser(
            "room-1@muclight.carbonio", "sender@carbonio", "recipient@carbonio");

    assertAll(
        () -> assertEquals("muc_light", body.getOperationName()),
        () -> assertTrue(body.getQuery().contains("room-1@muclight.carbonio")),
        () -> assertTrue(body.getQuery().contains("sender@carbonio")),
        () -> assertTrue(body.getQuery().contains("recipient@carbonio")),
        () -> assertTrue(body.getQuery().contains("inviteUser")),
        () -> assertTrue(body.getVariables().isEmpty()));
  }

  @Test
  @DisplayName("kickUser builds a muc_light mutation with room and user JIDs")
  void kickUser_buildsCorrectBody() {
    GraphQlBody body = GraphQlMutation.kickUser("room-1@muclight.carbonio", "user@carbonio");

    assertAll(
        () -> assertEquals("muc_light", body.getOperationName()),
        () -> assertTrue(body.getQuery().contains("room-1@muclight.carbonio")),
        () -> assertTrue(body.getQuery().contains("user@carbonio")),
        () -> assertTrue(body.getQuery().contains("kickUser")),
        () -> assertTrue(body.getVariables().isEmpty()));
  }

  @Test
  @DisplayName("setMutualSubscription builds a roster mutation with userA and userB JIDs")
  void setMutualSubscription_buildsCorrectBody() {
    GraphQlBody body = GraphQlMutation.setMutualSubscription("userA@carbonio", "userB@carbonio");

    assertAll(
        () -> assertEquals("roster", body.getOperationName()),
        () -> assertTrue(body.getQuery().contains("userA@carbonio")),
        () -> assertTrue(body.getQuery().contains("userB@carbonio")),
        () -> assertTrue(body.getQuery().contains("setMutualSubscription")),
        () -> assertTrue(body.getQuery().contains("CONNECT")),
        () -> assertTrue(body.getVariables().isEmpty()));
  }

  @Test
  @DisplayName(
      "sendStanza builds a stanza mutation embedding the raw stanza and selecting id and stanza_id")
  void sendStanza_buildsCorrectBody() {
    String rawStanza = "<message to='room@muclight.carbonio'/>";
    GraphQlBody body = GraphQlMutation.sendStanza(rawStanza);

    assertAll(
        () -> assertEquals("stanza", body.getOperationName()),
        () -> assertTrue(body.getQuery().contains(rawStanza)),
        () -> assertTrue(body.getQuery().contains("sendStanza")),
        () -> assertTrue(body.getQuery().contains("id")),
        () -> assertTrue(body.getQuery().contains("stanza_id")),
        () -> assertTrue(body.getVariables().isEmpty()));
  }
}
