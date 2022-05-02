// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import java.util.Optional;
import java.util.Properties;

public class PropertiesAppConfig extends AppConfig {

  private final Properties properties;

  public PropertiesAppConfig(Properties properties) {
    this.properties = properties;
  }

  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return Optional.ofNullable(properties.get(configName.getPropertyName())).map(clazz::cast);
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    return get(String.class, ConfigName.ENV).map(EnvironmentType::getByName);
  }
}
