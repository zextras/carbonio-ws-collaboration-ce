// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;

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

  void run() throws SQLException {
    ZoneId zone = ZoneId.systemDefault();
    List<TargetRoom> rooms = TargetRooms.load(target);
    FileLinker files = new FileLinker(target);
    RoomImporter.Counts imported;
    try (MarkerTranscriber markers = new MarkerTranscriber(source, target, zone);
        PinImporter pins = new PinImporter(source, target);
        RoomReader reader = new RoomReader(source, pins, markers);
        RoomImporter importer = new RoomImporter(target, ids, zone, files, pins, markers)) {
      ImportPipeline.run(rooms, reader, importer);
      imported = importer.counts();
    }
    UserTables.Counts users = UserTables.run(source.connection(), source.hasLast(), target, zone);
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
            + users.lastSeen()
            + " contacts="
            + users.contacts());
  }
}
