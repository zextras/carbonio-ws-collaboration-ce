// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;


@Transactional
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
    return db.find(RoomUserSettings.class)
      .where().eq("userId", userId)
      .findList();
  }

  @Override
  public void deleteByRoomId(String roomId) {
    db.find(RoomUserSettings.class).where().eq("id.roomId", roomId).delete();
  }

  @Override
  public RoomUserSettings save(RoomUserSettings roomUserSettings) {
    db.save(roomUserSettings);
    return roomUserSettings;
  }
}
