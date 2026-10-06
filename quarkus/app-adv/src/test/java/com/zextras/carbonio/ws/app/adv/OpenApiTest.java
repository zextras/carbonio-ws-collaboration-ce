// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.app.adv;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** A build-time (A) plugin's JAX-RS resource is part of the generated OpenAPI document. */
@QuarkusTest
@QuarkusTestResource(WireMockResource.class)
class OpenApiTest {

  @Test
  void pluginAndCorePathsAreDocumented() {
    given().queryParam("format", "json").get("/q/openapi").then().statusCode(200)
        .body("paths", hasKey("/meetings/{meetingId}/turnCredentials"))
        .body("paths", hasKey("/meetings/{meetingId}/start"))
        .body("paths", not(hasKey("/meetings/{meetingId}/startRecording")));
  }
}
