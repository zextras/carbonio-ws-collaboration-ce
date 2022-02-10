// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.EnvironmentType;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.Optional;

@SuppressWarnings("unchecked")
public class DotenvAppConfig implements AppConfig {

  private final Dotenv dotenv;

  public DotenvAppConfig(Dotenv dotenv) {
    this.dotenv = dotenv;
  }

  @Override
  public <T> Optional<T> get(Class<T> clazz, String key) {
    return Optional.ofNullable(dotenv.get(key)).map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  public EnvironmentType getEnvType() {
    return EnvironmentType.getByName(get(String.class, "ENV").orElse(EnvironmentType.PRODUCTION.getName()));
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
