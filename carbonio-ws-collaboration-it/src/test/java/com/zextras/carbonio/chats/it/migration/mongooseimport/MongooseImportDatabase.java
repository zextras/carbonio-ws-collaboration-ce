// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Loads the import fixtures: {@code mongoose.sql.gz}, an anonymised MongooseIM source, and {@code
 * wsc.sql.gz}, a {@code pg_dump --no-owner} of a WSC database migrated with {@code migration/ce} up
 * to Flyway 1.4.2 (V1_4_2 skipped, no dispatcher credentials) that holds the anonymised rows of a
 * release WSC database and has {@code pg_trgm} installed in {@code public}.
 */
public final class MongooseImportDatabase {

  public static final String SOURCE_DATABASE = "mongoose";
  private static final String TEMPLATE_DATABASE = "wsc_template";
  private static final String USER = "user";
  private static final String PASSWORD = "password";
  private static final String APPLICATION_ROLE = "carbonio-ws-collaboration-db";
  private static final String DUMP_FOLDER = "mongoose-import";

  private static MongooseImportDatabase instance;

  private final PostgreSQLContainer<?> container;
  private final AtomicInteger sequence = new AtomicInteger();

  private MongooseImportDatabase() {
    container =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("postgres_it")
            .withUsername(USER)
            .withPassword(PASSWORD);
    container.start();
    execute("postgres_it", "CREATE ROLE \"" + APPLICATION_ROLE + "\"");
    createDatabase(SOURCE_DATABASE);
    createDatabase(TEMPLATE_DATABASE);
    load("mongoose.sql.gz", SOURCE_DATABASE);
    load("wsc.sql.gz", TEMPLATE_DATABASE);
  }

  public static synchronized MongooseImportDatabase get() {
    if (instance == null) {
      instance = new MongooseImportDatabase();
    }
    return instance;
  }

  public String newTargetDatabase() {
    String name = "wsc_" + sequence.incrementAndGet();
    execute("postgres_it", "CREATE DATABASE " + name + " TEMPLATE " + TEMPLATE_DATABASE);
    return name;
  }

  public void resetApplicationRole() {
    execute("postgres_it", "ALTER ROLE \"" + APPLICATION_ROLE + "\" RESET ALL");
  }

  public String newEmptyDatabase() {
    String name = "empty_" + sequence.incrementAndGet();
    createDatabase(name);
    return name;
  }

  public String newSourceDatabase() {
    String name = "mongoose_" + sequence.incrementAndGet();
    execute("postgres_it", "CREATE DATABASE " + name + " TEMPLATE " + SOURCE_DATABASE);
    return name;
  }

  public MessageDispatcherCredentials sourceCredentials() {
    return credentialsFor(SOURCE_DATABASE);
  }

  public MessageDispatcherCredentials credentialsFor(String database) {
    return new MessageDispatcherCredentials(
        container.getHost(), container.getFirstMappedPort(), database, USER, PASSWORD);
  }

  public String jdbcUrl(String database) {
    return "jdbc:postgresql://%s:%d/%s"
        .formatted(container.getHost(), container.getFirstMappedPort(), database);
  }

  public String user() {
    return USER;
  }

  public String password() {
    return PASSWORD;
  }

  public Connection connect(String database) throws SQLException {
    return DriverManager.getConnection(jdbcUrl(database), USER, PASSWORD);
  }

  public void execute(String database, String... statements) {
    try (Connection connection = connect(database);
        Statement statement = connection.createStatement()) {
      for (String sql : statements) {
        statement.execute(sql);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Statement failed on " + database, e);
    }
  }

  private void createDatabase(String name) {
    execute("postgres_it", "CREATE DATABASE " + name);
  }

  private void load(String resource, String database) {
    try (Connection connection = connect(database);
        Statement statement = connection.createStatement();
        BufferedReader reader = dumpReader(resource)) {
      CopyManager copyManager = connection.unwrap(PGConnection.class).getCopyAPI();
      StringBuilder sql = new StringBuilder();
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("COPY ") && line.endsWith(" FROM stdin;")) {
          copyManager.copyIn(line.substring(0, line.length() - 1), copyData(reader));
        } else if (!sql.isEmpty()
            || !(line.isBlank() || line.startsWith("--") || line.startsWith("\\"))) {
          sql.append(line).append('\n');
          if (line.endsWith(";")) {
            statement.execute(sql.toString());
            sql.setLength(0);
          }
        }
      }
    } catch (IOException | SQLException e) {
      throw new IllegalStateException("Loading " + resource + " into " + database + " failed", e);
    }
  }

  private static BufferedReader dumpReader(String resource) throws IOException {
    InputStream stream =
        MongooseImportDatabase.class
            .getClassLoader()
            .getResourceAsStream(DUMP_FOLDER + "/" + resource);
    if (stream == null) {
      throw new IllegalStateException("Missing test resource " + resource);
    }
    return new BufferedReader(
        new InputStreamReader(new GZIPInputStream(stream), StandardCharsets.UTF_8));
  }

  private static Reader copyData(BufferedReader reader) throws IOException {
    StringBuilder data = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null && !line.equals("\\.")) {
      data.append(line).append('\n');
    }
    return new StringReader(data.toString());
  }
}
