// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import java.util.List;
import java.util.Optional;

public interface MessagePinRepository {

  /**
   * Pins a message in a room with a single upsert. A room has a single pin: any existing pin is
   * replaced. The message row is share-locked and must be in the room and not deleted, so a
   * concurrent delete either waits for this pin to commit or turns it into a no-op.
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param pinnedBy user identifier of the user pinning the message
   * @return the written {@link MessagePin}, or empty when the message is missing, in another room
   *     or deleted
   */
  Optional<MessagePin> pin(String roomId, String messageId, String pinnedBy);

  /**
   * Unpins a message from a room, only when it is the room pin
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @return the number of removed pins (0 or 1)
   */
  int unpin(String roomId, String messageId);

  /**
   * Gets the pinned messages for a room (at most one, since a room has a single pin)
   *
   * @param roomId room identifier
   * @return {@link List} of {@link MessagePin} (0 or 1 element)
   */
  List<MessagePin> getPinnedMessages(String roomId);

  /**
   * Checks whether a message is currently pinned in any room
   *
   * @param messageId message identifier
   * @return {@code true} if pinned, {@code false} otherwise
   */
  boolean isPinned(String messageId);
}
