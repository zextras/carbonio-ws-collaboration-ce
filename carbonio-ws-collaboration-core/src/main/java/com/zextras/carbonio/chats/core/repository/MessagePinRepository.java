// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import java.util.List;

public interface MessagePinRepository {

  /**
   * Pins a message in a room. A room has a single pin: any existing pin is replaced.
   *
   * @param roomId room identifier
   * @param messageId message identifier
   * @param pinnedBy user identifier of the user pinning the message
   * @return the created {@link MessagePin}
   */
  MessagePin pin(String roomId, String messageId, String pinnedBy);

  /**
   * Unpins a message from a room
   *
   * @param roomId room identifier
   * @param messageId message identifier
   */
  void unpin(String roomId, String messageId);

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
