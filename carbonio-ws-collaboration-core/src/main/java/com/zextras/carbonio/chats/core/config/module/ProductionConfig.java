// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.orbitz.consul.Consul;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentialReader;
import com.zextras.carbonio.chats.core.config.ServerConfiguration;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.ConsulCredentialReader;
import com.zextras.carbonio.chats.core.config.impl.InfrastructureAppConfig;
import com.zextras.carbonio.chats.core.web.socket.ConsulWsInstanceRegistry;
import com.zextras.carbonio.chats.core.web.socket.WsInstanceRegistry;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ProductionConfig extends AbstractModule {

  @Override
  protected void configure() {
    bind(WsInstanceRegistry.class).to(ConsulWsInstanceRegistry.class);
  }

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

  @Singleton
  @Provides
  private MessageDispatcherCredentialReader getCredentialReader(
      AppConfig appConfig, Set<ConfigContribution> contributions) {
    String host = appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow();
    Integer port = appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow();
    Map<String, String> consulKeys = new HashMap<>();
    contributions.forEach(contribution -> consulKeys.putAll(contribution.consulKvMappings()));
    return new ConsulCredentialReader(
        () -> {
          try {
            return Consul.builder()
                .withUrl(new URL("http", host, port, ""))
                .withPing(false)
                .build();
          } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Consul address", e);
          }
        },
        System.getenv("CONSUL_HTTP_TOKEN"),
        consulKeys);
  }

  @Provides
  private ServerConfiguration getServerConfiguration() {
    return new ServerConfiguration("127.78.0.4", 10000);
  }
}
