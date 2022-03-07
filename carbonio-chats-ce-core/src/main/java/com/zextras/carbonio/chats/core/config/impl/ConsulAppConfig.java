package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.util.Optional;

/**
 * Stub configuration until consul config is implemented
 */
public class ConsulAppConfig extends AppConfig {

  @Override
  public <T> Optional<T> getAttributeByImplementation(Class<T> clazz, ConfigValue configName) {
    return Optional.empty();
  }

  @Override
  public Optional<EnvironmentType> getEnvTypeByImplementation() {
    return Optional.empty();
  }
}
