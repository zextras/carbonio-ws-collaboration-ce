// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MongooseImporter {

  private final MongooseSource.Ready source;
  private final Connection target;
  private final LegacyIds ids;

  MongooseImporter(MongooseSource.Ready source, Connection target, LegacyIds ids) {
    this.source = source;
    this.target = target;
    this.ids = ids;
  }

  public static void migrate(
      MessageDispatcherCredentials credentials, Connection target, byte[] runKey) {
    switch (MongooseSource.open(credentials)) {
      case MongooseSource.NoOp noOp ->
          ChatsLogger.info(MongooseImporter.class, "MongooseIM import skipped: " + noOp.reason());
      case MongooseSource.Ready ready -> {
        try {
          new MongooseImporter(ready, target, new LegacyIds(runKey)).run();
        } catch (SQLException e) {
          throw new IllegalStateException(
              "MongooseIM import failed (SQLState " + e.getSQLState() + ")");
        } finally {
          MongooseSource.close(ready.connection());
        }
      }
    }
  }

  private static final String ORPHAN_ARCHIVES =
      "SELECT s.user_name FROM mam_server_user s WHERE s.server = 'muclight.carbonio'"
          + " AND EXISTS (SELECT 1 FROM mam_muc_message m WHERE m.room_id = s.id)";

  void run() throws SQLException {
    ImportContext context = ImportContext.start();
    ZoneId pinZone = ZoneId.systemDefault();
    ChatsLogger.info(
        MongooseImporter.class, "MongooseIM pin_message.pinned_at read in zone " + pinZone);
    List<TargetRoom> rooms = TargetRooms.load(target);
    Set<String> roomIds = new HashSet<>();
    for (TargetRoom room : rooms) {
      roomIds.add(room.id());
    }
    FileLinker files = new FileLinker(target);
    RoomImporter.Counts imported;
    int unarchivedPins;
    try (MarkerTranscriber markers = new MarkerTranscriber(source, target, context);
        PinImporter pins = new PinImporter(source, target, pinZone, context);
        RoomReader reader = new RoomReader(source, pins, markers, context);
        RoomImporter importer =
            new RoomImporter(target, ids, files, pins, markers, context, roomIds)) {
      ImportPipeline.run(rooms, reader, importer, context);
      importer.finish();
      imported = importer.counts();
      unarchivedPins = pins.unarchived();
    }
    if (unarchivedPins > 0) {
      ChatsLogger.warn(
          MongooseImporter.class,
          "MongooseIM pins without an archived pin line: "
              + unarchivedPins
              + ", pin_message.pinned_at read in zone "
              + pinZone);
    }
    UserTables.Counts users =
        UserTables.run(source.connection(), source.hasLast(), target, context);
    countOrphanArchives(roomIds, context);
    ChatsLogger.info(
        MongooseImporter.class,
        "MongooseIM import completed: rooms="
            + imported.rooms()
            + " messages="
            + imported.messages()
            + " events="
            + imported.events()
            + " edits="
            + imported.edits()
            + " reactions="
            + imported.reactions()
            + " lastSeen="
            + users.lastSeen());
    String summary = context.summary();
    if (!summary.isEmpty()) {
      ChatsLogger.warn(
          MongooseImporter.class, "MongooseIM import skipped or adjusted source data: " + summary);
    }
    if (context.rowsRead() > 0
        && imported.messages() == 0
        && imported.events() <= imported.rooms()) {
      ChatsLogger.warn(
          MongooseImporter.class,
          "MongooseIM import imported no messages although the source holds "
              + context.rowsRead()
              + " rows: check the skip reasons above");
    }
  }

  private void countOrphanArchives(Set<String> roomIds, ImportContext context) throws SQLException {
    try (Statement statement = source.connection().createStatement();
        ResultSet rs = statement.executeQuery(ORPHAN_ARCHIVES)) {
      while (rs.next()) {
        String room = rs.getString(1);
        if (!roomIds.contains(room)) {
          context.count(SkipReason.ORPHAN_ARCHIVE, String.valueOf(room), 0);
        }
      }
    }
  }
}
