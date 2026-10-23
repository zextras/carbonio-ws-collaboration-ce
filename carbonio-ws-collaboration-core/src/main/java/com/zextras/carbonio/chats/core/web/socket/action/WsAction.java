// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.action;

public enum WsAction {
  SEND_MESSAGE("send-message"),
  EDIT_MESSAGE("edit-message"),
  DELETE_MESSAGE("delete-message"),
  FORWARD_MESSAGE("forward-message"),
  PING("ping"),
  TYPING("Typing");

  private final String value;

  WsAction(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static WsAction fromValue(String value) {
    for (WsAction a : values()) {
      if (a.value.equals(value)) return a;
    }
    throw new IllegalArgumentException("Unknown action: " + value);
  }
}
