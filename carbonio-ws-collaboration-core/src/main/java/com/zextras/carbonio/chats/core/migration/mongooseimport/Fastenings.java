// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public final class Fastenings implements AutoCloseable {

  public record Counts(int edits, int reactions) {}

  private static final int BATCH_SIZE = 500;
  private static final int BATCH_CHARS = 8_000_000;

  private static final String FOLD_EDITS =
      "UPDATE chats.message m SET text = w.body, edited = TRUE, updated_at = w.edit_time"
          + " FROM (SELECT DISTINCT ON (e.target_id) e.target_id, e.body, e.edit_time"
          + " FROM mongoose_edits e JOIN chats.message t ON t.id = e.target_id"
          + " AND NOT t.deleted AND t.sender_id = e.sender_id AND e.mam > e.target_mam"
          + " ORDER BY e.target_id, e.mam DESC) w WHERE m.id = w.target_id";

  private static final String FOLD_REACTIONS =
      "INSERT INTO chats.message_reaction (message_id, user_id, reaction, updated_at)"
          + " SELECT w.target_id, w.sender_id, w.body, w.reaction_time FROM"
          + " (SELECT DISTINCT ON (x.target_id, x.sender_id) x.target_id, x.sender_id, x.body,"
          + " x.reaction_time FROM mongoose_reactions x JOIN chats.message t"
          + " ON t.id = x.target_id AND NOT t.deleted AND x.mam > x.target_mam"
          + " WHERE char_length(x.body) <= 32 ORDER BY x.target_id, x.sender_id, x.mam DESC) w"
          + " WHERE w.body <> ''";

  private final Connection target;
  private final CopyRows edits =
      new CopyRows(
          "mongoose_edits", "room_id, mam, target_mam, target_id, sender_id, body, edit_time");
  private final CopyRows reactions =
      new CopyRows(
          "mongoose_reactions", "mam, target_mam, target_id, sender_id, body, reaction_time");
  private int pendingEditChars;
  private int pendingReactionChars;
  private int staged;

  public Fastenings(Connection target) throws SQLException {
    this.target = target;
    try (Statement st = target.createStatement()) {
      st.execute(
          "CREATE TEMP TABLE mongoose_edits (room_id TEXT NOT NULL, mam BIGINT NOT NULL,"
              + " target_mam BIGINT NOT NULL, target_id TEXT NOT NULL, sender_id TEXT NOT NULL,"
              + " body TEXT NOT NULL, edit_time TIMESTAMPTZ NOT NULL) ON COMMIT DROP");
      st.execute("CREATE INDEX mongoose_edits_mam_idx ON mongoose_edits (mam)");
      st.execute(
          "CREATE TEMP TABLE mongoose_reactions (mam BIGINT NOT NULL, target_mam BIGINT NOT NULL,"
              + " target_id TEXT NOT NULL, sender_id TEXT NOT NULL, body TEXT NOT NULL,"
              + " reaction_time TIMESTAMPTZ NOT NULL) ON COMMIT DROP");
    }
  }

  public void stageEdit(
      String roomId,
      long mam,
      long targetMam,
      String targetId,
      String senderId,
      String body,
      Instant editTime)
      throws SQLException {
    edits
        .text(roomId)
        .number(mam)
        .number(targetMam)
        .text(targetId)
        .text(senderId)
        .text(body)
        .time(editTime)
        .endRow();
    pendingEditChars += body.length();
    staged++;
    if (edits.rows() >= BATCH_SIZE || pendingEditChars >= BATCH_CHARS) {
      flushEdits();
    }
  }

  public void stageReaction(
      long mam, long targetMam, String targetId, String senderId, String body, Instant time)
      throws SQLException {
    reactions
        .number(mam)
        .number(targetMam)
        .text(targetId)
        .text(senderId)
        .text(body)
        .time(time)
        .endRow();
    pendingReactionChars += body.length();
    staged++;
    if (reactions.rows() >= BATCH_SIZE || pendingReactionChars >= BATCH_CHARS) {
      flushReactions();
    }
  }

  public Counts fold() throws SQLException {
    if (staged == 0) {
      return new Counts(0, 0);
    }
    flushEdits();
    flushReactions();
    try (Statement st = target.createStatement()) {
      st.execute("ANALYZE mongoose_edits");
      st.execute("ANALYZE mongoose_reactions");
      int folded = st.executeUpdate(FOLD_EDITS);
      int inserted = st.executeUpdate(FOLD_REACTIONS);
      return new Counts(folded, inserted);
    }
  }

  void flushEdits() throws SQLException {
    edits.flush(target);
    pendingEditChars = 0;
  }

  private void flushReactions() throws SQLException {
    reactions.flush(target);
    pendingReactionChars = 0;
  }

  @Override
  public void close() {
    edits.clear();
    reactions.clear();
  }
}
