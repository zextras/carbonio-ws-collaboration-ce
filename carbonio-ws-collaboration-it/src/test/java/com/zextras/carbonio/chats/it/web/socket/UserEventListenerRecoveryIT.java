// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class UserEventListenerRecoveryIT {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final DomainEvent EVENT = DomainEvent.create().type(EventType.PONG);

  private static final GenericContainer<?> RABBITMQ =
      new GenericContainer<>("rabbitmq:4.2-alpine")
          .withEnv("RABBITMQ_DEFAULT_USER", "test")
          .withEnv("RABBITMQ_DEFAULT_PASS", "test")
          .withExposedPorts(5672)
          .waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));

  @BeforeAll
  static void startBroker() {
    RABBITMQ.start();
  }

  @AfterAll
  static void stopBroker() {
    RABBITMQ.stop();
  }

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
    RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
    when(session.getAsyncRemote()).thenReturn(remote);
    sessions.add(session);
    return remote;
  }

  @Test
  void afterTheBrokerDropsEveryConnection_eventsFlowAgain() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper();
    String json = objectMapper.writeValueAsString(EVENT);
    EventWebSocketSessions sessions = new EventWebSocketSessions(mock(SessionPingManager.class));
    RemoteEndpoint.Async remote = connect(sessions, USER_A);
    WebsocketVersionMigrator migrator = mock(WebsocketVersionMigrator.class);
    when(migrator.downgradeIfNeeded(anyString(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    MessageBrokerVideoserverHealthMonitor healthMonitor =
        mock(MessageBrokerVideoserverHealthMonitor.class);
    UserEventListener listener =
        new UserEventListener(brokerConnectionFactory(), sessions, migrator, healthMonitor);
    EventDispatcherRabbitMq dispatcher =
        new EventDispatcherRabbitMq(brokerConnectionFactory(), objectMapper);
    try {
      dispatcher.sendToUserExchange(USER_A, EVENT);
      verify(remote, timeout(5000)).sendText(json);

      RABBITMQ.execInContainer("rabbitmqctl", "close_all_connections", "test");

      verify(healthMonitor, timeout(5000)).notifyBrokerDown();
      verify(healthMonitor, timeout(10000)).notifyBrokerRecovered();
      await().atMost(Duration.ofSeconds(10)).until(dispatcher::isAlive);
      dispatcher.sendToUserExchange(USER_A, EVENT);
      verify(remote, timeout(5000).times(2)).sendText(json);
    } finally {
      dispatcher.stop();
      listener.stop();
    }
  }
}
