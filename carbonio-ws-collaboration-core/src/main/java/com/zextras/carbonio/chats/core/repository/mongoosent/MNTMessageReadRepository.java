// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageRead;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class MNTMessageReadRepository {

  private final Database db;

  @Inject
  public MNTMessageReadRepository(Database db) {
    this.db = db;
  }

  public Optional<MNTMessageRead> getByUserIdAndRoomId(String userId, String roomId) {
    return db.find(MNTMessageRead.class)
        .where()
        .eq("id.userId", userId)
        .eq("id.roomId", roomId)
        .findOneOrEmpty();
  }

  public List<MNTMessageRead> getByRoomId(String roomId) {
    return db.find(MNTMessageRead.class).where().eq("id.roomId", roomId).findList();
  }

  public void upsert(String userId, String roomId, String messageId) {
    Optional<MNTMessageRead> existing = getByUserIdAndRoomId(userId, roomId);
    if (existing.isPresent()) {
      MNTMessageRead read = existing.get();
      read.messageId(messageId).readAt(OffsetDateTime.now());
      db.update(read);
    } else {
      db.insert(MNTMessageRead.create(userId, roomId, messageId));
    }
  }

  public void deleteByRoomId(String roomId) {
    db.find(MNTMessageRead.class).where().eq("id.roomId", roomId).delete();
  }
}
