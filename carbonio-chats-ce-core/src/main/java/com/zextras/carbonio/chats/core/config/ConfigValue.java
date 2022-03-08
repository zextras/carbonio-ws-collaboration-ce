package com.zextras.carbonio.chats.core.config;

public enum ConfigValue {
  ENV("project.env", "CHATS_ENV"),
  JDBC_DRIVER("database.jdbc.driver", "DATABASE_JDBC_DRIVER"),
  DATABASE_JDBC_URL("database.jdbc.url", "DATABASE_JDBC_URL"),
  DATABASE_USERNAME("database.username", "DATABASE_USERNAME"),
  DATABASE_PASSWORD("database.password", "DATABASE_PASSWORD"),
  HIKARI_IDLE_TIMEOUT("datasource.hikari.idleTimeout", "HIKARI_IDLE_TIMEOUT"),
  HIKARI_MIN_POOL_SIZE("datasource.hikari.minPoolSize", "HIKARI_MIN_POOL_SIZE"),
  HIKARI_MAX_POOL_SIZE("datasource.hikari.maxPoolSize", "HIKARI_MAX_POOL_SIZE"),
  HIKARI_LEAK_DETECTION_THRESHOLD("datasource.hikari.leakDetectionThreshold", "HIKARI_LEAK_DETECTION_THRESHOLD"),
  XMPP_SERVER_HOST("xmpp.host", "XMPP_HOST"),
  XMPP_SERVER_HTTP_PORT("xmpp.http.port", "XMPP_HTTP_PORT"),
  STORAGES_HOST("carbonio.storages.host", "STORAGES_HOST"),
  STORAGES_PORT("carbonio.storages.port", "STORAGES_PORT"),
  USER_MANAGEMENT_HOST("carbonio.usermanagement.host", "USER_MANAGEMENT_HOST"),
  USER_MANAGEMENT_PORT("carbonio.usermanagement.port", "USER_MANAGEMENT_PORT"),
  PREVIEWER_HOST("carbonio.preview.host", "PREVIEWER_HOST"),
  PREVIEWER_PORT("carbonio.preview.port", "PREVIEWER_PORT");


  private final String propertyName;
  private final String envName;

  ConfigValue(String propertyName, String envName) {

    this.propertyName = propertyName;
    this.envName = envName;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getEnvName() {
    return envName;
  }
}
