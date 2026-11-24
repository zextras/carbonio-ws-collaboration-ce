// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.module.DatabaseModule;
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

  private static final List<String> COLUMNS_KEPT_FOR_ROLLBACK =
      List.of(
          "chats_user.created_at_v1",
          "chats_user.updated_at_v1",
          "file_metadata.created_at_v1",
          "file_metadata.updated_at_v1",
          "meeting.created_at_v1",
          "meeting.expiration_v1",
          "meeting.started_at_v1",
          "participant.created_at_v1",
          "participant.hand_raised_at_v1",
          "participant.updated_at_v1",
          "room.created_at_v1",
          "room.picture_updated_at_v1",
          "room.updated_at_v1",
          "room_user_settings.cleared_at_v1",
          "room_user_settings.created_at_v1",
          "room_user_settings.muted_until_v1",
          "room_user_settings.updated_at_v1",
          "subscription.created_at_v1",
          "subscription.joined_at_v1",
          "subscription.updated_at_v1");

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
        .placeholders(Map.of("jvm_zone", DatabaseModule.migrationZone(ZoneId.systemDefault())))
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
  @DisplayName("The only timestamp without time zone columns are the 1.x ones kept as _v1")
  void naiveTimestampColumnsAreOnlyThe1xOnes() throws SQLException {
    assertEquals(
        COLUMNS_KEPT_FOR_ROLLBACK,
        query(
            "SELECT table_name || '.' || column_name FROM information_schema.columns"
                + " WHERE table_schema = 'chats' AND table_name <> 'flyway_schema_history'"
                + " AND data_type = 'timestamp without time zone'"
                + " ORDER BY table_name || '.' || column_name COLLATE \"C\""));
  }

  @Test
  @DisplayName("The 1.x columns kept as _v1 accept rows written by 2.x: nullable, no default")
  void columnsKeptForRollbackAreNullableWithoutDefault() throws SQLException {
    assertEquals(
        List.of(),
        query(
            "SELECT table_name || '.' || column_name FROM information_schema.columns"
                + " WHERE table_schema = 'chats' AND column_name LIKE '%\\_v1'"
                + " AND (is_nullable = 'NO' OR column_default IS NOT NULL)"));
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
            "message_reaction.updated_at",
            "message_pin.pinned_at",
            "system_event.created_at",
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

  @Test
  @DisplayName("The roster table of the 1.x line does not exist: nothing reads it")
  void userContactTableIsGone() throws SQLException {
    assertEquals(
        List.of("true"), query("SELECT (to_regclass('chats.user_contact') IS NULL)::text"));
  }
}
