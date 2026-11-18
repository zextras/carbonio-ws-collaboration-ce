// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import java.time.OffsetDateTime;

/**
 * Repository for write-path message operations using single-CTE queries. Each method performs
 * membership validation, data mutation, and ancillary updates in a single database round-trip.
 */
public interface MessageWriteRepository {

  /**
   * Inserts a new message atomically. When the message has an attachment, the attachment's {@code
   * file_metadata.created_at} and {@code updated_at} take the message's {@code created_at}.
   *
   * @return the outcome: {@link SendStatus#NOT_MEMBER} if the user is not a room member, {@link
   *     SendStatus#REPLY_NOT_FOUND} if the reply target is missing, in another room or hidden by
   *     the user's cleared history, {@link SendStatus#ID_CONFLICT} if the message ID already exists
   */
  SendResult sendMessage(
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
   * Edits a message text, setting edited=true. Only the original sender can edit. {@code pinned}
   * tells whether the message is the current room pin.
   *
   * @return the outcome: {@link EditStatus#NOT_VISIBLE} if the message is at or before the user's
   *     cleared-history instant, {@link EditStatus#NOT_ALLOWED} if user is not a member, not the
   *     sender, or message is deleted
   */
  EditResult editMessage(String messageId, String roomId, String userId, String newText);

  /**
   * Soft-deletes a message. Only the original sender can delete. When the message is the room pin,
   * the pin is removed and a MESSAGE_UNPINNED system event authored by the deleter is written in
   * the same transaction.
   *
   * @return the outcome: {@link DeleteStatus#NOT_VISIBLE} if the message is at or before the user's
   *     cleared-history instant, {@link DeleteStatus#NOT_ALLOWED} if user is not a member, not the
   *     sender, or message already deleted
   */
  DeleteResult deleteMessage(String messageId, String roomId, String userId);

  /**
   * Forwards a message from one room to another, inserting a new message. The {@code
   * file_metadata.created_at} and {@code updated_at} of the {@code newAttachmentId} copy (never of
   * the original attachment) take the new message's {@code created_at}.
   *
   * @param newAttachmentId the ID of the already-copied blob (null if no attachment)
   * @return the outcome: {@link ForwardStatus#NOT_MEMBER} if the user is not a member of either
   *     room, {@link ForwardStatus#MESSAGE_NOT_FOUND} if the original message is missing, deleted,
   *     in another room or hidden by the user's cleared history
   */
  ForwardResult forwardMessage(
      String sourceRoomId,
      String targetRoomId,
      String userId,
      String originalMessageId,
      String newMessageId,
      String newAttachmentId);

  enum SendStatus {
    SENT,
    NOT_MEMBER,
    REPLY_NOT_FOUND,
    ID_CONFLICT
  }

  record SendResult(SendStatus status, String messageId, OffsetDateTime createdAt) {

    public static SendResult rejected(SendStatus status) {
      return new SendResult(status, null, null);
    }
  }

  enum EditStatus {
    EDITED,
    NOT_ALLOWED,
    NOT_VISIBLE
  }

  record EditResult(
      EditStatus status,
      String messageId,
      String text,
      OffsetDateTime updatedAt,
      OffsetDateTime createdAt,
      boolean pinned) {

    public static EditResult rejected(EditStatus status) {
      return new EditResult(status, null, null, null, null, false);
    }
  }

  enum DeleteStatus {
    DELETED,
    NOT_ALLOWED,
    NOT_VISIBLE
  }

  record DeleteResult(
      DeleteStatus status,
      String messageId,
      String attachmentId,
      OffsetDateTime deletedAt,
      OffsetDateTime createdAt,
      Unpinned unpinned) {

    public static DeleteResult rejected(DeleteStatus status) {
      return new DeleteResult(status, null, null, null, null, null);
    }
  }

  record Unpinned(String systemEventId, OffsetDateTime systemEventCreatedAt) {}

  enum ForwardStatus {
    FORWARDED,
    NOT_MEMBER,
    MESSAGE_NOT_FOUND
  }

  record ForwardResult(
      ForwardStatus status,
      String messageId,
      OffsetDateTime createdAt,
      String originalText,
      String forwardedSenderId,
      OffsetDateTime forwardedAt,
      String attachmentId,
      String attachmentName,
      String attachmentMime,
      Long attachmentSize,
      String attachmentArea) {

    public static ForwardResult rejected(ForwardStatus status) {
      return new ForwardResult(status, null, null, null, null, null, null, null, null, null, null);
    }
  }

  enum SendOnceStatus {
    SENT,
    DUPLICATE,
    NOT_MEMBER,
    REPLY_NOT_FOUND
  }

  record SendOnceResult(SendOnceStatus status, String messageId, OffsetDateTime createdAt) {}
}
