// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
      Instant itemTime,
      Instant rowTime,
      boolean deleted,
      String senderId,
      long targetMam) {

    public static Row notImported(long mam, String senderId, Instant rowTime) {
      return new Row(mam, NOT_IMPORTED, null, null, rowTime, false, senderId, 0);
    }

    public static Row message(
        long mam,
        String senderId,
        String itemId,
        Instant itemTime,
        Instant rowTime,
        boolean deleted) {
      return new Row(mam, MESSAGE, itemId, itemTime, rowTime, deleted, senderId, 0);
    }

    public static Row event(
        long mam, String senderId, String itemId, Instant itemTime, Instant rowTime) {
      return new Row(mam, EVENT, itemId, itemTime, rowTime, false, senderId, 0);
    }

    public static Row edit(long mam, String senderId, long targetMam, Instant rowTime) {
      return new Row(mam, EDIT, null, null, rowTime, false, senderId, targetMam);
    }
  }

  interface Ids {
    String message(long mam);
  }

  interface Flush {
    void run() throws SQLException;
  }

  private static final class Tracker {
    private final long upToMicros;
    private Row latest;

    private Tracker(long upToMicros) {
      this.upToMicros = upToMicros;
    }
  }

  private static final int RECENT_ROWS = 8192;

  private static final String STORED =
      "SELECT m.id, m.deleted FROM chats.message m WHERE m.id = ?"
          + " UNION ALL SELECT m.id, m.deleted FROM mongoose_edits e"
          + " JOIN chats.message m ON m.id = e.target_id WHERE e.mam = ? AND e.room_id = ? LIMIT 1";

  private static final String STORED_MESSAGE =
      "SELECT m.id, m.deleted FROM chats.message m WHERE m.id = ?";

  private final PreparedStatement stored;
  private final PreparedStatement storedMessage;
  private final Map<Long, Row> recent =
      new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Row> eldest) {
          return size() > RECENT_ROWS;
        }
      };
  private final Map<String, Instant> lastBySender = new HashMap<>();
  private final Map<String, List<Tracker>> trackers = new HashMap<>();
  private String roomId;
  private Ids ids;
  private Flush pending;
  private int recorded;

  public RoomRows(Connection target) throws SQLException {
    this.stored = target.prepareStatement(STORED);
    this.storedMessage = target.prepareStatement(STORED_MESSAGE);
  }

  void beginRoom(
      String roomId, Ids ids, Flush pending, Map<String, MarkerTranscriber.Marker> markers) {
    this.roomId = roomId;
    this.ids = ids;
    this.pending = pending;
    recent.clear();
    lastBySender.clear();
    trackers.clear();
    recorded = 0;
    for (MarkerTranscriber.Marker marker : markers.values()) {
      if (!marker.msgId().isEmpty()) {
        trackers
            .computeIfAbsent(marker.msgId(), k -> new ArrayList<>())
            .add(new Tracker(marker.timestamp()));
      }
    }
  }

  public void record(String clientId, Row row) {
    recent.put(row.mam(), row);
    recorded++;
    if (row.senderId() != null) {
      Instant t = MamTime.roundToMicros(row.itemTime() != null ? row.itemTime() : row.rowTime());
      lastBySender.merge(row.senderId(), t, (a, b) -> a.isAfter(b) ? a : b);
    }
    if (clientId != null && !trackers.isEmpty()) {
      List<Tracker> list = trackers.get(clientId);
      if (list != null) {
        for (Tracker tracker : list) {
          if ((row.mam() >> 8) <= tracker.upToMicros) {
            tracker.latest = row;
          }
        }
      }
    }
  }

  public int recorded() {
    return recorded;
  }

  public Row resolveStanza(long mam) throws SQLException {
    Row row = recent.get(mam);
    if (row == null) {
      return stored(mam);
    }
    if (row.outcome() != EDIT) {
      return row;
    }
    Row original = recent.get(row.targetMam());
    if (original == null) {
      original = storedMessage(row.targetMam());
    }
    return original == null ? row : original;
  }

  public Row latestByClientIdUpTo(String clientId, long epochMicros) {
    List<Tracker> list = trackers.get(clientId);
    if (list != null) {
      for (Tracker tracker : list) {
        if (tracker.upToMicros == epochMicros) {
          return tracker.latest;
        }
      }
    }
    throw new IllegalStateException("read marker not registered at room start");
  }

  public Map<String, Instant> lastRowTimeBySender() {
    return new HashMap<>(lastBySender);
  }

  @Override
  public void close() throws SQLException {
    stored.close();
    storedMessage.close();
  }

  private Row stored(long mam) throws SQLException {
    pending.run();
    stored.setString(1, ids.message(mam));
    stored.setLong(2, mam);
    stored.setString(3, roomId);
    return single(stored, mam);
  }

  private Row storedMessage(long mam) throws SQLException {
    pending.run();
    storedMessage.setString(1, ids.message(mam));
    return single(storedMessage, mam);
  }

  private static Row single(PreparedStatement ps, long mam) throws SQLException {
    try (ResultSet rs = ps.executeQuery()) {
      if (!rs.next()) {
        return null;
      }
      return new Row(mam, MESSAGE, rs.getString(1), null, null, rs.getBoolean(2), null, 0);
    }
  }
}
