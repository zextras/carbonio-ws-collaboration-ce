// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import java.util.Optional;
import org.eclipse.microprofile.config.Config;

/**
 * Head of the CE configuration chain on Quarkus: any {@code ConfigName} can be overridden through
 * MicroProfile Config as {@code ws.config.<CONFIG_NAME>} (application.properties, system properties,
 * {@code WS_CONFIG_<CONFIG_NAME>} environment variables, test profiles). Unset keys fall through to
 * the CE infrastructure defaults and Consul.
 */
public class MicroProfileAppConfig extends AppConfig {

  public static final String PREFIX = "ws.config.";

  private final Config config;

  public MicroProfileAppConfig(Config config) {
    this.config = config;
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
    return config
        .getOptionalValue(PREFIX + key, String.class)
        .map(stringValue -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected boolean setConfigByImplementation(String key, String value) {
    return false;
  }

  @Override
  public AppConfigType getType() {
    return AppConfigType.MICROPROFILE;
  }
}
