package com.zextras.carbonio.chats.core.config.impl;

import com.ecwid.consul.v1.ConsulClient;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.util.Optional;

/**
 * Stub configuration until consul config is implemented
 */
public class ConsulAppConfig extends AppConfig {

  private final ConsulClient consulClient;

  public ConsulAppConfig(ConsulClient consulClient) {
    super();
    this.consulClient = consulClient;
  }

  @Override
  protected <T> Optional<T> getAttributeByImplementation(Class<T> clazz, ConfigValue configName) {
    return Optional.empty();
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    return Optional.empty();
  }
}
