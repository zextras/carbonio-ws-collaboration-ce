// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.migration.scripts.V1_4_2__backfill_attachment_ids;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.flywaydb.core.Flyway;
import org.flywaydb.database.postgresql.PostgreSQLConfigurationExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("V2_0_1 import: availability of the MongooseIM source and failure atomicity")
class SourceAvailabilityIT {

  private static final String ZONE_PARITY_ROOM = "e0df7f4a-7a19-4ab7-9af7-49be3b7874b6";
  private static final long ZONE_ROOM_MEMBER_ADDED_MAM = 458596905521228033L;
  private static final String PREEXISTING_MESSAGE_ID = "preexisting-message";
  private static final String PREEXISTING_MESSAGE_TEXT = "preexisting message text";
  private static final String ADMIN_DATABASE = "postgres_it";
  private static final String MISSING_DATABASE = "no_such_mongoose_database";
  private static final Pattern BODY = Pattern.compile("<body>([^<&]{24,})</body>");

  private static final List<String> IMPORTED_TABLES =
      List.of(
          "chats.message",
          "chats.system_event",
          "chats.message_reaction",
          "chats.message_pin",
          "chats.read_marker",
          "chats.user_contact",
          "chats.user_last_seen");

  private static final String ROOM_FINGERPRINT =
      "SELECT md5(coalesce(string_agg(r::text, ',' ORDER BY r.id), '')) FROM (SELECT id, name,"
          + " description, type, picture_updated_at, meeting_id, created_at, updated_at FROM"
          + " chats.room) r";
  private static final String SUBSCRIPTION_FINGERPRINT =
      "SELECT md5(coalesce(string_agg(r::text, ',' ORDER BY r.user_id, r.room_id), '')) FROM"
          + " (SELECT user_id, room_id, joined_at, owner, external, temporary, created_at,"
          + " updated_at FROM chats.subscription) r";
  private static final String SETTINGS_FINGERPRINT =
      "SELECT md5(coalesce(string_agg(r::text, ',' ORDER BY r.user_id, r.room_id), '')) FROM"
          + " (SELECT user_id, room_id, muted_until, cleared_at, created_at, updated_at FROM"
          + " chats.room_user_settings) r";
  private static final String FILE_FINGERPRINT =
      "SELECT md5(coalesce(string_agg(r::text, ',' ORDER BY r.id), '')) FROM (SELECT id, name,"
          + " original_size, mime_type, type, user_id, room_id, created_at, updated_at, message_id"
          + " FROM chats.file_metadata) r";

  private static List<String> dumpTexts;
  private static ImportRun pinlessRun;

  private record TargetState(
      long rooms,
      long subscriptions,
      long files,
      String roomFingerprint,
      String subscriptionFingerprint,
      String settingsFingerprint,
      String fileFingerprint) {}

  private static MongooseImportDatabase database() {
    return MongooseImportDatabase.get();
  }

  private static TargetState targetState(String targetDatabase) {
    ImportedChats chats = new ImportedChats(targetDatabase);
    return new TargetState(
        chats.count("SELECT count(*) FROM chats.room"),
        chats.count("SELECT count(*) FROM chats.subscription"),
        chats.count("SELECT count(*) FROM chats.file_metadata"),
        chats.query(ROOM_FINGERPRINT, rs -> rs.getString(1)).get(0),
        chats.query(SUBSCRIPTION_FINGERPRINT, rs -> rs.getString(1)).get(0),
        chats.query(SETTINGS_FINGERPRINT, rs -> rs.getString(1)).get(0),
        chats.query(FILE_FINGERPRINT, rs -> rs.getString(1)).get(0));
  }

