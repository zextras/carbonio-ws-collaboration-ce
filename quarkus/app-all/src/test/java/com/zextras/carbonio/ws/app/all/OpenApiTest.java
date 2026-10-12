// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.app.all;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;

import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Every plugin assembled at build time (A) is part of the generated OpenAPI document. */
@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class OpenApiTest {

  @Test
  void allPluginAndCorePathsAreDocumented() {
    given().queryParam("format", "json").get("/q/openapi").then().statusCode(200)
        .body("paths", hasKey("/meetings/{meetingId}/turnCredentials"))
        .body("paths", hasKey("/meetings/{meetingId}/startRecording"))
        .body("paths", hasKey("/meetings/{meetingId}/stopRecording"))
        .body("paths", hasKey("/meetings/{meetingId}/start"));
  }
}
