// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config;

public class AppConfigBuilder {

  private AppConfig appConfig;
  private boolean   hasConfig = false;

  public static AppConfigBuilder create() {
    return new AppConfigBuilder();
  }

  public AppConfigBuilder add(AppConfig appConfig) {
    if (appConfig != null) {
      appConfig.load();
      if (appConfig.isLoaded()) {
        if (this.appConfig == null) {
          this.appConfig = appConfig;
          this.hasConfig = true;
        } else {
          this.appConfig.addToChain(appConfig);
        }
      }
    }
    return this;
  }

  public AppConfig getAppConfig() {
    return this.appConfig;
  }

  public boolean hasConfig() {
    return hasConfig;
  }
}
