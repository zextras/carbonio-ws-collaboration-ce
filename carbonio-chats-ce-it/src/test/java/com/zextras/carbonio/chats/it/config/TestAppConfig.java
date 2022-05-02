package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.Optional;

public class TestAppConfig extends AppConfig {

  @Override
  protected  <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return InMemoryConfigStore.get(configName).map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    return get(String.class, ConfigName.ENV).map(EnvironmentType::getByName)
      .or(() -> Optional.of(EnvironmentType.TEST));
  }
}
