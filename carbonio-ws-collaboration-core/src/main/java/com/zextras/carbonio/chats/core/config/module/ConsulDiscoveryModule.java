// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.infrastructure.consul.DiscoveryService;
import com.zextras.carbonio.chats.core.infrastructure.consul.impl.ConsulDiscoveryService;
import java.net.URL;

public class ConsulDiscoveryModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(DiscoveryService.class).to(ConsulDiscoveryService.class);
  }

  @Singleton
  @Provides
  private Consul getConsul(AppConfig appConfig) {
    try {
      return Consul.builder()
          .withUrl(
              new URL(
                  "http",
                  appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
                  appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
                  ""))
          .build();
    } catch (Exception e) {
      throw new ConsulException("Unable to connect to consul ", e);
    }
  }
}
