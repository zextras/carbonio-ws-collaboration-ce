// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Repository for write-path message operations using single-CTE queries. Each method performs
 * membership validation, data mutation, and ancillary updates (read markers) in a single database
 * round-trip.
 */
public interface MessageWriteRepository {

  /**
   * Inserts a new message and updates the sender's read marker atomically.
   *
   * @return empty if the user is not a room member or message ID conflicts
   */
  Optional<SendResult> sendMessage(
      String roomId,
      String userId,
      String text,
      String messageId,
      String replyToId,
      String attachmentId);

  /**
   * Text send that is idempotent on {@code tempId}: in the same statement it claims {@code (roomId,
   * userId, tempId)} (taking over a claim older than one hour) and inserts the message only when
   * the claim is new. A duplicate returns the message id of the live claim ({@code null} when that
   * claim vanished meanwhile) and no {@code createdAt}.
   */
  SendOnceResult sendMessageOnce(
      String roomId, String userId, String text, String messageId, String replyToId, String tempId);

  /**
   * Edits a message text, setting edited=true. Only the original sender can edit.
   *
   * @return empty if user is not a member, not the sender, or message is deleted
   */
  Optional<EditResult> editMessage(String messageId, String roomId, String userId, String newText);

  /**
   * Soft-deletes a message. Only the original sender can delete. When the message is the room pin,
   * the pin is removed and a MESSAGE_UNPINNED system event authored by the deleter is written in
   * the same transaction.
   *
   * @return empty if user is not a member, not the sender, or message already deleted
   */
  Optional<DeleteResult> deleteMessage(String messageId, String roomId, String userId);

  /**
   * Forwards a message from one room to another, inserting a new message and updating read marker.
   *
   * @param newAttachmentId the ID of the already-copied blob (null if no attachment)
   * @return empty if the user is not a member of either room or original message not found
   */
  Optional<ForwardResult> forwardMessage(
      String sourceRoomId,
      String targetRoomId,
      String userId,
      String originalMessageId,
      String newMessageId,
      String newAttachmentId);

  record SendResult(String messageId, OffsetDateTime createdAt) {}

  record EditResult(
      String messageId, String text, OffsetDateTime updatedAt, OffsetDateTime createdAt) {}

  record DeleteResult(String messageId, String attachmentId, Unpinned unpinned) {

    public DeleteResult(String messageId, String attachmentId) {
      this(messageId, attachmentId, null);
    }
  }

  record Unpinned(String systemEventId, OffsetDateTime systemEventCreatedAt) {}

  record ForwardResult(
      String messageId,
      OffsetDateTime createdAt,
      String originalText,
      String forwardedSenderId,
      OffsetDateTime forwardedAt,
      String attachmentId,
      String attachmentName,
      String attachmentMime,
      Long attachmentSize,
      String attachmentArea) {}

  enum SendOnceStatus {
    SENT,
    DUPLICATE,
    NOT_MEMBER,
    REPLY_NOT_FOUND
  }

  record SendOnceResult(SendOnceStatus status, String messageId, OffsetDateTime createdAt) {}
}
