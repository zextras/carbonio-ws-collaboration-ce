// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.lifecycle;

import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.migration.JavaMigrationsProvider;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import com.zextras.carbonio.chats.openapi.versioning.VersionProvider;
import io.quarkus.runtime.StartupEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;

/**
 * What CE's {@code Boot} did besides starting Jetty: core Flyway migrations (SQL + the Java
 * backfill, which needs injected credentials and therefore cannot be discovered by Quarkus Flyway),
 * then the videoserver event listener. Plugin migrations run after core's (priority 20).
 */
@ApplicationScoped
public class CoreStartup {

  public static final String CORE_MIGRATIONS = "classpath:db/migration/core";

  @Inject DataSource dataSource;
  @Inject JavaMigrationsProvider javaMigrationsProvider;
  @Inject VideoServerEventListener videoServerEventListener;

  void migrate(@Observes @Priority(10) StartupEvent event) {
    ChatsLogger.info("Application latest version: " + VersionProvider.getVersion());
    ChatsLogger.info("Supported versions: " + VersionProvider.getSupportedVersions());
    Flyway.configure()
        .locations(CORE_MIGRATIONS)
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .javaMigrations(javaMigrationsProvider.get().toArray(JavaMigration[]::new))
        .load()
        .migrate();
  }

  void startListeners(@Observes @Priority(30) StartupEvent event) {
    videoServerEventListener.start();
  }
}
