// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.socket;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
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
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserEventRoutingIT {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String USER_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final DomainEvent EVENT = DomainEvent.create().type(EventType.PONG);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final ConnectionFactory broker = new MockConnectionFactory();
  private Instance first;
  private Instance second;
  private EventDispatcherRabbitMq dispatcher;
  private String json;

  private record Instance(EventWebSocketSessions sessions, UserEventListener listener) {}

  @BeforeEach
  void setUp() throws Exception {
    first = startInstance();
    second = startInstance();
    dispatcher = new EventDispatcherRabbitMq(broker, objectMapper);
    json = objectMapper.writeValueAsString(EVENT);
  }

  @AfterEach
  void tearDown() {
    dispatcher.stop();
    first.listener().stop();
    second.listener().stop();
  }

  private Instance startInstance() throws Exception {
    EventWebSocketSessions sessions = new EventWebSocketSessions(mock(SessionPingManager.class));
    WebsocketVersionMigrator migrator = mock(WebsocketVersionMigrator.class);
    when(migrator.downgradeIfNeeded(anyString(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    UserEventListener listener =
        new UserEventListener(
            broker, sessions, migrator, mock(MessageBrokerVideoserverHealthMonitor.class));
    return new Instance(sessions, listener);
  }

  private static RemoteEndpoint.Async connect(Instance instance, String sessionId, String userId) {
    Session session = mock(Session.class);
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> userProps = new HashMap<>();
    userProps.put(HttpSession.class.getName(), httpSession);
    when(httpSession.getAttribute("userId")).thenReturn(userId);
    when(session.getUserProperties()).thenReturn(userProps);
    when(session.getId()).thenReturn(sessionId);
    when(session.isOpen()).thenReturn(true);
    when(session.getNegotiatedSubprotocol()).thenReturn("1.6.2");
    RemoteEndpoint.Async remote = mock(RemoteEndpoint.Async.class);
    when(session.getAsyncRemote()).thenReturn(remote);
    instance.sessions().add(session);
    return remote;
  }

  @Test
  void anEventForAUser_reachesTheirSessionsOnEveryInstanceOnly() {
    RemoteEndpoint.Async userAOnFirst = connect(first, "a1", USER_A);
    RemoteEndpoint.Async userAOnSecond = connect(second, "a2", USER_A);
    RemoteEndpoint.Async userBOnFirst = connect(first, "b1", USER_B);

    dispatcher.sendToUserExchange(USER_A, EVENT);

    verify(userAOnFirst, timeout(2000)).sendText(json);
    verify(userAOnSecond, timeout(2000)).sendText(json);
    verify(userBOnFirst, after(300).never()).sendText(anyString());
  }

  @Test
  void aSessionTargetedEvent_reachesOnlyThatSessionWhicheverInstanceHoldsIt() {
    RemoteEndpoint.Async userAOnFirst = connect(first, "a1", USER_A);
    RemoteEndpoint.Async userAOnSecond = connect(second, "a2", USER_A);

    dispatcher.sendToUserQueue(USER_A, "a2", EVENT);

    verify(userAOnSecond, timeout(2000)).sendText(json);
    verify(userAOnFirst, after(300).never()).sendText(anyString());
  }
}
