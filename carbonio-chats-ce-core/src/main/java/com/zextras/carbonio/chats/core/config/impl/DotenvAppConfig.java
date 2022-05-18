// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.impl;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class DotenvAppConfig extends AppConfig {

  private static final AppConfigType CONFIG_TYPE = AppConfigType.ENVIRONMENT;

  private final Path envDirectory;

  private Dotenv  dotenv;
  private boolean loaded = false;

  private DotenvAppConfig(Path envDirectory) {
    this.envDirectory = envDirectory;
  }

  public static AppConfig create(Path envDirectory) {
    if (envDirectory == null || !Files.exists(envDirectory)) {
      ChatsLogger.warn("Environment not found");
      return null;
    }
    return new DotenvAppConfig(envDirectory);
  }

  @Override
  public AppConfig load() {
    this.dotenv = Dotenv.configure()
      .ignoreIfMissing()
      .directory(envDirectory.toString())
      .filename(".env")
      .load();
    loaded = true;
    ChatsLogger.info("Env config loaded");
    return this;
  }

  @Override
  public boolean isLoaded() {
    return loaded;
  }

  @Override
  protected <T> Optional<T> getConfigByImplementation(Class<T> clazz, ConfigName configName) {
    return Optional.ofNullable(dotenv.get(configName.getEnvName()))
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
