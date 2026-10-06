// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.boot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** P1 smoke test: the ported CE core boots on Quarkus and serves the generated CE API. */
@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class CoreBootTest {

  @Inject DataSource dataSource;

  @BeforeEach
  void clean() {
    Seed.clean(dataSource);
  }

  @Test
  void liveHealthIsAnonymous() {
    given().get("/health/live").then().statusCode(204);
  }

  @Test
  void authenticatedUserWithoutRoomsGetsEmptyList() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    given()
        .cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/rooms")
        .then()
        .statusCode(200)
        .contentType(startsWith("application/json"))
        .body(equalTo("[]"));
  }

  @Test
  void roomsRequireAuthentication() {
    given().get("/rooms").then().statusCode(401);
  }
}
