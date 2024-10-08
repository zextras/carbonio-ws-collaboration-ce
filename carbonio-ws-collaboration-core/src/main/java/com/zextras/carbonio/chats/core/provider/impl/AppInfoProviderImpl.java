// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.provider.impl;

import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.provider.AppInfoProvider;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;

@Singleton
public class AppInfoProviderImpl implements AppInfoProvider {

  private static final String VERSION_PROP = "ws-collaboration.version";

  private Properties properties;

  public AppInfoProviderImpl() {
    load();
  }

  @Override
  public Optional<String> getVersion() {
    return properties == null
        ? Optional.empty()
        : Optional.of(properties.getProperty(VERSION_PROP));
  }

  private void load() {
    Properties properties = new Properties();
    try (InputStream propertiesStream =
        this.getClass().getClassLoader().getResourceAsStream("build-information")) {
      properties.load(propertiesStream);
      ChatsLogger.info("Internal properties config loaded");
      this.properties = properties;
    } catch (Exception e) {
      ChatsLogger.warn("Could not load application information", e);
    }
  }
}
