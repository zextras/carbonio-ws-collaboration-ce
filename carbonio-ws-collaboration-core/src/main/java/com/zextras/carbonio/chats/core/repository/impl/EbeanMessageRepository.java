// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.model.InboxLastItemRefs;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.utils.SearchTextUtils;
import io.ebean.Database;
import io.ebean.DuplicateKeyException;
import io.ebean.Query;
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class EbeanMessageRepository implements MessageRepository {

  private final Database db;

  @Inject
  public EbeanMessageRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<Message> getById(String messageId) {
    return db.find(Message.class).where().eq("id", messageId).findOneOrEmpty();
  }

  @Override
  @Transactional
  public Optional<Message> getByIdWithReactions(String messageId) {
    return db.find(Message.class).fetch("reactions").where().eq("id", messageId).findOneOrEmpty();
  }

  @Override
  @Transactional
  public List<Message> getByRoomId(
      String roomId, OffsetDateTime before, OffsetDateTime after, int limit) {
    Query<Message> query =
        db.find(Message.class).fetch("reactions").where().eq("roomId", roomId).query();

    if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  @Transactional
  public List<Message> getByRoomId(
      String roomId, OffsetDateTime before, String beforeId, OffsetDateTime after, int limit) {
    Query<Message> query =
        db.find(Message.class).fetch("reactions").where().eq("roomId", roomId).query();

    if (before != null && beforeId != null) {
      // Composite cursor: (created_at, id) < (before, beforeId)
      // Avoids skipping messages that share the same millisecond timestamp across page boundaries.
      query = query.where().raw("(created_at, id) < (?,?)", before, beforeId).query();
    } else if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  @Transactional
  public List<Message> getByRoomId(
      String roomId, OffsetDateTime before, OffsetDateTime after, int limit, boolean ascending) {
    Query<Message> query =
        db.find(Message.class).fetch("reactions").where().eq("roomId", roomId).query();

    if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    if (ascending) {
      return query.orderBy("createdAt asc, id asc").setMaxRows(limit).findList();
    }
    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  @Transactional
  public List<Message> getByRoomIdAndIds(String roomId, List<String> ids) {
    return db.find(Message.class)
        .where()
        .eq("roomId", roomId)
        .in("id", ids)
        .orderBy("createdAt desc, id desc")
        .findList();
  }

  @Override
  @Transactional
  public List<Message> searchByText(
      String roomId, List<String> words, OffsetDateTime before, OffsetDateTime after, int limit) {
    Query<Message> query = db.find(Message.class).where().eq("roomId", roomId).query();
    for (String word : words) {
      query =
          query
              .where()
              .raw("text ILIKE ? ESCAPE '\\'", "%" + SearchTextUtils.escapeLike(word) + "%")
              .query();
    }

    if (before != null) {
      query = query.where().lt("createdAt", before).query();
    }
    if (after != null) {
      query = query.where().gt("createdAt", after).query();
    }

    return query.orderBy("createdAt desc, id desc").setMaxRows(limit).findList();
  }

  @Override
  @Transactional
  public Map<String, InboxLastItemRefs> getLastItemRefsByUserId(String userId) {
    String sql =
        """
        SELECT s.room_id,
               lm.id AS message_id, le.id AS event_id,
               (le.id IS NOT NULL
                AND (lm.id IS NULL OR (le.created_at, le.id COLLATE "C") > (lm.created_at, lm.id COLLATE "C")))
                 AS event_is_newer
        FROM chats.subscription s
        LEFT JOIN chats.room_user_settings rus
               ON rus.room_id = s.room_id AND rus.user_id = s.user_id
        LEFT JOIN LATERAL (
          SELECT m.id, m.created_at FROM chats.message m
          WHERE m.room_id = s.room_id AND m.deleted = false
            AND (rus.cleared_at IS NULL OR m.created_at > rus.cleared_at)
          ORDER BY m.created_at DESC, m.id DESC LIMIT 1) lm ON true
        LEFT JOIN LATERAL (
          SELECT e.id, e.created_at FROM chats.system_event e
          WHERE e.room_id = s.room_id
            AND (rus.cleared_at IS NULL OR e.created_at > rus.cleared_at)
          ORDER BY e.created_at DESC, e.id DESC LIMIT 1) le ON true
        WHERE s.user_id = :userId
        """;
    Map<String, InboxLastItemRefs> result = new HashMap<>();
    db.sqlQuery(sql)
        .setParameter("userId", userId)
        .findEachRow(
            (row, rowNum) -> {
              String messageId = row.getString("message_id");
              String eventId = row.getString("event_id");
              if (messageId == null && eventId == null) {
                return;
              }
              result.put(
                  row.getString("room_id"),
                  new InboxLastItemRefs(messageId, eventId, row.getBoolean("event_is_newer")));
            });
    return result;
  }

  @Override
  @Transactional
  public Map<String, Long> getUnreadCountsByUserId(String userId, int cap) {
    String sql =
        """
        SELECT s.room_id, u.n AS unread_count
        FROM chats.subscription s
        LEFT JOIN chats.read_marker mr
               ON mr.room_id = s.room_id AND mr.user_id = s.user_id
        LEFT JOIN chats.room_user_settings rus
               ON rus.room_id = s.room_id AND rus.user_id = s.user_id
        CROSS JOIN LATERAL (
          SELECT count(*) AS n FROM (
            (SELECT 1 FROM chats.message m
             WHERE m.room_id = s.room_id AND m.deleted = false AND m.sender_id <> s.user_id
               AND m.created_at > COALESCE(GREATEST(s.joined_at, rus.cleared_at, mr.item_created_at), '-infinity'::timestamp)
             LIMIT :cap)
            UNION ALL
            (SELECT 1 FROM chats.system_event e
             WHERE e.room_id = s.room_id AND e.actor_id <> s.user_id
               AND e.created_at > COALESCE(GREATEST(s.joined_at, rus.cleared_at, mr.item_created_at), '-infinity'::timestamp)
             LIMIT :cap)
            LIMIT :cap) x) u
        WHERE s.user_id = :userId
        """;
    Map<String, Long> result = new HashMap<>();
    db.sqlQuery(sql)
        .setParameter("userId", userId)
        .setParameter("cap", cap)
        .findEachRow(
            (row, rowNum) -> {
              long count = row.getLong("unread_count");
              if (count > 0) {
                result.put(row.getString("room_id"), count);
              }
            });
    return result;
  }

  @Override
  @Transactional
  public List<Message> getVisibleRepliedOriginals(
      String userId, Collection<String> replyingMessageIds) {
    if (replyingMessageIds.isEmpty()) {
      return List.of();
    }
    String placeholders = String.join(",", Collections.nCopies(replyingMessageIds.size(), "?"));
    List<Object> params = new ArrayList<>();
    params.add(userId);
    params.addAll(replyingMessageIds);
    return db.find(Message.class)
        .where()
        .raw(
            "id IN (SELECT o.id FROM chats.message w"
                + " JOIN chats.message o ON o.id = w.reply_to_id AND o.room_id = w.room_id"
                + " LEFT JOIN chats.room_user_settings rus"
                + " ON rus.room_id = w.room_id AND rus.user_id = ?"
                + " WHERE w.id IN ("
                + placeholders
                + ") AND (rus.cleared_at IS NULL OR o.created_at > rus.cleared_at))",
            params.toArray())
        .findList();
  }

  @Override
  @Transactional
  public List<Message> getByIdsWithReactions(Collection<String> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return db.find(Message.class).fetch("reactions").where().in("id", ids).findList();
  }

  @Override
  @Transactional
  public List<Message> getByRoomIdAfterCursor(
      String roomId,
      OffsetDateTime lowerBoundExclusive,
      OffsetDateTime cursorCreatedAt,
      String cursorId,
      int limit) {
    Query<Message> query = db.find(Message.class).where().eq("roomId", roomId).query();

    if (lowerBoundExclusive != null) {
      query = query.where().gt("createdAt", lowerBoundExclusive).query();
    }
    if (cursorCreatedAt != null && cursorId != null) {
      // Composite keyset: strictly after the last returned (createdAt, id).
      query = query.where().raw("(created_at, id) > (?,?)", cursorCreatedAt, cursorId).query();
    }

    return query.orderBy("createdAt asc, id asc").setMaxRows(limit).findList();
  }

  @Override
  public Message insert(Message message) {
    try {
      db.insert(message);
    } catch (DuplicateKeyException ignored) {
      // server-minted UUID; treat duplicate as a no-op (ON CONFLICT DO NOTHING semantics)
    }
    return message;
  }

  @Override
  public Message update(Message message) {
    db.update(message);
    return message;
  }

  @Override
  public void deleteByRoomId(String roomId) {
    db.find(Message.class).where().eq("roomId", roomId).delete();
  }
}
