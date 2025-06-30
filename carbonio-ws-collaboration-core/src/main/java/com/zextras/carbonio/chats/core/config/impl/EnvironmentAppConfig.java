// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class EnvironmentAppConfig extends AppConfig {
  private static final AppConfigType CONFIG_TYPE = AppConfigType.DOCKER;

  private static final String LOCAL_SERVICE_ADDRESS = "localhost";

  private static final Map<ConfigName, String> configs;

  static {
    configs = new EnumMap<>(ConfigName.class);
    configs.put(
        ConfigName.DATABASE_JDBC_DRIVER, System.getenv(ConfigName.DATABASE_JDBC_DRIVER.name()));
    configs.put(ConfigName.DATABASE_JDBC_URL, System.getenv(ConfigName.DATABASE_JDBC_URL.name()));
    configs.put(ConfigName.CONSUL_HOST, System.getenv(ConfigName.CONSUL_HOST.name()));
    configs.put(ConfigName.CONSUL_PORT, System.getenv(ConfigName.CONSUL_PORT.name()));
    configs.put(
        ConfigName.USER_MANAGEMENT_HOST, System.getenv(ConfigName.USER_MANAGEMENT_HOST.name()));
    configs.put(
        ConfigName.USER_MANAGEMENT_PORT, System.getenv(ConfigName.USER_MANAGEMENT_PORT.name()));
    configs.put(ConfigName.PREVIEWER_HOST, System.getenv(ConfigName.PREVIEWER_HOST.name()));
    configs.put(ConfigName.PREVIEWER_PORT, System.getenv(ConfigName.PREVIEWER_PORT.name()));
    configs.put(ConfigName.XMPP_SERVER_HOST, System.getenv(ConfigName.XMPP_SERVER_HOST.name()));
    configs.put(
        ConfigName.XMPP_SERVER_HTTP_PORT, System.getenv(ConfigName.XMPP_SERVER_HTTP_PORT.name()));
    configs.put(
        ConfigName.XMPP_SERVER_USERNAME, System.getenv(ConfigName.XMPP_SERVER_USERNAME.name()));
    configs.put(
        ConfigName.XMPP_SERVER_PASSWORD, System.getenv(ConfigName.XMPP_SERVER_PASSWORD.name()));
    configs.put(
        ConfigName.EVENT_DISPATCHER_HOST, System.getenv(ConfigName.EVENT_DISPATCHER_HOST.name()));
    configs.put(
        ConfigName.EVENT_DISPATCHER_PORT, System.getenv(ConfigName.EVENT_DISPATCHER_PORT.name()));
    configs.put(
        ConfigName.EVENT_DISPATCHER_USER_USERNAME,
        System.getenv(ConfigName.EVENT_DISPATCHER_USER_USERNAME.name()));
    configs.put(
        ConfigName.EVENT_DISPATCHER_USER_PASSWORD,
        System.getenv(ConfigName.EVENT_DISPATCHER_USER_PASSWORD.name()));
    configs.put(ConfigName.VIDEO_SERVER_HOST, System.getenv(ConfigName.VIDEO_SERVER_HOST.name()));
    configs.put(ConfigName.VIDEO_SERVER_PORT, System.getenv(ConfigName.VIDEO_SERVER_PORT.name()));
  }

  public static AppConfig create() {
    return new EnvironmentAppConfig();
  }

  @Override
  public AppConfig load() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return true;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return Optional.ofNullable(configs.get(configName))
        .map((stringValue) -> castToGeneric(clazz, stringValue));
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
