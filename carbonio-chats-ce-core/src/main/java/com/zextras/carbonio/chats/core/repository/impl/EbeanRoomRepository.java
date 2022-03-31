// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Query;
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
  public List<Room> getByUserId(String userId, boolean addSubscriptions) {
    Query<Room> roomQuery = db.find(Room.class);
    if (addSubscriptions) {
      roomQuery = roomQuery.fetch("subscriptions");
    }
    return roomQuery.where().eq("subscriptions.userId", userId)
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
