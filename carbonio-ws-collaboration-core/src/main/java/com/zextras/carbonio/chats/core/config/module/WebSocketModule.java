// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;
import com.zextras.carbonio.chats.core.web.socket.PresenceBroadcaster;
import com.zextras.carbonio.chats.core.web.socket.ServiceIdProvider;
import com.zextras.carbonio.chats.core.web.socket.SessionPingManager;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceBootCleaner;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceJanitorService;
import com.zextras.carbonio.chats.core.web.socket.WsPresenceManager;
import com.zextras.carbonio.chats.core.web.socket.action.WsActionRouter;
import com.zextras.carbonio.chats.core.web.socket.action.impl.TypingHandler;
import com.zextras.carbonio.chats.core.web.socket.impl.EventsWebSocketManagerImpl;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;

public class WebSocketModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventsWebSocketManager.class).to(eventsWebSocketManager());
    bind(EventWebSocketSessions.class);
    bind(SessionPingManager.class);
    bind(WebsocketVersionMigrator.class);

    bind(WsActionRouter.class);
    bind(TypingHandler.class);
    bind(ServiceIdProvider.class);
    bind(PresenceBroadcaster.class);
    bind(WsPresenceManager.class);
    bind(WsPresenceBootCleaner.class);
    bind(WsPresenceJanitorService.class);
  }

  protected Class<? extends EventsWebSocketManager> eventsWebSocketManager() {
    return EventsWebSocketManagerImpl.class;
  }
}
