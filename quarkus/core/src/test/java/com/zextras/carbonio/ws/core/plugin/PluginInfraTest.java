// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;

import com.zextras.carbonio.ws.plugin.api.ServiceDiscovery;
import com.zextras.carbonio.ws.testsupport.Seed;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PluginInfraTest {

  @Inject DataSource ds;
  @Inject ServiceDiscovery discovery;

  @Test
  void pluginsEndpointListsLoadedPlugins() {
    given().get("/plugins").then().statusCode(200).body("id", hasItem("probe"));
  }

  @Test
  void pluginMigrationRunsOnNonEmptySchema() {
    assertThat(Seed.count(ds, "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'chats' AND table_name = 'probe'"))
        .isEqualTo(1);
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.flyway_history_probe WHERE version = '1' AND success"))
        .isEqualTo(1);
    // core history is untouched by plugin migrations
    assertThat(Seed.count(ds, "SELECT count(*) FROM chats.flyway_schema_history WHERE script LIKE '%probe%'"))
        .isZero();
  }

  @Test
  void discoveryUsesTheConfiguredListWhenConsulIsDisabled() {
    assertThat(discovery.isAvailable("carbonio-turn-server-auth")).isTrue();
    assertThat(discovery.isAvailable("carbonio-something-else")).isFalse();
  }
}
