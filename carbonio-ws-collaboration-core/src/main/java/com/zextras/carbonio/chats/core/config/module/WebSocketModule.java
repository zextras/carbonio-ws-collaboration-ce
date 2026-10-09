// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.SessionPingManager;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;

public class WebSocketModule extends AbstractModule {

  @Override
  protected void configure() {
    bindEventsWebSocketManager();
    bind(EventWebSocketSessions.class);
    bind(SessionPingManager.class);
    bind(WebsocketVersionMigrator.class);
  }

  // Not a Class hook: Guice rejects bind(X).to(X).
  protected void bindEventsWebSocketManager() {
    bind(EventsWebSocketManager.class);
  }
}
