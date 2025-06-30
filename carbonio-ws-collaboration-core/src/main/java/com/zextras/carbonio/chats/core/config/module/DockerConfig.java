// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ServerConfiguration;
import com.zextras.carbonio.chats.core.config.impl.EnvironmentAppConfig;

public class DockerConfig extends AbstractModule {

  @Singleton
  @Provides
  private AppConfig getAppConfig() {
    return EnvironmentAppConfig.create().load();
  }

  @Provides
  private ServerConfiguration getServerConfiguration() {
    return new ServerConfiguration("0.0.0.0", 8080);
  }
}
