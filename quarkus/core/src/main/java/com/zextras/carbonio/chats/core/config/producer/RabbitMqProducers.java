// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.config.producer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.RabbitConnectionPoolService;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * RabbitMQ part of CE's Guice {@code CoreModule}. The {@link ConnectionFactory} is a
 * {@code @DefaultBean} so tests can swap in rabbitmq-mock (see test-support). Connections and
 * channels stay unscoped ({@code @Dependent}) as in Guice: each injection point gets its own.
 */
@ApplicationScoped
public class RabbitMqProducers {

  @Produces
  @Singleton
  @DefaultBean
  ConnectionFactory connectionFactory(AppConfig appConfig) {
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
    return factory;
  }

  @Produces
  @Dependent
  Connection rabbitMqConnection(ConnectionFactory factory) {
    try {
      return factory.newConnection();
    } catch (IOException | TimeoutException e) {
      throw new EventDispatcherException("Could not create connection with message broker", e);
    }
  }

  @Produces
  @Dependent
  Channel rabbitMqChannel(Connection connection) {
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

  @Produces
  @Singleton
  RabbitConnectionPoolService rabbitConnectionPoolService(
      ConnectionFactory factory,
      AppConfig appConfig,
      MessageBrokerVideoserverHealthMonitor healthMonitor) {
    int poolSize = appConfig.get(Integer.class, ConfigName.EVENT_DISPATCHER_POOL_SIZE).orElse(10);
    return new RabbitConnectionPoolService(factory, poolSize, healthMonitor);
  }
}
