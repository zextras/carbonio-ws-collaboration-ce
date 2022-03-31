package com.zextras.carbonio.chats.core.config;

public enum ConfigValue {
  ENV("chats.env", "CHATS_ENV", "TODO"),
  JDBC_DRIVER("database.jdbc.driver", "DATABASE_JDBC_DRIVER", "TODO"),
  DATABASE_JDBC_URL("database.jdbc.url", "DATABASE_JDBC_URL", "TODO"),
  DATABASE_USERNAME("database.username", "DATABASE_USERNAME", "TODO"),
  DATABASE_PASSWORD("database.password", "DATABASE_PASSWORD", "TODO"),
  HIKARI_IDLE_TIMEOUT("datasource.hikari.idleTimeout", "HIKARI_IDLE_TIMEOUT", "TODO"),
  HIKARI_MIN_POOL_SIZE("datasource.hikari.minPoolSize", "HIKARI_MIN_POOL_SIZE", "TODO"),
  HIKARI_MAX_POOL_SIZE("datasource.hikari.maxPoolSize", "HIKARI_MAX_POOL_SIZE", "TODO"),
  HIKARI_LEAK_DETECTION_THRESHOLD("datasource.hikari.leakDetectionThreshold", "HIKARI_LEAK_DETECTION_THRESHOLD",
    "TODO"),
  XMPP_SERVER_HOST("xmpp.host", "XMPP_HOST", "TODO"),
  XMPP_SERVER_HTTP_PORT("xmpp.http.port", "XMPP_HTTP_PORT", "TODO"),
  STORAGES_HOST("carbonio.storages.host", "STORAGES_HOST", "TODO"),
  STORAGES_PORT("carbonio.storages.port", "STORAGES_PORT", "TODO"),
  USER_MANAGEMENT_HOST("carbonio.usermanagement.host", "USER_MANAGEMENT_HOST", "TODO"),
  USER_MANAGEMENT_PORT("carbonio.usermanagement.port", "USER_MANAGEMENT_PORT", "TODO"),
  PREVIEWER_HOST("carbonio.preview.host", "PREVIEWER_HOST", "TODO"),
  PREVIEWER_PORT("carbonio.preview.port", "PREVIEWER_PORT", "TODO");


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
