// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.testsupport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

/** Plain-JDBC fixtures, so test-support does not depend on core's entities. */
public final class Seed {

  private Seed() {}

  public static void clean(DataSource ds) {
    exec(ds, "TRUNCATE chats.room, chats.meeting CASCADE");
  }

  public static void room(DataSource ds, UUID roomId, String type, Map<UUID, Boolean> membersToOwner) {
    exec(ds,
        "INSERT INTO chats.room (id, name, type, created_at, updated_at) VALUES (?, 'room', ?, now(), now())",
        roomId.toString(), type);
    membersToOwner.forEach(
        (userId, owner) ->
            exec(ds,
                "INSERT INTO chats.subscription (user_id, room_id, owner, created_at, updated_at)"
                    + " VALUES (?, ?, ?, now(), now())",
                userId.toString(), roomId.toString(), owner));
  }

  public static void meeting(DataSource ds, UUID meetingId, UUID roomId, boolean active) {
    exec(ds,
        "INSERT INTO chats.meeting (id, room_id, name, meeting_type, active, started_at, created_at)"
            + " VALUES (?, ?, 'meeting', 'PERMANENT', ?, CASE WHEN ? THEN now() END, now())",
        meetingId.toString(), roomId.toString(), active, active);
    exec(ds, "UPDATE chats.room SET meeting_id = ? WHERE id = ?", meetingId.toString(), roomId.toString());
  }

  public static void participant(DataSource ds, UUID meetingId, UUID userId) {
    exec(ds,
        "INSERT INTO chats.participant (meeting_id, user_id, queue_id, created_at, updated_at)"
            + " VALUES (?, ?, ?, now(), now())",
        meetingId.toString(), userId.toString(), UUID.randomUUID().toString());
  }

  public static void videoServerMeeting(DataSource ds, UUID meetingId, String serverId) {
    exec(ds,
        "INSERT INTO chats.videoserver_meeting (meeting_id, server_id, connection_id, audio_handle_id,"
            + " video_handle_id, audio_room_id, video_room_id)"
            + " VALUES (?, ?, 'conn-1', 'h-audio', 'h-video', 'audio-room', 'video-room')",
        meetingId.toString(), serverId);
  }

  public static void exec(DataSource ds, String sql, Object... params) {
    try (Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      bind(ps, params);
      ps.execute();
    } catch (SQLException e) {
      throw new IllegalStateException("Seed failed: " + sql, e);
    }
  }

  public static long count(DataSource ds, String sql, Object... params) {
    try (Connection c = ds.getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      bind(ps, params);
      try (var rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Count failed: " + sql, e);
    }
  }

  private static void bind(PreparedStatement ps, Object... params) throws SQLException {
    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        ps.setNull(i + 1, java.sql.Types.VARCHAR);
      } else {
        ps.setObject(i + 1, params[i]);
      }
    }
  }
}
