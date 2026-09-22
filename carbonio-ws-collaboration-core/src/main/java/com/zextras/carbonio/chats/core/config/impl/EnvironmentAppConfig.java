// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class EnvironmentAppConfig extends AppConfig {
  private static final AppConfigType CONFIG_TYPE = AppConfigType.DOCKER;

  private final Map<String, String> configs;

  private EnvironmentAppConfig(Collection<ConfigContribution> catalog) {
    this.configs = new HashMap<>();
    catalog.forEach(
        contribution ->
            contribution
                .environmentKeys()
                .forEach(
                    key -> {
                      if (configs.containsKey(key)) {
                        throw new IllegalStateException("Duplicate environment key " + key);
                      }
                      configs.put(key, System.getenv(key));
                    }));
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
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, String key) {
    return Optional.ofNullable(configs.get(key))
        .map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected boolean setConfigByImplementation(String key, String value) {
    return false;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }
}
