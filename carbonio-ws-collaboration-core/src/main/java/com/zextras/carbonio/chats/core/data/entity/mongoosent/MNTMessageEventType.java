// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

/**
 * Event types for message audit log. Expandable for future operations.
 */
public enum MNTMessageEventType {
  MESSAGE_CREATED,
  MESSAGE_EDITED,
  MESSAGE_DELETED,
  REACTION_ADDED,
  REACTION_REMOVED,
  MESSAGE_FORWARDED,
  ATTACHMENT_ADDED,
  ATTACHMENT_DELETED
  // Future: MESSAGE_PINNED, MESSAGE_REPORTED, MESSAGE_STARRED, etc.
}
