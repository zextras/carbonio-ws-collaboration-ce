// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.events;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@ApplicationScoped
public class RabbitProducers {

  @Produces
  @Singleton
  @DefaultBean
  ConnectionFactory connectionFactory(RabbitConfig config) {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(config.host());
    factory.setPort(config.port());
    factory.setUsername(config.username());
    factory.setPassword(config.password());
    factory.setVirtualHost(config.virtualHost());
    factory.setAutomaticRecoveryEnabled(true);
    return factory;
  }

  @Produces
  @Singleton
  Connection connection(ConnectionFactory factory) throws IOException, TimeoutException {
    return factory.newConnection("carbonio-ws-collaboration");
  }

  @Produces
  @Singleton
  Channel publishingChannel(Connection connection) throws IOException {
    return connection.createChannel();
  }

  void close(@Disposes Connection connection) throws IOException {
    if (connection.isOpen()) {
      connection.close();
    }
  }
}
