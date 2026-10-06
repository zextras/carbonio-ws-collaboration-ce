// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.RoomDeleted;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.UpgradeRejectedException;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketConnectOptions;
import jakarta.inject.Inject;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class EventsWebSocketTest {

  @TestHTTPResource("/events")
  URI events;

  @Inject EventDispatcher eventDispatcher;
  @Inject ObjectMapper objectMapper;

  @Inject Vertx vertx;

  private final LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();

  private JsonNode next() throws Exception {
    String message = messages.poll(10, TimeUnit.SECONDS);
    assertThat(message).as("websocket message").isNotNull();
    return objectMapper.readTree(message);
  }

  private WebSocket connect(String cookie, List<String> subprotocols) throws Exception {
    WebSocketConnectOptions options =
        new WebSocketConnectOptions()
            .setHost(events.getHost())
            .setPort(events.getPort())
            .setURI(events.getPath())
            .setSubProtocols(subprotocols);
    if (cookie != null) {
      options.addHeader("Cookie", "ZM_AUTH_TOKEN=" + cookie);
    }
    WebSocket ws =
        vertx
            .createWebSocketClient()
            .connect(options)
            .toCompletionStage()
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);
    ws.textMessageHandler(messages::add);
    return ws;
  }

  private static int refusedStatus(ThrowingCallable connect) {
    Throwable failure = catchThrowable(connect);
    assertThat(failure).hasCauseInstanceOf(UpgradeRejectedException.class);
    return ((UpgradeRejectedException) failure.getCause()).getStatus();
  }

  @Test
  void authenticatedClientNegotiatesASubprotocolAndReceivesEvents() throws Exception {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    String latest = VersionProvider.getVersion();
    WebSocket ws = connect(TestUsers.token(TestUsers.OWNER), List.of("0.0.1", latest));
    try {
      assertThat(ws.subProtocol()).isEqualTo(latest);

      JsonNode connected = next();
      assertThat(connected.get("type").asText()).isEqualTo(EventType.WEBSOCKET_CONNECTED.toString());
      UUID queueId = UUID.fromString(connected.get("queueId").asText());
      assertThat(queueId).isNotNull();

      UUID roomId = UUID.randomUUID();
      eventDispatcher.sendToUserExchange(
          TestUsers.OWNER.toString(),
          RoomDeleted.create()
              .roomId(roomId)
              .type(EventType.ROOM_DELETED)
              .sentDate(OffsetDateTime.now()));
      JsonNode event = next();
      assertThat(event.get("type").asText()).isEqualTo(EventType.ROOM_DELETED.toString());
      assertThat(event.get("roomId").asText()).isEqualTo(roomId.toString());

      ws.writeTextMessage("{\"type\":\"ping\"}");
      assertThat(next().get("type").asText())
          .isEqualTo(EventType.PONG.toString());
    } finally {
      ws.close();
    }
  }

  @Test
  void anonymousClientIsRefused() {
    assertThat(refusedStatus(() -> connect(null, List.of(VersionProvider.getVersion()))))
        .isEqualTo(401);
  }

  @Test
  void deniedUserIsRefused() {
    Stubs.myself(WireMockResource.server(), TestUsers.MEMBER, "INTERNAL", "locked", true, Map.of());
    assertThat(
            refusedStatus(
                () -> connect(TestUsers.token(TestUsers.MEMBER), List.of(VersionProvider.getVersion()))))
        .isEqualTo(403);
  }
}
