// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TargetRooms {

  public record TargetRoom(
      String id, String type, LocalDateTime createdAt, String creatorId, String meetingId) {}

  private static final String ROOMS =
      "SELECT r.id, r.type, r.created_at, COALESCE((SELECT s.user_id FROM chats.subscription s"
          + " WHERE s.room_id = r.id AND s.owner ORDER BY s.joined_at, s.user_id LIMIT 1),"
          + " (SELECT s.user_id FROM chats.subscription s WHERE s.room_id = r.id"
          + " ORDER BY s.joined_at, s.user_id LIMIT 1)),"
          + " COALESCE(r.meeting_id, (SELECT m.id FROM chats.meeting m WHERE m.room_id = r.id"
          + " ORDER BY m.created_at, m.id LIMIT 1))"
          + " FROM chats.room r"
          + " WHERE EXISTS (SELECT 1 FROM chats.subscription s WHERE s.room_id = r.id)"
          + " ORDER BY r.id";

  private TargetRooms() {}

  public static List<TargetRoom> load(Connection target) throws SQLException {
    List<TargetRoom> rooms = new ArrayList<>();
    try (PreparedStatement query = target.prepareStatement(ROOMS);
        ResultSet rows = query.executeQuery()) {
      while (rows.next()) {
        rooms.add(
            new TargetRoom(
                rows.getString(1),
                rows.getString(2),
                rows.getObject(3, LocalDateTime.class),
                rows.getString(4),
                rows.getString(5)));
      }
    }
    return rooms;
  }
}
