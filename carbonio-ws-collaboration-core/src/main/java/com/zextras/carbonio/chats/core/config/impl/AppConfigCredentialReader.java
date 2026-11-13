// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentialReader;
import java.util.Optional;

public class AppConfigCredentialReader implements MessageDispatcherCredentialReader {

  private final AppConfig appConfig;

  @Inject
  public AppConfigCredentialReader(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public Optional<String> read(ConfigName name) {
    return appConfig.get(String.class, name);
  }
}
