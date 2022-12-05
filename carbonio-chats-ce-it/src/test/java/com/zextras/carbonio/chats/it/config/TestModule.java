package com.zextras.carbonio.chats.it.config;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.AppConfigBuilder;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Optional;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bind(IntegrationTestUtils.class);
    bind(MeetingTestUtils.class);
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

  @Provides
  @Singleton
  public Optional<Connection> getRabbitMqConnection() {
    return Optional.of(new MockConnectionFactory().newConnection());
  }
}
