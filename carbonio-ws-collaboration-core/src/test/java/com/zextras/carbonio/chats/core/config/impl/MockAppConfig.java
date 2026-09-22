// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MockAppConfig extends AppConfig {

  private final AppConfigType appConfigType;
  private final Map<String, String> configMap;

  private boolean loaded = true;

  private MockAppConfig() {
    this(AppConfigType.IN_MEMORY);
  }

  private MockAppConfig(AppConfigType type) {
    this.appConfigType = type;
    configMap = new HashMap<>();
  }

  public static MockAppConfig create() {
    return new MockAppConfig();
  }

  public static MockAppConfig create(AppConfigType appConfigType) {
    return new MockAppConfig(appConfigType);
  }

  @Override
  public MockAppConfig load() {
    return this;
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  public MockAppConfig setLoaded(boolean loaded) {
    this.loaded = loaded;
    return this;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, String key) {
    return Optional.ofNullable(configMap.get(key))
        .map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  public AppConfigType getType() {
    return appConfigType;
  }

  @Override
  public boolean setConfigByImplementation(String key, String value) {
    configMap.put(key, value);
    return true;
  }
}
