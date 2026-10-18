// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.module;

import static com.zextras.carbonio.chats.core.config.module.CoreModule.URL_PATTERN;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.config.MessageDispatcherCredentials;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcherClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.MessageDispatcherHttpClient;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.MessageDispatcherMongooseImpl;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.core.web.utility.HttpClient;
import java.io.IOException;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class MessagingModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventDispatcher.class).to(EventDispatcherRabbitMq.class);
    bind(MessageDispatcher.class).to(MessageDispatcherMongooseImpl.class);
    bind(MessageBrokerVideoserverHealthMonitor.class);
  }

  @Singleton
  @Provides
  private MessageDispatcherCredentials getMessageDispatcherCredentials(AppConfig appConfig) {
    return new MessageDispatcherCredentials(
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_HOST).orElseThrow(),
        appConfig.get(Integer.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_PORT).orElseThrow(),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_NAME).orElse(null),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_USERNAME).orElse(null),
        appConfig.get(String.class, ConfigName.MESSAGE_DISPATCHER_DATABASE_PASSWORD).orElse(null));
  }

  @Singleton
  @Provides
  private MessageDispatcherClient getMessageDispatcherClient(
      AppConfig appConfig, HttpClient httpClient, ObjectMapper objectMapper) {
    String mongooseimUrl =
        String.format(
            URL_PATTERN,
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HOST).orElseThrow(),
            appConfig.get(String.class, ConfigName.XMPP_SERVER_HTTP_PORT).orElseThrow());
    String authToken =
        Base64.getEncoder()
            .encodeToString(
                String.join(
                        ":",
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_USERNAME).orElseThrow(),
                        appConfig.get(String.class, ConfigName.XMPP_SERVER_PASSWORD).orElseThrow())
                    .getBytes());
    return new MessageDispatcherHttpClient(httpClient, mongooseimUrl, authToken, objectMapper);
  }

  @Provides
  private Connection getRabbitMqConnection(AppConfig appConfig) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_HOST).orElseThrow());
    factory.setPort(appConfig.get(Integer.class, ConfigName.EVENT_DISPATCHER_PORT).orElseThrow());
    factory.setUsername(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_USERNAME).orElseThrow());
    factory.setPassword(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_PASSWORD).orElseThrow());
    factory.setVirtualHost(appConfig.get(String.class, ConfigName.VIRTUAL_HOST).orElse("/"));
    factory.setRequestedHeartbeat(
        appConfig.get(Integer.class, ConfigName.REQUESTED_HEARTBEAT_IN_SEC).orElse(60));
    factory.setNetworkRecoveryInterval(
        appConfig.get(Integer.class, ConfigName.NETWORK_RECOVERY_INTERVAL_IN_MILLI).orElse(30000));
    factory.setConnectionTimeout(
        appConfig.get(Integer.class, ConfigName.CONNECTION_TIMEOUT_IN_MILLI).orElse(60000));
    factory.setAutomaticRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.AUTOMATIC_RECOVERY_ENABLED).orElse(true));
    factory.setTopologyRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.TOPOLOGY_RECOVERY_ENABLED).orElse(false));
    try {
      return factory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not create connection with message broker", e);
    }
  }

  @Singleton
  @Provides
  private RabbitConnectionPoolService getRabbitConnectionPoolFactory(
      AppConfig appConfig, MessageBrokerVideoserverHealthMonitor healthMonitor) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_HOST).orElseThrow());
    factory.setPort(appConfig.get(Integer.class, ConfigName.EVENT_DISPATCHER_PORT).orElseThrow());
    factory.setUsername(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_USERNAME).orElseThrow());
    factory.setPassword(
        appConfig.get(String.class, ConfigName.EVENT_DISPATCHER_USER_PASSWORD).orElseThrow());
    factory.setVirtualHost(appConfig.get(String.class, ConfigName.VIRTUAL_HOST).orElse("/"));
    factory.setRequestedHeartbeat(
        appConfig.get(Integer.class, ConfigName.REQUESTED_HEARTBEAT_IN_SEC).orElse(60));
    factory.setNetworkRecoveryInterval(
        appConfig.get(Integer.class, ConfigName.NETWORK_RECOVERY_INTERVAL_IN_MILLI).orElse(30000));
    factory.setConnectionTimeout(
        appConfig.get(Integer.class, ConfigName.CONNECTION_TIMEOUT_IN_MILLI).orElse(60000));
    factory.setAutomaticRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.AUTOMATIC_RECOVERY_ENABLED).orElse(true));
    factory.setTopologyRecoveryEnabled(
        appConfig.get(Boolean.class, ConfigName.TOPOLOGY_RECOVERY_ENABLED).orElse(false));
    int poolSize = appConfig.get(Integer.class, ConfigName.EVENT_DISPATCHER_POOL_SIZE).orElse(10);
    return new RabbitConnectionPoolService(factory, poolSize, healthMonitor);
  }

  @Provides
  private Channel getRabbitMqChannel(Connection connection) {
    if (connection == null || !connection.isOpen()) {
      throw new EventDispatcherException("Message broker connection is not up!");
    }
    try {
      return connection.createChannel();
    } catch (IOException e) {
      throw new EventDispatcherException(
          "Could not create channel on message broker connection", e);
    }
  }
}
