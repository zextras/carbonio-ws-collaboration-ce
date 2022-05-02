package com.zextras.carbonio.chats.core.config.impl;

import com.ecwid.consul.v1.ConsulClient;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConsulAppConfig extends AppConfig {

  private final ConsulClient        consulClient;
  private final String              authToken;
  private final Map<String, String> cache;

  public ConsulAppConfig(ConsulClient consulClient, String authToken) {
    super();
    this.consulClient = consulClient;
    this.authToken = authToken;
    this.cache = new HashMap<>();
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    try {
      String value;
      if (cache.containsKey(configName.getConsulName())) {
        value = cache.get(configName.getConsulName());
      } else {
        value = consulClient.getKVValue(configName.getConsulName(), authToken).getValue().getDecodedValue();
        cache.put(configName.getConsulName(), value);
      }
      return Optional.ofNullable(castToGeneric(clazz, value));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading %s from consul config: %s", configName.getConsulName(), ex.getMessage()));
      return Optional.empty();
    }
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    try {
      return Optional.ofNullable(consulClient.getKVValue(ConfigName.ENV.getConsulName(), authToken)
        .getValue().getDecodedValue()).map(EnvironmentType::getByName);
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading environment settings from consul config: %s", ex.getMessage()));
      return Optional.empty();
    }
  }


  /**
   * Loads all Consul configurations and saves them in cache
   */
  public void loadConfigurations() {
    Arrays.stream(ConfigName.values()).forEach(configName -> cache.put(configName.getConsulName(), null));
    ConfigName.getConsulPrefixes().forEach(prefix ->
      consulClient.getKVValues(prefix, authToken).getValue().forEach(config -> {
        if (cache.containsKey(config.getKey())) {
          cache.put(config.getKey(), config.getDecodedValue());
        }
      }));
  }
}
