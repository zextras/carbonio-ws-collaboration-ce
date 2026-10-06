// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.app.ce;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasSize;

import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class PluginAbsentTest {

  @Test
  void ceHasNoPlugins() {
    given().get("/plugins").then().statusCode(200).body("$", hasSize(0));
  }

  @Test
  void pluginEndpointsDoNotExist() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/meetings/" + UUID.randomUUID() + "/turnCredentials").then().statusCode(404);
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .post("/meetings/" + UUID.randomUUID() + "/startRecording").then().statusCode(404);
  }
}
