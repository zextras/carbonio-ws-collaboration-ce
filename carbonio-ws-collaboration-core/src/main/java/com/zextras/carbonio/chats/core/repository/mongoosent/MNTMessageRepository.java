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
    // Single query using UNION ALL - reduces from 3 DB round-trips to 1
    // The subquery finds the target message's createdAt inline
    String sql = """
        (SELECT * FROM CHATS.MONGOOSENT_MESSAGE
         WHERE ROOM_ID = :roomId
           AND CREATED_AT <= (SELECT CREATED_AT FROM CHATS.MONGOOSENT_MESSAGE WHERE ID = :messageId)
         ORDER BY CREATED_AT DESC
         LIMIT :limitBefore)
        UNION ALL
        (SELECT * FROM CHATS.MONGOOSENT_MESSAGE
         WHERE ROOM_ID = :roomId
           AND CREATED_AT > (SELECT CREATED_AT FROM CHATS.MONGOOSENT_MESSAGE WHERE ID = :messageId)
         ORDER BY CREATED_AT ASC
         LIMIT :limitAfter)
        """;

    List<MNTMessage> results = db.findNative(MNTMessage.class, sql)
        .setParameter("roomId", roomId)
        .setParameter("messageId", messageId)
        .setParameter("limitBefore", limitBefore + 1)
        .setParameter("limitAfter", limitAfter)
        .findList();

    // Results come as: [before DESC] + [after ASC]
    // We need to reverse the "before" part to get chronological order
    // Find the split point: messages where createdAt <= target are "before"
    if (results.isEmpty()) {
      return List.of();
    }

    // The first part (before) is in DESC order, second part (after) is in ASC
    // Since UNION ALL preserves order of each SELECT, we need to:
    // 1. Find where "before" ends (last message with createdAt <= target)
    // 2. Reverse that portion
    // Actually, simpler: sort the entire result by createdAt ASC
    results.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
    return results;
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
