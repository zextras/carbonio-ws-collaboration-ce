package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import java.util.Optional;

public class TestAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.IN_MEMORY;

  public static AppConfig create() {
    return new TestAppConfig();
  }

  @Override
  public AppConfig load() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return true;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return InMemoryConfigStore.get(configName).map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected boolean setConfigByImplementation(ConfigName configName, String value) {
    InMemoryConfigStore.set(configName, value);
    return true;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }
}
