// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket.action;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.websocket.Session;

public interface WsActionHandler {
  void handle(String userId, JsonNode payload, Session session);
}
