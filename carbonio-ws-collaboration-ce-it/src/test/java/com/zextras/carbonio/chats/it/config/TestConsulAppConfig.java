// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.config;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.AppConfigType;
import io.ebeaninternal.server.expression.Op;
import io.vavr.control.Option;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TestConsulAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.CONSUL;
  private final Map<String, String> cache;

  public TestConsulAppConfig() {
    cache = new HashMap<>();
  }

  public static AppConfig create() {
    return new TestConsulAppConfig();
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
    return Optional.of(cache.get(configName.getConsulName())).map((stringValue) -> castToGeneric(clazz, stringValue));
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
