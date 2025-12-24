// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

/** Enum representing the different events that can be sent via the chat WebSocket. */
public enum ChatEvent {
  // Room events
  ROOM_CREATED,

  // Message events (server -> client)
  MESSAGE_RECEIVED,
  MESSAGE_EDITED,
  MESSAGE_DELETED,

  // Reaction events
  REACTION_ADDED,
  REACTION_REMOVED,

  // Attachment events
  ATTACHMENT_ADDED,
  ATTACHMENT_DELETED,
  ATTACHMENT_RESPONSE,

  // Read markers
  MESSAGE_READ,

  // Typing indicators
  USER_TYPING,
  USER_PAUSED,

  // Presence events
  USER_ONLINE,
  USER_OFFLINE,

  // History responses
  HISTORY_RESPONSE,
  SEARCH_RESPONSE,
  MESSAGES_AROUND_RESPONSE,

  // Read status
  READ_STATUS_RESPONSE,

  // Inbox response
  INBOX_RESPONSE,

  // Connection events
  CONNECTED,
  ERROR,

  // Ping/Pong
  PING,
  PONG
}
