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
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.PostgreSQLContainer;

public class DatabaseExtension implements BeforeAllCallback, AfterEachCallback {

  private final static Namespace EXTENSION_NAMESPACE  = Namespace.create(DatabaseExtension.class);
  private final static String    FLYWAY_STORE_ENTRY   = "flyway";
  private final static String    DATABASE_STORE_ENTRY = "database";

  private final static String DATABASE_USER     = "user";
  private final static String DATABASE_PASSWORD = "password";
  private final static String DATABASE_DRIVER   = "org.postgresql.Driver";

  @Override
  public void beforeAll(ExtensionContext context) {
    PostgreSQLContainer<?> postgreDatabase = context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(
      DATABASE_STORE_ENTRY,
      (key) -> {
        ChatsLogger.debug("Starting test DB...");
        PostgreSQLContainer<?> database = new CloseablePostgreSQLContainer<>("postgres:14")
          .withDatabaseName("chats_it")
          .withUsername(DATABASE_USER)
          .withPassword(DATABASE_PASSWORD);
        database.start();
        InMemoryConfigStore.set(ConfigName.DATABASE_JDBC_URL, database.getJdbcUrl());
        InMemoryConfigStore.set(ConfigName.DATABASE_JDBC_DRIVER, DATABASE_DRIVER);
        InMemoryConfigStore.set(ConfigName.DATABASE_USERNAME, DATABASE_USER);
        InMemoryConfigStore.set(ConfigName.DATABASE_PASSWORD, DATABASE_PASSWORD);
        return database;
      },
      PostgreSQLContainer.class
    );

    context.getRoot().getStore(EXTENSION_NAMESPACE).getOrComputeIfAbsent(
      FLYWAY_STORE_ENTRY,
      (key) -> {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(postgreDatabase.getJdbcUrl());
        config.setUsername(DATABASE_USER);
        config.setPassword(DATABASE_PASSWORD);
        config.addDataSourceProperty("driverClassName", DATABASE_DRIVER);

        ChatsLogger.debug("Migrating test DB...");
        Flyway flyway = Flyway.configure()
          .locations("classpath:migration")
          .schemas("chats")
          .dataSource(new HikariDataSource(config))
          .load();
        flyway.migrate();
        return flyway;
      },
      Flyway.class
    );
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    Optional.ofNullable(extensionContext.getRoot().getStore(EXTENSION_NAMESPACE).get(FLYWAY_STORE_ENTRY))
      .map(objectFlyway -> (Flyway) objectFlyway)
      .ifPresent(flyway -> {
        ChatsLogger.debug("Cleaning up test DB...");
        flyway.clean();
        flyway.migrate();
      });
  }
}
