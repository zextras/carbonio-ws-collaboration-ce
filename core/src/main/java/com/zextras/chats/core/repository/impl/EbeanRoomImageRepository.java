package com.zextras.chats.core.repository.impl;

import com.zextras.chats.core.data.entity.RoomImage;
import com.zextras.chats.core.repository.RoomImageRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
import javax.inject.Inject;

@Transactional
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
  public void save(RoomImage roomImage) {
     db.save(roomImage);
  }
}
