// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import com.zextras.carbonio.chats.core.config.ConfigName;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnvironmentAppConfig extends AppConfig {
  private static final AppConfigType CONFIG_TYPE = AppConfigType.DOCKER;

  private final Map<ConfigName, String> configs;

  private EnvironmentAppConfig(Collection<ConfigContribution> catalog) {
    this.configs = new EnumMap<>(ConfigName.class);
    catalog.forEach(
        contribution ->
            contribution
                .environmentKeys()
                .forEach(key -> configs.put(key, System.getenv(key.name()))));
  }

  public static AppConfig create() {
    // No-catalog overload: CE's own key set, preserving pre-registry behavior.
    return create(List.of(new CoreConfigContribution()));
  }

  public static AppConfig create(Collection<ConfigContribution> catalog) {
    return new EnvironmentAppConfig(catalog);
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
