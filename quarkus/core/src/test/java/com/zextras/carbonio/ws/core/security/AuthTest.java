// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.security;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class AuthTest {

  @BeforeEach
  void reset() {
    WireMockResource.server().resetAll();
  }

  @Test
  void noCookieIsUnauthorized() {
    given().get("/test/whoami").then().statusCode(401);
  }

  @Test
  void validTokenAuthenticates() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/test/whoami")
        .then().statusCode(200)
        .body("id", equalTo(TestUsers.OWNER.toString()))
        .body("type", equalTo("INTERNAL"));
  }

  @Test
  void queueIdHeaderIsExposed() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    UUID queueId = UUID.randomUUID();
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .header("queue-id", queueId.toString())
        .get("/test/whoami")
        .then().statusCode(200).body("queueId", equalTo(queueId.toString()));
  }

  @Test
  void invalidQueueIdIsUnauthorized() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .header("queue-id", "not-a-uuid")
        .get("/test/whoami")
        .then().statusCode(401);
  }

  @Test
  void tokenRejectedByUserManagementIsUnauthorized() {
    WireMockResource.server()
        .stubFor(get(urlPathEqualTo("/internal/users/myself")).willReturn(unauthorized()));
    given().cookie("ZM_AUTH_TOKEN", "bad").get("/test/whoami").then().statusCode(401);
  }

  @Test
  void inactiveUserIsForbidden() {
    Stubs.myself(WireMockResource.server(), TestUsers.OWNER, "INTERNAL", "locked", true, Map.of());
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/test/whoami")
        .then().statusCode(403).body(equalTo("User is not active"));
  }

  @Test
  void userWithoutWscFeatureIsForbidden() {
    Stubs.myself(WireMockResource.server(), TestUsers.OWNER, "INTERNAL", "active", false, Map.of());
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/test/whoami")
        .then().statusCode(403).body(equalTo("WSC feature not enabled for user"));
  }

  @Test
  void userManagementFailureIsServerError() {
    WireMockResource.server()
        .stubFor(get(urlPathEqualTo("/internal/users/myself")).willReturn(serverError()));
    given().cookie("ZM_AUTH_TOKEN", "any").get("/test/whoami").then().statusCode(500);
  }

  @Test
  void healthIsAnonymous() {
    given().get("/q/health/live").then().statusCode(200);
  }
}
