// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.producer;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.EnvironmentAppConfig;
import com.zextras.carbonio.chats.core.config.impl.InfrastructureAppConfig;
import com.zextras.carbonio.chats.core.config.impl.MicroProfileAppConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/** Replaces CE's Guice {@code ProductionConfig}/{@code DockerConfig} modules. */
@ApplicationScoped
public class AppConfigProducer {

  @Produces
  @Singleton
  AppConfig appConfig(
      Instance<ConfigContribution> contributions,
      Config config,
      @ConfigProperty(name = "ws.consul.enabled", defaultValue = "false") boolean consulEnabled) {
    List<ConfigContribution> catalog = contributions.stream().toList();
    AppConfig appConfig = new MicroProfileAppConfig(config);
    if (isDockerEnvironment()) {
      return appConfig.add(EnvironmentAppConfig.create(catalog).load());
    }
    AppConfig infrastructure = InfrastructureAppConfig.create(catalog).load();
    appConfig.add(infrastructure);
    if (consulEnabled) {
      Optional.ofNullable(
              ConsulAppConfig.create(
                  appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
                  appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
                  System.getenv("CONSUL_HTTP_TOKEN"),
                  catalog))
          .ifPresent(consulConfig -> appConfig.add(consulConfig.load()));
    }
    return appConfig;
  }

  private static boolean isDockerEnvironment() {
    return "docker".equals(System.getenv("APP_ENVIRONMENT"));
  }
}
