// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigValue;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class DotenvAppConfig extends AppConfig {

  private final Dotenv dotenv;

  public DotenvAppConfig(Dotenv dotenv) {
    this.dotenv = dotenv;
  }

  @Override
  protected <T> Optional<T> getAttributeByImplementation(Class<T> clazz, ConfigValue configName) {
    return Optional.ofNullable(dotenv.get(configName.getEnvName()))
      .map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected Optional<EnvironmentType> getEnvTypeByImplementation() {
    return get(String.class, ConfigValue.ENV).map(EnvironmentType::getByName);
  }

  private <T> T castToGeneric(Class<T> clazz, String stringValue) {
    if (clazz.equals(String.class)) {
      return (T) stringValue;
    } else if (clazz.equals(Boolean.class)) {
      return (T) Boolean.valueOf(stringValue);
    } else if (clazz.equals(Integer.class)) {
      return (T) Integer.valueOf(stringValue);
    } else if (clazz.equals(Double.class)) {
      return (T) Double.valueOf(stringValue);
    } else {
      throw new RuntimeException("Missing support for " + clazz.getSimpleName());
    }
  }

}
