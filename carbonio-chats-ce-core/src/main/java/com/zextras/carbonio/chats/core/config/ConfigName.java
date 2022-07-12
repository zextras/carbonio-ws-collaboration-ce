package com.zextras.carbonio.chats.core.config;

import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ConfigName {
  ENV("chats.env", "CHATS_ENV", "carbonio-chats/chats-env"),
  JDBC_DRIVER("database.jdbc.driver", "DATABASE_JDBC_DRIVER", "carbonio-chats/jdbc-driver"),
  DATABASE_JDBC_URL("database.jdbc.url", "DATABASE_JDBC_URL", "carbonio-chats/jdbc-url"),
  DATABASE_USERNAME("database.username", "DATABASE_USERNAME", "carbonio-chats/db-username"),
  DATABASE_PASSWORD("database.password", "DATABASE_PASSWORD", "carbonio-chats/db-password"),
  HIKARI_IDLE_TIMEOUT("datasource.hikari.idleTimeout", "HIKARI_IDLE_TIMEOUT", "carbonio-chats/hikari-idle-timeout"),
  HIKARI_MIN_POOL_SIZE("datasource.hikari.minPoolSize", "HIKARI_MIN_POOL_SIZE", "carbonio-chats/hikari-min-pool-size"),
  HIKARI_MAX_POOL_SIZE("datasource.hikari.maxPoolSize", "HIKARI_MAX_POOL_SIZE", "carbonio-chats/hikari-max-pool-size"),
  HIKARI_LEAK_DETECTION_THRESHOLD("datasource.hikari.leakDetectionThreshold", "HIKARI_LEAK_DETECTION_THRESHOLD",
    "carbonio-chats/hikari-leak-detection-threshold"),
  XMPP_SERVER_HOST("xmpp.host", "XMPP_HOST", "carbonio-chats/xmpp-host"),
  XMPP_SERVER_HTTP_PORT("xmpp.http.port", "XMPP_HTTP_PORT", "carbonio-chats/xmpp-http-port"),
  STORAGES_HOST("carbonio.storages.host", "STORAGES_HOST", "carbonio-chats/storages-host"),
  STORAGES_PORT("carbonio.storages.port", "STORAGES_PORT", "carbonio-chats/storages-port"),
  USER_MANAGEMENT_HOST("carbonio.usermanagement.host", "USER_MANAGEMENT_HOST", "carbonio-chats/user-management-host"),
  USER_MANAGEMENT_PORT("carbonio.usermanagement.port", "USER_MANAGEMENT_PORT", "carbonio-chats/user-management-port"),
  PREVIEWER_HOST("carbonio.preview.host", "PREVIEWER_HOST", "carbonio-chats/previewer-host"),
  PREVIEWER_PORT("carbonio.preview.port", "PREVIEWER_PORT", "carbonio-chats/previewer-port"),
  CONSUL_TOKEN("consul.agent.token", "CONSUL_HTTP_TOKEN", "carbonio-chats/consul-http-token"),
  CONSUL_HOST("consul.host", "CONSUL_HOST", "carbonio-chats/consul-host"),
  CONSUL_PORT("consul.port", "CONSUL_PORT", "carbonio-chats/consul-port"),
  EVENT_DISPATCHER_HOST("event.dispatcher.host", "EVENT_DISPATCHER_HOST", "carbonio-chats/event-dispatcher-host"),
  EVENT_DISPATCHER_PORT("event.dispatcher.port", "EVENT_DISPATCHER_PORT", "carbonio-chats/event-dispatcher-port");

  private final String propertyName;
  private final String envName;
  private final String consulName;

  ConfigName(String propertyName, String envName, String consulName) {
    this.propertyName = propertyName;
    this.envName = envName;
    this.consulName = consulName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getEnvName() {
    return envName;
  }

  public String getConsulName() {
    return consulName;
  }

  public static Optional<ConfigName> getByName(String name, AppConfigType configType) {
    Map<AppConfigType, Function<ConfigName, String>> typeMap = Map.of(
      AppConfigType.ENVIRONMENT, configName -> configName.envName,
      AppConfigType.PROPERTY, configName -> configName.propertyName,
      AppConfigType.CONSUL, configName -> configName.consulName
    );
    return Arrays.stream(ConfigName.values())
      .filter(item -> typeMap.get(configType).apply(item).equals(name)
      ).findAny();
  }

  public static Set<String> getConsulPrefixes() {
    return Arrays.stream(ConfigName.values())
      .map(name -> name.consulName.substring(0, name.consulName.indexOf("/") + 1))
      .collect(Collectors.toSet());
  }
}