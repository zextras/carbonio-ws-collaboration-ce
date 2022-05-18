package com.zextras.carbonio.chats.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
import com.zextras.carbonio.chats.core.config.impl.DotenvAppConfig;
import com.zextras.carbonio.chats.core.config.impl.PropertiesAppConfig;
import com.zextras.carbonio.chats.it.Utils.IntegrationTestUtils;
import java.nio.file.Path;
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
     AppConfig appConfig = TestAppConfig.create().load();
     appConfig.addToChain(ConsulAppConfig.create(
      appConfig.get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
      appConfig.get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
      appConfig.get(String.class, ConfigName.CONSUL_TOKEN).orElse(null)).load()
    );
    return appConfig;


  }


  @Provides
  @Singleton
  public Clock getClock() {
    return AppClock.create(ZoneId.systemDefault());
  }
}
