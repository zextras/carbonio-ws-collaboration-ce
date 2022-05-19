package com.zextras.carbonio.chats.core;

import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;

public class AppConfigBuilder {

  private AppConfig appConfig;
  private boolean   hasConfig = false;

  public static AppConfigBuilder create() {
    return new AppConfigBuilder();
  }

  public AppConfigBuilder add(AppConfig appConfig) {
    if (appConfig != null) {
      appConfig.load();
      if (this.appConfig == null) {
        this.appConfig = appConfig;
        this.hasConfig = true;
      } else {
        this.appConfig.addToChain(appConfig);
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
