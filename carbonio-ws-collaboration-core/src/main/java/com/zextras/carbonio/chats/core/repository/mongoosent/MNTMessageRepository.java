// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessage;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageReaction;
import io.ebean.Database;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
public class MNTMessageRepository {

  private final Database db;

  @Inject
  public MNTMessageRepository(Database db) {
    this.db = db;
  }

  public Optional<MNTMessage> getById(String messageId) {
    return Optional.ofNullable(db.find(MNTMessage.class, messageId));
  }

  public List<MNTMessage> getByRoomId(String roomId, int limit, String beforeMessageId) {
    var query = db.find(MNTMessage.class).where().eq("roomId", roomId);

    if (beforeMessageId != null) {
      Optional<MNTMessage> beforeMessage = getById(beforeMessageId);
      if (beforeMessage.isPresent()) {
        query.lt("createdAt", beforeMessage.get().getCreatedAt());
      }
    }

    return query.orderBy().desc("createdAt").setMaxRows(limit).findList();
  }

  public List<MNTMessage> getByRoomIdAfter(String roomId, int limit, String afterMessageId) {
    var query = db.find(MNTMessage.class).where().eq("roomId", roomId);

    if (afterMessageId != null) {
      Optional<MNTMessage> afterMessage = getById(afterMessageId);
      if (afterMessage.isPresent()) {
        query.gt("createdAt", afterMessage.get().getCreatedAt());
      }
    }

    // Get oldest first (ASC), then we'll reverse in service or keep as-is
    return query.orderBy().asc("createdAt").setMaxRows(limit).findList();
  }

  public List<MNTMessage> getMessagesAround(String roomId, String messageId, int limitBefore, int limitAfter) {
    Optional<MNTMessage> targetMessage = getById(messageId);
    if (targetMessage.isEmpty()) {
      return List.of();
    }

    var targetTime = targetMessage.get().getCreatedAt();

    // Get messages before (including target)
    List<MNTMessage> before = db.find(MNTMessage.class)
        .where()
        .eq("roomId", roomId)
        .le("createdAt", targetTime)
        .orderBy().desc("createdAt")
        .setMaxRows(limitBefore + 1)
        .findList();

    // Get messages after
    List<MNTMessage> after = db.find(MNTMessage.class)
        .where()
        .eq("roomId", roomId)
        .gt("createdAt", targetTime)
        .orderBy().asc("createdAt")
        .setMaxRows(limitAfter)
        .findList();

    // Combine: before is DESC so reverse it, then add after
    List<MNTMessage> result = new java.util.ArrayList<>();
    for (int i = before.size() - 1; i >= 0; i--) {
      result.add(before.get(i));
    }
    result.addAll(after);
    return result;
  }

  public List<MNTMessage> searchByText(String roomId, String searchText, int limit) {
    return db.find(MNTMessage.class)
        .where()
        .eq("roomId", roomId)
        .eq("deleted", false)
        .ilike("text", "%" + searchText + "%")
        .orderBy()
        .desc("createdAt")
        .setMaxRows(limit)
        .findList();
  }

  public Optional<MNTMessage> getLastByRoomId(String roomId) {
    return db.find(MNTMessage.class)
        .where()
        .eq("roomId", roomId)
        .eq("deleted", false)
        .orderBy()
        .desc("createdAt")
        .setMaxRows(1)
        .findOneOrEmpty();
  }

  public long countUnreadMessages(String roomId, String userId, String lastReadMessageId) {
    var query =
        db.find(MNTMessage.class)
            .where()
            .eq("roomId", roomId)
            .eq("deleted", false)
            .ne("senderId", userId);

    if (lastReadMessageId != null) {
      Optional<MNTMessage> lastReadMessage = getById(lastReadMessageId);
      if (lastReadMessage.isPresent()) {
        query.gt("createdAt", lastReadMessage.get().getCreatedAt());
      }
    }

    return query.findCount();
  }

  public MNTMessage insert(MNTMessage message) {
    db.insert(message);
    return message;
  }

  public MNTMessage update(MNTMessage message) {
    db.update(message);
    return message;
  }

  public void markAsDeleted(String messageId) {
    db.find(MNTMessage.class)
        .where()
        .eq("id", messageId)
        .asUpdate()
        .set("deleted", true)
        .set("text", "")
        .set("updatedAt", OffsetDateTime.now())
        .update();
  }

  public void deleteByRoomId(String roomId) {
    db.find(MNTMessage.class).where().eq("roomId", roomId).delete();
  }

  // Reactions
  public boolean hasReaction(String messageId, String userId, String reaction) {
    return db.find(MNTMessageReaction.class)
        .where()
        .eq("id.messageId", messageId)
        .eq("id.userId", userId)
        .eq("id.reaction", reaction)
        .exists();
  }

  public void addReaction(MNTMessageReaction reaction) {
    db.insert(reaction);
  }

  public void removeReaction(String messageId, String userId, String reaction) {
    db.find(MNTMessageReaction.class)
        .where()
        .eq("id.messageId", messageId)
        .eq("id.userId", userId)
        .eq("id.reaction", reaction)
        .delete();
  }

  public List<MNTMessageReaction> getReactionsByMessageId(String messageId) {
    return db.find(MNTMessageReaction.class).where().eq("id.messageId", messageId).findList();
  }
}
