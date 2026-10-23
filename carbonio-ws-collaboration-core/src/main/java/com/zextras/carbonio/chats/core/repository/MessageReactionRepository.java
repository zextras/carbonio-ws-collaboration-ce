// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
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

  /**
   * Retrieves the single reaction a user set on a message
   *
   * @param messageId message identifier
   * @param userId user identifier
   * @return The {@link MessageReaction} wrapped in an {@link Optional}
   */
  Optional<MessageReaction> getByUserAndMessage(String messageId, String userId);

  /**
   * Inserts a new reaction
   *
   * @param reaction {@link MessageReaction} to insert
   * @return {@link MessageReaction} inserted
   */
  MessageReaction insert(MessageReaction reaction);

  /**
   * Updates an existing reaction (e.g. when a user changes emoji)
   *
   * @param reaction {@link MessageReaction} to update
   * @return {@link MessageReaction} updated
   */
  MessageReaction update(MessageReaction reaction);

  /**
   * Deletes the reaction a user set on a message
   *
   * @param messageId message identifier
   * @param userId user identifier
   */
  void delete(String messageId, String userId);

  /**
   * Deletes all reactions for a message
   *
   * @param messageId message identifier
   */
  void deleteByMessageId(String messageId);
}
