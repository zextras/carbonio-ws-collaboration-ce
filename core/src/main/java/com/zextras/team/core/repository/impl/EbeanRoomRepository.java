package com.zextras.team.core.repository.impl;

import com.zextras.team.core.data.entity.Room;
import com.zextras.team.core.repository.RoomRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;

@Transactional
public class EbeanRoomRepository implements RoomRepository {

  private final Database db;

  @Inject
  public EbeanRoomRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<Room> getByUserId(String userId) {
    return db.find(Room.class)
      .fetch("subscriptions")
      .where().eq("subscriptions.userId", userId)
      .findList();
  }

  @Override
  public Optional<Room> getById(String id) {
    return db.find(Room.class)
      .fetch("subscriptions")
      .where().eq("id", id)
      .findOneOrEmpty();
  }

  @Override
  public Room save(Room room) {
    if (room.getId() != null && db.find(Room.class, room.getId()) != null) {
      db.update(room);
    } else {
      room.setId(Optional.ofNullable(room.getId()).orElse(UUID.randomUUID().toString()));
      db.save(room);
    }
    return db.find(Room.class, room.getId());
  }

  @Override
  public void delete(String id) {
    db.delete(Room.class, id);
  }
}
