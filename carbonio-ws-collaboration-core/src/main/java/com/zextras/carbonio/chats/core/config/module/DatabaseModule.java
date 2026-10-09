// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.migration.JavaMigrationsProvider;
import java.util.List;
import java.util.Properties;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;

// Replaceable as a whole by a downstream product: only bindings it changes belong here.
public class DatabaseModule extends AbstractModule {

  @Singleton
  @Provides
  private HikariDataSource getHikariDataSource(AppConfig appConfig) {
    HikariConfig config = baseHikariConfig(appConfig);

    Properties properties = new Properties();
    properties.setProperty("sslmode", "disable");
    properties.setProperty("ApplicationName", "ws-collaboration");
    config.setDataSourceProperties(properties);

    return new HikariDataSource(config);
  }

  @Singleton
  @Provides
  private Flyway getFlywayInstance(
      HikariDataSource dataSource, JavaMigrationsProvider javaMigrationsProvider) {
    return buildFlyway(dataSource, "classpath:migration/ce", javaMigrationsProvider.get());
  }

  public static HikariConfig baseHikariConfig(AppConfig appConfig) {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(appConfig.get(String.class, ConfigName.DATABASE_JDBC_URL).orElseThrow());
    config.setPoolName("ws-collaboration-db-pool");
    config.setUsername(appConfig.get(String.class, ConfigName.DATABASE_USERNAME).orElse("admin"));
    config.setPassword(appConfig.get(String.class, ConfigName.DATABASE_PASSWORD).orElse("admin"));
    config.setIdleTimeout(
        appConfig.get(Integer.class, ConfigName.HIKARI_IDLE_TIMEOUT).orElse(10000));
    config.setMinimumIdle(appConfig.get(Integer.class, ConfigName.HIKARI_MIN_POOL_SIZE).orElse(10));
    config.setMaximumPoolSize(
        appConfig.get(Integer.class, ConfigName.HIKARI_MAX_POOL_SIZE).orElse(10));
    config.setLeakDetectionThreshold(
        appConfig.get(Integer.class, ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD).orElse(5000));
    config.setMaxLifetime(
        appConfig.get(Integer.class, ConfigName.HIKARI_MAX_LIFETIME).orElse(600000));
    return config;
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
