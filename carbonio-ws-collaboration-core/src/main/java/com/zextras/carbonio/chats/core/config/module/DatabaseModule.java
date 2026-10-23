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
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.infrastructure.database.DatabaseInfoService;
import com.zextras.carbonio.chats.core.infrastructure.database.impl.EbeanDatabaseInfoService;
import com.zextras.carbonio.chats.core.migration.V2_0_1__migrate_mongoose_data;
import com.zextras.carbonio.chats.core.migration.scripts.V1_4_2__backfill_attachment_ids;
import io.ebean.Database;
import io.ebean.annotation.Platform;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.migration.JavaMigration;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;

public class DatabaseModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DatabaseInfoService.class).to(EbeanDatabaseInfoService.class);
  }

  protected String migrationsLocation() {
    return "classpath:migration/ce";
  }

  protected List<JavaMigration> javaMigrations(MessageDispatcherCredentials credentials) {
    return List.of(
        new V1_4_2__backfill_attachment_ids(credentials),
        new V2_0_1__migrate_mongoose_data(credentials, randomRunKey()));
  }

  protected static byte[] randomRunKey() {
    byte[] key = new byte[32];
    new SecureRandom().nextBytes(key);
    return key;
  }

  @Singleton
  @Provides
  private HikariDataSource getHikariDataSource(AppConfig appConfig) {
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
    config.addDataSourceProperty("ApplicationName", "ws-collaboration");
    return new HikariDataSource(config);
  }

  @Singleton
  @Provides
  private Database getDatabase(HikariDataSource dataSource, Clock clock) {
    return Database.builder()
        .dataSource(dataSource)
        .clock(clock)
        .databasePlatformName(Platform.POSTGRES.toString())
        .build();
  }

  @Singleton
  @Provides
  private Flyway getFlyway(HikariDataSource dataSource, MessageDispatcherCredentials credentials) {
    Flyway flyway =
        Flyway.configure()
            .locations(migrationsLocation())
            .schemas("chats")
            .dataSource(dataSource)
            .validateMigrationNaming(true)
            .lockRetryCount(-1)
            // One transaction for all pending migrations: a failed upgrade changes nothing.
            .group(true)
            .javaMigrations(javaMigrations(credentials).toArray(JavaMigration[]::new))
            .load();
    // Session lock on the migration connection: the default xact lock idles on a second connection
    // that the mesh (Envoy tcp_proxy, 1h idle timeout) can drop mid-migration, releasing the lock.
    flyway
        .getConfigurationExtension(PostgreSQLConfigurationExtension.class)
        .setTransactionalLock(false);
    return flyway;
  }
}
