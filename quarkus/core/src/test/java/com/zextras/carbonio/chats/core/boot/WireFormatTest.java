// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.boot;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import com.zextras.carbonio.ws.testsupport.Seed;
import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The CE UI compares the content type strictly and negotiates the API version. */
@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class WireFormatTest {

  private static final String VERSION = "X-WSC-API-VERSION";

  @Inject DataSource dataSource;

  @BeforeEach
  void setUp() {
    Seed.clean(dataSource);
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
  }

  private static RequestSpecification owner() {
    return given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER));
  }

  private static String oldest() {
    List<String> versions = VersionProvider.getSupportedVersions();
    return versions.get(versions.size() - 1);
  }

  @Test
  void jsonResponsesCarryExactlyApplicationJson() {
    owner().get("/rooms").then().statusCode(200).header("Content-Type", equalTo("application/json"));
    owner()
        .get("/users/capabilities")
        .then()
        .statusCode(200)
        .header("Content-Type", equalTo("application/json"));
  }

  @Test
  void withoutVersionHeaderTheOldestVersionIsAnswered() {
    owner().get("/rooms").then().statusCode(200).header(VERSION, equalTo(oldest()));
  }

  @Test
  void requestedVersionIsEchoed() {
    String latest = VersionProvider.getVersion();
    owner().header(VERSION, latest).get("/rooms").then().statusCode(200).header(VERSION, equalTo(latest));
  }

  @Test
  void newerVersionIsUnprocessableAndInvalidIsBadRequest() {
    owner().header(VERSION, "999.0.0").get("/rooms").then().statusCode(422);
    owner().header(VERSION, "not-a-version").get("/rooms").then().statusCode(400);
  }

  @Test
  void errorsCarryTheCurrentVersion() {
    given().get("/rooms").then().statusCode(401).header(VERSION, equalTo(VersionProvider.getVersion()));
  }
}
