// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.socket;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.restassured.http.ContentType;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class EventsSocketTest {

  @Inject DataSource ds;
  @Inject Instance<BasicWebSocketConnector> connectors;

  @TestHTTPResource("/")
  URI baseUri;

  @BeforeEach
  void setUp() {
    var wm = WireMockResource.server();
    wm.resetAll();
    Stubs.janus(wm);
    Stubs.activeUser(wm, TestUsers.OWNER);
    Stubs.activeUser(wm, TestUsers.MEMBER);
    Seed.clean(ds);
  }

  WebSocketClientConnection connect(String cookie, List<String> sink) {
    BasicWebSocketConnector connector = connectors.get().baseUri(baseUri).path("/events");
    if (cookie != null) {
      connector = connector.addHeader("Cookie", cookie);
    }
    return connector.onTextMessage((c, m) -> sink.add(m)).connectAndAwait();
  }

  @Test
  void connectedClientReceivesItsEvents() {
    UUID roomId = UUID.randomUUID();
    UUID meetingId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));
    Seed.meeting(ds, meetingId, roomId, false);
    List<String> messages = new CopyOnWriteArrayList<>();

    WebSocketClientConnection connection =
        connect("ZM_AUTH_TOKEN=" + TestUsers.token(TestUsers.MEMBER), messages);
    await().atMost(Duration.ofSeconds(5)).until(() -> !messages.isEmpty());
    assertThat(messages.get(0)).contains("\"type\":\"WebsocketConnected\"").contains("queueId");

    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER)).contentType(ContentType.JSON)
        .post("/meetings/" + meetingId + "/start").then().statusCode(200);

    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        assertThat(String.join("\n", messages)).contains("\"type\":\"MeetingStarted\"").contains(meetingId.toString()));
    connection.closeAndAwait();
  }

  @Test
  void anonymousUpgradeIsRefused() {
    assertThatThrownBy(() -> connect(null, new CopyOnWriteArrayList<>()))
        .hasStackTraceContaining("401");
  }

  @Test
  void deniedUserUpgradeIsRefused() {
    Stubs.myself(WireMockResource.server(), TestUsers.OUTSIDER, "INTERNAL", "locked", true, Map.of());
    assertThatThrownBy(() ->
            connect("ZM_AUTH_TOKEN=" + TestUsers.token(TestUsers.OUTSIDER), new CopyOnWriteArrayList<>()))
        .hasStackTraceContaining("403");
  }
}
