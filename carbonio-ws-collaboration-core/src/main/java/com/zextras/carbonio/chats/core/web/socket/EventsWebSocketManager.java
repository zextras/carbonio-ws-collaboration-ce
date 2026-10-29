// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import jakarta.websocket.Session;
import java.io.IOException;

public interface EventsWebSocketManager {

  void onOpen(Session session) throws IOException;

  void onMessage(Session session, String message);

  void onClose(Session session);

  void onError(Session session, Throwable throwable);
}