  private static long sourceCount(String sourceDatabase, String sql) {
    try (Connection connection = database().connect(sourceDatabase);
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql)) {
      rs.next();
      return rs.getLong(1);
    } catch (SQLException e) {
      throw new IllegalStateException("Source query failed: " + sql, e);
    }
  }

  private static synchronized List<String> dumpTexts() {
    if (dumpTexts == null) {
      Set<String> prefixes = new LinkedHashSet<>();
      try (Connection connection = database().connect(MongooseImportDatabase.SOURCE_DATABASE);
          Statement statement = connection.createStatement();
          ResultSet rs =
              statement.executeQuery("SELECT message FROM mam_muc_message ORDER BY id")) {
        while (rs.next() && prefixes.size() < 400) {
          Matcher matcher = BODY.matcher(new String(rs.getBytes(1), StandardCharsets.UTF_8));
          if (matcher.find()) {
            prefixes.add(matcher.group(1).substring(0, 24));
          }
        }
      } catch (SQLException e) {
        throw new IllegalStateException("Cannot read the dump texts", e);
      }
      dumpTexts = new ArrayList<>(prefixes);
    }
    return dumpTexts;
  }

  private static void assertNoDumpTextInOutput(ImportRun run) {
    List<String> texts = dumpTexts();
    String output = run.allOutput();
    assertAll(
        () -> assertFalse(texts.isEmpty(), "no message text available to look for"),
        () ->
            assertTrue(
                texts.stream().noneMatch(output::contains),
                () ->
                    "message text leaked into the output: "
                        + texts.stream().filter(output::contains).limit(3).toList()));
  }

  private static long successRows(ImportedChats chats, String version) {
    return chats.count(
        "SELECT count(*) FROM chats.flyway_schema_history WHERE version = ? AND success", version);
  }

  private static void assertNothingImported(ImportedChats chats, long expectedMessages) {
    assertAll(
        IMPORTED_TABLES.stream()
            .map(
                table ->
                    (Executable)
                        () ->
                            assertEquals(
                                table.equals("chats.message") ? expectedMessages : 0,
                                chats.count("SELECT count(*) FROM " + table),
                                table)));
  }

  private static void assertTargetUntouched(TargetState before, String targetDatabase) {
    TargetState after = targetState(targetDatabase);
    assertAll(
        () -> assertEquals(before.rooms(), after.rooms(), "rooms count"),
        () -> assertEquals(before.subscriptions(), after.subscriptions(), "subscriptions count"),
        () -> assertEquals(before.files(), after.files(), "file_metadata count"),
        () -> assertEquals(before.roomFingerprint(), after.roomFingerprint(), "room content"),
        () ->
            assertEquals(
                before.subscriptionFingerprint(),
                after.subscriptionFingerprint(),
                "subscription content"),
        () ->
            assertEquals(
                before.settingsFingerprint(), after.settingsFingerprint(), "settings content"),
        () ->
            assertEquals(
                before.fileFingerprint(), after.fileFingerprint(), "file_metadata content"));
  }

  private static void assertFailedAtomically(ImportRun run, TargetState before) {
    ImportedChats chats = run.chats();
    assertNotNull(run.failure(), "the migration must throw");
    assertAll(
        () -> assertFalse(run.succeeded(), "the migration must not succeed"),
        () -> assertFalse(run.failureText().isBlank()),
        () ->
            assertFalse(
                run.failureText().contains("INSERT INTO")
                    || run.failureText().contains("BatchUpdateException"),
                "the failure must be detected before any row is written"),
        () ->
            assertEquals(
                0,
                chats.count(
                    "SELECT count(*) FROM chats.flyway_schema_history WHERE version LIKE '2.%'"),
                "no 2.x migration is recorded: the whole upgrade rolled back"),
        () ->
            assertEquals(
                0,
                chats.count(
                    "SELECT count(*) FROM pg_tables WHERE schemaname = 'chats'"
                        + " AND tablename = 'message'"),
                "the 2.0.0 tables are rolled back too"),
        () ->
            assertEquals(
                "",
                chats
                    .query(
                        "SELECT coalesce(array_to_string(rolconfig, ','), '') FROM pg_roles"
                            + " WHERE rolname = 'carbonio-ws-collaboration-db'",
                        rs -> rs.getString(1))
                    .get(0),
                "the role settings of 2.0.0 are rolled back too"),
        () -> assertTargetUntouched(before, run.targetDatabase()),
        () -> assertNoDumpTextInOutput(run));
  }

  private static void assertNoOpSuccess(ImportRun run, TargetState before) {
    ImportedChats chats = run.chats();
    assertAll(
        () -> assertTrue(run.succeeded(), run::failureText),
        () -> assertEquals(1, successRows(chats, "2.0.1"), "2.0.1 must be recorded as success"),
        () ->
            assertEquals(
                0,
                chats.count("SELECT count(*) FROM chats.flyway_schema_history WHERE NOT success")),
        () -> assertNothingImported(chats, 0),
        () -> assertTargetUntouched(before, run.targetDatabase()),
        () ->
            assertTrue(
                run.logLines().stream()
                    .anyMatch(
                        line ->
                            line.startsWith("INFO com.zextras")
                                || line.startsWith("WARN com.zextras")),
                "the NO-OP must be logged by the application"),
        () -> assertNoDumpTextInOutput(run));
  }

  private static String hostOf(MessageDispatcherCredentials credentials) {
    return URI.create(credentials.getJdbcUrl().substring("jdbc:".length())).getHost();
  }

  private static int portOf(MessageDispatcherCredentials credentials) {
    return URI.create(credentials.getJdbcUrl().substring("jdbc:".length())).getPort();
  }

  private static MessageDispatcherCredentials credentials(
      MessageDispatcherCredentials base, String name, String username, String password) {
    return new MessageDispatcherCredentials(hostOf(base), portOf(base), name, username, password);
  }

  private static void assertSourceHoldsArchives(String sourceDatabase) {
    assertTrue(sourceCount(sourceDatabase, "SELECT count(*) FROM mam_muc_message") > 0);
  }

  private static synchronized ImportRun pinlessRun() {
    if (pinlessRun == null) {
      MongooseImportDatabase database = database();
      String source = database.newSourceDatabase();
      database.execute(source, "DROP TABLE pin_message");
      pinlessRun =
          MongooseImport.run(database.newTargetDatabase(), database.credentialsFor(source));
    }
    return pinlessRun;
  }

  @Nested
  @DisplayName("OPS-2: sources that positively do not exist are a logged NO-OP")
  class LoggedNoOp {

    @Test
    @DisplayName("OPS-1/OPS-2: no dispatcher-db credentials at all -> NO-OP success, nothing read")
    void absentCredentialsAreNoOp() {
      MongooseImportDatabase database = database();
      assertSourceHoldsArchives(MongooseImportDatabase.SOURCE_DATABASE);
      String target = database.newTargetDatabase();
      TargetState before = targetState(target);
      assertTrue(before.rooms() > 0 && before.subscriptions() > 0 && before.files() > 0);

      ImportRun run =
          MongooseImport.run(target, credentials(database.sourceCredentials(), null, null, null));

      assertNoOpSuccess(run, before);
    }

    @Test
    @DisplayName("OPS-2: the database does not exist on the server (3D000) -> NO-OP success")
    void missingDatabaseIsNoOp() {
      MongooseImportDatabase database = database();
      SQLException refused =
          assertThrows(SQLException.class, () -> database.connect(MISSING_DATABASE).close());
      assertEquals("3D000", refused.getSQLState());
      String target = database.newTargetDatabase();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, database.credentialsFor(MISSING_DATABASE));

      assertNoOpSuccess(run, before);
    }

    @Test
    @DisplayName("OPS-2: the database exists but holds no MongooseIM table -> NO-OP success")
    void databaseWithoutMongooseTablesIsNoOp() throws SQLException {
      MongooseImportDatabase database = database();
      String empty = database.newEmptyDatabase();
      try (Connection connection = database.connect(empty);
          Statement statement = connection.createStatement();
          ResultSet rs =
              statement.executeQuery(
                  "SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN"
                      + " ('pg_catalog', 'information_schema')")) {
        rs.next();
        assertEquals(0, rs.getLong(1));
      }
      String target = database.newTargetDatabase();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, database.credentialsFor(empty));

      assertNoOpSuccess(run, before);
    }
  }

  @Nested
  @DisplayName("OPS-2: an unusable or ambiguous source fails the whole migration")
  class AmbiguousSourceFails {

    @ParameterizedTest(name = "missing {0}")
    @ValueSource(strings = {"name", "username", "password"})
    @DisplayName("OPS-2: partial credentials (one of name/username/password null) -> FAIL")
    void partialCredentialsFail(String missing) {
      MongooseImportDatabase database = database();
      MessageDispatcherCredentials full = database.sourceCredentials();
      MessageDispatcherCredentials partial =
          credentials(
              full,
              missing.equals("name") ? null : full.getName(),
              missing.equals("username") ? null : full.getUsername(),
              missing.equals("password") ? null : full.getPassword());
      assertFalse(partial.isAvailable());
      assertSourceHoldsArchives(MongooseImportDatabase.SOURCE_DATABASE);
      String target = database.newTargetDatabase();
      database.resetApplicationRole();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, partial);

      assertFailedAtomically(run, before);
    }

    @Test
    @DisplayName("OPS-2: the server is unreachable (nothing listens on the port) -> FAIL")
    void unreachableServerFails() throws Exception {
      MongooseImportDatabase database = database();
      int closedPort;
      try (ServerSocket socket = new ServerSocket(0)) {
        closedPort = socket.getLocalPort();
      }
      assertThrows(
          IOException.class,
          () -> new Socket().connect(new InetSocketAddress("127.0.0.1", closedPort), 2000));
      MessageDispatcherCredentials full = database.sourceCredentials();
      MessageDispatcherCredentials unreachable =
          new MessageDispatcherCredentials(
              "127.0.0.1",
              closedPort,
              MongooseImportDatabase.SOURCE_DATABASE,
              full.getUsername(),
              full.getPassword());
      String target = database.newTargetDatabase();
      database.resetApplicationRole();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, unreachable);

      assertFailedAtomically(run, before);
    }

    @Test
    @DisplayName("OPS-2: the server refuses the password (28P01) -> FAIL")
    void wrongPasswordFails() {
      MongooseImportDatabase database = database();
      MessageDispatcherCredentials full = database.sourceCredentials();
      MessageDispatcherCredentials wrong =
          credentials(full, full.getName(), full.getUsername(), "definitely-not-the-password");
      SQLException refused =
          assertThrows(
              SQLException.class,
              () ->
                  DriverManager.getConnection(
                          wrong.getJdbcUrl(), wrong.getUsername(), wrong.getPassword())
                      .close());
      assertEquals("28P01", refused.getSQLState());
      assertSourceHoldsArchives(MongooseImportDatabase.SOURCE_DATABASE);
      String target = database.newTargetDatabase();
      database.resetApplicationRole();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, wrong);

      assertFailedAtomically(run, before);
    }

    @Test
    @DisplayName("OPS-2: partial MongooseIM schema (core table mam_server_user dropped) -> FAIL")
    void partialMongooseSchemaFails() {
      MongooseImportDatabase database = database();
      String source = database.newSourceDatabase();
      database.execute(source, "DROP TABLE mam_server_user CASCADE");
      assertEquals(
          0,
          sourceCount(
              source, "SELECT count(*) FROM pg_tables WHERE tablename = 'mam_server_user'"));
      assertSourceHoldsArchives(source);
      assertTrue(sourceCount(source, "SELECT count(*) FROM smart_markers") > 0);
      String target = database.newTargetDatabase();
      database.resetApplicationRole();
      TargetState before = targetState(target);

      ImportRun run = MongooseImport.run(target, database.credentialsFor(source));

      assertAll(
          () -> assertFailedAtomically(run, before),
          () ->
              assertFalse(
                  run.failureText().contains("42P01"),
                  "the missing table must be detected up front, not as a raw SQL error"),
          () ->
              assertTrue(
                  run.failureText().contains("mam_server_user"), "the missing table is named"));
    }
  }

  @Nested
  @DisplayName("OPS-2/SEM-V2: older dispatcher schema without pin_message")
  class MissingPinTable {

    @Test
    @DisplayName("OPS-2/SEM-V2: pin_message dropped -> import succeeds with every pin absent")
    void importSucceedsWithoutPins() {
      ImportRun dump = MongooseImport.ofDump();
      assertTrue(dump.succeeded(), dump::failureText);
      assertTrue(dump.chats().count("SELECT count(*) FROM chats.message_pin") > 0);

      ImportRun run = pinlessRun();
      ImportedChats chats = run.chats();

      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () -> assertEquals(1, successRows(chats, "2.0.1")),
          () -> assertEquals(0, chats.count("SELECT count(*) FROM chats.message_pin")),
          () ->
              assertEquals(
                  dump.chats().count("SELECT count(*) FROM chats.message"),
                  chats.count("SELECT count(*) FROM chats.message")),
          () ->
              assertEquals(
                  dump.chats().count("SELECT count(*) FROM chats.system_event"),
                  chats.count("SELECT count(*) FROM chats.system_event")));
    }
  }

  private static long dumpCount(String sql) {
    ImportRun control = MongooseImport.ofDump();
    assertTrue(control.succeeded(), control::failureText);
    long count = control.chats().count(sql);
    assertTrue(count > 0, sql);
    return count;
  }

  private static void assertImportedLikeTheDump(ImportRun run, long extraMessages) {
    ImportedChats chats = run.chats();
    assertAll(
        () -> assertTrue(run.succeeded(), run::failureText),
        () -> assertEquals(1, successRows(chats, "2.0.1"), "2.0.1 must be recorded as success"),
        () ->
            assertEquals(
                dumpCount("SELECT count(*) FROM chats.message") + extraMessages,
                chats.count("SELECT count(*) FROM chats.message")),
        () ->
            assertEquals(
                dumpCount("SELECT count(*) FROM chats.system_event"),
                chats.count("SELECT count(*) FROM chats.system_event")),
        () -> assertNoDumpTextInOutput(run));
  }

  @Nested
  @DisplayName("OPS-4: the source must not be in use")
  class QuiescentSource {

    @Test
    @DisplayName(
        "OPS-4: another session open on the source database during the run -> import with a"
            + " warning")
    void foreignSessionOnSourceImportsWithWarning() throws SQLException {
      MongooseImportDatabase database = database();
      String source = database.newSourceDatabase();
      String target = database.newTargetDatabase();
      ImportRun run;
      try (Connection holder = database.connect(source);
          Statement statement = holder.createStatement()) {
        statement.execute("SELECT 1");
        assertTrue(holder.isValid(2));
        try (Connection monitor = database.connect(ADMIN_DATABASE);
            Statement query = monitor.createStatement();
            ResultSet rs =
                query.executeQuery(
                    "SELECT count(*) FROM pg_stat_activity WHERE datname = '" + source + "'")) {
          rs.next();
          assertEquals(1, rs.getLong(1));
        }

        run = MongooseImport.run(target, database.credentialsFor(source));
      }

      ImportRun finished = run;
      assertAll(
          () -> assertImportedLikeTheDump(finished, 0),
          () ->
              assertTrue(
                  finished.logLines().stream()
                      .anyMatch(line -> line.startsWith("WARN com.zextras")),
                  "the open source session must be reported as a warning"));
    }
  }

  @Nested
  @DisplayName("OPS-10/ID-7: an already populated target")
  class PopulatedTarget {

    @Test
    @DisplayName(
        "OPS-10/ID-7: a chats.message row already exists before the import -> import anyway")
    void nonEmptyTargetStillImports() {
      MongooseImportDatabase database = database();
      String target = database.newTargetDatabase();
      Flyway flyway =
          Flyway.configure()
              .locations("classpath:migration/ce")
              .schemas("chats")
              .dataSource(database.jdbcUrl(target), database.user(), database.password())
              .validateMigrationNaming(true)
              .lockRetryCount(-1)
              .group(true)
              .javaMigrations(new V1_4_2__backfill_attachment_ids(database.sourceCredentials()))
              .target("2.0.0")
              .load();
      flyway
          .getConfigurationExtension(PostgreSQLConfigurationExtension.class)
          .setTransactionalLock(false);
      flyway.migrate();
      database.execute(
          target,
          "INSERT INTO chats.message (id, room_id, sender_id, text) SELECT '"
              + PREEXISTING_MESSAGE_ID
              + "', id, 'preexisting-sender', '"
              + PREEXISTING_MESSAGE_TEXT
              + "' FROM chats.room ORDER BY id LIMIT 1");
      ImportedChats chats = new ImportedChats(target);
      assertEquals(1, chats.count("SELECT count(*) FROM chats.message"));
      assertEquals(1, successRows(chats, "2.0.0"));
      assertEquals(
          0,
          chats.count("SELECT count(*) FROM chats.flyway_schema_history WHERE version = '2.0.1'"));
      ImportRun run = MongooseImport.run(target, database.sourceCredentials());

      assertAll(
          () -> assertImportedLikeTheDump(run, 1),
          () ->
              assertEquals(
                  PREEXISTING_MESSAGE_TEXT,
                  run.chats()
                      .query(
                          "SELECT text FROM chats.message WHERE id = ?",
                          rs -> rs.getString(1),
                          PREEXISTING_MESSAGE_ID)
                      .get(0)),
          () -> assertEquals(1, successRows(run.chats(), "2.0.0")));
    }
  }

  @Nested
  @DisplayName("ROOM-2: archives without any paired WSC room")
  class UnpairedDatabase {

    @Test
    @DisplayName(
        "ROOM-2: WSC has no rooms while archives exist -> success, no room history, users kept")
    void unpairedWscDatabaseImportsNoRoomHistory() {
      MongooseImportDatabase database = database();
      String target = database.newTargetDatabase();
      database.execute(target, "TRUNCATE chats.room CASCADE");
      ImportedChats chats = new ImportedChats(target);
      assertEquals(0, chats.count("SELECT count(*) FROM chats.room"));
      assertEquals(0, chats.count("SELECT count(*) FROM chats.subscription"));
      assertSourceHoldsArchives(MongooseImportDatabase.SOURCE_DATABASE);
      long lastSeen = dumpCount("SELECT count(*) FROM chats.user_last_seen");

      ImportRun run = MongooseImport.run(target, database.sourceCredentials());

      ImportedChats imported = run.chats();
      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () -> assertEquals(1, successRows(imported, "2.0.1")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.message")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.system_event")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.message_reaction")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.message_pin")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.read_marker")),
          () -> assertEquals(0, imported.count("SELECT count(*) FROM chats.user_contact")),
          () -> assertEquals(lastSeen, imported.count("SELECT count(*) FROM chats.user_last_seen")),
          () -> assertNoDumpTextInOutput(run));
    }
  }

  @Nested
  @DisplayName("ROOM-V1/TIME-2/APP-2: no zone check, times follow the JVM zone")
  class ForeignJvmZone {

    @Test
    @DisplayName("ROOM-V1/TIME-2/APP-2: JVM in Europe/Rome -> import, wall clock in Europe/Rome")
    void foreignJvmZoneImportsInThatZone() {
      MongooseImportDatabase database = database();
      ZoneId rome = ZoneId.of("Europe/Rome");

      ImportRun run =
          MongooseImport.run(database.newTargetDatabase(), database.sourceCredentials(), rome);

      assertAll(
          () -> assertImportedLikeTheDump(run, 0),
          () ->
              assertEquals(
                  ExpectedTimes.mamMillis(ZONE_ROOM_MEMBER_ADDED_MAM, rome),
                  run.chats()
                      .systemEvent(ZONE_PARITY_ROOM, ZONE_ROOM_MEMBER_ADDED_MAM)
                      .orElseThrow()
                      .createdAt()));
    }
  }

  @Nested
  @DisplayName("OPS-9: logs never carry message text")
  class Logging {

    @Test
    @DisplayName("OPS-9: a successful import on a pin-less source logs, without any message text")
    void successfulRunLogsNoMessageText() {
      ImportRun run = pinlessRun();

      assertAll(
          () -> assertTrue(run.succeeded(), run::failureText),
          () ->
              assertTrue(
                  run.logLines().stream().anyMatch(line -> line.contains("com.zextras")),
                  "the migration must log"),
          () -> assertNoDumpTextInOutput(run));
    }
  }

  @Nested
  @DisplayName("OPS-13: a migrated target is never imported again")
  class Rerun {

    @Test
    @DisplayName("OPS-13: a second migrate on an already migrated target changes nothing")
    void secondMigrateIsNoOp() {
      MongooseImportDatabase database = database();
      String target = database.newTargetDatabase();
      ImportRun first = MongooseImport.run(target, database.sourceCredentials());
      assertTrue(first.succeeded(), first::failureText);
      ImportedChats chats = first.chats();
      long messages = chats.count("SELECT count(*) FROM chats.message");
      assertTrue(messages > 0);
      List<Long> before = counts(chats);
      long historyBefore = chats.count("SELECT count(*) FROM chats.flyway_schema_history");

      ImportRun second = MongooseImport.run(target, database.sourceCredentials());

      assertAll(
          () -> assertTrue(second.succeeded(), second::failureText),
          () -> assertEquals(before, counts(chats)),
          () ->
              assertEquals(
                  historyBefore, chats.count("SELECT count(*) FROM chats.flyway_schema_history")),
          () -> assertEquals(1, successRows(chats, "2.0.1")),
          () -> assertNoDumpTextInOutput(second));
    }

    private List<Long> counts(ImportedChats chats) {
      List<Long> counts = new ArrayList<>();
      for (String table : IMPORTED_TABLES) {
        counts.add(chats.count("SELECT count(*) FROM " + table));
      }
      counts.add(chats.count("SELECT count(*) FROM chats.room"));
      counts.add(chats.count("SELECT count(*) FROM chats.subscription"));
      counts.add(chats.count("SELECT count(*) FROM chats.file_metadata"));
      return counts;
    }
  }
}
