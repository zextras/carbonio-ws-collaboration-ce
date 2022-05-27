package com.zextras.carbonio.chats.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.AppConfigBuilder;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import java.time.Clock;
import java.time.ZoneId;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bind(IntegrationTestUtils.class);
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    return AppConfigBuilder.create().add(TestAppConfig.create()).getAppConfig();
  }

  @Provides
  @Singleton
  public Clock getClock() {
    return AppClock.create(ZoneId.systemDefault());
  }
}
