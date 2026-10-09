// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.zextras.carbonio.chats.core.web.socket.EventsWebSocketManager;

// Replaced as a whole downstream: holds only the bindings that get replaced.
public class WebSocketModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventsWebSocketManager.class);
  }
}
