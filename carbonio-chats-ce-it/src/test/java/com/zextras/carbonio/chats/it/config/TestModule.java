package com.zextras.carbonio.chats.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    return new TestAppConfig();
  }
}
