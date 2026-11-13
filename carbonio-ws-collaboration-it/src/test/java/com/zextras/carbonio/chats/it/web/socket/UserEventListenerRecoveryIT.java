// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.infrastructure.event.impl.EventDispatcherRabbitMq;
import com.zextras.carbonio.chats.core.web.socket.EventWebSocketSessions;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerVideoserverHealthMonitor;
import com.zextras.carbonio.chats.core.web.socket.SessionPingManager;
import com.zextras.carbonio.chats.core.web.socket.UserEventListener;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class UserEventListenerRecoveryIT {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final DomainEvent EVENT = DomainEvent.create().type(EventType.PONG);

  @Container
  private static final GenericContainer<?> RABBITMQ =
      new GenericContainer<>("rabbitmq:4.2-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final MessageBrokerVideoserverHealthMonitor healthMonitor =
      mock(MessageBrokerVideoserverHealthMonitor.class);
  private String json;
  private RemoteEndpoint.Async remote;
  private UserEventListener listener;
  private EventDispatcherRabbitMq dispatcher;

  private static ConnectionFactory brokerConnectionFactory() {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(RABBITMQ.getHost());
    factory.setPort(RABBITMQ.getMappedPort(5672));
    factory.setUsername("test");
    factory.setPassword("test");
    factory.setAutomaticRecoveryEnabled(true);
    factory.setNetworkRecoveryInterval(200);
    return factory;
  }

  private static RemoteEndpoint.Async connect(EventWebSocketSessions sessions, String userId) {
    Session session = mock(Session.class);
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> userProps = new HashMap<>();
    userProps.put(HttpSession.class.getName(), httpSession);
    when(httpSession.getAttribute("userId")).thenReturn(userId);
    when(session.getUserProperties()).thenReturn(userProps);
    when(session.getId()).thenReturn("a1");
    when(session.isOpen()).thenReturn(true);
    when(session.getNegotiatedSubprotocol()).thenReturn("1.6.2");
    RemoteEndpoint.Async async = mock(RemoteEndpoint.Async.class);
    when(session.getAsyncRemote()).thenReturn(async);
    sessions.add(session);
    return async;
  }

  private static List<String> userEventQueues() throws Exception {
    return RABBITMQ
        .execInContainer("rabbitmqctl", "list_queues", "--quiet", "--no-table-headers", "name")
        .getStdout()
        .lines()
        .map(String::strip)
        .filter(name -> name.startsWith("amq.gen-"))
        .toList();
  }

  @BeforeEach
  void setUp() throws Exception {
    json = objectMapper.writeValueAsString(EVENT);
    EventWebSocketSessions sessions = new EventWebSocketSessions(mock(SessionPingManager.class));
    remote = connect(sessions, USER_A);
    listener =
        new UserEventListener(
            brokerConnectionFactory(),
            sessions,
            new WebsocketVersionMigrator(objectMapper),
            healthMonitor);
    dispatcher = new EventDispatcherRabbitMq(brokerConnectionFactory(), objectMapper);
  }

  @AfterEach
  void tearDown() {
    dispatcher.stop();
    listener.stop();
  }

  private void reconnectEverything() throws Exception {
    RABBITMQ.execInContainer("rabbitmqctl", "close_all_connections", "test");
    verify(healthMonitor, timeout(5000)).notifyBrokerDown();
    verify(healthMonitor, timeout(10000)).notifyBrokerRecovered();
    await().atMost(Duration.ofSeconds(10)).until(dispatcher::isAlive);
  }

  @Test
  void afterTheBrokerDropsEveryConnection_eventsFlowAgain() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);
    verify(remote, timeout(5000)).sendText(json);

    reconnectEverything();

    dispatcher.sendToUserExchange(USER_A, EVENT);
    verify(remote, timeout(5000).times(2)).sendText(json);
  }

  @Test
  void anEventWithUnreadableRecipients_doesNotStopLaterEvents() throws Exception {
    try (Connection connection = brokerConnectionFactory().newConnection();
        Channel channel = connection.createChannel()) {
      channel.basicPublish(
          "wsc.events",
          "",
          new BasicProperties.Builder()
              .headers(Map.of("users", Arrays.asList((Object) null)))
              .build(),
          new byte[0]);
    }

    dispatcher.sendToUserExchange(USER_A, EVENT);

    verify(remote, timeout(5000)).sendText(json);
  }

  @Test
  void afterTheBrokerDeletesTheQueue_eachEventFlowsOnceAgain_evenAfterAReconnect()
      throws Exception {
    String deleted = userEventQueues().getFirst();

    RABBITMQ.execInContainer("rabbitmqctl", "delete_queue", deleted);

    await()
        .atMost(Duration.ofSeconds(10))
        .until(
            UserEventListenerRecoveryIT::userEventQueues,
            queues -> queues.size() == 1 && !queues.contains(deleted));
    dispatcher.sendToUserExchange(USER_A, EVENT);
    verify(remote, timeout(5000)).sendText(json);

    reconnectEverything();

    dispatcher.sendToUserExchange(USER_A, EVENT);
    verify(remote, after(2000).times(2)).sendText(json);
  }
}
