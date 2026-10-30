// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

/**
 * Remembers for one hour which message the first send of a sender in a room with a given client
 * {@code tempId} created, so that a retried send returns that message instead of a duplicate.
 */
public interface MessageSendDedupRepository {

  /**
   * Claims {@code (roomId, senderId, tempId)} for {@code messageId}; a claim older than one hour is
   * taken over. When not claimed, {@code existingMessageId} is the message id of the live claim, or
   * {@code null} if that claim was released meanwhile.
   */
  Claim claim(String roomId, String senderId, String tempId, String messageId);

  /** Deletes the claim only while it still belongs to {@code messageId}. */
  void release(String roomId, String senderId, String tempId, String messageId);

  /** Deletes up to {@code batchSize} expired claims, skipping rows locked by others. */
  int purgeExpired(int batchSize);

  record Claim(boolean claimed, String existingMessageId) {}
}
