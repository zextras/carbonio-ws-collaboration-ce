// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.End;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Entry;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Item;
import com.zextras.carbonio.chats.core.migration.mongooseimport.ImportPipeline.Start;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

final class RoomReader implements AutoCloseable, ImportPipeline.Source {

  interface Sink {
    void put(Item item) throws InterruptedException;
  }

  private static final int PAGE_SIZE = 500;
  private static final int CLEAR_FETCH_SIZE = 4;
  private static final int INLINE_BYTES = 65_536;
  private static final long MAX_BODY_BYTES = 33_554_432L;

  private static final String BODY =
      "CASE WHEN octet_length(m.message) <= " + MAX_BODY_BYTES + " THEN m.message END";

  private static final String ROOM_ARCHIVE =
      "SELECT id FROM mam_server_user WHERE server = 'muclight.carbonio' AND user_name = ?";

  private static final String PAGE =
      "SELECT m.id, s.server, s.user_name, CASE WHEN octet_length(m.message) <= "
          + INLINE_BYTES
          + " THEN m.message END, octet_length(m.message)"
          + " FROM mam_muc_message m LEFT JOIN mam_server_user s ON s.id = m.sender_id"
          + " WHERE m.room_id = ? AND m.id > ? ORDER BY m.id LIMIT ?";

  private static final String LARGE_ROW =
      "SELECT message FROM mam_muc_message WHERE room_id = ? AND id = ?";

  private static final String CLEAR_CANDIDATES =
      "SELECT m.id, "
          + BODY
          + " FROM mam_muc_message m WHERE m.room_id = ?"
          + " AND position('roomHistoryCleared'::bytea in m.message) > 0 ORDER BY m.id DESC";

  private final PinImporter pins;
  private final MarkerTranscriber markers;
  private final PreparedStatement archiveQuery;
  private final PreparedStatement pageQuery;
  private final PreparedStatement largeRowQuery;
  private final PreparedStatement clearQuery;
  private volatile long currentMam;

  RoomReader(MongooseSource.Ready source, PinImporter pins, MarkerTranscriber markers)
      throws SQLException {
    this.pins = pins;
    this.markers = markers;
    this.archiveQuery = source.connection().prepareStatement(ROOM_ARCHIVE);
    this.pageQuery = source.connection().prepareStatement(PAGE);
    this.largeRowQuery = source.connection().prepareStatement(LARGE_ROW);
    this.clearQuery = source.connection().prepareStatement(CLEAR_CANDIDATES);
    this.clearQuery.setFetchSize(CLEAR_FETCH_SIZE);
  }

  @Override
  public long currentMam() {
    return currentMam;
  }

  @Override
  public void read(TargetRoom room, Sink sink) throws SQLException, InterruptedException {
    currentMam = 0;
    long archive = archiveKey(room.id());
    if (archive < 0) {
      sink.put(new Start(room, 0));
      sink.put(new End(List.of(), Map.of()));
      return;
    }
    sink.put(new Start(room, "TEMPORARY".equals(room.type()) ? clearFloor(archive) : 0));
    boolean archived = false;
    long last = 0;
    int page;
    do {
      page = 0;
      pageQuery.setLong(1, archive);
      pageQuery.setLong(2, last);
      pageQuery.setInt(3, PAGE_SIZE);
      try (ResultSet rs = pageQuery.executeQuery()) {
        while (rs.next()) {
          page++;
          last = rs.getLong(1);
          currentMam = last;
          archived = true;
          byte[] stored = rs.getBytes(4);
          if (stored == null && rs.getLong(5) <= MAX_BODY_BYTES) {
            stored = largeRow(archive, last);
          }
          int weight = 1 + (stored == null ? 0 : stored.length / 1024);
          sink.put(
              new Entry(
                  last, rs.getString(2), rs.getString(3), StanzaParser.parse(stored), weight));
        }
      }
    } while (page == PAGE_SIZE);
    currentMam = 0;
    sink.put(
        archived ? new End(pins.read(room), markers.read(room)) : new End(List.of(), Map.of()));
  }

  @Override
  public void close() throws SQLException {
    archiveQuery.close();
    pageQuery.close();
    largeRowQuery.close();
    clearQuery.close();
  }

  private long archiveKey(String roomId) throws SQLException {
    archiveQuery.setString(1, roomId);
    try (ResultSet rs = archiveQuery.executeQuery()) {
      return rs.next() ? rs.getLong(1) : -1;
    }
  }

  private byte[] largeRow(long archive, long mam) throws SQLException {
    largeRowQuery.setLong(1, archive);
    largeRowQuery.setLong(2, mam);
    try (ResultSet rs = largeRowQuery.executeQuery()) {
      return rs.next() ? rs.getBytes(1) : null;
    }
  }

  private long clearFloor(long archive) throws SQLException {
    clearQuery.setLong(1, archive);
    try (ResultSet rs = clearQuery.executeQuery()) {
      while (rs.next()) {
        if (StanzaParser.parse(rs.getBytes(2)).kind() == ParsedRow.Kind.ROOM_HISTORY_CLEARED) {
          return rs.getLong(1);
        }
      }
    }
    return 0;
  }
}
