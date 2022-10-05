package com.zextras.carbonio.chats.core.config.impl;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

public class ConsulAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE                        = AppConfigType.CONSUL;
  private static final int           CONSUL_CLIENT_READ_TIMEOUT_SECONDS = 15;
  private static final int           CONSUL_CONFIG_WATCH_SECONDS        = 10;

  private final Consul              consulClient;
  private final List<KVCache>       kvCacheList;
  private final String              consulToken;
  private final Map<String, String> cache;

  private boolean loaded = false;

  private ConsulAppConfig(Consul consulClient, String consulToken) {
    super();
    this.consulClient = consulClient;
    this.kvCacheList = new ArrayList<>();
    this.consulToken = consulToken;
    this.cache = new HashMap<>();
    Arrays.stream(ConfigName.values()).forEach(configName -> cache.put(configName.getConsulName(), null));
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
        Consul.builder()
          .withHostAndPort(HostAndPort.fromParts(consulHost, consulPort))
          .withReadTimeoutMillis(CONSUL_CLIENT_READ_TIMEOUT_SECONDS * 1000)
          .build(),
        consulToken);
    } catch (Exception e) {
      ChatsLogger.warn("Unable to connect to Consul", e);
      return null;
    }
  }

  @Override
  public AppConfig load() {
    try {
      Arrays.stream(ConfigName.values())
        .map(name -> name.getConsulName().substring(0, name.getConsulName().indexOf("/") + 1))
        .distinct()
        .forEach(prefix -> {
          KVCache kvCache = KVCache.newCache(consulClient.keyValueClient(), prefix, CONSUL_CONFIG_WATCH_SECONDS,
            ImmutableQueryOptions.builder().token(consulToken).build());
          kvCache.addListener(values -> values.values().forEach(this::addToCache));
          kvCache.start();
          kvCacheList.add(kvCache);
        });
      loaded = true;
      ChatsLogger.info("Consul config loaded");
    } catch (Exception e) {
      loaded = false;
      kvCacheList.forEach(ConsulCache::close);
      ChatsLogger.warn("Error while loading consul config", e);
    }
    return this;
  }

  private void addToCache(@Nullable Value value) {
    if (value != null && cache.containsKey(value.getKey())) {
      cache.put(value.getKey(), value.getValueAsString().orElse(null));
    }
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    try {
      cache.computeIfAbsent(configName.getConsulName(), key ->
        consulClient.keyValueClient()
          .getValue(key, ImmutableQueryOptions.builder().token(consulToken).build())
          .flatMap(Value::getValueAsString).orElse(null));
      return Optional.ofNullable(cache.get(configName.getConsulName()))
        .map(configValue -> castToGeneric(clazz, configValue));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading %s from consul config: %s: %s", configName.getConsulName(),
          ex.getClass().getSimpleName(), ex.getMessage()));
      return Optional.empty();
    }
  }

  @Override
  protected boolean setConfigByImplementation(ConfigName configName, String value) {
    consulClient.keyValueClient().putValue(configName.getConsulName(), value);
    return true;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }
}
