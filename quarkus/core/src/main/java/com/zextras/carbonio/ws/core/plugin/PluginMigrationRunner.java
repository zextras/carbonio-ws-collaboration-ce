// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.plugin;

import com.zextras.carbonio.ws.plugin.api.PluginMigrations;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.jboss.logging.Logger;

/** Core is migrated by quarkus-flyway before StartupEvent, so plugin FKs to core tables resolve. */
@ApplicationScoped
public class PluginMigrationRunner {

  private static final Logger LOG = Logger.getLogger(PluginMigrationRunner.class);

  @Inject Instance<PluginMigrations> migrations;
  @Inject DataSource dataSource;

  void onStart(@Observes @Priority(20) StartupEvent event) {
    migrations.forEach(this::migrate);
  }

  void migrate(PluginMigrations plugin) {
    if (!plugin.pluginId().matches("[a-z0-9_]+")) {
      throw new IllegalStateException("Invalid plugin id for migrations: " + plugin.pluginId());
    }
    var result =
        Flyway.configure(plugin.getClass().getClassLoader())
            .dataSource(dataSource)
            .schemas("chats")
            .table("flyway_history_" + plugin.pluginId())
            .locations("classpath:" + plugin.location())
            // the chats schema already holds core's tables and history
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .load()
            .migrate();
    LOG.infof("Plugin %s: %d migration(s) applied", plugin.pluginId(), result.migrationsExecuted);
  }
}
