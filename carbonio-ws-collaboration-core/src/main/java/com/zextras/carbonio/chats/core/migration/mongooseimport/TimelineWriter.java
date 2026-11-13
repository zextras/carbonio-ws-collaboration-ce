// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.migration.mongooseimport;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.zextras.carbonio.chats.core.migration.mongooseimport.TargetRooms.TargetRoom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public final class TimelineWriter implements AutoCloseable {

  public record Message(
      String id,
      String sender,
      String text,
      String replyTo,
      boolean forwarded,
      String forwardedSender,
      Instant forwardedAt,
      String attachmentId,
      boolean deleted,
      Instant deletedAt,
      Instant createdAt,
      Instant updatedAt) {

    public static Message text(String id, String sender, String text, String replyTo, Instant t) {
      return new Message(id, sender, text, replyTo, false, null, null, null, false, null, t, t);
    }

    public static Message attachment(
        String id, String sender, String text, String replyTo, String attachmentId, Instant t) {
      return new Message(
          id, sender, text, replyTo, false, null, null, attachmentId, false, null, t, t);
    }

    public static Message forward(
        String id,
        String sender,
        String text,
        String forwardedSender,
        Instant forwardedAt,
        String attachmentId,
        Instant t) {
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
        String id, String sender, String attachmentId, Instant deletedAt, Instant t) {
      return new Message(
          id, sender, "", null, false, null, null, attachmentId, true, deletedAt, t, deletedAt);
    }

    public static Message placeholder(String id, String sender, Instant t) {
      return new Message(id, sender, "", null, false, null, null, null, true, t, t, t);
    }
  }

  private static final int BATCH_SIZE = 10_000;
  private static final int BATCH_CHARS = 8_000_000;

  private final Connection target;
  private final FileLinker files;
  private final ImportContext context;
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
  private String firstMessageRoom;
  private String firstEventRoom;
  private long failedMam;
  private String failedRoom;
  private int messages;
  private int events;

  public TimelineWriter(Connection target, FileLinker files, ImportContext context) {
    this.target = target;
    this.files = files;
    this.context = context;
  }

  public void beginRoom(String roomId) {
    this.roomId = roomId;
    this.failedMam = 0;
    this.failedRoom = null;
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

  public String failedRoom() {
    return failedRoom;
  }

  public void addMessage(long mam, Message m) throws SQLException {
    int chars = m.text().length();
    try {
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
    } catch (RuntimeException e) {
      insertMessage.discardRow();
      throw e;
    }
    if (pendingMessages == 0) {
      firstMessageMam = mam;
      firstMessageRoom = roomId;
    }
    pendingMessageChars += chars;
    messages++;
    if (++pendingMessages >= BATCH_SIZE || pendingMessageChars >= BATCH_CHARS) {
      flushMessages();
    }
  }

  public void addEvent(
      long mam, String id, String type, String actorId, String content, Instant time)
      throws SQLException {
    bindEvent(id, type, actorId, content, time);
    if (pendingEvents == 0) {
      firstEventMam = mam;
      firstEventRoom = roomId;
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
    Instant createdAt = room.createdAt();
    Instant first = clock.first();
    if (createdAt == null || !MamTime.inRange(createdAt) || createdAt.isAfter(context.t0())) {
      context.count(SkipReason.ROOM_CREATED_AT_OUT_OF_RANGE, room.id(), 0);
      createdAt = first == null ? context.t0() : first.minus(1, ChronoUnit.MICROS);
    }
    if (first != null) {
      Instant before = first.minus(1, ChronoUnit.MICROS);
      if (before.isBefore(createdAt)) {
        createdAt = before;
      }
    }
    String content =
        JsonNodeFactory.instance.objectNode().put("creatorId", room.creatorId()).toString();
    if (pendingEvents == 0) {
      firstEventMam = 0;
      firstEventRoom = roomId;
    }
    bindEvent(id, "ROOM_CREATED", room.creatorId(), content, createdAt);
    events++;
    if (++pendingEvents >= BATCH_SIZE) {
      flushEvents();
    }
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
      failedRoom = firstMessageRoom;
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
      failedRoom = firstEventRoom;
      throw e;
    }
    pendingEvents = 0;
  }

  private void bindEvent(String id, String type, String actorId, String content, Instant time)
      throws SQLException {
    try {
      insertEvent.text(id).text(roomId).text(type).text(actorId).text(content).time(time).endRow();
    } catch (RuntimeException e) {
      insertEvent.discardRow();
      throw e;
    }
  }
}
