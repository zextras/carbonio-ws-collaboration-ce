// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoom;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTRoomMember;
import io.ebean.Database;
import java.util.List;
import java.util.Optional;

@Singleton
public class MNTRoomRepository {

  private final Database db;

  @Inject
  public MNTRoomRepository(Database db) {
    this.db = db;
  }

  public Optional<MNTRoom> getById(String roomId) {
    return Optional.ofNullable(
        db.find(MNTRoom.class)
            .fetch("members")
            .where()
            .eq("id", roomId)
            .findOne());
  }

  public List<MNTRoom> getByUserId(String userId) {
    return db.find(MNTRoom.class)
        .fetch("members")
        .where()
        .eq("members.userId", userId)
        .orderBy()
        .desc("updatedAt")
        .findList();
  }

  public Optional<MNTRoom> findOneToOneRoom(String userId1, String userId2) {
    // Find a ONE_TO_ONE room where both users are members
    String sql =
        "SELECT r.* FROM CHATS.MONGOOSENT_ROOM r "
            + "JOIN CHATS.MONGOOSENT_ROOM_MEMBER m1 ON r.ID = m1.ROOM_ID AND m1.USER_ID = :userId1 "
            + "JOIN CHATS.MONGOOSENT_ROOM_MEMBER m2 ON r.ID = m2.ROOM_ID AND m2.USER_ID = :userId2 "
            + "WHERE r.TYPE = 'ONE_TO_ONE'";

    return db.findNative(MNTRoom.class, sql)
        .setParameter("userId1", userId1)
        .setParameter("userId2", userId2)
        .findOneOrEmpty();
  }

  public MNTRoom insert(MNTRoom room) {
    db.insert(room);
    return room;
  }

  public MNTRoom update(MNTRoom room) {
    db.update(room);
    return room;
  }

  public void delete(String roomId) {
    db.find(MNTRoom.class).where().eq("id", roomId).delete();
  }

  public void addMember(MNTRoomMember member) {
    db.insert(member);
  }

  public void removeMember(String roomId, String userId) {
    db.find(MNTRoomMember.class)
        .where()
        .eq("id.roomId", roomId)
        .eq("id.userId", userId)
        .delete();
  }

  public List<String> getMemberIds(String roomId) {
    return db.find(MNTRoomMember.class)
        .select("userId")
        .where()
        .eq("id.roomId", roomId)
        .findList()
        .stream()
        .map(MNTRoomMember::getUserId)
        .toList();
  }

  public boolean isMember(String roomId, String userId) {
    return db.find(MNTRoomMember.class)
        .where()
        .eq("id.roomId", roomId)
        .eq("id.userId", userId)
        .exists();
  }
}
