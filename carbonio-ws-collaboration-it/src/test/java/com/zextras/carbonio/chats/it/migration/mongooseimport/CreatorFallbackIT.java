// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zextras.carbonio.chats.it.migration.mongooseimport.ImportedChats.SystemEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import: ROOM_CREATED creator fallback for rooms without owners")
class CreatorFallbackIT {

  private static final String ROOM = "7e7d6c0a-efce-4852-b197-8bf33d267290";

  @Test
  @DisplayName("A room with members but no owner is imported with its earliest member as creator")
  void roomWithoutOwnersFallsBackToEarliestMember() throws Exception {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    String target = database.newTargetDatabase();
    String expected;
    try (Connection connection = database.connect(target);
        Statement statement = connection.createStatement()) {
      statement.execute(
          "UPDATE chats.subscription SET owner = false WHERE room_id = '" + ROOM + "'");
      try (ResultSet rs =
          statement.executeQuery(
              "SELECT user_id FROM chats.subscription WHERE room_id = '"
                  + ROOM
                  + "' ORDER BY joined_at, user_id LIMIT 1")) {
        assertTrue(rs.next());
        expected = rs.getString(1);
      }
    }

    ImportRun run = MongooseImport.run(target, database.sourceCredentials());

    assertTrue(run.succeeded(), run.failureText());
    SystemEvent created =
        run.chats().systemEvents(ROOM).stream()
            .filter(e -> "ROOM_CREATED".equals(e.type()))
            .findFirst()
            .orElseThrow();
    assertAll(
        () -> assertEquals(expected, created.actorId()),
        () -> assertEquals(expected, created.json().get("creatorId").asText()),
        () ->
            assertEquals(
                0,
                run.chats()
                    .count("SELECT count(*) FROM chats.system_event WHERE actor_id IS NULL")));
  }
}
