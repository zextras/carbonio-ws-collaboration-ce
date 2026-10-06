// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.app.adv;

import static io.restassured.RestAssured.given;

import com.zextras.carbonio.ws.testsupport.Stubs;
import com.zextras.carbonio.ws.testsupport.TestUsers;
import com.zextras.carbonio.ws.testsupport.WireMockResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(TurnUnavailableTest.NoTurn.class)
@QuarkusTestResource(WireMockResource.class)
class TurnUnavailableTest {

  public static class NoTurn implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("ws.discovery.available-services", "none");
    }
  }

  @Test
  void unavailableTurnServiceIsNoContent() {
    Stubs.activeUser(WireMockResource.server(), TestUsers.OWNER);
    given().cookie("ZM_AUTH_TOKEN", TestUsers.token(TestUsers.OWNER))
        .get("/meetings/" + UUID.randomUUID() + "/turnCredentials")
        .then().statusCode(204);
  }
}
