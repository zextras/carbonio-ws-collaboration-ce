// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("V2_0_1 import: only pin_message.pinned_at depends on the JVM zone")
class ZoneIndependenceIT {

  private static final ZoneId LEGACY_COLUMNS_ZONE = ZoneId.of("UTC");
  private static final String PIN_TABLE = "message_pin";

  private static ImportedChats utc;
  private static ImportedChats rome;

  @BeforeAll
  static void importTwice() {
    MongooseImportDatabase database = MongooseImportDatabase.get();
    ImportRun inUtc =
        MongooseImport.run(
            database.newTargetDatabase(),
            database.sourceCredentials(),
            ZoneId.of("UTC"),
            LEGACY_COLUMNS_ZONE);
    ImportRun inRome =
        MongooseImport.run(
            database.newTargetDatabase(),
            database.sourceCredentials(),
            ZoneId.of("Europe/Rome"),
            LEGACY_COLUMNS_ZONE);
    assertTrue(inUtc.succeeded(), inUtc::failureText);
    assertTrue(inRome.succeeded(), inRome::failureText);
    utc = inUtc.chats();
    rome = inRome.chats();
  }

  private static List<String> tables() {
    return utc.query(
        "SELECT table_name FROM information_schema.tables WHERE table_schema = 'chats'"
            + " AND table_type = 'BASE TABLE' AND table_name <> 'flyway_schema_history'"
            + " ORDER BY table_name",
        rs -> rs.getString(1));
  }

  private static List<String> rows(ImportedChats chats, String table) {
    return chats.query(
        "SELECT t::text FROM chats." + table + " t ORDER BY 1", rs -> rs.getString(1));
  }

  @Test
  @DisplayName("TIME-2: every imported table is identical under Europe/Rome and UTC")
  void everyTableExceptPinsIsIdentical() {
    List<String> tables = tables();
    long imported = 0;
    for (String table : tables) {
      if (PIN_TABLE.equals(table)) {
        continue;
      }
      List<String> inUtc = rows(utc, table);
      imported += inUtc.size();
      assertEquals(inUtc, rows(rome, table), "table " + table);
    }

    assertTrue(tables.contains("message") && tables.contains("system_event"));
    assertTrue(imported > 1000, "rows compared: " + imported);
  }

  @Test
  @DisplayName("TIME-2: pins differ only in pinned_at, and only for pins without an archived row")
  void pinsDifferOnlyWhenNoArchivedRowExists() {
    String sql =
        "SELECT room_id || '|' || message_id || '|' || pinned_by, pinned_at,"
            + " EXISTS (SELECT 1 FROM chats.system_event e WHERE e.room_id = p.room_id"
            + " AND e.type = 'MESSAGE_PINNED' AND e.actor_id = p.pinned_by"
            + " AND e.content::jsonb ->> 'messageId' = p.message_id)"
            + " FROM chats.message_pin p ORDER BY 1";
    Map<String, OffsetDateTime> inUtc = new TreeMap<>();
    Map<String, OffsetDateTime> inRome = new TreeMap<>();
    Map<String, Boolean> archived = new TreeMap<>();
    utc.query(
        sql,
        rs -> {
          inUtc.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class));
          archived.put(rs.getString(1), rs.getBoolean(3));
          return 0;
        });
    rome.query(
        sql,
        rs -> {
          inRome.put(rs.getString(1), rs.getObject(2, OffsetDateTime.class));
          return 0;
        });

    assertTrue(inUtc.size() > 0);
    assertEquals(inUtc.keySet(), inRome.keySet());
    for (String pin : inUtc.keySet()) {
      OffsetDateTime a = inUtc.get(pin);
      OffsetDateTime b = inRome.get(pin);
      if (archived.get(pin)) {
        assertEquals(a.toInstant(), b.toInstant(), "pin with an archived row " + pin);
      } else if (!a.toInstant().equals(b.toInstant())) {
        long hours = Duration.between(b, a).toHours();
        assertTrue(hours == 1 || hours == 2, "zone offset applied to " + pin + ": " + hours);
      }
    }
  }
}
