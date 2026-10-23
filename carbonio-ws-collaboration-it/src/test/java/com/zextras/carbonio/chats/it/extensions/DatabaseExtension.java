// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.extensions;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import com.zextras.carbonio.chats.it.tools.CloseablePostgreSQLContainer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.PostgreSQLContainer;

public class DatabaseExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final Namespace EXTENSION_NAMESPACE = Namespace.create(DatabaseExtension.class);
  private static final String FLYWAY_STORE_ENTRY = "flyway";
  private static final String DATABASE_STORE_ENTRY = "database";

  private static final String DATABASE_USER = "user";
  private static final String DATABASE_PASSWORD = "password";
  private static final String DATABASE_DRIVER = "org.postgresql.Driver";

  @Override
  public void beforeAll(ExtensionContext context) {
    PostgreSQLContainer<?> postgreDatabase =
        context
            .getRoot()
            .getStore(EXTENSION_NAMESPACE)
            .getOrComputeIfAbsent(
                DATABASE_STORE_ENTRY,
                (key) -> {
                  ChatsLogger.debug("Starting test DB...");
                  PostgreSQLContainer<?> database =
                      new CloseablePostgreSQLContainer<>("postgres:16")
                          .withDatabaseName("chats_it")
                          .withUsername(DATABASE_USER)
                          .withPassword(DATABASE_PASSWORD);
                  database.start();
                  InMemoryConfigStore.set(ConfigName.DATABASE_JDBC_URL, database.getJdbcUrl());
                  InMemoryConfigStore.set(ConfigName.DATABASE_USERNAME, DATABASE_USER);
                  InMemoryConfigStore.set(ConfigName.DATABASE_PASSWORD, DATABASE_PASSWORD);
                  return database;
                },
                PostgreSQLContainer.class);

    context
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .getOrComputeIfAbsent(
            FLYWAY_STORE_ENTRY,
            (key) -> {
              HikariConfig config = new HikariConfig();
              config.setJdbcUrl(postgreDatabase.getJdbcUrl());
              config.setUsername(DATABASE_USER);
              config.setPassword(DATABASE_PASSWORD);
              config.setDriverClassName(DATABASE_DRIVER);

              HikariDataSource dataSource = new HikariDataSource(config);
              ensureApplicationRoleExists(dataSource);

              ChatsLogger.debug("Migrating test DB...");
              return Flyway.configure()
                  .cleanDisabled(false)
                  .locations("classpath:migration/ce")
                  .schemas("chats")
                  .dataSource(dataSource)
                  .validateMigrationNaming(true)
                  .load();
            },
            Flyway.class);
  }

  /**
   * Creates the application DB role referenced by migrations (e.g. {@code ALTER ROLE
   * "carbonio-ws-collaboration-db" ...} in V2_0_0). The role exists in deployed environments but
   * not in the throwaway Testcontainers database, so it is created here (idempotently) before
   * Flyway runs.
   */
  private void ensureApplicationRoleExists(HikariDataSource dataSource) {
    try (Connection connection = dataSource.getConnection()) {
      connection
          .prepareStatement(
              "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname ="
                  + " 'carbonio-ws-collaboration-db') THEN CREATE ROLE"
                  + " \"carbonio-ws-collaboration-db\"; END IF; END $$;")
          .execute();
    } catch (SQLException e) {
      ChatsLogger.error("Failed to create application role for test DB", e);
    }
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    Optional.ofNullable(
            extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(FLYWAY_STORE_ENTRY))
        .map(objectFlyway -> (Flyway) objectFlyway)
        .ifPresent(
            flyway -> {
              ChatsLogger.debug("Start migration test DB...");
              flyway.migrate();
            });
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    Optional.ofNullable(
            extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(FLYWAY_STORE_ENTRY))
        .map(objectFlyway -> (Flyway) objectFlyway)
        .ifPresent(
            flyway -> {
              ChatsLogger.debug("Cleaning up test DB...");
              flyway.clean();
            });
  }
}
