package com.zextras.carbonio.chats.core.config.impl;

import com.ecwid.consul.v1.ConsulClient;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Optional;

public class ConsulAppConfig extends AppConfig {

  private final ConsulClient consulClient;
  private final String       authToken;

  public ConsulAppConfig(ConsulClient consulClient, String authToken) {
    super();
    this.consulClient = consulClient;
    this.authToken = authToken;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigValue configName) {
    try {
      //We might want to cache configuration values and to add some consul watchers to allow for quicker config lookup
      return Optional.ofNullable(consulClient.getKVValue(configName.getConsulName(), authToken)
        .getValue().getDecodedValue()).map(value -> castToGeneric(clazz, value));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading %s from consul config: %s", configName.getConsulName(), ex.getMessage()));
      return Optional.empty();
    }
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    try {
      return Optional.ofNullable(consulClient.getKVValue(ConfigValue.ENV.getConsulName(), authToken)
        .getValue().getDecodedValue()).map(EnvironmentType::getByName);
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading environment settings from consul config: %s", ex.getMessage()));
      return Optional.empty();
    }
  }
}
