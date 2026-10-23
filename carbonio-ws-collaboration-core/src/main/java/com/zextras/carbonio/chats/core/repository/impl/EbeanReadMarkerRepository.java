// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.ReadMarkerId;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanReadMarkerRepository implements ReadMarkerRepository {

  private static final String ADVANCE_SQL =
      """
      INSERT INTO chats.read_marker (user_id, room_id, item_created_at)
      VALUES (:userId, :roomId, :at)
      ON CONFLICT (user_id, room_id)
      DO UPDATE SET item_created_at = EXCLUDED.item_created_at
      WHERE chats.read_marker.item_created_at < EXCLUDED.item_created_at
      RETURNING item_created_at
      """;

  private final Database db;

  @Inject
  public EbeanReadMarkerRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<ReadMarker> get(String userId, String roomId) {
    ReadMarkerId id = new ReadMarkerId(userId, roomId);
    return Optional.ofNullable(db.find(ReadMarker.class, id));
  }

  @Override
  public List<ReadMarker> getByRoomId(String roomId) {
    return db.find(ReadMarker.class).where().eq("id.roomId", roomId).findList();
  }

  @Override
  public List<ReadMarker> getByRoomIds(List<String> roomIds) {
    if (roomIds == null || roomIds.isEmpty()) return List.of();
    return db.find(ReadMarker.class).where().in("id.roomId", roomIds).findList();
  }

  @Override
  public List<ReadMarker> getByUserId(String userId) {
    return db.find(ReadMarker.class).where().eq("id.userId", userId).findList();
  }

  @Override
  public Optional<OffsetDateTime> advance(String userId, String roomId, OffsetDateTime at) {
    return db.sqlQuery(ADVANCE_SQL)
        .setParameter("userId", userId)
        .setParameter("roomId", roomId)
        .setParameter("at", at)
        .mapToScalar(OffsetDateTime.class)
        .findOneOrEmpty();
  }

  @Override
  public void deleteByRoomId(String roomId) {
    db.find(ReadMarker.class).where().eq("id.roomId", roomId).delete();
  }
}
