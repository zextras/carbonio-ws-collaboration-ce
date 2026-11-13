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
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class VideoServerEventListenerIT {

  private static final byte[] HEARTBEAT = "{\"type\":256}".getBytes(StandardCharsets.UTF_8);

  @Container
  private static final GenericContainer<?> RABBITMQ =
      new GenericContainer<>("rabbitmq:4.2-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test")
          .withCreateContainerCmdModifier(
              cmd ->
                  cmd.withEntrypoint("sh", "-c")
                      .withCmd(
                          "echo 'deprecated_features.permit.transient_nonexcl_queues = false'"
                              + " > /etc/rabbitmq/conf.d/90-like-rabbitmq-4.3.conf"
                              + " && exec docker-entrypoint.sh rabbitmq-server"))
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  private final MessageBrokerVideoserverHealthMonitor healthMonitor =
      mock(MessageBrokerVideoserverHealthMonitor.class);
  private Connection connection;
  private VideoServerEventListener listener;

  private static ConnectionFactory brokerConnectionFactory() {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(RABBITMQ.getHost());
    factory.setPort(RABBITMQ.getMappedPort(5672));
    factory.setUsername("test");
    factory.setPassword("test");
    return factory;
  }

  private static void publishHeartbeat(String exchange, String routingKey) throws Exception {
    try (Connection janus = brokerConnectionFactory().newConnection();
        Channel channel = janus.createChannel()) {
      channel.basicPublish(exchange, routingKey, null, HEARTBEAT);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    connection = brokerConnectionFactory().newConnection();
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
    try (Connection janus = brokerConnectionFactory().newConnection();
        Channel channel = janus.createChannel()) {
      channel.exchangeDeclare("videoserver.events", "fanout", false, false, false, null);
      channel.basicPublish("videoserver.events", "janus-events", null, HEARTBEAT);
    }

    verify(healthMonitor, timeout(5000)).notifyVideoServerReady();
  }
}
