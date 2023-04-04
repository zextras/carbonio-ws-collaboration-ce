// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class PropertiesAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.PROPERTY;

  private final Path propertiesPath;

  private Properties properties;
  private boolean    loaded = false;

  private PropertiesAppConfig(Path propertiesPath) {
    this.propertiesPath = propertiesPath;
  }

  public static AppConfig create(Path propertiesPath) {
    if (propertiesPath == null || !Files.exists(propertiesPath)) {
      ChatsLogger.warn("Property files not found");
      return null;
    }
    return new PropertiesAppConfig(propertiesPath);
  }

  @Override
  public AppConfig load() {
    Properties properties = new Properties();
    try (InputStream propertiesStream = new FileInputStream(propertiesPath.toString())) {
      properties.load(propertiesStream);
      ChatsLogger.info("Properties config loaded");
      this.properties = properties;
      loaded = true;
    } catch (Exception e) {
      ChatsLogger.warn("Could not load properties file: " + propertiesPath.getFileName(), e);
      loaded = false;
    }
    return this;
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return Optional.ofNullable(properties.getProperty(configName.getPropertyName()))
      .map((stringValue) -> castToGeneric(clazz, stringValue));
  }

  @Override
  protected boolean setConfigByImplementation(ConfigName configName, String value) {
    properties.setProperty(configName.getPropertyName(), value);
    return true;
  }

  @Override
  public AppConfigType getType() {
    return CONFIG_TYPE;
  }
}
