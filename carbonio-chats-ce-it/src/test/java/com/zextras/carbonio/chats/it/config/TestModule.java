package com.zextras.carbonio.chats.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bind(IntegrationTestUtils.class);
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    return new TestAppConfig();
  }
}
