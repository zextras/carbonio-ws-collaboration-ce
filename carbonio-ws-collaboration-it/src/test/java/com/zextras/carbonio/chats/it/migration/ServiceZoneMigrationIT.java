// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.module.DatabaseModule;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
@DisplayName("The 1.x naive times are converted with the service zone by the real migration")
class ServiceZoneMigrationIT {

  private static final String SUMMER = "2026-07-01 12:00:00";
  private static final String WINTER = "2026-01-15 12:00:00";
  private static final Instant SUMMER_INSTANT = Instant.parse("2026-07-01T10:00:00Z");
  private static final Instant WINTER_INSTANT = Instant.parse("2026-01-15T11:00:00Z");

  private record Check(String table, String where, String column, Instant expected) {}

  @Container
  private static final PostgreSQLContainer<?> db =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("carbonio-ws-collaboration-db")
          .withUsername("carbonio-ws-collaboration-db")
          .withPassword("password");

  private static HikariDataSource dataSource;

  private static Flyway flyway(String target, ZoneId zone) {
    return Flyway.configure()
        .locations("classpath:migration/ce")
        .placeholders(Map.of("jvm_zone", DatabaseModule.migrationZone(zone)))
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .target(target)
        .load();
  }

  @BeforeAll
  static void setUp() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(db.getJdbcUrl());
    config.setUsername(db.getUsername());
    config.setPassword(db.getPassword());
    dataSource = new HikariDataSource(config);
  }

  @AfterAll
  static void close() {
    dataSource.close();
  }

  private static String ts(String value) {
    return value == null ? "NULL" : "TIMESTAMP '" + value + "'";
  }

  private static String insertRows(String kind, String value, boolean nullable) {
    String nullableValue = nullable ? null : value;
    String id = kind;
    return String.join(
        ";\n",
        String.format(
            "INSERT INTO chats.chats_user (id, created_at, updated_at) VALUES ('%s', %s, %s)",
            id, ts(value), ts(value)),
        String.format(
            "INSERT INTO chats.room (id, name, type, picture_updated_at, created_at, updated_at)"
                + " VALUES ('%s', 'n', 'GROUP', %s, %s, %s)",
            id, ts(nullableValue), ts(value), ts(value)),
        String.format(
            "INSERT INTO chats.subscription (user_id, room_id, joined_at, created_at, updated_at)"
                + " VALUES ('u', '%s', %s, %s, %s)",
            id, ts(nullableValue), ts(value), ts(value)),
        String.format(
            "INSERT INTO chats.room_user_settings"
                + " (user_id, room_id, muted_until, cleared_at, created_at, updated_at)"
                + " VALUES ('u', '%s', %s, %s, %s, %s)",
            id, ts(nullableValue), ts(nullableValue), ts(value), ts(value)),
        String.format(
            "INSERT INTO chats.file_metadata"
                + " (id, name, original_size, mime_type, type, user_id, room_id, created_at,"
                + " updated_at) VALUES ('%s', 'f', 1, 'text/plain', 'ATTACHMENT', 'u', '%s', %s,"
                + " %s)",
            id, id, ts(value), ts(value)),
        String.format(
            "INSERT INTO chats.meeting (id, room_id, created_at, started_at, expiration)"
                + " VALUES ('%s', '%s', %s, %s, %s)",
            id, id, ts(value), ts(nullableValue), ts(nullableValue)),
        String.format(
            "INSERT INTO chats.participant"
                + " (meeting_id, user_id, created_at, updated_at, hand_raised_at)"
                + " VALUES ('%s', 'u', %s, %s, %s)",
            id, ts(value), ts(value), ts(nullableValue)));
  }

  private static List<Check> checks(String kind, Instant instant, boolean nullable) {
    Instant optional = nullable ? null : instant;
    String id = "id = '" + kind + "'";
    String room = "room_id = '" + kind + "'";
    return List.of(
        new Check("chats_user", id, "created_at", instant),
        new Check("chats_user", id, "updated_at", instant),
        new Check("room", id, "picture_updated_at", optional),
        new Check("room", id, "created_at", instant),
        new Check("room", id, "updated_at", instant),
        new Check("subscription", room, "joined_at", optional),
        new Check("subscription", room, "created_at", instant),
        new Check("subscription", room, "updated_at", instant),
        new Check("room_user_settings", room, "muted_until", optional),
        new Check("room_user_settings", room, "cleared_at", optional),
        new Check("room_user_settings", room, "created_at", instant),
        new Check("room_user_settings", room, "updated_at", instant),
        new Check("file_metadata", id, "created_at", instant),
        new Check("file_metadata", id, "updated_at", instant),
        new Check("meeting", id, "created_at", instant),
        new Check("meeting", id, "started_at", optional),
        new Check("meeting", id, "expiration", optional),
        new Check("participant", "meeting_id = '" + kind + "'", "created_at", instant),
        new Check("participant", "meeting_id = '" + kind + "'", "updated_at", instant),
        new Check("participant", "meeting_id = '" + kind + "'", "hand_raised_at", optional));
  }

  @Test
  @DisplayName("Summer, winter and NULL values keep the right instant per column")
  void convertsEveryTimeColumn() throws SQLException {
    convertsWith(ZoneId.of("Europe/Rome"), SUMMER_INSTANT, WINTER_INSTANT);
  }

  @Test
  @DisplayName("A fixed offset shifts summer and winter alike by that offset")
  void convertsWithFixedOffset() throws SQLException {
    convertsWith(
        ZoneOffset.ofHours(1),
        Instant.parse("2026-07-01T11:00:00Z"),
        Instant.parse("2026-01-15T11:00:00Z"));
  }

  private void convertsWith(ZoneId zone, Instant summerInstant, Instant winterInstant)
      throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("DROP SCHEMA IF EXISTS chats CASCADE");
    }
    flyway("1.4.1", zone).migrate();
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(insertRows("summer", SUMMER, false));
      statement.execute(insertRows("winter", WINTER, false));
      statement.execute(insertRows("nulls", SUMMER, true));
    }

    flyway("2.0.0", zone).migrate();

    List<Check> all = new java.util.ArrayList<>();
    all.addAll(checks("summer", summerInstant, false));
    all.addAll(checks("winter", winterInstant, false));
    all.addAll(checks("nulls", summerInstant, true));
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      for (Check check : all) {
        try (ResultSet resultSet =
            statement.executeQuery(
                "SELECT "
                    + check.column()
                    + " FROM chats."
                    + check.table()
                    + " WHERE "
                    + check.where())) {
          resultSet.next();
          Timestamp value = resultSet.getTimestamp(1);
          String label = check.table() + "." + check.column() + " " + check.where();
          if (check.expected() == null) {
            assertNull(value, label);
          } else {
            assertEquals(check.expected(), value.toInstant(), label);
          }
        }
      }
    }
  }
}
