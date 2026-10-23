// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class UserTables {

  public record Counts(int lastSeen, int contacts) {}

  private static final String INSERT_LAST_SEEN =
      "INSERT INTO chats.user_last_seen (user_id, last_seen_at) VALUES (?, ?)"
          + " ON CONFLICT (user_id) DO NOTHING";

  private static final String INSERT_CONTACTS =
      "INSERT INTO chats.user_contact (user_id, contact_id, created_at)"
          + " SELECT a.user_id, b.user_id, r.created_at FROM chats.room r"
          + " JOIN chats.subscription a ON a.room_id = r.id"
          + " JOIN chats.subscription b ON b.room_id = r.id AND b.user_id <> a.user_id"
          + " WHERE r.type = 'ONE_TO_ONE' ON CONFLICT DO NOTHING";

  private static final int PAGE_SIZE = 1000;

  private UserTables() {}

  public static Counts run(Connection source, boolean hasLast, Connection target, ZoneId zone)
      throws SQLException {
    int lastSeen = hasLast ? importLastSeen(source, target, zone) : 0;
    int contacts;
    try (Statement st = target.createStatement()) {
      contacts = st.executeUpdate(INSERT_CONTACTS);
    }
    return new Counts(lastSeen, contacts);
  }

  private static int sum(int[] counts) {
    int total = 0;
    for (int c : counts) {
      total += Math.max(c, 0);
    }
    return total;
  }

  private static int importLastSeen(Connection source, Connection target, ZoneId zone)
      throws SQLException {
    int inserted = 0;
    int pending = 0;
    try (Statement read = source.createStatement();
        PreparedStatement insert = target.prepareStatement(INSERT_LAST_SEEN)) {
      read.setFetchSize(PAGE_SIZE);
      try (ResultSet rows = read.executeQuery("SELECT username, seconds FROM last")) {
        while (rows.next()) {
          String user = rows.getString(1);
          if (!CanonicalUuid.matches(user)) {
            continue;
          }
          insert.setString(1, user);
          insert.setObject(
              2, LocalDateTime.ofInstant(Instant.ofEpochSecond(rows.getLong(2)), zone));
          insert.addBatch();
          if (++pending == PAGE_SIZE) {
            inserted += sum(insert.executeBatch());
            pending = 0;
          }
        }
      }
      if (pending > 0) {
        inserted += sum(insert.executeBatch());
      }
    }
    return inserted;
  }
}
