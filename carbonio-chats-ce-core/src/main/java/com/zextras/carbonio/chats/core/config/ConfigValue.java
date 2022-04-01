package com.zextras.carbonio.chats.core.config;

public enum ConfigValue {
  ENV("chats.env", "CHATS_ENV", "TODO"),
  JDBC_DRIVER("database.jdbc.driver", "DATABASE_JDBC_DRIVER", "jdbc-driver"),
  DATABASE_JDBC_URL("database.jdbc.url", "DATABASE_JDBC_URL", "jdbc-url"),
  DATABASE_USERNAME("database.username", "DATABASE_USERNAME", "db-username"),
  DATABASE_PASSWORD("database.password", "DATABASE_PASSWORD", "db-password"),
  HIKARI_IDLE_TIMEOUT("datasource.hikari.idleTimeout", "HIKARI_IDLE_TIMEOUT", "hikari-idle-timeout"),
  HIKARI_MIN_POOL_SIZE("datasource.hikari.minPoolSize", "HIKARI_MIN_POOL_SIZE", "hikari-min-pool-size"),
  HIKARI_MAX_POOL_SIZE("datasource.hikari.maxPoolSize", "HIKARI_MAX_POOL_SIZE", "hikari-max-pool-size"),
  HIKARI_LEAK_DETECTION_THRESHOLD("datasource.hikari.leakDetectionThreshold", "hikari-leak-detection-threshold",
    "TODO"),
  XMPP_SERVER_HOST("xmpp.host", "XMPP_HOST", "xmpp-host"),
  XMPP_SERVER_HTTP_PORT("xmpp.http.port", "XMPP_HTTP_PORT", "xmpp-http-port"),
  STORAGES_HOST("carbonio.storages.host", "STORAGES_HOST", "storages-host"),
  STORAGES_PORT("carbonio.storages.port", "STORAGES_PORT", "storages-port"),
  USER_MANAGEMENT_HOST("carbonio.usermanagement.host", "USER_MANAGEMENT_HOST", "user-management-host"),
  USER_MANAGEMENT_PORT("carbonio.usermanagement.port", "USER_MANAGEMENT_PORT", "user-management-port"),
  PREVIEWER_HOST("carbonio.preview.host", "PREVIEWER_HOST", "previewer-host"),
  PREVIEWER_PORT("carbonio.preview.port", "PREVIEWER_PORT", "previewer-port"),
  CONSUL_HOST("consul.agent.host", "CONSUL_HOST", "consul-host"),
  CONSUL_PORT("consul.agent.port", "CONSUL_PORT", "consul-port"),
  CONSUL_TOKEN("consul.agent.token", "CONSUL_TOKEN", "consul-token");


  private final String propertyName;
  private final String envName;
  private final String consulName;

  ConfigValue(String propertyName, String envName, String consulName) {
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
}
