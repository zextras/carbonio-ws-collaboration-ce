// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.ServerConfiguration;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.InfrastructureAppConfig;
import java.util.Optional;
import java.util.Set;

public class ProductionConfig extends AbstractModule {

  @Singleton
  @Provides
  private AppConfig getAppConfig(Set<ConfigContribution> contributions) {
    AppConfig appConfig = InfrastructureAppConfig.create(contributions).load();
    Optional.ofNullable(
            ConsulAppConfig.create(
                appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
                appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
                System.getenv("CONSUL_HTTP_TOKEN"),
                contributions))
        .ifPresent(consulConfig -> appConfig.add(consulConfig.load()));
    return appConfig;
  }

  @Provides
  private ServerConfiguration getServerConfiguration() {
    return new ServerConfiguration("127.78.0.4", 10000);
  }
}
