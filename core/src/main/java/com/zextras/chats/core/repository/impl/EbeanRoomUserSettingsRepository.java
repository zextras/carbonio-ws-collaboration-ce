package com.zextras.chats.core.repository.impl;

import com.zextras.chats.core.data.entity.RoomUserSettings;
import com.zextras.chats.core.data.entity.SubscriptionId;
import com.zextras.chats.core.repository.RoomUserSettingsRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;


@Transactional
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
  public void deleteByRoomId(String roomId) {
    db.find(RoomUserSettings.class).where().eq("id.roomId", roomId).delete();
  }
}
