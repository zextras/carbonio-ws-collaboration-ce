// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot.config;

import com.zextras.carbonio.chats.core.config.module.CoreModule;
import com.zextras.carbonio.chats.core.config.module.DockerConfig;
import com.zextras.carbonio.chats.core.config.module.ProductionConfig;
import dev.resteasy.guice.ext.RequestScopeModule;

public class BootModule extends RequestScopeModule {

  public BootModule() {
    super();
  }

  @Override
  protected void configure() {
    super.configure();
    if (isDockerEnvironment()) {
      install(new DockerConfig());
    } else {
      install(new ProductionConfig());
    }
    install(new CoreModule());
  }

  private static boolean isDockerEnvironment() {
    return System.getenv("APP_ENVIRONMENT") != null
        && System.getenv("APP_ENVIRONMENT").equals("docker");
  }
}
