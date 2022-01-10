package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.RoomImage;
import com.zextras.carbonio.chats.core.repository.RoomImageRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Transactional
@Singleton
public class EbeanRoomImageRepository implements RoomImageRepository {

  private final Database db;

  @Inject
  public EbeanRoomImageRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<RoomImage> getByRoomId(String roomId) {
    return Optional.ofNullable(db.find(RoomImage.class, roomId));
  }

  @Override
  public RoomImage save(RoomImage roomImage) {
     db.save(roomImage);
     return roomImage;
  }
}
