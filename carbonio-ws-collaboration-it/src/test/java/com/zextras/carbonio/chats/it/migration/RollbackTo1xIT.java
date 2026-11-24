// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zextras.carbonio.chats.core.config.module.DatabaseModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@DisplayName("The rollback script gives back the 1.x database after the 2.0 migrations")
class RollbackTo1xIT {

  private static final String LOCATION = "classpath:migration/ce";
  private static final Path SCRIPT = Path.of("..", "docs", "migration", "rollback-2.0-to-1.x.sql");
  private static final String SCRIPT_IN_CONTAINER = "/tmp/rollback-2.0-to-1.x.sql";
  private static final ZoneId ZONE = ZoneId.of("Europe/Rome");

  @Container
  private static final PostgreSQLContainer<?> db =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("carbonio-ws-collaboration-db")
          .withUsername("carbonio-ws-collaboration-db")
          .withPassword("password");

  private static HikariDataSource dataSource;

  @BeforeAll
  static void setUp() throws Exception {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(db.getJdbcUrl());
    config.setUsername(db.getUsername());
    config.setPassword(db.getPassword());
    dataSource = new HikariDataSource(config);
    String script = Base64.getEncoder().encodeToString(Files.readAllBytes(SCRIPT));
    ExecResult copy =
        db.execInContainer("sh", "-c", "echo " + script + " | base64 -d > " + SCRIPT_IN_CONTAINER);
    assertEquals(0, copy.getExitCode(), copy.getStderr());
  }

  @AfterAll
  static void close() {
    dataSource.close();
  }

  @BeforeEach
  void reset() throws SQLException {
    execute(
        "DROP SCHEMA IF EXISTS chats CASCADE;"
            + " ALTER ROLE \"carbonio-ws-collaboration-db\" RESET ALL");
  }

  private static Flyway flyway(String target) {
    return Flyway.configure()
        .locations(LOCATION)
        .placeholders(Map.of("jvm_zone", DatabaseModule.migrationZone(ZONE)))
        .schemas("chats")
        .dataSource(dataSource)
        .validateMigrationNaming(true)
        .target(target)
        .load();
  }

