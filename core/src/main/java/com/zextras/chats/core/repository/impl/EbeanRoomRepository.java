package com.zextras.chats.core.repository.impl;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.repository.RoomRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Singleton;

@Transactional
@Singleton
public class EbeanRoomRepository implements RoomRepository {

  private final Database db;

  @Inject
  public EbeanRoomRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<Room> getByUserId(String userId) {
    return db.find(Room.class)
      .where().eq("subscriptions.userId", userId)
      .findList();
  }

  @Override
  public Optional<Room> getById(String roomId) {
    return db.find(Room.class)
      .fetch("subscriptions")
      .where()
      .eq("id", roomId)
      .findOneOrEmpty();
  }

  @Override
  public Optional<Room> getByIdAndUserId(String roomId, String userId) {
    return db.find(Room.class)
      .where()
      .eq("id", roomId)
      .eq("subscriptions.id.userId", userId)
      .findOneOrEmpty();
  }

  @Override
  public Room insert(Room room) {
    db.insert(room);
    return room;
  }

  @Override
  public Room update(Room room) {
    db.update(room);
    return room;
  }

  @Override
  public void delete(String id) {
    db.delete(Room.class, id);
  }
}
