// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.model.message;

/** Enum representing the different actions that can be sent via the chat WebSocket. */
public enum ChatAction {
  // Room operations
  CREATE_ROOM,

  // Message operations
  SEND_MESSAGE,
  EDIT_MESSAGE,
  DELETE_MESSAGE,
  FORWARD_MESSAGE,

  // Reaction operations
  ADD_REACTION,
  REMOVE_REACTION,

  // Read markers
  MARK_AS_READ,
  GET_READ_STATUS,

  // Typing indicators
  TYPING,
  PAUSED,

  // Presence
  SET_ONLINE,
  SET_OFFLINE,

  // History requests
  GET_HISTORY,
  GET_HISTORY_BETWEEN_DATES,
  GET_MESSAGES_AROUND,
  SEARCH_MESSAGES,

  // Inbox
  GET_INBOX,

  // Ping/Pong
  PING,
  PONG
}
