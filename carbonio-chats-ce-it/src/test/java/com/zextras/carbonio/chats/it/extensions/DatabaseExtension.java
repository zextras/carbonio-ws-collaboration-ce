package com.zextras.carbonio.chats.it.extensions;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.it.Utils.TimeUtils;
import com.zextras.carbonio.chats.it.config.InMemoryConfigStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class DatabaseExtension implements AfterAllCallback, BeforeAllCallback, AfterEachCallback {

  private final static Namespace EXTENSION_NAMESPACE  = Namespace.create(DatabaseExtension.class);
  private final static String    FLYWAY_STORE_ENTRY   = "flyway";
  private final static String    DATABASE_STORE_ENTRY = "database";

  private final static String DATABASE_USER     = "user";
  private final static String DATABASE_PASSWORD = "password";
  private final static String DATABASE_DRIVER   = "org.postgresql.Driver";

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Instant startTime = Instant.now();
    ChatsLogger.debug("Starting test DB...");
    PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:14")
      .withDatabaseName("chats_it")
      .withUsername(DATABASE_USER)
      .withPassword(DATABASE_PASSWORD);
    database.start();

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(database.getJdbcUrl());
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
    context.getStore(EXTENSION_NAMESPACE).put(FLYWAY_STORE_ENTRY, flyway);
    context.getStore(EXTENSION_NAMESPACE).put(DATABASE_STORE_ENTRY, database);
    InMemoryConfigStore.set("DATABASE_JDBC_URL", database.getJdbcUrl());
    InMemoryConfigStore.set("DATASOURCE_DRIVER", DATABASE_DRIVER);
    InMemoryConfigStore.set("DATABASE_USERNAME", DATABASE_USER);
    InMemoryConfigStore.set("DATABASE_PASSWORD", DATABASE_PASSWORD);
    ChatsLogger.debug(
      "Database extension startup took " + TimeUtils.durationToString(Duration.between(startTime, Instant.now())));
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) throws Exception {
    Optional.ofNullable(extensionContext.getStore(EXTENSION_NAMESPACE).get(FLYWAY_STORE_ENTRY))
      .map(objectFlyway -> (Flyway) objectFlyway)
      .ifPresent(flyway -> {
        ChatsLogger.debug("Cleaning up test DB...");
        flyway.clean();
        flyway.migrate();
      });
  }

  @Override
  public void afterAll(ExtensionContext context) throws Exception {
    ChatsLogger.debug("Closing test db...");
    Optional.ofNullable(context.getStore(EXTENSION_NAMESPACE).get(DATABASE_STORE_ENTRY))
      .map(objectDatabase -> (PostgreSQLContainer<?>) objectDatabase)
      .ifPresent(GenericContainer::stop);
    ChatsLogger.debug("Test DB closed");
  }
}
