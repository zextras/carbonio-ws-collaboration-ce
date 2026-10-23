// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public final class Fastenings implements AutoCloseable {

  public record Counts(int edits, int reactions) {}

  private static final int BATCH_SIZE = 500;
  private static final int BATCH_CHARS = 8_000_000;

  private static final String STAGE_EDIT =
      "INSERT INTO mongoose_edits (mam, target_mam, sender_id, body, edit_time)"
          + " VALUES (?, ?, ?, ?, ?)";

  private static final String STAGE_REACTION =
      "INSERT INTO mongoose_reactions (mam, target_mam, sender_id, body, reaction_time)"
          + " VALUES (?, ?, ?, ?, ?)";

  private static final String FOLD_EDITS =
      "UPDATE chats.message m SET text = w.body, edited = TRUE, updated_at = w.edit_time"
          + " FROM (SELECT DISTINCT ON (e.target_mam) t.item_id, e.body, e.edit_time"
          + " FROM mongoose_edits e JOIN mongoose_room_rows t ON t.mam = e.target_mam"
          + " AND t.outcome = 1 AND NOT t.deleted AND t.sender_id = e.sender_id AND e.mam > t.mam"
          + " ORDER BY e.target_mam, e.mam DESC) w WHERE m.id = w.item_id";

  private static final String FOLD_REACTIONS =
      "INSERT INTO chats.message_reaction (message_id, user_id, reaction, created_at)"
          + " SELECT w.item_id, w.sender_id, w.body, w.reaction_time FROM"
          + " (SELECT DISTINCT ON (x.target_mam, x.sender_id) t.item_id, x.sender_id, x.body,"
          + " x.reaction_time FROM mongoose_reactions x JOIN mongoose_room_rows t"
          + " ON t.mam = x.target_mam AND t.outcome = 1 AND NOT t.deleted AND x.mam > t.mam"
          + " WHERE char_length(x.body) <= 32 ORDER BY x.target_mam, x.sender_id, x.mam DESC) w"
          + " WHERE w.body <> ''";

  private final Connection target;
  private final PreparedStatement edits;
  private final PreparedStatement reactions;
  private int pendingEdits;
  private int pendingEditChars;
  private int pendingReactions;
  private int pendingReactionChars;
  private int staged;

  public Fastenings(Connection target) throws SQLException {
    this.target = target;
    try (Statement st = target.createStatement()) {
      st.execute(
          "CREATE TEMP TABLE mongoose_edits (mam BIGINT, target_mam BIGINT NOT NULL,"
              + " sender_id TEXT NOT NULL, body TEXT NOT NULL, edit_time TIMESTAMP NOT NULL)"
              + " ON COMMIT DROP");
      st.execute(
          "CREATE TEMP TABLE mongoose_reactions (mam BIGINT, target_mam BIGINT NOT NULL,"
              + " sender_id TEXT NOT NULL, body TEXT NOT NULL, reaction_time TIMESTAMP NOT NULL)"
              + " ON COMMIT DROP");
    }
    this.edits = target.prepareStatement(STAGE_EDIT);
    this.reactions = target.prepareStatement(STAGE_REACTION);
  }

  public void reset() throws SQLException {
    pendingEdits = 0;
    pendingEditChars = 0;
    pendingReactions = 0;
    pendingReactionChars = 0;
    staged = 0;
    edits.clearBatch();
    reactions.clearBatch();
    try (Statement st = target.createStatement()) {
      st.execute("TRUNCATE mongoose_edits, mongoose_reactions");
    }
  }

  public void stageEdit(
      long mam, long targetMam, String senderId, String body, LocalDateTime editTime)
      throws SQLException {
    stage(edits, mam, targetMam, senderId, body, editTime);
    pendingEditChars += body.length();
    staged++;
    if (++pendingEdits >= BATCH_SIZE || pendingEditChars >= BATCH_CHARS) {
      flushEdits();
    }
  }

  public void stageReaction(
      long mam, long targetMam, String senderId, String body, LocalDateTime reactionTime)
      throws SQLException {
    stage(reactions, mam, targetMam, senderId, body, reactionTime);
    pendingReactionChars += body.length();
    staged++;
    if (++pendingReactions >= BATCH_SIZE || pendingReactionChars >= BATCH_CHARS) {
      flushReactions();
    }
  }

  public int staged() {
    return staged;
  }

  public Counts fold() throws SQLException {
    if (staged == 0) {
      return new Counts(0, 0);
    }
    flushEdits();
    flushReactions();
    try (Statement st = target.createStatement()) {
      int folded = st.executeUpdate(FOLD_EDITS);
      int inserted = st.executeUpdate(FOLD_REACTIONS);
      return new Counts(folded, inserted);
    }
  }

  public void analyze() throws SQLException {
    flushEdits();
    flushReactions();
    try (Statement st = target.createStatement()) {
      st.execute("ANALYZE mongoose_edits");
      st.execute("ANALYZE mongoose_reactions");
    }
  }

  @Override
  public void close() throws SQLException {
    edits.close();
    reactions.close();
  }

  private void flushEdits() throws SQLException {
    if (pendingEdits > 0) {
      edits.executeBatch();
      pendingEdits = 0;
      pendingEditChars = 0;
    }
  }

  private void flushReactions() throws SQLException {
    if (pendingReactions > 0) {
      reactions.executeBatch();
      pendingReactions = 0;
      pendingReactionChars = 0;
    }
  }

  private static void stage(
      PreparedStatement ps, long mam, long targetMam, String senderId, String body, LocalDateTime t)
      throws SQLException {
    ps.setLong(1, mam);
    ps.setLong(2, targetMam);
    ps.setString(3, senderId);
    ps.setString(4, body);
    ps.setObject(5, t);
    ps.addBatch();
  }
}
