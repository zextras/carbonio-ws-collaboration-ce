// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot.config;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.CoreModule;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import io.github.cdimascio.dotenv.Dotenv;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;

public class BootModule extends RequestScopeModule {

  public BootModule() {
    super();
  }

  @Override
  protected void configure() {
    super.configure();
    install(new CoreModule());
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    return new DotenvAppConfig(
      Dotenv.configure()
        .ignoreIfMissing()
        .directory("./")
        .filename(".env")
        .load()
    );
  }
}
