// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.seam;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.databind.JsonNode;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.ws.plugin.api.MeetingAccess;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.UserType;
import com.zextras.carbonio.ws.plugin.api.VideoServerGateway;
import com.zextras.carbonio.ws.plugin.api.VideoServerRooms;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.TransactionManager;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class PluginSeamsTest {

  @Inject DataSource dataSource;
  @Inject SeamProbes.StoppingProbe stopping;
  @Inject SeamProbes.ProbeEnricher enricher;
  @Inject VideoServerGateway gateway;
  @Inject MeetingAccess meetingAccess;
  @Inject TransactionManager transactionManager;

  private final UUID roomId = UUID.randomUUID();
  private final UUID meetingId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    Seed.clean(dataSource);
    stopping.seen.clear();
    enricher.enabled = false;
    WireMockResource.server().resetAll();
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    Stubs.activeUser(WireMockResource.server(), TestUsers.MEMBER);
    Stubs.janus(WireMockResource.server());
    Seed.room(dataSource, roomId, "GROUP", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));
  }

  @AfterEach
  void tearDown() {
    enricher.enabled = false;
  }

  private void activeMeeting() {
    Seed.meeting(dataSource, meetingId, roomId, true);
    Seed.participant(dataSource, meetingId, TestUsers.OWNER);
    Seed.videoServerMeeting(dataSource, meetingId, "server-1");
  }

  private static io.restassured.specification.RequestSpecification as(UUID user) {
    return given().cookie("ZM_AUTH_TOKEN", TestUsers.token(user));
  }

  private static UserPrincipal principal(UUID id) {
    return new UserPrincipal(id, UserType.INTERNAL, null, null, null, null, Map.of());
  }

  @Test
  void stoppingAnActiveMeetingFiresMeetingStoppingBeforeTheVideoserverTeardown() {
    activeMeeting();
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stop").then().statusCode(200);

    assertThat(stopping.seen).hasSize(1);
    assertThat(stopping.seen.get(0).event().meetingId()).isEqualTo(meetingId);
    assertThat(stopping.seen.get(0).event().userId()).isEqualTo(TestUsers.OWNER);
    assertThat(stopping.seen.get(0).videoServerRows()).isEqualTo(1);
  }

  @Test
  void deletingAnActiveMeetingFiresMeetingStopping() {
    activeMeeting();
    as(TestUsers.OWNER).delete("/meetings/" + meetingId).then().statusCode(204);
    assertThat(stopping.seen).hasSize(1);
    assertThat(stopping.seen.get(0).videoServerRows()).isEqualTo(1);
  }

  @Test
  void inactiveMeetingStopOrDeleteDoesNotFireMeetingStopping() {
    Seed.meeting(dataSource, meetingId, roomId, false);
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stop").then().statusCode(200);
    as(TestUsers.OWNER).delete("/meetings/" + meetingId).then().statusCode(204);
    assertThat(stopping.seen).isEmpty();
  }

  @Test
  void enricherExtrasAreTopLevelAndAbsentWithoutContribution() {
    activeMeeting();
    as(TestUsers.OWNER)
        .get("/meetings/" + meetingId)
        .then()
        .statusCode(200)
        .body("id", org.hamcrest.Matchers.equalTo(meetingId.toString()))
        .body("probe", nullValue());

    enricher.enabled = true;
    as(TestUsers.OWNER)
        .get("/meetings/" + meetingId)
        .then()
        .statusCode(200)
        .body("id", org.hamcrest.Matchers.equalTo(meetingId.toString()))
        .body("roomId", org.hamcrest.Matchers.equalTo(roomId.toString()))
        .body("active", org.hamcrest.Matchers.equalTo(true))
        .body("probe", org.hamcrest.Matchers.equalTo("meeting"));
    as(TestUsers.OWNER)
        .get("/meetings")
        .then()
        .statusCode(200)
        .body("[0].probe", org.hamcrest.Matchers.equalTo("meeting"));
  }

  @Test
  void gatewayExposesTheRoomsAndSendsPluginMessages() {
    activeMeeting();
    assertThat(gateway.roomsOf(meetingId))
        .contains(new VideoServerRooms("server-1", "audio-room", "video-room"));
    assertThat(gateway.roomsOf(UUID.randomUUID())).isEmpty();

    JsonNode response =
        gateway.sendVideoRoomMessage(meetingId, Map.of("request", "edit", "room", "video-room"));
    assertThat(response.path("janus").asText()).isEqualTo("success");
    WireMockResource.server()
        .verify(
            postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
                .withRequestBody(matchingJsonPath("$.body.request", equalTo("edit")))
                .withRequestBody(matchingJsonPath("$.janus", equalTo("message"))));
    assertThat(
            gateway
                .sendAudioBridgeMessage(meetingId, Map.of("request", "enable_mjrs"))
                .path("janus")
                .asText())
        .isEqualTo("success");
  }

  @Test
  void gatewayFailureDoesNotMarkTheCallerTransactionRollbackOnly() {
    activeMeeting();
    WireMockResource.server()
        .stubFor(post(urlPathEqualTo("/janus/conn-1/h-video")).atPriority(0).willReturn(serverError()));
    QuarkusTransaction.requiringNew()
        .run(
            () -> {
              assertThatThrownBy(() -> gateway.sendVideoRoomMessage(meetingId, Map.of("request", "x")))
                  .isInstanceOf(RuntimeException.class);
              assertThatThrownBy(() -> gateway.sendVideoRoomMessage(UUID.randomUUID(), Map.of()))
                  .isInstanceOf(RuntimeException.class);
              try {
                assertThat(transactionManager.getStatus()).isEqualTo(Status.STATUS_ACTIVE);
              } catch (jakarta.transaction.SystemException e) {
                throw new IllegalStateException(e);
              }
            });
  }

  @Test
  void meetingAccessRequiresAnActiveMeetingModerator() {
    Seed.meeting(dataSource, meetingId, roomId, false);
    assertThat(meetingAccess.find(meetingId)).hasValueSatisfying(v -> assertThat(v.active()).isFalse());
    assertThatThrownBy(() -> meetingAccess.requireActiveMeetingModerator(meetingId, principal(TestUsers.OWNER)))
        .isInstanceOf(BadRequestException.class);

    Seed.exec(dataSource, "UPDATE chats.meeting SET active = true WHERE id = ?", meetingId.toString());
    Seed.participant(dataSource, meetingId, TestUsers.OWNER);
    Seed.participant(dataSource, meetingId, TestUsers.MEMBER);
    assertThat(meetingAccess.requireActiveMeetingModerator(meetingId, principal(TestUsers.OWNER)).participantUserIds())
        .containsExactlyInAnyOrder(TestUsers.OWNER, TestUsers.MEMBER);
    assertThatThrownBy(() -> meetingAccess.requireActiveMeetingModerator(meetingId, principal(TestUsers.MEMBER)))
        .isInstanceOf(ForbiddenException.class);
  }
}
