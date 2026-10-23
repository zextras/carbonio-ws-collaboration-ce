// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import of the real MongooseIM dump: overall outcome")
class ImportOutcomeIT {

  private static final Pattern CANONICAL_V4 =
      Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
  private static final Pattern ROOT_ID = Pattern.compile("^<message[^>]* id='([^']*)'");

  private static ImportRun run;
  private static ImportedChats chats;

  @BeforeAll
  static void importDump() {
    run = MongooseImport.ofDump();
    chats = run.chats();
  }

  @Test
  @DisplayName("The import commits and Flyway records every migration up to the latest as success")
  void importCommitsAndReachesLatestVersion() {
    assertTrue(run.succeeded(), run::failureText);
    assertEquals(
        0, chats.count("SELECT count(*) FROM chats.flyway_schema_history WHERE NOT success"));
    assertEquals(
        1,
        chats.count(
            "SELECT count(*) FROM chats.flyway_schema_history WHERE version = '2.0.1' AND"
                + " success"));
  }

  @Test
  @DisplayName("V2_0_2 installs pg_trgm in the chats schema when the database does not have it")
  void trigramExtensionIsInstalledInChatsSchema() {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String target = database.newTargetDatabase();
    database.execute(target, "DROP EXTENSION pg_trgm");
    ImportRun pristine = MongooseImport.run(target, database.sourceCredentials());

    assertAll(
        () -> assertTrue(pristine.succeeded(), pristine::failureText),
        () ->
            assertEquals(
                List.of("chats"),
                pristine
                    .chats()
                    .query(
                        "SELECT extnamespace::regnamespace::text FROM pg_extension"
                            + " WHERE extname = 'pg_trgm'",
                        rs -> rs.getString(1))));
  }

  @Test
  @DisplayName("The legacy FILE_METADATA message_id and stanza_id values survive the upgrade")
  void legacyFileColumnsSurviveForRollback() {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String target = database.newTargetDatabase();
    String legacy =
        "SELECT id || '=' || coalesce(message_id, '') || '|' || coalesce(stanza_id, '')"
            + " FROM chats.file_metadata ORDER BY id";
    ImportedChats release = new ImportedChats(target);
    List<String> before = release.query(legacy, rs -> rs.getString(1));
    assertTrue(release.count("SELECT count(message_id) FROM chats.file_metadata") > 0);
    assertTrue(release.count("SELECT count(stanza_id) FROM chats.file_metadata") > 0);

    ImportRun upgraded = MongooseImport.run(target, database.sourceCredentials());

    assertAll(
        () -> assertTrue(upgraded.succeeded(), upgraded::failureText),
        () -> assertEquals(before, upgraded.chats().query(legacy, rs -> rs.getString(1))));
  }

  @Test
  @DisplayName("Every imported message and system event id is a canonical lowercase UUIDv4")
  void everyImportedIdIsCanonicalUuidV4() {
    List<String> ids =
        chats.query(
            "SELECT id FROM chats.message UNION ALL SELECT id FROM chats.system_event",
            rs -> rs.getString(1));

    assertFalse(ids.isEmpty());
    assertTrue(
        ids.stream().allMatch(id -> CANONICAL_V4.matcher(id).matches()),
        () ->
            "non canonical ids: "
                + ids.stream().filter(id -> !CANONICAL_V4.matcher(id).matches()).limit(5).toList());
  }

  @Test
  @DisplayName("No client-generated MongooseIM id is reused as a message id")
  void clientIdsAreNeverReusedAsMessageIds() throws SQLException {
    Set<String> clientIds = sourceClientIds();
    List<String> ids = chats.query("SELECT id FROM chats.message", rs -> rs.getString(1));

    assertFalse(ids.isEmpty());
    assertTrue(ids.stream().noneMatch(clientIds::contains));
  }

  @Test
  @DisplayName("No message text reaches the migration logs or the failure output")
  void messageTextNeverReachesLogs() {
    List<String> bodies =
        chats.query(
            "SELECT text FROM chats.message WHERE char_length(text) >= 24 ORDER BY id LIMIT 200",
            rs -> rs.getString(1));
    String output = run.allOutput();

    assertFalse(bodies.isEmpty());
    assertTrue(bodies.stream().noneMatch(body -> output.contains(body.substring(0, 24))));
  }

  @Test
  @DisplayName("Messages are imported only into rooms that exist in WSC and still have members")
  void messagesLandOnlyInLiveWscRooms() {
    assertTrue(chats.count("SELECT count(*) FROM chats.message") > 0);
    assertEquals(
        0,
        chats.count(
            "SELECT count(*) FROM chats.message m WHERE NOT EXISTS (SELECT 1 FROM"
                + " chats.subscription s WHERE s.room_id = m.room_id)"));
  }

  private static Set<String> sourceClientIds() throws SQLException {
    Set<String> ids = new HashSet<>();
    try (Connection connection =
            MongooseImportDatabase.get().connect(MongooseImportDatabase.SOURCE_DATABASE);
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery("SELECT message FROM mam_muc_message")) {
      while (rs.next()) {
        Matcher matcher = ROOT_ID.matcher(new String(rs.getBytes(1), StandardCharsets.UTF_8));
        if (matcher.find()) {
          ids.add(matcher.group(1).toLowerCase());
        }
      }
    }
    return ids;
  }
}
