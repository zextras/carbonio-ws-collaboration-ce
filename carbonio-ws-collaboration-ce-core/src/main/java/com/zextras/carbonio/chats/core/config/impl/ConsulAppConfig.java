// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.cache.KVCache;
import com.orbitz.consul.config.CacheConfig;
import com.orbitz.consul.config.ClientConfig;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.ImmutableQueryOptions;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.time.Duration;
import java.util.ArrayList;
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

  private static Map<ConfigName, String> namesMapping;

  static {
    namesMapping = new HashMap<>();
    namesMapping.put(ConfigName.DATABASE_USERNAME, "carbonio-ws-collaboration-db/db-username");
    namesMapping.put(ConfigName.DATABASE_PASSWORD, "carbonio-ws-collaboration-db/db-password");
    namesMapping.put(ConfigName.HIKARI_IDLE_TIMEOUT, "carbonio-ws-collaboration/hikari-idle-timeout");
    namesMapping.put(ConfigName.HIKARI_MIN_POOL_SIZE, "carbonio-ws-collaboration/hikari-min-pool-size");
    namesMapping.put(ConfigName.HIKARI_MAX_POOL_SIZE, "carbonio-ws-collaboration/hikari-max-pool-size");
    namesMapping.put(ConfigName.HIKARI_LEAK_DETECTION_THRESHOLD,
      "carbonio-ws-collaboration/hikari-leak-detection-threshold");
    namesMapping.put(ConfigName.XMPP_SERVER_USERNAME, "carbonio-message-dispatcher/api/username");
    namesMapping.put(ConfigName.XMPP_SERVER_PASSWORD, "carbonio-message-dispatcher/api/password");
    namesMapping.put(ConfigName.EVENT_DISPATCHER_USER_USERNAME, "carbonio-message-broker/username");
    namesMapping.put(ConfigName.EVENT_DISPATCHER_USER_PASSWORD, "carbonio-message-broker/password");
    namesMapping.put(ConfigName.CAN_SEE_MESSAGE_READS, "carbonio-ws-collaboration/configs/can-see-message-reads");
    namesMapping.put(ConfigName.CAN_SEE_USERS_PRESENCE, "carbonio-ws-collaboration/configs/can-see-users-presence");
    namesMapping.put(ConfigName.MAX_USER_IMAGE_SIZE_IN_KB,
      "carbonio-ws-collaboration/configs/max-user-image-size-in-kb");
    namesMapping.put(ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB,
      "carbonio-ws-collaboration/configs/max-room-image-size-in-kb");
    namesMapping.put(ConfigName.EDIT_MESSAGE_TIME_LIMIT_IN_MINUTES,
      "carbonio-ws-collaboration/configs/edit-message-time-limit-in-minutes");
    namesMapping.put(ConfigName.DELETE_MESSAGE_TIME_LIMIT_IN_MINUTES,
      "carbonio-ws-collaboration/configs/delete-message-time-limit-in-minutes");
    namesMapping.put(ConfigName.MAX_GROUP_MEMBERS, "carbonio-ws-collaboration/configs/max-group-members");
  }

  private boolean loaded = false;

  private ConsulAppConfig(Consul consulClient, String consulToken) {
    super();
    this.consulClient = consulClient;
    this.kvCacheList = new ArrayList<>();
    this.consulToken = consulToken;
    this.cache = new HashMap<>();

    namesMapping.values().forEach(consulName -> cache.put(consulName, null));
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
          .withClientConfiguration(
            new ClientConfig(
              CacheConfig
                .builder()
                .withMinDelayBetweenRequests(Duration.ofSeconds(60))
                .withMinDelayOnEmptyResult(Duration.ofSeconds(30))
                .withBackOffDelay(Duration.ofSeconds(30))
                .build()
            )
          )
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
      namesMapping.values().stream()
        .map(consulName -> consulName.substring(0, consulName.indexOf("/") + 1))
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
    String consulName = namesMapping.get(configName);
    if (consulName == null) {
      return Optional.empty();
    }
    try {
      cache.computeIfAbsent(consulName, key ->
        consulClient.keyValueClient()
          .getValue(key, ImmutableQueryOptions.builder().token(consulToken).build())
          .flatMap(Value::getValueAsString).orElse(null));
      return Optional.ofNullable(cache.get(consulName))
        .map(configValue -> castToGeneric(clazz, configValue));
    } catch (RuntimeException ex) {
      ChatsLogger.debug(
        String.format("Error while reading %s from consul config: %s: %s", configName,
          ex.getClass().getSimpleName(), ex.getMessage()));
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
