// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RoomRows implements AutoCloseable {

  public static final int NOT_IMPORTED = 0;
  public static final int MESSAGE = 1;
  public static final int EVENT = 2;
  public static final int EDIT = 3;

  public record Row(
      long mam,
      int outcome,
      String itemId,
      LocalDateTime itemTime,
      LocalDateTime rowTime,
      boolean deleted,
      String senderId,
      long targetMam) {

    public static Row notImported(long mam, String senderId, LocalDateTime rowTime) {
      return new Row(mam, NOT_IMPORTED, null, null, rowTime, false, senderId, 0);
    }

    public static Row message(
        long mam,
        String senderId,
        String itemId,
        LocalDateTime itemTime,
        LocalDateTime rowTime,
        boolean deleted) {
      return new Row(mam, MESSAGE, itemId, itemTime, rowTime, deleted, senderId, 0);
    }

    public static Row event(
        long mam, String senderId, String itemId, LocalDateTime itemTime, LocalDateTime rowTime) {
      return new Row(mam, EVENT, itemId, itemTime, rowTime, false, senderId, 0);
    }

    public static Row edit(long mam, String senderId, long targetMam, LocalDateTime rowTime) {
      return new Row(mam, EDIT, null, null, rowTime, false, senderId, targetMam);
    }
  }

  private record Pending(String clientId, Row row) {}

  private static final int BUFFER_ROWS = 500;
  private static final int BUFFER_CHARS = 8_000_000;

  private static final String COLUMNS =
      "mam, outcome, item_id, item_time, row_time, deleted, sender_id, target_mam";

  private final Connection target;
  private final CopyRows insert =
      new CopyRows(
          "mongoose_room_rows",
          "mam, client_id, outcome, target_mam, sender_id, item_id, item_time, row_time, deleted");
  private final PreparedStatement selectByMam;
  private final PreparedStatement selectByClient;
  private final PreparedStatement selectLastBySender;
  private final List<Pending> buffer = new ArrayList<>();
  private final Map<Long, Row> bufferByMam = new HashMap<>();
  private int bufferChars;
  private int recorded;

  public RoomRows(Connection target) throws SQLException {
    this.target = target;
    try (Statement st = target.createStatement()) {
      st.execute(
          "CREATE TEMP TABLE mongoose_room_rows (mam BIGINT PRIMARY KEY, client_id TEXT,"
              + " outcome SMALLINT NOT NULL, target_mam BIGINT, sender_id TEXT, item_id TEXT,"
              + " item_time TIMESTAMP, row_time TIMESTAMP NOT NULL,"
              + " deleted BOOLEAN NOT NULL DEFAULT FALSE) ON COMMIT DROP");
      st.execute(
          "CREATE INDEX mongoose_room_rows_client_idx ON mongoose_room_rows"
              + " (hashtext(client_id), mam)");
    }
    this.selectByMam =
        target.prepareStatement("SELECT " + COLUMNS + " FROM mongoose_room_rows WHERE mam = ?");
    this.selectByClient =
        target.prepareStatement(
            "SELECT "
                + COLUMNS
                + " FROM mongoose_room_rows WHERE hashtext(client_id) = hashtext(?)"
                + " AND client_id = ? AND (mam >> 8) <= ? ORDER BY mam DESC LIMIT 1");
    this.selectLastBySender =
        target.prepareStatement(
            "SELECT sender_id, max(COALESCE(item_time, row_time)) FROM mongoose_room_rows"
                + " WHERE sender_id IS NOT NULL GROUP BY sender_id");
  }

  public void reset() throws SQLException {
    clearBuffer();
    recorded = 0;
    try (Statement st = target.createStatement()) {
      st.execute("TRUNCATE mongoose_room_rows");
    }
  }

  public void record(String clientId, Row row) throws SQLException {
    buffer.add(new Pending(clientId, row));
    bufferByMam.put(row.mam(), row);
    recorded++;
    if (clientId != null) {
      bufferChars += clientId.length();
    }
    if (buffer.size() >= BUFFER_ROWS || bufferChars >= BUFFER_CHARS) {
      flush();
    }
  }

  public int recorded() {
    return recorded;
  }

  public void flush() throws SQLException {
    if (buffer.isEmpty()) {
      return;
    }
    for (Pending p : buffer) {
      Row r = p.row();
      insert
          .number(r.mam())
          .text(p.clientId())
          .number(r.outcome())
          .nullableNumber(r.targetMam())
          .text(r.senderId())
          .text(r.itemId())
          .time(r.itemTime())
          .time(r.rowTime())
          .bool(r.deleted())
          .endRow();
    }
    insert.flush(target);
    clearBuffer();
  }

  public void analyze() throws SQLException {
    flush();
    try (Statement st = target.createStatement()) {
      st.execute("ANALYZE mongoose_room_rows");
    }
  }

  public Row resolveStanza(long mam) throws SQLException {
    return followAlias(byMam(mam));
  }

  public Row latestByClientIdUpTo(String clientId, long epochMicros) throws SQLException {
    flush();
    selectByClient.setString(1, clientId);
    selectByClient.setString(2, clientId);
    selectByClient.setLong(3, epochMicros);
    return single(selectByClient);
  }

  public Map<String, LocalDateTime> lastRowTimeBySender() throws SQLException {
    flush();
    Map<String, LocalDateTime> times = new HashMap<>();
    try (ResultSet rs = selectLastBySender.executeQuery()) {
      while (rs.next()) {
        times.put(rs.getString(1), rs.getObject(2, LocalDateTime.class));
      }
    }
    return times;
  }

  @Override
  public void close() throws SQLException {
    insert.clear();
    selectByMam.close();
    selectByClient.close();
    selectLastBySender.close();
  }

  private Row followAlias(Row row) throws SQLException {
    if (row == null || row.outcome() != EDIT) {
      return row;
    }
    Row original = byMam(row.targetMam());
    return original == null ? row : original;
  }

  private void clearBuffer() {
    buffer.clear();
    bufferByMam.clear();
    bufferChars = 0;
  }

  private Row byMam(long mam) throws SQLException {
    Row buffered = bufferByMam.get(mam);
    if (buffered != null) {
      return buffered;
    }
    selectByMam.setLong(1, mam);
    return single(selectByMam);
  }

  private static Row single(PreparedStatement ps) throws SQLException {
    try (ResultSet rs = ps.executeQuery()) {
      if (!rs.next()) {
        return null;
      }
      return new Row(
          rs.getLong(1),
          rs.getInt(2),
          rs.getString(3),
          rs.getObject(4, LocalDateTime.class),
          rs.getObject(5, LocalDateTime.class),
          rs.getBoolean(6),
          rs.getString(7),
          rs.getLong(8));
    }
  }
}
