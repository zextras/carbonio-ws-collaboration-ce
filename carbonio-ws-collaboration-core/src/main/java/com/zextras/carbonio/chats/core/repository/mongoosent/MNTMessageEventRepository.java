// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.mongoosent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageEvent;
import com.zextras.carbonio.chats.core.data.entity.mongoosent.MNTMessageEventType;
import io.ebean.Database;
import java.util.Map;
import java.util.UUID;

/**
 * Repository for message event log. This is an APPEND-ONLY repository: only insert operations are
 * supported. No queries should be performed on this table in normal application flow.
 */
@Singleton
public class MNTMessageEventRepository {

  private final Database db;

  @Inject
  public MNTMessageEventRepository(Database db) {
    this.db = db;
  }

  /**
   * Logs a message event. This is a fire-and-forget operation for audit purposes.
   *
   * @param messageId The ID of the message this event relates to
   * @param roomId The room where the message belongs
   * @param userId The user who performed the action
   * @param eventType The type of event
   * @param payload Additional event data (flexible JSONB)
   */
  public void logEvent(
      String messageId,
      String roomId,
      String userId,
      MNTMessageEventType eventType,
      Map<String, Object> payload) {
    MNTMessageEvent event =
        MNTMessageEvent.create()
            .id(UUID.randomUUID().toString())
            .messageId(messageId)
            .roomId(roomId)
            .userId(userId)
            .eventType(eventType)
            .payload(payload);
    db.insert(event);
  }

  /**
   * Convenience method for logging MESSAGE_CREATED events.
   */
  public void logMessageCreated(String messageId, String roomId, String userId, String text, String replyToId) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.MESSAGE_CREATED,
        Map.of(
            "text", text,
            "replyToId", replyToId != null ? replyToId : ""));
  }

  /**
   * Convenience method for logging MESSAGE_EDITED events.
   */
  public void logMessageEdited(String messageId, String roomId, String userId, String newText, String previousText) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.MESSAGE_EDITED,
        Map.of(
            "newText", newText,
            "previousText", previousText != null ? previousText : ""));
  }

  /**
   * Convenience method for logging MESSAGE_DELETED events.
   */
  public void logMessageDeleted(String messageId, String roomId, String userId, String previousText) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.MESSAGE_DELETED,
        Map.of("previousText", previousText != null ? previousText : ""));
  }

  /**
   * Convenience method for logging REACTION_ADDED events.
   */
  public void logReactionAdded(String messageId, String roomId, String userId, String reaction) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.REACTION_ADDED,
        Map.of("reaction", reaction));
  }

  /**
   * Convenience method for logging REACTION_REMOVED events.
   */
  public void logReactionRemoved(String messageId, String roomId, String userId, String reaction) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.REACTION_REMOVED,
        Map.of("reaction", reaction));
  }

  /**
   * Convenience method for logging MESSAGE_FORWARDED events.
   */
  public void logMessageForwarded(
      String newMessageId,
      String targetRoomId,
      String userId,
      String originalMessageId,
      String originalRoomId) {
    logEvent(
        newMessageId,
        targetRoomId,
        userId,
        MNTMessageEventType.MESSAGE_FORWARDED,
        Map.of(
            "originalMessageId", originalMessageId,
            "originalRoomId", originalRoomId));
  }

  /**
   * Convenience method for logging ATTACHMENT_ADDED events.
   */
  public void logAttachmentAdded(
      String messageId,
      String roomId,
      String userId,
      String attachmentId,
      String fileName,
      String mimeType,
      long fileSize) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.ATTACHMENT_ADDED,
        Map.of(
            "attachmentId", attachmentId,
            "fileName", fileName,
            "mimeType", mimeType,
            "fileSize", fileSize));
  }

  /**
   * Convenience method for logging ATTACHMENT_DELETED events.
   */
  public void logAttachmentDeleted(
      String messageId, String roomId, String userId, String attachmentId, String fileName) {
    logEvent(
        messageId,
        roomId,
        userId,
        MNTMessageEventType.ATTACHMENT_DELETED,
        Map.of("attachmentId", attachmentId, "fileName", fileName));
  }
}
