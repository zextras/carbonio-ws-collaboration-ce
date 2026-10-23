// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.Database;
import io.ebean.Query;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanRoomRepository implements RoomRepository {

  private final Database db;

  @Inject
  public EbeanRoomRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<String> getIdsByUserId(String userId) {
    return db.find(Room.class)
        .where()
        .eq("subscriptions.userId", userId)
        .select("id")
        .findSingleAttributeList();
  }

  @Override
  @Transactional
  public List<Room> getByUserId(String userId, boolean withSubscriptions) {
    Query<Room> roomQuery = db.find(Room.class);
    if (withSubscriptions) {
      roomQuery = roomQuery.fetch("subscriptions");
    }
    return roomQuery.where().eq("subscriptions.userId", userId).findList();
  }

  @Override
  @Transactional
  public Optional<Room> getById(String roomId) {
    return db.find(Room.class).fetch("subscriptions").where().eq("id", roomId).findOneOrEmpty();
  }

  @Override
  @Transactional
  public Optional<Room> getOneToOneByAllUserIds(String user1Id, String user2Id) {
    return db.find(Room.class)
        .where()
        .eq("type", RoomTypeDto.ONE_TO_ONE)
        .and()
        .raw(
            "id in ( "
                + "select distinct a.room_id from chats.subscription a "
                + "inner join chats.subscription b on a.room_id = b.room_id "
                + "and (a.user_id = '"
                + user1Id
                + "' or a.user_id = '"
                + user2Id
                + "') "
                + "and (b.user_id = '"
                + user1Id
                + "' or b.user_id = '"
                + user2Id
                + "') "
                + "where (a.user_id = '"
                + user1Id
                + "' and b.user_id = '"
                + user2Id
                + "') "
                + "or (a.user_id = '"
                + user2Id
                + "' and b.user_id = '"
                + user1Id
                + "'))")
        .findOneOrEmpty();
  }

  @Override
  public Room insert(Room room) {
    if (room.getCreatedAt() == null) {
      throw new IllegalArgumentException("Room createdAt must be set before insert");
    }
    db.insert(room);
    return room;
  }

  @Override
  public Room update(Room room) {
    db.update(room);
    return room;
  }

  @Override
  public void delete(String roomId) {
    db.delete(Room.class, roomId);
  }

  @Override
  public List<String> getOnlineUserIdsSharingRoomWith(String userId) {
    // Distinct users subscribed to any room of this user, keeping only those with an open session
    return db.sqlQuery(
            "SELECT u.user_id FROM ("
                + "SELECT DISTINCT s2.user_id "
                + "FROM chats.subscription s1 "
                + "INNER JOIN chats.subscription s2 ON s1.room_id = s2.room_id "
                + "WHERE s1.user_id = :userId AND s2.user_id != :userId) u "
                + "WHERE EXISTS (SELECT 1 FROM chats.ws_session w WHERE w.user_id = u.user_id)")
        .setParameter("userId", userId)
        .mapToScalar(String.class)
        .findList();
  }
}
