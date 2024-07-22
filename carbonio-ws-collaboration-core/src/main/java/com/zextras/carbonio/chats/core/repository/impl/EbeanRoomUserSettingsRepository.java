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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class EbeanRoomUserSettingsRepository implements RoomUserSettingsRepository {

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
}
