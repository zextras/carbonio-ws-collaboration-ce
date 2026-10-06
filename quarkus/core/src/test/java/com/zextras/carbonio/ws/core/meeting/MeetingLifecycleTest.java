// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import com.zextras.carbonio.ws.core.events.RabbitSessions;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
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
class MeetingLifecycleTest {

  @Inject DataSource ds;
  @Inject StoppingProbe stoppingProbe;
  @Inject RabbitSessions sessions;

  UUID roomId;

  @BeforeEach
  void setUp() {
    var wm = WireMockResource.server();
    wm.resetAll();
    Stubs.janus(wm);
    Stubs.activeUser(wm, TestUsers.OWNER);
    Stubs.activeUser(wm, TestUsers.MEMBER);
    Stubs.activeUser(wm, TestUsers.OUTSIDER);
    Seed.clean(ds);
    stoppingProbe.seen.clear();
    roomId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true, TestUsers.MEMBER, false));
  }

  static RequestSpecification as(UUID user) {
    return given().cookie("ZM_AUTH_TOKEN", TestUsers.token(user)).contentType(ContentType.JSON);
  }

  String createMeeting(String name) {
    return as(TestUsers.OWNER)
        .body(Map.of("name", name, "meetingType", "permanent", "roomId", roomId.toString()))
        .post("/meetings")
        .then().statusCode(200)
        .body("active", equalTo(false))
        .body("meetingType", equalTo("permanent"))
        .extract().path("id");
  }

  @Test
  void fullLifecycle() {
    List<String> memberEvents = new CopyOnWriteArrayList<>();
    sessions.setup(TestUsers.MEMBER.toString(), UUID.randomUUID().toString(), memberEvents::add);

    String id = createMeeting("weekly");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start")
        .then().statusCode(200).body("active", equalTo(true)).body("startedAt", notNullValue());
    as(TestUsers.MEMBER).post("/meetings/" + id + "/join")
        .then().statusCode(200).body("status", equalTo("ACCEPTED"));
    as(TestUsers.MEMBER).get("/meetings/" + id)
        .then().statusCode(200)
        .body("participants", hasSize(1))
        .body("participants[0].userId", equalTo(TestUsers.MEMBER.toString()))
        .body("$", not(hasKey("extras")))
        .body("$", not(hasKey("probe")));
    as(TestUsers.OWNER).post("/meetings/" + id + "/stop")
        .then().statusCode(200).body("active", equalTo(false)).body("startedAt", nullValue())
        .body("participants", hasSize(0));

    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.videoserver_meeting")).isZero();
    await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(String.join("\n", memberEvents))
        .contains("\"type\":\"MeetingCreated\"")
        .contains("\"type\":\"MeetingStarted\"")
        .contains("\"type\":\"MeetingParticipantJoined\"")
        .contains("\"type\":\"MeetingStopped\""));
  }

  @Test
  void secondMeetingInTheSameRoomConflicts() {
    createMeeting("first");
    as(TestUsers.OWNER)
        .body(Map.of("name", "second", "meetingType", "permanent", "roomId", roomId.toString()))
        .post("/meetings").then().statusCode(409);
  }

  @Test
  void unknownMeetingTypeIsBadRequest() {
    as(TestUsers.OWNER)
        .body(Map.of("name", "x", "meetingType", "weird", "roomId", roomId.toString()))
        .post("/meetings").then().statusCode(400);
  }

  @Test
  void outsiderCannotCreateOrRead() {
    as(TestUsers.OUTSIDER)
        .body(Map.of("name", "x", "meetingType", "permanent", "roomId", roomId.toString()))
        .post("/meetings").then().statusCode(403);
    String id = createMeeting("private");
    as(TestUsers.OUTSIDER).get("/meetings/" + id).then().statusCode(403);
  }

  @Test
  void unknownMeetingIsNotFound() {
    as(TestUsers.OWNER).get("/meetings/" + UUID.randomUUID()).then().statusCode(404);
  }

  @Test
  void joiningAnInactiveMeetingIsBadRequest() {
    String id = createMeeting("idle");
    as(TestUsers.MEMBER).post("/meetings/" + id + "/join").then().statusCode(400);
  }

  @Test
  void joinIsIdempotent() {
    String id = createMeeting("twice");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.MEMBER).post("/meetings/" + id + "/join").then().statusCode(200);
    as(TestUsers.MEMBER).post("/meetings/" + id + "/join").then().statusCode(200);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.participant WHERE meeting_id = ?", id)).isEqualTo(1);
  }

  @Test
  void leavingWhenNotParticipantIsNotFound() {
    String id = createMeeting("empty");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.MEMBER).post("/meetings/" + id + "/leave").then().statusCode(404);
  }

  @Test
  void lastParticipantLeavingStopsTheMeeting() {
    String id = createMeeting("solo");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.OWNER).post("/meetings/" + id + "/join").then().statusCode(200);
    as(TestUsers.OWNER).post("/meetings/" + id + "/leave").then().statusCode(204);

    as(TestUsers.OWNER).get("/meetings/" + id).then().statusCode(200).body("active", equalTo(false));
    assertThat(stoppingProbe.seen).hasSize(1);
  }

  @Test
  void meetingStoppingFiresBeforeVideoServerStop() {
    String id = createMeeting("ordered");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.OWNER).post("/meetings/" + id + "/stop").then().statusCode(200);

    assertThat(stoppingProbe.seen).singleElement().satisfies(seen -> {
      assertThat(seen.event().meetingId()).isEqualTo(UUID.fromString(id));
      assertThat(seen.event().userId()).isEqualTo(TestUsers.OWNER);
      assertThat(seen.videoServerRowPresent()).isTrue();
    });
  }

  @Test
  void deleteFiresMeetingStoppingAndRemovesTheMeeting() {
    String id = createMeeting("gone");
    as(TestUsers.OWNER).post("/meetings/" + id + "/start").then().statusCode(200);
    as(TestUsers.OWNER).delete("/meetings/" + id).then().statusCode(204);

    as(TestUsers.OWNER).get("/meetings/" + id).then().statusCode(404);
    assertThat(stoppingProbe.seen).hasSize(1);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.room WHERE id = ? AND meeting_id IS NULL", roomId.toString()))
        .isEqualTo(1);
  }

  @Test
  void enricherFieldsAreTopLevel() {
    String id = createMeeting("enrich-me");
    as(TestUsers.OWNER).get("/meetings/" + id)
        .then().statusCode(200).body("probe", equalTo("enriched-1"));
  }

  @Test
  void anonymousIsUnauthorized() {
    given().get("/meetings/" + UUID.randomUUID()).then().statusCode(401);
  }
}
