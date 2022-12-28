// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.boot.config;

import com.zextras.carbonio.chats.core.config.CoreModule;
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
}
