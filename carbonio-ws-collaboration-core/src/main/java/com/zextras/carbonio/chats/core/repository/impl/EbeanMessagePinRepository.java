// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import io.ebean.Database;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanMessagePinRepository implements MessagePinRepository {

  private static final String PIN_SQL =
      """
      WITH target AS (
          SELECT id FROM chats.message
          WHERE id = :messageId AND room_id = :roomId AND deleted = false
          FOR SHARE
      )
      INSERT INTO chats.message_pin (room_id, message_id, pinned_by, pinned_at)
      SELECT :roomId, target.id, :pinnedBy, clock_timestamp() FROM target
      ON CONFLICT (room_id)
      DO UPDATE SET message_id = EXCLUDED.message_id,
                    pinned_by = EXCLUDED.pinned_by,
                    pinned_at = EXCLUDED.pinned_at
      RETURNING room_id, message_id, pinned_by, pinned_at
      """;

  private final Database db;

  @Inject
  public EbeanMessagePinRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<MessagePin> pin(String roomId, String messageId, String pinnedBy) {
    return db.sqlQuery(PIN_SQL)
        .setParameter("roomId", roomId)
        .setParameter("messageId", messageId)
        .setParameter("pinnedBy", pinnedBy)
        .findOneOrEmpty()
        .map(
            row ->
                MessagePin.create(
                        row.getString("room_id"),
                        row.getString("message_id"),
                        row.getString("pinned_by"))
                    .pinnedAt(row.getTimestamp("pinned_at").toInstant().atOffset(ZoneOffset.UTC)));
  }

  @Override
  public int unpin(String roomId, String messageId) {
    return db.find(MessagePin.class)
        .where()
        .eq("roomId", roomId)
        .eq("messageId", messageId)
        .delete();
  }

  @Override
  public List<MessagePin> getPinnedMessages(String roomId) {
    return db.find(MessagePin.class).fetch("message").where().eq("roomId", roomId).findList();
  }

  @Override
  public boolean isPinned(String messageId) {
    return db.find(MessagePin.class).where().eq("messageId", messageId).exists();
  }
}
