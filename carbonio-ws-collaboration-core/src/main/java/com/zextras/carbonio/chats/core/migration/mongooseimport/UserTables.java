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
import java.time.ZoneOffset;

public final class UserTables {

  public record Counts(int lastSeen) {}

  private static final String INSERT_LAST_SEEN =
      "INSERT INTO chats.user_last_seen (user_id, last_seen_at) VALUES (?, ?)"
          + " ON CONFLICT (user_id) DO NOTHING";

  private static final int PAGE_SIZE = 1000;

  private UserTables() {}

  public static Counts run(Connection source, boolean hasLast, Connection target)
      throws SQLException {
    int lastSeen = hasLast ? importLastSeen(source, target) : 0;
    return new Counts(lastSeen);
  }

  private static int sum(int[] counts) {
    int total = 0;
    for (int c : counts) {
      total += Math.max(c, 0);
    }
    return total;
  }

  private static int importLastSeen(Connection source, Connection target) throws SQLException {
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
          insert.setObject(2, Instant.ofEpochSecond(rows.getLong(2)).atOffset(ZoneOffset.UTC));
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
