// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import io.ebean.Database;
import io.ebean.SqlRow;

@Singleton
public class EbeanMessageSendDedupRepository implements MessageSendDedupRepository {

  private static final String CLAIM_SQL =
      """
      WITH ts AS (
          SELECT clock_timestamp() AS t
      ),
      claim AS (
          INSERT INTO chats.message_send_dedup AS d (room_id, sender_id, temp_id, message_id, created_at)
          SELECT :roomId, :senderId, :tempId, :messageId, ts.t FROM ts
          ON CONFLICT (room_id, sender_id, temp_id) DO UPDATE
              SET message_id = EXCLUDED.message_id, created_at = EXCLUDED.created_at
              WHERE d.created_at < EXCLUDED.created_at - INTERVAL '1 hour'
          RETURNING d.message_id
      )
      SELECT (SELECT message_id FROM claim) AS claimed_message_id,
             (SELECT d.message_id
              FROM chats.message_send_dedup d CROSS JOIN ts
              WHERE d.room_id = :roomId AND d.sender_id = :senderId AND d.temp_id = :tempId
                AND d.created_at >= CAST(ts.t AS timestamp) - INTERVAL '1 hour') AS existing_message_id
      """;

  private static final String LIVE_CLAIM_SQL =
      """
      SELECT message_id FROM chats.message_send_dedup
      WHERE room_id = :roomId AND sender_id = :senderId AND temp_id = :tempId
        AND created_at >= CAST(clock_timestamp() AS timestamp) - INTERVAL '1 hour'
      """;

  private static final String RELEASE_SQL =
      """
      DELETE FROM chats.message_send_dedup
      WHERE room_id = :roomId AND sender_id = :senderId AND temp_id = :tempId
        AND message_id = :messageId
      """;

  private static final String PURGE_SQL =
      """
      DELETE FROM chats.message_send_dedup d
      USING (
          SELECT room_id, sender_id, temp_id
          FROM chats.message_send_dedup
          WHERE created_at < CAST(clock_timestamp() AS timestamp) - INTERVAL '1 hour'
          ORDER BY created_at
          LIMIT :batchSize
          FOR UPDATE SKIP LOCKED
      ) expired
      WHERE d.room_id = expired.room_id
        AND d.sender_id = expired.sender_id
        AND d.temp_id = expired.temp_id
      """;

  private final Database db;

  @Inject
  public EbeanMessageSendDedupRepository(Database db) {
    this.db = db;
  }

  static String findLiveClaim(Database db, String roomId, String senderId, String tempId) {
    return db.sqlQuery(LIVE_CLAIM_SQL)
        .setParameter("roomId", roomId)
        .setParameter("senderId", senderId)
        .setParameter("tempId", tempId)
        .findOneOrEmpty()
        .map(row -> row.getString("message_id"))
        .orElse(null);
  }

  @Override
  public Claim claim(String roomId, String senderId, String tempId, String messageId) {
    SqlRow row =
        db.sqlQuery(CLAIM_SQL)
            .setParameter("roomId", roomId)
            .setParameter("senderId", senderId)
            .setParameter("tempId", tempId)
            .setParameter("messageId", messageId)
            .findOne();
    if (row.getString("claimed_message_id") != null) {
      return new Claim(true, null);
    }
    String existing = row.getString("existing_message_id");
    if (existing == null) {
      existing = findLiveClaim(db, roomId, senderId, tempId);
    }
    return new Claim(false, existing);
  }

  @Override
  public void release(String roomId, String senderId, String tempId, String messageId) {
    db.sqlUpdate(RELEASE_SQL)
        .setParameter("roomId", roomId)
        .setParameter("senderId", senderId)
        .setParameter("tempId", tempId)
        .setParameter("messageId", messageId)
        .execute();
  }

  @Override
  public int purgeExpired(int batchSize) {
    return db.sqlUpdate(PURGE_SQL).setParameter("batchSize", batchSize).execute();
  }
}
