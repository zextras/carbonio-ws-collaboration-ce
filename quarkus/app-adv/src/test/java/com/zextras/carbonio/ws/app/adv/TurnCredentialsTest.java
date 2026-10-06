// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.app.adv;

import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.serviceUnavailable;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class TurnCredentialsTest {

  static final String CREDS =
      "{\"configured\":true,\"url\":\"turn:turn.test\",\"username\":\"u\",\"credential\":\"c\",\"ttl\":3600}";

  @Inject DataSource ds;
  WireMockServer wm;
  UUID meetingId;

  @BeforeEach
  void setUp() {
    wm = WireMockResource.server();
    wm.resetAll();
    Stubs.activeUser(wm, TestUsers.OWNER);
    Seed.clean(ds);
    UUID roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
    Seed.room(ds, roomId, "group", Map.of(TestUsers.OWNER, true));
    Seed.meeting(ds, meetingId, roomId, true);
  }

  io.restassured.response.ValidatableResponse call() {
    return given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/meetings/" + meetingId + "/turnCredentials").then();
  }

  @Test
  void returnsCredentialsFromTheColocatedInstance() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials"))
        .withQueryParam("userId", com.github.tomakehurst.wiremock.client.WireMock.equalTo(TestUsers.OWNER.toString()))
        .withHeader("service-id", com.github.tomakehurst.wiremock.client.WireMock.equalTo("vs-1"))
        .willReturn(okJson(CREDS)));
    call().statusCode(200).body("url", equalTo("turn:turn.test")).body("ttl", equalTo(3600));
  }

  @Test
  void notConfiguredIsNoContent() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials")).willReturn(okJson("{\"configured\":false}")));
    call().statusCode(204);
  }

  @Test
  void fallsBackToAnyInstance() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials"))
        .withHeader("service-id", com.github.tomakehurst.wiremock.client.WireMock.equalTo("vs-1"))
        .willReturn(serviceUnavailable()));
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials"))
        .withHeader("service-id", absent()).willReturn(okJson(CREDS)));
    call().statusCode(200).body("username", equalTo("u"));
  }

  @Test
  void bothInstancesFailingIsNotFound() {
    Seed.videoServerMeeting(ds, meetingId, "vs-1");
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials"))
        .withHeader("service-id", com.github.tomakehurst.wiremock.client.WireMock.equalTo("vs-1"))
        .willReturn(serviceUnavailable()));
    wm.stubFor(get(urlPathEqualTo("/internal/turn/credentials"))
        .withHeader("service-id", absent()).willReturn(serverError()));
    call().statusCode(404);
  }

  @Test
  void noServerIdReturns404() {
    Seed.videoServerMeeting(ds, meetingId, null);
    call().statusCode(404);
  }

  @Test
  void noVideoServerMeetingReturns404() {
    call().statusCode(404);
  }

  @Test
  void turnPluginIsListed() {
    given().get("/plugins").then().statusCode(200).body("id", hasItem("turn"));
  }
}