  private static void execute(String sql) throws SQLException {
    try (Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(sql);
    }
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

  private static String ts(String value) {
    return value == null ? "NULL" : "TIMESTAMP '" + value + "'";
  }

  private static void insert1xRows(String id, String value, boolean withNulls) throws SQLException {
    String optional = ts(withNulls ? null : value);
    String time = ts(value);
    execute(
        String.join(
            ";\n",
            String.format(
                "INSERT INTO chats.chats_user (id, created_at, updated_at) VALUES ('%s', %s, %s)",
                id, time, time),
            String.format(
                "INSERT INTO chats.room (id, name, type, picture_updated_at, created_at,"
                    + " updated_at) VALUES ('%s', 'n', 'GROUP', %s, %s, %s)",
                id, optional, time, time),
            String.format(
                "INSERT INTO chats.subscription (user_id, room_id, joined_at, created_at,"
                    + " updated_at) VALUES ('u', '%s', %s, %s, %s)",
                id, optional, time, time),
            String.format(
                "INSERT INTO chats.room_user_settings (user_id, room_id, muted_until, cleared_at,"
                    + " created_at, updated_at) VALUES ('u', '%s', %s, %s, %s, %s)",
                id, optional, optional, time, time),
            String.format(
                "INSERT INTO chats.file_metadata (id, name, original_size, mime_type, type,"
                    + " user_id, room_id, created_at, updated_at) VALUES ('%s', 'f', 1,"
                    + " 'text/plain', 'ATTACHMENT', 'u', '%s', %s, %s)",
                id, id, time, time),
            String.format(
                "INSERT INTO chats.meeting (id, room_id, created_at, started_at, expiration)"
                    + " VALUES ('%s', '%s', %s, %s, %s)",
                id, id, time, optional, optional),
            String.format(
                "INSERT INTO chats.participant (meeting_id, user_id, created_at, updated_at,"
                    + " hand_raised_at) VALUES ('%s', 'u', %s, %s, %s)",
                id, time, time, optional)));
  }

  private static void insert1xData() throws SQLException {
    insert1xRows("summer", "2026-07-01 12:00:00.123456", false);
    insert1xRows("winter", "2026-01-15 08:00:00", false);
    insert1xRows("spring-gap", "2026-03-29 02:30:00", false);
    insert1xRows("autumn-overlap", "2026-10-25 02:30:00", false);
    insert1xRows("year-one", "0001-01-01 00:00:00", false);
    insert1xRows("nulls", "2026-05-05 05:05:05", true);
  }

  private static void rollback() throws IOException, InterruptedException {
    ExecResult result =
        db.execInContainer(
            "psql",
            "-X",
            "-q",
            "-v",
            "ON_ERROR_STOP=1",
            "-v",
            "zone=" + DatabaseModule.migrationZone(ZONE),
            "-U",
            db.getUsername(),
            "-d",
            db.getDatabaseName(),
            "-f",
            SCRIPT_IN_CONTAINER);
    assertEquals(0, result.getExitCode(), result.getStderr());
  }

  private static List<String> dump(String... options) throws IOException, InterruptedException {
    List<String> command =
        new ArrayList<>(
            List.of("pg_dump", "--no-owner", "-U", db.getUsername(), "-d", db.getDatabaseName()));
    command.addAll(List.of(options));
    ExecResult result = db.execInContainer(command.toArray(String[]::new));
    assertEquals(0, result.getExitCode(), result.getStderr());
    return result
        .getStdout()
        .lines()
        .filter(line -> !line.startsWith("\\restrict") && !line.startsWith("\\unrestrict"))
        .sorted()
        .toList();
  }

  @Test
  @DisplayName("Schema, data, history and role settings are those of 1.x again, DST times included")
  void restoresThe1xDatabase() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    List<String> before = dump();

    flyway("latest").migrate();
    execute(
        "UPDATE chats.room_user_settings SET muted_until = clock_timestamp(),"
            + " updated_at = clock_timestamp() WHERE room_id = 'summer'");
    rollback();

    assertEquals(before, dump());
    assertEquals(
        List.of(),
        query(
            "SELECT s.setconfig::text FROM pg_db_role_setting s JOIN pg_roles r ON r.oid ="
                + " s.setrole WHERE r.rolname = 'carbonio-ws-collaboration-db'"));
  }

  @Test
  @DisplayName("A room created on 2.x stays, with its times as wall clock of the 1.x zone")
  void keepsRowsCreatedOn2x() throws Exception {
    flyway("1.4.1").migrate();
    flyway("latest").migrate();
    execute(
        "INSERT INTO chats.room (id, name, type, created_at, updated_at) VALUES ('new', 'n',"
            + " 'GROUP', TIMESTAMPTZ '2026-07-01 10:00:00Z', TIMESTAMPTZ '2026-03-29 01:30:00Z')");
    rollback();

    assertEquals(
        List.of("2026-07-01 12:00:00|2026-03-29 03:30:00|NO"),
        query(
            "SELECT r.created_at::text || '|' || r.updated_at::text || '|' || c.is_nullable"
                + " FROM chats.room r, information_schema.columns c WHERE r.id = 'new'"
                + " AND c.table_schema = 'chats' AND c.table_name = 'room'"
                + " AND c.column_name = 'created_at'"));
  }

  @Test
  @DisplayName("Upgrading again after a rollback gives the same database as the first upgrade")
  void upgradesAgainAfterRollback() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    flyway("latest").migrate();
    String[] withoutHistoryData = {"--exclude-table-data=chats.flyway_schema_history"};
    List<String> firstUpgrade = dump(withoutHistoryData);

    rollback();
    flyway("latest").migrate();

    assertEquals(firstUpgrade, dump(withoutHistoryData));
  }
}
