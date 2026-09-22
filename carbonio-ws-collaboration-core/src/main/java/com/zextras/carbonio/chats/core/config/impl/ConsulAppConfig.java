// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.config.CacheConfig;
import com.orbitz.consul.config.ClientConfig;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import jakarta.annotation.Nullable;
import java.net.URL;
import java.time.Duration;
import java.util.*;

public class ConsulAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.CONSUL;
  private static final int CONSUL_CLIENT_READ_TIMEOUT_SECONDS = 15;
  private static final int CONSUL_CONFIG_WATCH_SECONDS = 10;

  private final Consul consulClient;
  private final List<KVCache> kvCacheList;
  private final String consulToken;
  private final Map<String, String> cache;
  private final Map<ConfigName, String> namesMapping;

  private boolean loaded = false;

  private ConsulAppConfig(
      Consul consulClient, String consulToken, Collection<ConfigContribution> catalog) {
    super();
    this.consulClient = consulClient;
    this.kvCacheList = new ArrayList<>();
    this.consulToken = consulToken;
    this.cache = new HashMap<>();
    this.namesMapping = mergeConsulKvMappings(catalog);

    namesMapping.values().forEach(consulName -> cache.put(consulName, null));
  }

  private static Map<ConfigName, String> mergeConsulKvMappings(
      Collection<ConfigContribution> catalog) {
    Map<ConfigName, String> merged = new EnumMap<>(ConfigName.class);
    catalog.forEach(contribution -> merged.putAll(contribution.consulKvMappings()));
    return merged;
  }

  public static AppConfig create(Consul consulClient, @Nullable String consulToken) {
    // No-catalog overload: CE's own key set, preserving pre-registry behavior.
    return create(consulClient, consulToken, List.of(new CoreConfigContribution()));
  }

  public static AppConfig create(
      Consul consulClient, @Nullable String consulToken, Collection<ConfigContribution> catalog) {
    if (consulToken == null) {
      ChatsLogger.warn("Consul token not found");
      return null;
    }
    return new ConsulAppConfig(consulClient, consulToken, catalog);
  }

  public static AppConfig create(
      String consulHost, Integer consulPort, @Nullable String consulToken) {
    return create(consulHost, consulPort, consulToken, List.of(new CoreConfigContribution()));
  }

  public static AppConfig create(
      String consulHost,
      Integer consulPort,
      @Nullable String consulToken,
      Collection<ConfigContribution> catalog) {
    try {
      return create(
          Consul.builder()
              .withUrl(new URL("http", consulHost, consulPort, ""))
              .withReadTimeoutMillis(CONSUL_CLIENT_READ_TIMEOUT_SECONDS * 1000)
              .withClientConfiguration(
                  new ClientConfig(
                      CacheConfig.builder()
                          .withMinDelayBetweenRequests(Duration.ofSeconds(60))
                          .withMinDelayOnEmptyResult(Duration.ofSeconds(30))
                          .withBackOffDelay(Duration.ofSeconds(30))
                          .build()))
              .build(),
          consulToken,
          catalog);
    } catch (Exception e) {
      ChatsLogger.warn("Unable to connect to Consul", e);
      return null;
    }
  }

  @Override
  public AppConfig load() {
    try {
      namesMapping.values().stream()
          .map(consulName -> consulName.substring(0, consulName.indexOf("/") + 1))
          .distinct()
          .forEach(
              prefix -> {
                KVCache kvCache =
                    KVCache.newCache(
                        consulClient.keyValueClient(),
                        prefix,
                        CONSUL_CONFIG_WATCH_SECONDS,
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
    String consulName = namesMapping.get(configName);
    if (consulName == null) {
      return Optional.empty();
    }
    try {
      cache.computeIfAbsent(
          consulName,
          key ->
              consulClient
                  .keyValueClient()
                  .getValue(key, ImmutableQueryOptions.builder().token(consulToken).build())
                  .flatMap(Value::getValueAsString)
                  .orElse(null));
      return Optional.ofNullable(cache.get(consulName))
          .map(configValue -> castToGeneric(clazz, configValue));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
          String.format(
              "Error while reading %s from consul config: %s: %s",
              configName, ex.getClass().getSimpleName(), ex.getMessage()));
      return Optional.empty();
    }
  }

  @Override
  protected boolean setConfigByImplementation(ConfigName configName, String value) {
    return false;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }
}
