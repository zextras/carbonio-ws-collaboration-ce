// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.LinkedHashMap;
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
@DisplayName("The rollback script gives back the exact 1.x database after the 2.0 migrations")
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
        "DROP SCHEMA IF EXISTS chats CASCADE; DROP SCHEMA IF EXISTS chats_v1 CASCADE;"
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

  private static final List<String> TABLES_1X =
      List.of(
          "chats_user",
          "room",
          "subscription",
          "room_user_settings",
          "file_metadata",
          "meeting",
          "participant",
          "videoserver_meeting",
          "videoserver_session");

  private static void insert1xData() throws SQLException {
    execute(
        String.join(
            ";\n",
            "INSERT INTO chats.chats_user (id, status_message, created_at, updated_at) VALUES"
                + " ('u1', 'hello', '2026-07-01 12:00:00.123456', '2026-07-01 12:00:00'),"
                + " ('u2', '', '2026-01-15 08:00:00', '2026-01-15 08:00:00'),"
                + " ('u3', 'dst', '2026-10-25 02:30:00', '2026-03-29 02:30:00')",
            "INSERT INTO chats.room (id, name, description, type, picture_updated_at, created_at,"
                + " updated_at) VALUES"
                + " ('r1', 'Room one', 'first', 'GROUP', '2026-02-02 10:00:00',"
                + " '2026-07-01 12:00:00.5', '2026-07-02 12:00:00'),"
                + " ('r2', 'Room two', NULL, 'GROUP', NULL, '2026-01-15 08:00:00',"
                + " '2026-01-15 08:00:00'),"
                + " ('r3', NULL, NULL, 'ONE_TO_ONE', NULL, '2026-03-29 02:30:00',"
                + " '2026-03-29 02:30:00'),"
                + " ('r4', 'Room four', 'fourth', 'GROUP', '2026-05-05 05:05:05',"
                + " '2026-05-05 05:05:05', '2026-05-06 06:06:06')",
            "INSERT INTO chats.meeting (id, name, meeting_type, room_id, active, created_at,"
                + " started_at, expiration) VALUES ('m1', 'standup', 'PERMANENT', 'r1', true,"
                + " '2026-07-01 12:00:00', '2026-07-01 12:05:00', '2026-07-01 18:00:00'),"
                + " ('m2', NULL, 'SCHEDULED', 'r4', NULL, '2026-05-05 05:05:05', NULL, NULL)",
            "UPDATE chats.room SET meeting_id = 'm1' WHERE id = 'r1'",
            "INSERT INTO chats.subscription (user_id, room_id, owner, temporary, external,"
                + " joined_at, created_at, updated_at) VALUES"
                + " ('u1', 'r1', true, false, false, '2026-07-01 12:00:00', '2026-07-01 12:00:00',"
                + " '2026-07-01 12:00:00'),"
                + " ('u2', 'r1', false, true, false, NULL, '2026-07-01 12:01:00',"
                + " '2026-07-01 12:01:00'),"
                + " ('u1', 'r2', true, false, true, '2026-01-15 08:00:00', '2026-01-15 08:00:00',"
                + " '2026-01-15 08:00:00'),"
                + " ('u2', 'r2', false, false, false, '2026-01-15 08:00:00',"
                + " '2026-01-15 08:00:00', '2026-01-15 08:00:00'),"
                + " ('u3', 'r3', true, false, false, '2026-03-29 02:30:00',"
                + " '2026-03-29 02:30:00', '2026-03-29 02:30:00'),"
                + " ('u3', 'r4', NULL, NULL, NULL, NULL, '2026-05-05 05:05:05',"
                + " '2026-05-05 05:05:05'),"
                + " ('u1', 'r4', false, true, true, '2026-05-05 05:06:00', '2026-05-05 05:06:00',"
                + " '2026-05-05 05:07:00')",
            "INSERT INTO chats.room_user_settings (user_id, room_id, muted_until, cleared_at,"
                + " created_at, updated_at) VALUES"
                + " ('u1', 'r1', '2026-08-01 00:00:00', NULL, '2026-07-01 12:00:00',"
                + " '2026-07-01 12:00:00'),"
                + " ('u3', 'r3', NULL, '2026-04-01 09:00:00', '2026-03-29 02:30:00',"
                + " '2026-03-29 02:30:00')",
            "INSERT INTO chats.file_metadata (id, name, original_size, mime_type, type, user_id,"
                + " room_id, message_id, stanza_id, created_at, updated_at) VALUES"
                + " ('f1', 'a.txt', 10, 'text/plain', 'ATTACHMENT', 'u1', 'r1', 'mid', 'sid',"
                + " '2026-07-01 12:00:00', '2026-07-01 12:00:00'),"
                + " ('f2', 'b.png', 20, 'image/png', 'ATTACHMENT', 'u3', 'r3', NULL, NULL,"
                + " '0001-01-01 00:00:00', '2026-03-29 02:30:00'),"
                + " ('f3', 'c.pdf', 30, 'application/pdf', 'ATTACHMENT', 'u2', NULL, NULL, NULL,"
                + " '2026-05-05 05:05:05', '2026-05-05 05:05:06')",
            "INSERT INTO chats.participant (meeting_id, user_id, queue_id, video_stream_on,"
                + " audio_stream_on, screen_stream_on, hand_raised_at, created_at, updated_at)"
                + " VALUES ('m1', 'u1', 'q1', true, false, false, '2026-07-01 12:10:00',"
                + " '2026-07-01 12:05:00', '2026-07-01 12:10:00'),"
                + " ('m1', 'u2', NULL, NULL, NULL, NULL, NULL, '2026-07-01 12:06:00',"
                + " '2026-07-01 12:07:00'),"
                + " ('m2', 'u3', 'q2', false, true, true, NULL, '2026-05-05 05:05:05',"
                + " '2026-05-05 05:05:06')",
            "INSERT INTO chats.videoserver_meeting (meeting_id, connection_id, audio_handle_id,"
                + " video_handle_id, audio_room_id, video_room_id) VALUES"
                + " ('m1', 'c', 'ah', 'vh', 'ar', 'vr'),"
                + " ('m2', 'c2', 'ah2', 'vh2', 'ar2', 'vr2')",
            "INSERT INTO chats.videoserver_session (meeting_id, user_id, connection_id, queue_id,"
                + " audio_handle_id, videoout_handle_id, videoin_handle_id, screen_handle_id,"
                + " video_out_stream_on, video_in_stream_on, screen_stream_on, audio_stream_on)"
                + " VALUES ('m1', 'u1', 'c', 'q1', 'ah', 'vo', 'vi', NULL, true, false, false,"
                + " true),"
                + " ('m1', 'u2', 'c3', NULL, NULL, NULL, NULL, 'sc', NULL, NULL, NULL, NULL),"
                + " ('m2', 'u3', 'c4', 'q4', 'ah4', 'vo4', NULL, 'sc4', false, true, true,"
                + " false)"));
  }

  private static Map<String, String> data() throws SQLException {
    Map<String, String> rows = new LinkedHashMap<>();
    for (String table : TABLES_1X) {
      rows.put(
          table,
          query(
                  "SELECT coalesce(string_agg(t::text, E'\\n' ORDER BY t::text), '') FROM chats."
                      + table
                      + " t")
              .get(0));
    }
    return rows;
  }

  private static List<String> differencesFromV1(String table) throws SQLException {
    List<String> plain = new ArrayList<>();
    List<String> converted = new ArrayList<>();
    for (String row :
        query(
            "SELECT column_name || '|' || data_type FROM information_schema.columns WHERE"
                + " table_schema = 'chats_v1' AND table_name = '"
                + table
                + "' ORDER BY ordinal_position")) {
      String[] column = row.split("\\|");
      plain.add(column[0]);
      converted.add(
          column[1].equals("timestamp without time zone")
              ? column[0] + " AT TIME ZONE " + DatabaseModule.migrationZone(ZONE)
              : column[0]);
    }
    List<String> dropped = List.of("message_id", "stanza_id");
    if (table.equals("file_metadata")) {
      plain.removeIf(dropped::contains);
      converted.removeIf(c -> dropped.stream().anyMatch(c::startsWith));
    }
    String columns = String.join(", ", plain);
    String convertedColumns = String.join(", ", converted);
    return query(
        "SELECT 'new only: ' || t::text FROM (SELECT "
            + columns
            + " FROM chats."
            + table
            + " EXCEPT SELECT "
            + convertedColumns
            + " FROM chats_v1."
            + table
            + ") t UNION ALL SELECT 'v1 only: ' || t::text FROM (SELECT "
            + convertedColumns
            + " FROM chats_v1."
            + table
            + " EXCEPT SELECT "
            + columns
            + " FROM chats."
            + table
            + ") t");
  }

  private static ExecResult runRollback() throws IOException, InterruptedException {
    return db.execInContainer(
        "psql",
        "-X",
        "-q",
        "-v",
        "ON_ERROR_STOP=1",
        "-U",
        db.getUsername(),
        "-d",
        db.getDatabaseName(),
        "-f",
        SCRIPT_IN_CONTAINER);
  }

  private static void rollback() throws IOException, InterruptedException {
    ExecResult result = runRollback();
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
  @DisplayName("Schema, data, history and role settings are those of 1.x again, whatever 2.x did")
  void restoresThe1xDatabase() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    List<String> schemaBefore = dump("--schema-only");
    List<String> dumpBefore = dump();
    Map<String, String> dataBefore = data();

    flyway("latest").migrate();
    execute(
        String.join(
            ";\n",
            "INSERT INTO chats.room (id, name, type) VALUES ('rnew', 'New', 'GROUP')",
            "INSERT INTO chats.subscription (user_id, room_id, owner) VALUES ('u1', 'rnew', true),"
                + " ('u2', 'rnew', false)",
            "UPDATE chats.room SET name = 'Renamed', updated_at = clock_timestamp()"
                + " WHERE id = 'r2'",
            "DELETE FROM chats.subscription WHERE user_id = 'u2' AND room_id = 'r1'",
            "DELETE FROM chats.room WHERE id = 'r3'"));
    assertEquals(List.of("rnew"), query("SELECT id FROM chats.room WHERE id = 'rnew'"));
    assertEquals(List.of(), query("SELECT id FROM chats.room WHERE id = 'r3'"));
    assertEquals(List.of("Renamed"), query("SELECT name FROM chats.room WHERE id = 'r2'"));
    assertEquals(
        List.of(),
        query("SELECT user_id FROM chats.subscription WHERE user_id = 'u2' AND room_id = 'r1'"));
    rollback();

    assertEquals(schemaBefore, dump("--schema-only"));
    assertEquals(dumpBefore, dump());
    assertEquals(dataBefore, data());
    assertEquals(List.of("true"), query("SELECT (to_regnamespace('chats_v1') IS NULL)::text"));
    assertEquals(
        List.of(),
        query(
            "SELECT s.setconfig::text FROM pg_db_role_setting s JOIN pg_roles r ON r.oid ="
                + " s.setrole WHERE r.rolname = 'carbonio-ws-collaboration-db'"));
  }

  @Test
  @DisplayName("Every 1.x row and column is copied to the 2.x table, times converted")
  void copiesEveryColumnOfEveryTable() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    flyway("latest").migrate();

    for (String table : TABLES_1X) {
      assertEquals(List.of(), differencesFromV1(table), table);
      assertEquals(
          query("SELECT count(*)::text FROM chats_v1." + table),
          query("SELECT count(*)::text FROM chats." + table),
          table);
    }
    assertEquals(
        List.of("0"),
        query(
            "SELECT count(*)::text FROM information_schema.columns WHERE table_schema = 'chats'"
                + " AND table_name = 'file_metadata' AND column_name IN ('message_id',"
                + " 'stanza_id')"));
    assertEquals(
        List.of("0"),
        query(
            "SELECT count(*)::text FROM pg_indexes WHERE schemaname = 'chats' AND indexname ="
                + " 'file_metadata_message_id'"));
    assertEquals(
        List.of("f1=mid|sid"),
        query(
            "SELECT id || '=' || message_id || '|' || stanza_id FROM chats_v1.file_metadata WHERE"
                + " id = 'f1'"));
  }

  @Test
  @DisplayName("The seed has every column filled in some row and NULL in another where nullable")
  void seedCoversEveryColumn() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();

    List<String> gaps = new ArrayList<>();
    for (String table : TABLES_1X) {
      for (String column :
          query(
              "SELECT column_name || '|' || is_nullable FROM information_schema.columns WHERE"
                  + " table_schema = 'chats' AND table_name = '"
                  + table
                  + "'")) {
        String[] parts = column.split("\\|");
        String counts =
            query(
                    "SELECT count("
                        + parts[0]
                        + ")::text || '|' || count(*)::text FROM chats."
                        + table)
                .get(0);
        String[] n = counts.split("\\|");
        if (n[0].equals("0")) {
          gaps.add(table + "." + parts[0] + " is always NULL");
        }
        if (parts[1].equals("YES") && n[0].equals(n[1])) {
          gaps.add(table + "." + parts[0] + " is never NULL");
        }
      }
    }
    assertEquals(List.of(), gaps);
  }

  @Test
  @DisplayName("The script knows every table that the 2.x migrations create")
  void scriptKnowsEveryTableOfTheMigrations() throws Exception {
    flyway("1.4.1").migrate();
    flyway("latest").migrate();

    rollback();
  }

  @Test
  @DisplayName("A table in chats that is not a WSC table makes the script fail, nothing changes")
  void failsWithAForeignTableInChats() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    flyway("latest").migrate();
    execute("CREATE TABLE chats.room_backup AS SELECT * FROM chats.room");
    List<String> before = dump();

    ExecResult result = runRollback();

    assertNotEquals(0, result.getExitCode());
    assertTrue(result.getStderr().contains("room_backup"), result.getStderr());
    assertTrue(result.getStderr().contains("nothing was rolled back"), result.getStderr());
    assertEquals(before, dump());
  }

  @Test
  @DisplayName("Without chats_v1 the script fails and changes nothing")
  void failsWithoutTheV1Schema() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    flyway("latest").migrate();
    execute("DROP SCHEMA chats_v1 CASCADE");
    List<String> before = dump();

    ExecResult result = runRollback();

    assertNotEquals(0, result.getExitCode());
    assertTrue(result.getStderr().contains("nothing was rolled back"), result.getStderr());
    assertEquals(before, dump());
  }

  @Test
  @DisplayName("With a 1.x table missing from chats_v1 the script fails and changes nothing")
  void failsWithAnIncompleteV1Schema() throws Exception {
    flyway("1.4.1").migrate();
    insert1xData();
    flyway("latest").migrate();
    execute("DROP TABLE chats_v1.participant");
    List<String> before = dump();

    ExecResult result = runRollback();

    assertNotEquals(0, result.getExitCode());
    assertTrue(result.getStderr().contains("participant"), result.getStderr());
    assertEquals(before, dump());
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
