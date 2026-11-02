// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("Every stored time is a timestamptz written by the database clock")
class TimestampSchemaIT {

  @Container
  private static final PostgreSQLContainer<?> db =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("carbonio-ws-collaboration-db")
          .withUsername("carbonio-ws-collaboration-db")
          .withPassword("password");

  private static HikariDataSource dataSource;

  @BeforeAll
  static void migrate() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(db.getJdbcUrl());
    config.setUsername(db.getUsername());
    config.setPassword(db.getPassword());
    dataSource = new HikariDataSource(config);
    Flyway.configure()
        .locations("classpath:migration/ce")
        .placeholders(Map.of("jvm_zone", ZoneId.systemDefault().getId()))
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .load()
        .migrate();
  }

  @AfterAll
  static void close() {
    dataSource.close();
  }

  private static List<String> query(String sql) throws SQLException {
    List<String> rows = new ArrayList<>();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      while (resultSet.next()) {
        rows.add(resultSet.getString(1));
      }
    }
    return rows;
  }

  @Test
  @DisplayName("The chats schema has no timestamp without time zone column")
  void noNaiveTimestampColumns() throws SQLException {
    assertEquals(
        List.of(),
        query(
            "SELECT table_name || '.' || column_name FROM information_schema.columns"
                + " WHERE table_schema = 'chats' AND table_name <> 'flyway_schema_history'"
                + " AND data_type = 'timestamp without time zone'"
                + " ORDER BY 1"));
  }

  @Test
  @DisplayName("Created, updated and joined columns default to clock_timestamp()")
  void creationColumnsDefaultToTheDatabaseClock() throws SQLException {
    List<String> expected =
        List.of(
            "room.created_at",
            "room.updated_at",
            "subscription.joined_at",
            "subscription.created_at",
            "subscription.updated_at",
            "room_user_settings.created_at",
            "room_user_settings.updated_at",
            "file_metadata.created_at",
            "file_metadata.updated_at",
            "meeting.created_at",
            "participant.created_at",
            "participant.updated_at",
            "chats_user.created_at",
            "chats_user.updated_at",
            "message.created_at",
            "message.updated_at",
            "message_reaction.created_at",
            "message_pin.pinned_at",
            "system_event.created_at",
            "user_contact.created_at",
            "message_send_dedup.created_at");
    List<String> withDefault =
        query(
            "SELECT table_name || '.' || column_name FROM information_schema.columns"
                + " WHERE table_schema = 'chats' AND data_type = 'timestamp with time zone'"
                + " AND column_default LIKE '%clock_timestamp()%'");
    for (String column : expected) {
      assertTrue(withDefault.contains(column), column + " has no clock_timestamp() default");
    }
  }
}
