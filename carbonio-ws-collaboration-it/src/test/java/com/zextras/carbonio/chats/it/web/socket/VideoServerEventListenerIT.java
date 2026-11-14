// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.core.web.socket.VideoServerEventListener;
import com.zextras.carbonio.chats.it.extensions.RabbitMqExtension;
import com.zextras.carbonio.chats.it.tools.RabbitMqBroker;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(RabbitMqExtension.class)
class VideoServerEventListenerIT {

  private static final byte[] HEARTBEAT = "{\"type\":256}".getBytes(StandardCharsets.UTF_8);

  private final MessageBrokerVideoserverHealthMonitor healthMonitor =
      mock(MessageBrokerVideoserverHealthMonitor.class);
  private final ConnectionFactory broker;
  private Connection connection;
  private VideoServerEventListener listener;

  VideoServerEventListenerIT(RabbitMqBroker broker) {
    this.broker = broker.connectionFactory();
  }

  private void publishHeartbeat(String exchange, String routingKey) throws Exception {
    try (Connection janus = broker.newConnection();
        Channel channel = janus.createChannel()) {
      channel.basicPublish(exchange, routingKey, null, HEARTBEAT);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    connection = broker.newConnection();
    listener =
        new VideoServerEventListener(
            connection.createChannel(),
            mock(EventDispatcher.class),
            new ObjectMapper(),
            mock(VideoServerService.class),
            healthMonitor);
    listener.start();
  }

  @AfterEach
  void tearDown() throws Exception {
    listener.stop();
    connection.close();
  }

  @Test
  void anEventOnTheLegacyJanusExchange_reachesTheListener_onABrokerRefusingTransientSharedQueues()
      throws Exception {
    publishHeartbeat("janus-exchange", "janus-events");

    verify(healthMonitor, timeout(5000)).notifyVideoServerReady();
  }

  @Test
  void anEventOnTheExchangeJanusDeclares_reachesTheListener() throws Exception {
    try (Connection janus = broker.newConnection();
        Channel channel = janus.createChannel()) {
      channel.exchangeDeclare("videoserver.events", "fanout", false, false, false, null);
      channel.basicPublish("videoserver.events", "janus-events", null, HEARTBEAT);
    }

    verify(healthMonitor, timeout(5000)).notifyVideoServerReady();
  }
}
