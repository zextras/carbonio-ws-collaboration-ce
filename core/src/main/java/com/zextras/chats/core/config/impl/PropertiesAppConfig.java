package com.zextras.chats.core.config.impl;

import com.zextras.chats.core.config.AppConfig;
import java.util.Optional;
import java.util.Properties;

public class PropertiesAppConfig implements AppConfig {

  private final Properties properties;

  public PropertiesAppConfig(Properties properties) {
    this.properties = properties;
  }

  public <T> Optional<T> get(Class<T> clazz, String key) {
    return Optional.ofNullable(properties.get(key)).map(clazz::cast);
  }
}
