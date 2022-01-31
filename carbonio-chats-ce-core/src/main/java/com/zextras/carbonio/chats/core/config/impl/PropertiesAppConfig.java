// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.Optional;
import java.util.Properties;

public class PropertiesAppConfig implements AppConfig {

  private final Properties properties;

  public PropertiesAppConfig(Properties properties) {
    this.properties = properties;
  }

  public <T> Optional<T> get(Class<T> clazz, String key) {
    return Optional.ofNullable(properties.get(key)).map(clazz::cast);
  }
}
