// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import io.ebean.Database;
import io.ebean.SqlRow;
import io.ebean.annotation.Transactional;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Ebean implementation of {@link MessageWriteRepository}. Each public method executes a single CTE
 * query that validates membership, mutates data, and returns the result in one database round-trip;
 * {@link #deleteMessage} adds a second statement, in the same transaction, that drops the room pin
 * of the deleted message.
 */
@Singleton
public class EbeanMessageWriteRepository implements MessageWriteRepository {

  private static final String SEND_SQL =
"""
WITH ts AS (
    SELECT clock_timestamp() AS t
),
check_member AS (
    SELECT rus.cleared_at
    FROM chats.subscription s
    LEFT JOIN chats.room_user_settings rus ON rus.room_id = s.room_id AND rus.user_id = s.user_id
    WHERE s.room_id = :roomId AND s.user_id = :userId
),
new_msg AS (
    INSERT INTO chats.message (id, room_id, sender_id, text, reply_to_id, attachment_id, created_at, updated_at)
    SELECT :messageId, :roomId, :userId, :text, :replyToId, :attachmentId, ts.t, ts.t
    FROM check_member cm CROSS JOIN ts
    WHERE CAST(:replyToId AS text) IS NULL
       OR EXISTS (
            SELECT 1 FROM chats.message r
            WHERE r.id = :replyToId AND r.room_id = :roomId
              AND (cm.cleared_at IS NULL OR r.created_at > cm.cleared_at))
    ON CONFLICT (id) DO NOTHING
    RETURNING id, created_at
)
SELECT id, created_at FROM new_msg
""";

  private static final String SEND_ONCE_SQL =
      """
      WITH ts AS (
          SELECT clock_timestamp() AS t
      ),
      check_member AS (
          SELECT rus.cleared_at
          FROM chats.subscription s
          LEFT JOIN chats.room_user_settings rus ON rus.room_id = s.room_id AND rus.user_id = s.user_id
          WHERE s.room_id = :roomId AND s.user_id = :userId
      ),
      reply_ok AS (
          SELECT 1 AS ok
          FROM check_member cm
          WHERE CAST(:replyToId AS text) IS NULL
             OR EXISTS (
                  SELECT 1 FROM chats.message r
                  WHERE r.id = :replyToId AND r.room_id = :roomId
                    AND (cm.cleared_at IS NULL OR r.created_at > cm.cleared_at))
      ),
      claim AS (
          INSERT INTO chats.message_send_dedup AS d (room_id, sender_id, temp_id, message_id, created_at)
          SELECT :roomId, :userId, :tempId, :messageId, ts.t
          FROM reply_ok CROSS JOIN ts
          ON CONFLICT (room_id, sender_id, temp_id) DO UPDATE
              SET message_id = EXCLUDED.message_id, created_at = EXCLUDED.created_at
              WHERE d.created_at < EXCLUDED.created_at - INTERVAL '1 hour'
          RETURNING d.message_id
      ),
      new_msg AS (
          INSERT INTO chats.message (id, room_id, sender_id, text, reply_to_id, created_at, updated_at)
          SELECT claim.message_id, :roomId, :userId, :text, :replyToId, ts.t, ts.t
          FROM claim CROSS JOIN ts
          RETURNING id, created_at
      ),
      live_claim AS (
          SELECT d.message_id
          FROM chats.message_send_dedup d CROSS JOIN ts
          WHERE d.room_id = :roomId AND d.sender_id = :userId AND d.temp_id = :tempId
            AND d.created_at >= CAST(ts.t AS timestamp) - INTERVAL '1 hour'
      )
      SELECT EXISTS (SELECT 1 FROM check_member) AS is_member,
             EXISTS (SELECT 1 FROM reply_ok) AS reply_visible,
             new_msg.id,
             new_msg.created_at,
             (SELECT message_id FROM live_claim) AS existing_message_id
      FROM ts
      LEFT JOIN new_msg ON true
      """;

  private static final String EDIT_SQL =
      """
      WITH check_member AS (
          SELECT 1 FROM chats.subscription WHERE room_id = :roomId AND user_id = :userId
      ),
      edit_msg AS (
          UPDATE chats.message
          SET text = :newText, edited = true, updated_at = clock_timestamp()
          WHERE id = :messageId AND room_id = :roomId AND sender_id = :userId AND deleted = false
            AND EXISTS (SELECT 1 FROM check_member)
          RETURNING id, text, updated_at, created_at
      )
      SELECT id, text, updated_at, created_at FROM edit_msg
      """;

  private static final String DELETE_SQL =
      """
      WITH check_member AS (
          SELECT 1 FROM chats.subscription WHERE room_id = :roomId AND user_id = :userId
      ),
      del_msg AS (
          UPDATE chats.message
          SET deleted = true, deleted_at = clock_timestamp(), text = '', updated_at = clock_timestamp()
          WHERE id = :messageId AND room_id = :roomId AND sender_id = :userId AND deleted = false
            AND EXISTS (SELECT 1 FROM check_member)
          RETURNING id, attachment_id
      )
      SELECT id, attachment_id FROM del_msg
      """;

  private static final String UNPIN_DELETED_SQL =
      """
      WITH unpinned AS (
          DELETE FROM chats.message_pin
          WHERE room_id = :roomId AND message_id = :messageId
          RETURNING message_id
      )
      INSERT INTO chats.system_event (id, room_id, type, actor_id, content, created_at)
      SELECT :systemEventId, :roomId, 'MESSAGE_UNPINNED', :userId,
             CAST(json_build_object('unpinnedBy', CAST(:userId AS text),
                                    'messageId', CAST(:messageId AS text)) AS text),
             clock_timestamp()
      FROM unpinned
      RETURNING id, created_at
      """;

  private static final String FORWARD_SQL =
"""
WITH ts AS (
    SELECT clock_timestamp() AS t
),
check_source AS (
    SELECT rus.cleared_at
    FROM chats.subscription s
    LEFT JOIN chats.room_user_settings rus ON rus.room_id = s.room_id AND rus.user_id = s.user_id
    WHERE s.room_id = :sourceRoomId AND s.user_id = :userId
),
check_target AS (
    SELECT 1 FROM chats.subscription WHERE room_id = :targetRoomId AND user_id = :userId
),
orig_msg AS (
    SELECT om.id, om.text, om.sender_id, om.attachment_id, om.created_at
    FROM chats.message om
    WHERE om.id = :originalMessageId AND om.room_id = :sourceRoomId AND om.deleted = false
      AND EXISTS (SELECT 1 FROM check_source cs
                  WHERE cs.cleared_at IS NULL OR om.created_at > cs.cleared_at)
),
new_msg AS (
    INSERT INTO chats.message (id, room_id, sender_id, text, attachment_id,
                               forwarded, forwarded_sender_id, forwarded_at, created_at, updated_at)
    SELECT :newMessageId, :targetRoomId, :userId, orig_msg.text,
           COALESCE(:newAttachmentId, orig_msg.attachment_id),
           true, orig_msg.sender_id, orig_msg.created_at, ts.t, ts.t
    FROM orig_msg CROSS JOIN ts
    WHERE EXISTS (SELECT 1 FROM check_target)
    RETURNING id, created_at, attachment_id
),
file_meta AS (
    SELECT id, name, mime_type, original_size, area
    FROM chats.file_metadata
    WHERE id = (SELECT attachment_id FROM new_msg)
)
SELECT new_msg.id, new_msg.created_at,
       orig_msg.text AS original_text, orig_msg.sender_id AS forwarded_sender_id,
       orig_msg.created_at AS forwarded_at,
       new_msg.attachment_id,
       file_meta.name AS attachment_name,
       file_meta.mime_type AS attachment_mime,
       file_meta.original_size AS attachment_size,
       file_meta.area AS attachment_area
FROM new_msg
CROSS JOIN orig_msg
LEFT JOIN file_meta ON true
""";

  private final Database db;

  @Inject
  public EbeanMessageWriteRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<SendResult> sendMessage(
      String roomId,
      String userId,
      String text,
      String messageId,
      String replyToId,
      String attachmentId) {
    return db.sqlQuery(SEND_SQL)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .setParameter("messageId", messageId)
        .setParameter("text", text != null ? text : "")
        .setParameter("replyToId", replyToId)
        .setParameter("attachmentId", attachmentId)
        .findOneOrEmpty()
        .map(
            row ->
                new SendResult(
                    row.getString("id"), toOffsetDateTime(row.getTimestamp("created_at"))));
  }

  @Override
  public SendOnceResult sendMessageOnce(
      String roomId,
      String userId,
      String text,
      String messageId,
      String replyToId,
      String tempId) {
    SqlRow row =
        db.sqlQuery(SEND_ONCE_SQL)
            .setParameter("roomId", roomId)
            .setParameter("userId", userId)
            .setParameter("messageId", messageId)
            .setParameter("text", text != null ? text : "")
            .setParameter("replyToId", replyToId)
            .setParameter("tempId", tempId)
            .findOne();
    if (!Boolean.TRUE.equals(row.getBoolean("is_member"))) {
      return new SendOnceResult(SendOnceStatus.NOT_MEMBER, null, null);
    }
    String sentId = row.getString("id");
    if (sentId != null) {
      return new SendOnceResult(
          SendOnceStatus.SENT, sentId, toOffsetDateTime(row.getTimestamp("created_at")));
    }
    String existingId = row.getString("existing_message_id");
    if (existingId != null) {
      return new SendOnceResult(SendOnceStatus.DUPLICATE, existingId, null);
    }
    if (!Boolean.TRUE.equals(row.getBoolean("reply_visible"))) {
      return new SendOnceResult(SendOnceStatus.REPLY_NOT_FOUND, null, null);
    }
    return new SendOnceResult(
        SendOnceStatus.DUPLICATE,
        EbeanMessageSendDedupRepository.findLiveClaim(db, roomId, userId, tempId),
        null);
  }

  @Override
  public Optional<EditResult> editMessage(
      String messageId, String roomId, String userId, String newText) {
    return db.sqlQuery(EDIT_SQL)
        .setParameter("roomId", roomId)
        .setParameter("userId", userId)
        .setParameter("messageId", messageId)
        .setParameter("newText", newText)
        .findOneOrEmpty()
        .map(
            row ->
                new EditResult(
                    row.getString("id"),
                    row.getString("text"),
                    toOffsetDateTime(row.getTimestamp("updated_at")),
                    toOffsetDateTime(row.getTimestamp("created_at"))));
  }

  @Override
  @Transactional
  public Optional<DeleteResult> deleteMessage(String messageId, String roomId, String userId) {
    Optional<SqlRow> deleted =
        db.sqlQuery(DELETE_SQL)
            .setParameter("roomId", roomId)
            .setParameter("userId", userId)
            .setParameter("messageId", messageId)
            .findOneOrEmpty();
    if (deleted.isEmpty()) {
      return Optional.empty();
    }
    Unpinned unpinned =
        db.sqlQuery(UNPIN_DELETED_SQL)
            .setParameter("roomId", roomId)
            .setParameter("userId", userId)
            .setParameter("messageId", messageId)
            .setParameter("systemEventId", UUID.randomUUID().toString())
            .findOneOrEmpty()
            .map(
                row ->
                    new Unpinned(
                        row.getString("id"), toOffsetDateTime(row.getTimestamp("created_at"))))
            .orElse(null);
    return Optional.of(
        new DeleteResult(
            deleted.get().getString("id"), deleted.get().getString("attachment_id"), unpinned));
  }

  @Override
  public Optional<ForwardResult> forwardMessage(
      String sourceRoomId,
      String targetRoomId,
      String userId,
      String originalMessageId,
      String newMessageId,
      String newAttachmentId) {
    return db.sqlQuery(FORWARD_SQL)
        .setParameter("sourceRoomId", sourceRoomId)
        .setParameter("targetRoomId", targetRoomId)
        .setParameter("userId", userId)
        .setParameter("originalMessageId", originalMessageId)
        .setParameter("newMessageId", newMessageId)
        .setParameter("newAttachmentId", newAttachmentId)
        .findOneOrEmpty()
        .map(
            row ->
                new ForwardResult(
                    row.getString("id"),
                    toOffsetDateTime(row.getTimestamp("created_at")),
                    row.getString("original_text"),
                    row.getString("forwarded_sender_id"),
                    toOffsetDateTime(row.getTimestamp("forwarded_at")),
                    row.getString("attachment_id"),
                    row.getString("attachment_name"),
                    row.getString("attachment_mime"),
                    row.getLong("attachment_size"),
                    row.getString("attachment_area")));
  }

  private static OffsetDateTime toOffsetDateTime(Timestamp ts) {
    return ts != null ? ts.toInstant().atOffset(ZoneOffset.UTC) : null;
  }
}
