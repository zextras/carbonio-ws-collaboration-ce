// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public class InfrastructureAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.INFRASTRUCTURE;

  private static final Map<ConfigName, String> configs;

  static {
    configs = new EnumMap<>(ConfigName.class);
    configs.put(ConfigName.DATABASE_JDBC_DRIVER, "org.postgresql.Driver");
    configs.put(
        ConfigName.DATABASE_JDBC_URL,
        "jdbc:postgresql://127.78.0.4:20003/carbonio-ws-collaboration-db");
    configs.put(ConfigName.CONSUL_HOST, "localhost");
    configs.put(ConfigName.CONSUL_PORT, "8500");
    configs.put(ConfigName.USER_MANAGEMENT_HOST, "127.78.0.4");
    configs.put(ConfigName.USER_MANAGEMENT_PORT, "20001");
    configs.put(ConfigName.PREVIEWER_HOST, "127.78.0.4");
    configs.put(ConfigName.PREVIEWER_PORT, "20002");
    configs.put(ConfigName.XMPP_SERVER_HOST, "127.78.0.4");
    configs.put(ConfigName.XMPP_SERVER_HTTP_PORT, "20004");
    configs.put(ConfigName.EVENT_DISPATCHER_HOST, "127.78.0.4");
    configs.put(ConfigName.EVENT_DISPATCHER_PORT, "20005");
    configs.put(ConfigName.VIDEO_SERVER_HOST, "127.78.0.4");
    configs.put(ConfigName.VIDEO_SERVER_PORT, "20006");
    configs.put(ConfigName.VIDEO_RECORDER_HOST, "127.78.0.4");
    configs.put(ConfigName.VIDEO_RECORDER_PORT, "20007");
    configs.put(ConfigName.MAILBOX_NSLOOKUP_HOST, "127.78.0.4");
    configs.put(ConfigName.MAILBOX_NSLOOKUP_PORT, "20008");
  }

  public static AppConfig create() {
    return new InfrastructureAppConfig();
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
