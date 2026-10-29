// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigContribution;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InfrastructureAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.INFRASTRUCTURE;

  private final Map<String, String> configs;

  private InfrastructureAppConfig(Collection<ConfigContribution> catalog) {
    this.configs = new HashMap<>();
    catalog.forEach(
        contribution ->
            contribution
                .infrastructureDefaults()
                .forEach(
                    (key, value) -> {
                      if (configs.containsKey(key)) {
                        throw new IllegalStateException(
                            "Duplicate infrastructure default for config key " + key);
                      }
                      configs.put(key, value);
                    }));
  }

  public static AppConfig create(Collection<ConfigContribution> catalog) {
    return new InfrastructureAppConfig(catalog);
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
