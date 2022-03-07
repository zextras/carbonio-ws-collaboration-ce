package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class TestAppConfig extends AppConfig {

  @Override
  protected  <T> Optional<T> getAttributeByImplementation(Class<T> clazz, ConfigValue configName) {
    return InMemoryConfigStore.get(configName).map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    return get(String.class, ConfigValue.ENV).map(EnvironmentType::getByName)
      .or(() -> Optional.of(EnvironmentType.TEST));
  }

  private <T> T castToGeneric(Class<T> clazz, String stringValue) {
    if (clazz.equals(String.class)) {
      return (T) stringValue;
    } else if (clazz.equals(Boolean.class)) {
      return (T) Boolean.valueOf(stringValue);
    } else if (clazz.equals(Integer.class)) {
      return (T) Integer.valueOf(stringValue);
    } else if (clazz.equals(Double.class)) {
      return (T) Double.valueOf(stringValue);
    } else {
      throw new RuntimeException("Missing support for " + clazz.getSimpleName());
    }
  }
}
