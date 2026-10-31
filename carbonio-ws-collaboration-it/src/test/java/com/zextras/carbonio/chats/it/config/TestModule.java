// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.config;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MeetingTestUtils;
import com.zextras.carbonio.chats.it.web.api.versioning.DummyVersionedApi;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.concurrent.TimeoutException;

public class TestModule extends AbstractModule {

  @Override
  protected void configure() {
    super.configure();
    bind(IntegrationTestUtils.class);
    bind(MeetingTestUtils.class);
    bind(DummyVersionedApi.class);
  }

  @Provides
  @Singleton
  public AppConfig getAppConfig() {
    return TestAppConfig.create();
  }

  @Provides
  @Singleton
  public Clock getClock() {
    return AppClock.create(ZoneId.systemDefault());
  }

  @Provides
  @Singleton
  public ConnectionFactory getConnectionFactory() {
    return new MockConnectionFactory();
  }

  @Provides
  public Connection getRabbitMqConnection(ConnectionFactory connectionFactory) {
    try {
      return connectionFactory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Failed to create RabbitMQ connection", e);
    }
  }

  @Provides
  public Channel getRabbitMqChannel(Connection connection) {
    try {
      return connection.createChannel();
    } catch (IOException e) {
      throw new EventDispatcherException("Failed to create RabbitMQ channel", e);
    }
  }
}
