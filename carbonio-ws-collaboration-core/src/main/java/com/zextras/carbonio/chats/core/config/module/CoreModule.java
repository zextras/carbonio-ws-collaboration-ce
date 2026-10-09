// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;

public class CoreModule extends AbstractModule {

  public static final String URL_PATTERN = "http://%s:%s";

  @Override
  protected void configure() {
    install(new WebModule());
    install(new MessagingModule());
    install(new UserModule());
    install(new RoomModule());
  }
}
