package com.zextras.carbonio.chats.it.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.AppConfigBuilder;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.impl.ConsulAppConfig;
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
    AppConfigBuilder builder = AppConfigBuilder.create().add(TestAppConfig.create());
    builder.add(ConsulAppConfig.create(
      builder.getAppConfig().get(String.class, ConfigName.CONSUL_HOST).orElseThrow(),
      builder.getAppConfig().get(Integer.class, ConfigName.CONSUL_PORT).orElseThrow(),
      builder.getAppConfig().get(String.class, ConfigName.CONSUL_TOKEN).orElse(null)));

    return builder.getAppConfig();
  }

  @Provides
  @Singleton
  public Clock getClock() {
    return AppClock.create(ZoneId.systemDefault());
  }
}
