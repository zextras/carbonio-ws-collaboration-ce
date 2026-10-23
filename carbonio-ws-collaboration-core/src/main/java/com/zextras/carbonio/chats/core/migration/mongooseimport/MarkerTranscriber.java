// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.migration.mongooseimport.RoomRows.Row;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MarkerTranscriber implements AutoCloseable {

  public record Marker(String msgId, long timestamp) {}

  private static final String SELECT =
      "SELECT luser, msg_id, timestamp FROM smart_markers WHERE to_jid = ? AND type = 'D'"
          + " ORDER BY timestamp, msg_id";

  private static final String INSERT =
      "INSERT INTO chats.read_marker (user_id, room_id, item_created_at) VALUES (?, ?, ?)";

  private final ZoneId zone;
  private final PreparedStatement select;
  private final PreparedStatement insert;

  public MarkerTranscriber(MongooseSource.Ready source, Connection target, ZoneId zone)
      throws SQLException {
    this.zone = zone;
    this.select = source.hasMarkers() ? source.connection().prepareStatement(SELECT) : null;
    this.insert = target.prepareStatement(INSERT);
  }

  // MongooseIM zeroes a user's inbox unread on every row the user writes (writing implies having
  // read what came before); a displayed marker resets it only if it names the last inbox message.
  // Client markers can lag behind the user's own rows, so the read position is the later of both.
  public void endRoom(TargetRoom room, RoomRows rows, Map<String, Marker> markers)
      throws SQLException {
    Map<String, LocalDateTime> readUpTo = rows.lastRowTimeBySender();
    for (Map.Entry<String, Marker> entry : markers.entrySet()) {
      LocalDateTime marked = markedItemTime(entry.getValue(), rows);
      if (marked != null) {
        readUpTo.merge(entry.getKey(), marked, (a, b) -> a.isAfter(b) ? a : b);
      }
    }
    if (readUpTo.isEmpty()) {
      return;
    }
    for (Map.Entry<String, LocalDateTime> entry : readUpTo.entrySet()) {
      insert.setString(1, entry.getKey());
      insert.setString(2, room.id());
      insert.setObject(3, entry.getValue());
      insert.addBatch();
    }
    insert.executeBatch();
  }

  private LocalDateTime markedItemTime(Marker marker, RoomRows rows) throws SQLException {
    if (marker.msgId().isEmpty()) {
      return null;
    }
    Row row = rows.latestByClientIdUpTo(marker.msgId(), marker.timestamp());
    if (row == null || !MamTime.inRange(MamTime.micros(marker.timestamp(), zone))) {
      return null;
    }
    boolean imported = row.outcome() == RoomRows.MESSAGE || row.outcome() == RoomRows.EVENT;
    return imported ? row.itemTime() : row.rowTime();
  }

  public Map<String, Marker> read(TargetRoom room) throws SQLException {
    if (select == null) {
      return Map.of();
    }
    Map<String, Marker> markers = new LinkedHashMap<>();
    select.setString(1, room.id() + "@muclight.carbonio");
    try (ResultSet rs = select.executeQuery()) {
      while (rs.next()) {
        String user = rs.getString(1);
        if (!CanonicalUuid.matches(user)) {
          continue;
        }
        String msgId = rs.getString(2);
        markers.put(user, new Marker(msgId == null ? "" : msgId, rs.getLong(3)));
      }
    }
    return markers;
  }

  @Override
  public void close() throws SQLException {
    if (select != null) {
      select.close();
    }
    insert.close();
  }
}
