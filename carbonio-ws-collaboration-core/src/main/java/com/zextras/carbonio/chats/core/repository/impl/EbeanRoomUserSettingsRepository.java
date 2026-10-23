// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class EbeanRoomUserSettingsRepository implements RoomUserSettingsRepository {

  private static final String SET_CLEARED_AT_NOW_SQL =
      """
      WITH ts AS (SELECT clock_timestamp() AS t)
      INSERT INTO chats.room_user_settings (user_id, room_id, cleared_at, created_at, updated_at)
      SELECT :userId, :roomId, ts.t, ts.t, ts.t FROM ts
      ON CONFLICT (user_id, room_id)
      DO UPDATE SET cleared_at = EXCLUDED.cleared_at, updated_at = EXCLUDED.updated_at
      RETURNING cleared_at
      """;

  private final Database db;

  @Inject
  public EbeanRoomUserSettingsRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<RoomUserSettings> getByRoomIdAndUserId(String roomId, String userId) {
    return db.find(RoomUserSettings.class)
        .where()
        .eq("id", new SubscriptionId(roomId, userId))
        .findOneOrEmpty();
  }

  @Override
  public List<RoomUserSettings> getByUserId(String userId) {
    return db.find(RoomUserSettings.class).where().eq("userId", userId).findList();
  }

  @Override
  public Map<String, RoomUserSettings> getMapGroupedByUserId(String userId) {
    return db.find(RoomUserSettings.class)
        .where()
        .eq("userId", userId)
        .setMapKey("id.roomId")
        .findMap();
  }

  @Override
  public List<RoomUserSettings> getByRoomId(String roomId) {
    return db.find(RoomUserSettings.class).where().eq("id.roomId", roomId).findList();
  }

  @Override
  public void delete(RoomUserSettings userSettings) {
    db.delete(userSettings);
  }

  @Override
  public RoomUserSettings save(RoomUserSettings roomUserSettings) {
    db.save(roomUserSettings);
    return roomUserSettings;
  }

  @Override
  public void save(Collection<RoomUserSettings> roomUserSettingsList) {
    db.saveAll(roomUserSettingsList);
  }

  @Override
  public OffsetDateTime setClearedAtNow(String roomId, String userId) {
    return db.sqlQuery(SET_CLEARED_AT_NOW_SQL)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .findOne()
        .getTimestamp("cleared_at")
        .toInstant()
        .atOffset(ZoneOffset.UTC);
  }

  @Override
  public List<String> getUserIdsClearedAtOrAfter(String roomId, OffsetDateTime createdAt) {
    return db.find(RoomUserSettings.class)
        .select("userId")
        .where()
        .eq("id.roomId", roomId)
        .ge("clearedAt", createdAt)
        .findSingleAttributeList();
  }
}
