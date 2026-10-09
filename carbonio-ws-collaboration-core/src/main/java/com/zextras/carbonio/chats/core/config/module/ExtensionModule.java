// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;

public class ExtensionModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new DatabaseModule());
    install(new AttachmentModule());
    install(new MeetingModule());
    install(new WebSocketModule());
  }
}
