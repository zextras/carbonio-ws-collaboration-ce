// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.migration.JavaMigrationsProvider;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;

// Replaceable as a whole by a downstream product: only bindings it changes belong here.
public class DatabaseModule extends AbstractModule {

  @Singleton
  @Provides
  private Flyway getFlywayInstance(
      HikariDataSource dataSource, JavaMigrationsProvider javaMigrationsProvider) {
    return buildFlyway(dataSource, "classpath:migration/ce", javaMigrationsProvider.get());
  }

  public static Flyway buildFlyway(
      HikariDataSource dataSource, String location, List<JavaMigration> javaMigrations) {
    return Flyway.configure()
        .locations(location)
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .javaMigrations(javaMigrations.toArray(JavaMigration[]::new))
        .load();
  }
}
