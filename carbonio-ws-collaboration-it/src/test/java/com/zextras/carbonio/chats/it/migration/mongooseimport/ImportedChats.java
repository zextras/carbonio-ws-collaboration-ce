// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.migration.mongooseimport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ImportedChats {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final String database;

  ImportedChats(String database) {
    this.database = database;
  }

  public record Message(
      String id,
      String roomId,
      String senderId,
      String text,
      String replyToId,
      boolean forwarded,
      String forwardedText,
      String forwardedSenderId,
      LocalDateTime forwardedAt,
      String attachmentId,
      boolean edited,
      boolean deleted,
      LocalDateTime deletedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {}

  public record SystemEvent(
      String id,
      String roomId,
      String type,
      String actorId,
      String content,
      LocalDateTime createdAt) {

    public JsonNode json() {
      try {
        return JSON.readTree(content);
      } catch (Exception e) {
        throw new IllegalStateException("Invalid system event content: " + content, e);
      }
    }
  }

  public record Reaction(
      String messageId, String userId, String reaction, LocalDateTime createdAt) {}

  public record Pin(String roomId, String messageId, String pinnedBy, LocalDateTime pinnedAt) {}

  public record ReadMarker(String userId, String roomId, LocalDateTime itemCreatedAt) {}

  public record File(
      String id,
      String roomId,
      String userId,
      String messageId,
      String legacyMessageId,
      String name,
      String area) {}

  public record TimelineItem(String id, String kind, LocalDateTime createdAt) {}

  public Optional<Message> message(String roomId, long mamId) {
    return messageById(ExpectedIds.message(roomId, mamId));
  }

  public Optional<Message> messageById(String id) {
    return query(MESSAGE_SQL + " WHERE id = ?", this::message, id).stream().findFirst();
  }

  public List<Message> messages(String roomId) {
    return query(MESSAGE_SQL + " WHERE room_id = ? ORDER BY created_at, id", this::message, roomId);
  }

  public Optional<SystemEvent> systemEvent(String roomId, long mamId) {
    return query(EVENT_SQL + " WHERE id = ?", this::event, ExpectedIds.systemEvent(roomId, mamId))
        .stream()
        .findFirst();
  }

  public List<SystemEvent> systemEvents(String roomId) {
    return query(EVENT_SQL + " WHERE room_id = ? ORDER BY created_at, id", this::event, roomId);
  }

  public List<TimelineItem> timeline(String roomId) {
    return query(
        "SELECT id, 'MESSAGE' AS kind, created_at FROM chats.message WHERE room_id = ?"
            + " UNION ALL SELECT id, type, created_at FROM chats.system_event WHERE room_id = ?"
            + " ORDER BY created_at, id",
        rs -> new TimelineItem(rs.getString(1), rs.getString(2), time(rs, 3)),
        roomId,
        roomId);
  }

  public List<Reaction> reactions(String messageId) {
    return query(
        "SELECT message_id, user_id, reaction, created_at FROM chats.message_reaction"
            + " WHERE message_id = ? ORDER BY user_id",
        rs -> new Reaction(rs.getString(1), rs.getString(2), rs.getString(3), time(rs, 4)),
        messageId);
  }

  public Optional<Pin> pin(String roomId) {
    return query(
            "SELECT room_id, message_id, pinned_by, pinned_at FROM chats.message_pin"
                + " WHERE room_id = ?",
            rs -> new Pin(rs.getString(1), rs.getString(2), rs.getString(3), time(rs, 4)),
            roomId)
        .stream()
        .findFirst();
  }

  public Optional<ReadMarker> readMarker(String userId, String roomId) {
    return query(
            "SELECT user_id, room_id, item_created_at FROM chats.read_marker"
                + " WHERE user_id = ? AND room_id = ?",
            rs -> new ReadMarker(rs.getString(1), rs.getString(2), time(rs, 3)),
            userId,
            roomId)
        .stream()
        .findFirst();
  }

  public Optional<LocalDateTime> lastSeen(String userId) {
    return query(
            "SELECT last_seen_at FROM chats.user_last_seen WHERE user_id = ?",
            rs -> time(rs, 1),
            userId)
        .stream()
        .findFirst();
  }

  public List<String> contacts(String userId) {
    return query(
        "SELECT contact_id FROM chats.user_contact WHERE user_id = ? ORDER BY contact_id",
        rs -> rs.getString(1),
        userId);
  }

  public Optional<File> file(String fileId) {
    return query(
            "SELECT id, room_id, user_id, chat_message_id, message_id, name, area"
                + " FROM chats.file_metadata WHERE id = ?",
            rs ->
                new File(
                    rs.getString(1),
                    rs.getString(2),
                    rs.getString(3),
                    rs.getString(4),
                    rs.getString(5),
                    rs.getString(6),
                    rs.getString(7)),
            fileId)
        .stream()
        .findFirst();
  }

  public long count(String sql, Object... parameters) {
    return query(sql, rs -> rs.getLong(1), parameters).get(0);
  }

  public <T> List<T> query(String sql, RowMapper<T> mapper, Object... parameters) {
    try (Connection connection = MongooseImportDatabase.get().connect(database);
        PreparedStatement statement = connection.prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        statement.setObject(i + 1, parameters[i]);
      }
      try (ResultSet rs = statement.executeQuery()) {
        List<T> rows = new ArrayList<>();
        while (rs.next()) {
          rows.add(mapper.map(rs));
        }
        return rows;
      }
    } catch (SQLException e) {
      throw new IllegalStateException("Query failed on " + database + ": " + sql, e);
    }
  }

  @FunctionalInterface
  public interface RowMapper<T> {
    T map(ResultSet rs) throws SQLException;
  }

  private static final String MESSAGE_SQL =
      "SELECT id, room_id, sender_id, text, reply_to_id, forwarded, forwarded_text,"
          + " forwarded_sender_id, forwarded_at, attachment_id, edited, deleted, deleted_at,"
          + " created_at, updated_at FROM chats.message";

  private static final String EVENT_SQL =
      "SELECT id, room_id, type, actor_id, content, created_at FROM chats.system_event";

  private Message message(ResultSet rs) throws SQLException {
    return new Message(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        rs.getBoolean(6),
        rs.getString(7),
        rs.getString(8),
        time(rs, 9),
        rs.getString(10),
        rs.getBoolean(11),
        rs.getBoolean(12),
        time(rs, 13),
        time(rs, 14),
        time(rs, 15));
  }

  private SystemEvent event(ResultSet rs) throws SQLException {
    return new SystemEvent(
        rs.getString(1),
        rs.getString(2),
        rs.getString(3),
        rs.getString(4),
        rs.getString(5),
        time(rs, 6));
  }

  private static LocalDateTime time(ResultSet rs, int column) throws SQLException {
    return rs.getObject(column, LocalDateTime.class);
  }
}
