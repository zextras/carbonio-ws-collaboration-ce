package com.zextras.carbonio.chats.core.config.impl;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public class ConsulAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.CONSUL;

  private final Consul              consulClient;
  private final String              consulToken;
  private final Map<String, String> cache;

  private boolean loaded = false;

  private ConsulAppConfig(Consul consulClient, String consulToken) {
    super();
    this.consulClient = consulClient;
    this.consulToken = consulToken;
    this.cache = new HashMap<>();
  }

  public static AppConfig create(Consul consulClient, @Nullable String consulToken) {
    if (consulToken == null) {
      ChatsLogger.warn("Consul token not found");
      return null;
    }
    return new ConsulAppConfig(consulClient, consulToken);
  }

  public static AppConfig create(String consulHost, Integer consulPort, @Nullable String consulToken) {
    try {
      return create(
        Consul.builder().withHostAndPort(HostAndPort.fromParts(consulHost, consulPort)).build(), consulToken);
    } catch (Exception e) {
      ChatsLogger.warn("Unable to connect to Consul", e);
      return null;
    }
  }

  @Override
  public AppConfig load() {
    try {
      Arrays.stream(ConfigName.values()).forEach(configName -> cache.put(configName.getConsulName(), null));
      Arrays.stream(ConfigName.values())
        .map(name -> name.getConsulName().substring(0, name.getConsulName().indexOf("/") + 1))
        .distinct()
        .forEach(prefix ->
          consulClient.keyValueClient().getValues(prefix, ImmutableQueryOptions.builder().token(consulToken).build())
            .forEach(config -> {
              if (cache.containsKey(config.getKey()) && config.getValue().isPresent()) {
                cache.put(config.getKey(), config.getValue().get());
              }
            }));
      loaded = true;
      ChatsLogger.info("Consul config loaded");
    } catch (Exception e) {
      loaded = false;
      ChatsLogger.warn("Error while loading consul config", e);
    }
    return this;
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    try {
      String value;
      if (cache.containsKey(configName.getConsulName())) {
        value = cache.get(configName.getConsulName());
      } else {
        Optional<Value> consulValue = consulClient.keyValueClient()
          .getValue(configName.getConsulName(), ImmutableQueryOptions.builder().token(consulToken).build());
        value = consulValue.flatMap(Value::getValueAsString).orElse(null);
        cache.put(configName.getConsulName(), value);
      }
      return Optional.ofNullable(value).map(configValue -> castToGeneric(clazz, configValue));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading %s from consul config: %s: %s", configName.getConsulName(),
          ex.getClass().getSimpleName(), ex.getMessage()));
      return Optional.empty();
    }
  }

  @Override
  protected boolean setConfigByImplementation(ConfigName configName, String value) {
    cache.put(configName.getConsulName(), value);
    return true;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }

}
