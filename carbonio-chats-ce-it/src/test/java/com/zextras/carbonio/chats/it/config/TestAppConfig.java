package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.Optional;

public class TestAppConfig implements AppConfig {

  @Override
  public <T> Optional<T> get(Class<T> clazz, String key) {
    return InMemoryConfigStore.get(key).map((stringValue) -> castToGeneric(clazz, stringValue));
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
