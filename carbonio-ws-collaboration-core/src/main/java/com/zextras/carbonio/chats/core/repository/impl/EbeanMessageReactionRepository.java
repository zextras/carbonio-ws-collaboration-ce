// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import io.ebean.Database;
import io.ebean.SqlRow;
import io.ebean.annotation.Transactional;
import io.ebean.annotation.TxIsolation;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanMessageReactionRepository implements MessageReactionRepository {

  private static final int MAX_SET_ATTEMPTS = 5;

  private static final String SELECT_FOR_UPDATE_SQL =
      "SELECT reaction FROM chats.message_reaction WHERE message_id = :m AND user_id = :u"
          + " FOR UPDATE";

  private static final String INSERT_SQL =
      "INSERT INTO chats.message_reaction (message_id, user_id, reaction)"
          + " VALUES (:m, :u, :r) ON CONFLICT (message_id, user_id) DO NOTHING"
          + " RETURNING updated_at";

  private static final String UPDATE_SQL =
      "UPDATE chats.message_reaction SET reaction = :r, updated_at = clock_timestamp()"
          + " WHERE message_id = :m AND user_id = :u RETURNING updated_at";

  private static final String DELETE_SQL =
      "DELETE FROM chats.message_reaction WHERE message_id = :m AND user_id = :u"
          + " RETURNING reaction, clock_timestamp() AS deleted_at";

  private final Database db;

  @Inject
  public EbeanMessageReactionRepository(Database db) {
    this.db = db;
  }

  @Override
  public List<MessageReaction> getByMessageId(String messageId) {
    return db.find(MessageReaction.class).where().eq("id.messageId", messageId).findList();
  }

  @Override
  @Transactional(isolation = TxIsolation.READ_COMMITED)
  public SetResult set(String messageId, String userId, String reaction) {
    for (int attempt = 0; attempt < MAX_SET_ATTEMPTS; attempt++) {
      Optional<String> current = lockCurrent(messageId, userId);
      if (current.isEmpty()) {
        Optional<SqlRow> inserted =
            db.sqlQuery(INSERT_SQL)
                .setParameter("m", messageId)
                .setParameter("u", userId)
                .setParameter("r", reaction)
                .findOneOrEmpty();
        if (inserted.isPresent()) {
          return new SetResult(
              SetKind.ADDED,
              null,
              toOffsetDateTime(inserted.get().getTimestamp("updated_at")),
              null);
        }
        continue;
      }
      if (current.get().equals(reaction)) {
        return new SetResult(SetKind.UNCHANGED, null, null, null);
      }
      SqlRow updated =
          db.sqlQuery(UPDATE_SQL)
              .setParameter("m", messageId)
              .setParameter("u", userId)
              .setParameter("r", reaction)
              .findOne();
      return new SetResult(
          SetKind.UPDATED,
          current.get(),
          toOffsetDateTime(updated.getTimestamp("updated_at")),
          null);
    }
    throw new IllegalStateException(
        String.format("Reaction of user '%s' on message '%s' kept changing", userId, messageId));
  }

  @Override
  public Optional<Removed> remove(String messageId, String userId) {
    return db.sqlQuery(DELETE_SQL)
        .setParameter("m", messageId)
        .setParameter("u", userId)
        .findOneOrEmpty()
        .map(
            row ->
                new Removed(
                    row.getString("reaction"),
                    toOffsetDateTime(row.getTimestamp("deleted_at")),
                    null));
  }

  private Optional<String> lockCurrent(String messageId, String userId) {
    return db.sqlQuery(SELECT_FOR_UPDATE_SQL)
        .setParameter("m", messageId)
        .setParameter("u", userId)
        .findOneOrEmpty()
        .map(row -> row.getString("reaction"));
  }

  private static OffsetDateTime toOffsetDateTime(Timestamp ts) {
    return ts != null ? ts.toInstant().atOffset(ZoneOffset.UTC) : null;
  }

  @Override
  public void deleteByMessageId(String messageId) {
    db.find(MessageReaction.class).where().eq("id.messageId", messageId).delete();
  }
}
