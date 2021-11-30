package com.zextras.chats.core.utils;

import java.util.Properties;

public class EnvConfig {

  private final Properties properties;

  public EnvConfig(Properties properties) {
    this.properties = properties;
  }

  public String get(String key) {
    return (String) properties.get(key);
  }

  public String get(String key, String defaultValue) {
    return properties.contains(key) ? (String) properties.get(key) : defaultValue;
  }

  public int getInt(String key) {
    return Integer.parseInt(get(key));
  }
}
