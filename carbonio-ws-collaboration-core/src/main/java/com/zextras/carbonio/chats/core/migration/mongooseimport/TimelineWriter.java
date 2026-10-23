// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class TimelineWriter implements AutoCloseable {

  public record Message(
      String id,
      String sender,
      String text,
      String replyTo,
      boolean forwarded,
      String forwardedSender,
      LocalDateTime forwardedAt,
      String attachmentId,
      boolean deleted,
      LocalDateTime deletedAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {

    public static Message text(
        String id, String sender, String text, String replyTo, LocalDateTime t) {
      return new Message(id, sender, text, replyTo, false, null, null, null, false, null, t, t);
    }

    public static Message attachment(
        String id,
        String sender,
        String text,
        String replyTo,
        String attachmentId,
        LocalDateTime t) {
      return new Message(
          id, sender, text, replyTo, false, null, null, attachmentId, false, null, t, t);
    }

    public static Message forward(
        String id,
        String sender,
        String text,
        String forwardedSender,
        LocalDateTime forwardedAt,
        String attachmentId,
        LocalDateTime t) {
      return new Message(
          id,
          sender,
          text,
          null,
          true,
          forwardedSender,
          forwardedAt,
          attachmentId,
          false,
          null,
          t,
          t);
    }

    public static Message tombstone(
        String id, String sender, String attachmentId, LocalDateTime deletedAt, LocalDateTime t) {
      return new Message(
          id, sender, "", null, false, null, null, attachmentId, true, deletedAt, t, deletedAt);
    }

    public static Message placeholder(String id, String sender, LocalDateTime t) {
      return new Message(id, sender, "", null, false, null, null, null, true, t, t, t);
    }
  }

  private static final int BATCH_SIZE = 500;
  private static final int BATCH_CHARS = 8_000_000;

  private final Connection target;
  private final FileLinker files;
  private final CopyRows insertMessage =
      new CopyRows(
          "chats.message",
          "id, room_id, sender_id, text, reply_to_id, forwarded, forwarded_sender_id, forwarded_at,"
              + " attachment_id, deleted, deleted_at, created_at, updated_at");
  private final CopyRows insertEvent =
      new CopyRows("chats.system_event", "id, room_id, type, actor_id, content, created_at");
  private String roomId;
  private int pendingMessages;
  private int pendingMessageChars;
  private int pendingEvents;
  private long firstMessageMam;
  private long firstEventMam;
  private long failedMam;
  private int messages;
  private int events;

  public TimelineWriter(Connection target, FileLinker files) {
    this.target = target;
    this.files = files;
  }

  public void beginRoom(String roomId) {
    this.roomId = roomId;
    this.failedMam = 0;
  }

  public int messages() {
    return messages;
  }

  public int events() {
    return events;
  }

  public long failedMam() {
    return failedMam;
  }

  public void addMessage(long mam, Message m) throws SQLException {
    insertMessage
        .text(m.id())
        .text(roomId)
        .text(m.sender())
        .text(m.text())
        .text(m.replyTo())
        .bool(m.forwarded())
        .text(m.forwardedSender())
        .time(m.forwardedAt())
        .text(m.attachmentId())
        .bool(m.deleted())
        .time(m.deletedAt())
        .time(m.createdAt())
        .time(m.updatedAt())
        .endRow();
    if (pendingMessages == 0) {
      firstMessageMam = mam;
    }
    pendingMessageChars += m.text().length();
    messages++;
    if (++pendingMessages >= BATCH_SIZE || pendingMessageChars >= BATCH_CHARS) {
      flushMessages();
    }
  }

  public void addEvent(
      long mam, String id, String type, String actorId, String content, LocalDateTime time)
      throws SQLException {
    bindEvent(id, type, actorId, content, time);
    if (pendingEvents == 0) {
      firstEventMam = mam;
    }
    events++;
    if (++pendingEvents >= BATCH_SIZE) {
      flushEvents();
    }
  }

  public void flush() throws SQLException {
    flushMessages();
    flushEvents();
  }

  public void roomCreated(TargetRoom room, String id, RoomClock clock) throws SQLException {
    LocalDateTime createdAt = room.createdAt();
    LocalDateTime first = clock.first();
    if (first != null) {
      LocalDateTime before = first.minus(1, ChronoUnit.MILLIS);
      if (before.isBefore(createdAt)) {
        createdAt = before;
      }
    }
    String content =
        JsonNodeFactory.instance.objectNode().put("creatorId", room.creatorId()).toString();
    bindEvent(id, "ROOM_CREATED", room.creatorId(), content, createdAt);
    events++;
    pendingEvents++;
    firstEventMam = 0;
    flushEvents();
  }

  @Override
  public void close() {
    insertMessage.clear();
    insertEvent.clear();
  }

  private void flushMessages() throws SQLException {
    try {
      insertMessage.flush(target);
    } catch (SQLException e) {
      failedMam = firstMessageMam;
      throw e;
    }
    pendingMessages = 0;
    pendingMessageChars = 0;
    files.flush();
  }

  private void flushEvents() throws SQLException {
    try {
      insertEvent.flush(target);
    } catch (SQLException e) {
      failedMam = firstEventMam;
      throw e;
    }
    pendingEvents = 0;
  }

  private void bindEvent(String id, String type, String actorId, String content, LocalDateTime time)
      throws SQLException {
    insertEvent.text(id).text(roomId).text(type).text(actorId).text(content).time(time).endRow();
  }
}
