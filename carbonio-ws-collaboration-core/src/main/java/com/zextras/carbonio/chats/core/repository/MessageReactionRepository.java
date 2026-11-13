// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository {

  /**
   * Retrieves all reactions for a message
   *
   * @param messageId message identifier
   * @return {@link List} of {@link MessageReaction}
   */
  List<MessageReaction> getByMessageId(String messageId);

  enum SetKind {
    ADDED,
    UPDATED,
    UNCHANGED
  }

  record SetResult(
      SetKind kind,
      String previousReaction,
      OffsetDateTime updatedAt,
      OffsetDateTime messageCreatedAt) {

    public SetResult withMessageCreatedAt(OffsetDateTime messageCreatedAt) {
      return new SetResult(kind, previousReaction, updatedAt, messageCreatedAt);
    }
  }

  record Removed(String reaction, OffsetDateTime deletedAt, OffsetDateTime messageCreatedAt) {

    public Removed withMessageCreatedAt(OffsetDateTime messageCreatedAt) {
      return new Removed(reaction, deletedAt, messageCreatedAt);
    }
  }

  /**
   * Sets the single reaction a user has on a message: inserts it, replaces a different emoji, or
   * leaves the same emoji untouched. The write time is taken from the database.
   *
   * @param messageId message identifier
   * @param userId user identifier
   * @param reaction emoji
   * @return the outcome; updatedAt is the stored value on ADDED and UPDATED, null on UNCHANGED;
   *     messageCreatedAt is left null for the caller to fill
   */
  SetResult set(String messageId, String userId, String reaction);

  /**
   * Deletes the reaction a user has on a message
   *
   * @param messageId message identifier
   * @param userId user identifier
   * @return the removed emoji and the database time of the delete (messageCreatedAt is left null
   *     for the caller to fill), empty if there was none
   */
  Optional<Removed> remove(String messageId, String userId);

  /**
   * Deletes all reactions for a message
   *
   * @param messageId message identifier
   */
  void deleteByMessageId(String messageId);
}
