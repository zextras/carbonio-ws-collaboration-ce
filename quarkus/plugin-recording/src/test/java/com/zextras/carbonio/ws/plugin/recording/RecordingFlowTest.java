// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.plugin.recording;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class RecordingFlowTest {

  @Inject DataSource ds;
  @Inject MessageBrokerVideoserverHealthMonitor videoServerHealth;
  WireMockServer wm;
  UUID roomId;
  String meetingId;

  static RequestSpecification as(UUID user) {
    return given().cookie("ZM_AUTH_TOKEN", TestUsers.token(user)).contentType(ContentType.JSON);
  }

  @BeforeEach
  void setUp() {
    wm = WireMockResource.server();
    // CE refuses to start meetings until a Janus heartbeat arrived on the event queue.
    videoServerHealth.notifyVideoServerReady();
    wm.resetAll();
    Stubs.janus(wm);
    Stubs.activeUser(wm, TestUsers.OWNER, Map.of("carbonioWscRecordingEnabled", "true"));
    Stubs.activeUser(wm, TestUsers.MEMBER, Map.of("carbonioWscRecordingEnabled", "true"));
    Stubs.activeGuest(wm, TestUsers.GUEST);
    wm.stubFor(post(urlPathEqualTo("/internal/recordings/process")).willReturn(okJson("{}")));
    Seed.clean(ds);
    roomId = UUID.randomUUID();
    Seed.room(ds, roomId, "GROUP", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false, TestUsers.GUEST, false));
    meetingId = as(TestUsers.OWNER)
        .body(Map.of("name", "Weekly", "meetingType", "permanent", "roomId", roomId.toString()))
        .post("/meetings").then().statusCode(200).extract().path("id");
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/start").then().statusCode(200);
    as(TestUsers.OWNER).header("queue-id", UUID.randomUUID().toString()).body(Map.of("audioStreamEnabled", false, "videoStreamEnabled", false)).post("/meetings/" + meetingId + "/join").then().statusCode(200);
  }

  long startedRecordings() {
    return Seed.count(ds, "SELECT count(*) FROM chats.recording WHERE meeting_id = ? AND status = 'STARTED'", meetingId);
  }

  @Test
  void startStopAndEnrichment() {
    as(TestUsers.OWNER).body(Map.of("folderId", "f1"))
        .post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);

    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-video"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("edit"))));
    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_mjrs"))));
    as(TestUsers.OWNER).get("/meetings/" + meetingId).then().statusCode(200)
        .body("recStartedAt", notNullValue())
        .body("recUserId", org.hamcrest.Matchers.equalTo(TestUsers.OWNER.toString()));

    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stopRecording").then().statusCode(204);
    assertThat(startedRecordings()).isZero();
    as(TestUsers.OWNER).get("/meetings/" + meetingId).then().body("recStartedAt", nullValue());
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        wm.verify(postRequestedFor(urlPathEqualTo("/internal/recordings/process"))
            .withoutHeader("service-id") // CE selectServerId() is null: no videoserver routing
            .withRequestBody(matchingJsonPath("$.meetingId", equalTo(meetingId)))
            .withRequestBody(matchingJsonPath("$.folderId", equalTo("f1")))));
  }

  @Test
  void startIsIdempotentOverHttp() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    assertThat(startedRecordings()).isEqualTo(1);
  }

  @Test
  void stoppingTheMeetingStopsTheRecording() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stop").then().statusCode(200);
    assertThat(startedRecordings()).isZero();
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.recording WHERE meeting_id = ? AND status = 'STOPPED'", meetingId))
        .isEqualTo(1);
  }

  /** Janus answers HTTP 500 to the recording plugin's stop calls (enable_mjrs off). */
  void janusFailsToStopRecording() {
    wm.stubFor(post(urlPathEqualTo("/janus/conn-1/h-audio")).atPriority(1)
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_mjrs")))
        .withRequestBody(matchingJsonPath("$.body.mjrs", equalTo("false")))
        .willReturn(serverError()));
  }

  long meetingCount(String sql) {
    return Seed.count(ds, sql, meetingId);
  }

  @Test
  void stoppingTheMeetingSucceedsWhenJanusFailsToStopTheRecording() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    janusFailsToStopRecording();

    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stop").then().statusCode(200)
        .body("active", org.hamcrest.Matchers.equalTo(false));

    wm.verify(postRequestedFor(urlPathEqualTo("/janus/conn-1/h-audio"))
        .withRequestBody(matchingJsonPath("$.body.request", equalTo("enable_mjrs")))
        .withRequestBody(matchingJsonPath("$.body.mjrs", equalTo("false"))));
    assertThat(startedRecordings()).isZero();
    assertThat(meetingCount("SELECT count(*) FROM chats.recording WHERE meeting_id = ? AND status = 'STOPPED'"))
        .isEqualTo(1);
    assertThat(meetingCount("SELECT count(*) FROM chats.meeting WHERE id = ? AND active = false")).isEqualTo(1);
    assertThat(meetingCount("SELECT count(*) FROM chats.videoserver_meeting WHERE meeting_id = ?")).isZero();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        wm.verify(postRequestedFor(urlPathEqualTo("/internal/recordings/process"))));
  }

  @Test
  void deletingTheMeetingSucceedsWhenJanusFailsToStopTheRecording() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    janusFailsToStopRecording();

    as(TestUsers.OWNER).delete("/meetings/" + meetingId).then().statusCode(204);

    assertThat(meetingCount("SELECT count(*) FROM chats.meeting WHERE id = ?")).isZero();
    assertThat(meetingCount("SELECT count(*) FROM chats.recording WHERE meeting_id = ?")).isZero();
  }

  @Test
  void explicitStopRecordingStillPropagatesJanusFailures() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    janusFailsToStopRecording();

    // CE renders videoserver failures (VideoServerException, a dependency failure) as 424.
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/stopRecording").then().statusCode(424);
    assertThat(startedRecordings()).isEqualTo(1);
  }

  @Test
  void leavingLastParticipantStopsRecording() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/leave").then().statusCode(204);
    assertThat(startedRecordings()).isZero();
  }

  @Test
  void deletingTheMeetingRemovesItsRecordings() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
    as(TestUsers.OWNER).delete("/meetings/" + meetingId).then().statusCode(204);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.recording WHERE meeting_id = ?", meetingId)).isZero();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
        wm.verify(postRequestedFor(urlPathEqualTo("/internal/recordings/process"))));
  }

  @Test
  void guestIsForbidden() {
    as(TestUsers.GUEST).post("/meetings/" + meetingId + "/startRecording").then().statusCode(403);
  }

  @Test
  void userWithoutRecordingAttributeIsForbidden() {
    Stubs.activeUser(wm, TestUsers.OUTSIDER);
    as(TestUsers.OUTSIDER).post("/meetings/" + meetingId + "/startRecording")
        .then().statusCode(403);
  }

  @Test
  void nonOwnerParticipantIsForbidden() {
    as(TestUsers.MEMBER).header("queue-id", UUID.randomUUID().toString()).body(Map.of("audioStreamEnabled", false, "videoStreamEnabled", false)).post("/meetings/" + meetingId + "/join").then().statusCode(200);
    as(TestUsers.MEMBER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(403);
  }

  @Test
  void nonParticipantIsNotFound() {
    as(TestUsers.MEMBER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(404);
  }

  @Test
  void startWithoutBodyIsAccepted() {
    as(TestUsers.OWNER).post("/meetings/" + meetingId + "/startRecording").then().statusCode(204);
  }

  @Test
  void openApiDocumentsTheRecordingEndpoints() {
    given().queryParam("format", "json").get("/q/openapi").then().statusCode(200)
        .body("paths", org.hamcrest.Matchers.hasKey("/meetings/{meetingId}/startRecording"))
        .body("paths", org.hamcrest.Matchers.hasKey("/meetings/{meetingId}/stopRecording"));
  }
}
