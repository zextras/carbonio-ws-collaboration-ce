// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.LongStringHelper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.web.socket.versioning.WebsocketVersionMigrator;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class UserEventListenerTest {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String USER_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String EVENT = "{\"type\":\"roomCreated\"}";

  private ConnectionFactory factory;
  private Connection connection;
  private Channel channel;
  private EventWebSocketSessions sessions;
  private WebsocketVersionMigrator migrator;
  private MessageBrokerVideoserverHealthMonitor healthMonitor;
  private UserEventListener listener;

  @BeforeEach
  void setUp() throws Exception {
    factory = mock(ConnectionFactory.class);
    connection = mock(Connection.class, withSettings().extraInterfaces(Recoverable.class));
    channel = mock(Channel.class);
    when(factory.clone()).thenReturn(factory);
    when(factory.newConnection(anyString())).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.queueDeclare(eq(""), eq(false), eq(true), eq(true), any()))
        .thenReturn(new AMQP.Queue.DeclareOk.Builder().queue("amq.gen-1").build());
    sessions = new EventWebSocketSessions(mock(SessionPingManager.class));
    migrator = mock(WebsocketVersionMigrator.class);
    when(migrator.downgradeIfNeeded(anyString(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    healthMonitor = mock(MessageBrokerVideoserverHealthMonitor.class);
    listener = new UserEventListener(factory, sessions, migrator, healthMonitor);
  }

  private Session openSession(String id, String userId, String subprotocol) {
    Session session = mock(Session.class);
    HttpSession httpSession = mock(HttpSession.class);
    Map<String, Object> userProps = new HashMap<>();
    userProps.put(HttpSession.class.getName(), httpSession);
    when(httpSession.getAttribute("userId")).thenReturn(userId);
    when(session.getUserProperties()).thenReturn(userProps);
    when(session.getId()).thenReturn(id);
    when(session.isOpen()).thenReturn(true);
    when(session.getNegotiatedSubprotocol()).thenReturn(subprotocol);
    when(session.getAsyncRemote()).thenReturn(mock(RemoteEndpoint.Async.class));
    sessions.add(session);
    return session;
  }

  private DeliverCallback consumerCallback() throws Exception {
    ArgumentCaptor<DeliverCallback> callback = ArgumentCaptor.forClass(DeliverCallback.class);
    verify(channel)
        .basicConsume(eq("amq.gen-1"), eq(true), callback.capture(), any(CancelCallback.class));
    return callback.getValue();
  }

  private static Delivery delivery(String userId, Map<String, Object> headers) {
    return new Delivery(
        new Envelope(1, false, "wsc.events", userId),
        new BasicProperties.Builder().headers(headers).build(),
        EVENT.getBytes(StandardCharsets.UTF_8));
  }

  @Test
  void construction_bindsAServerNamedExclusiveQueueToEveryUserEvent() throws Exception {
    verify(factory).setTopologyRecoveryEnabled(true);
    verify(channel).exchangeDeclare("wsc.events", BuiltinExchangeType.TOPIC, true);
    verify(channel).queueDeclare("", false, true, true, Map.of("x-max-length", 10_000));
    verify(channel).queueBind("amq.gen-1", "wsc.events", "#");
  }

  @Test
  void delivery_reachesEveryLocalSessionOfTheRoutedUserOnly() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");
    Session a2 = openSession("a2", USER_A, "1.6.2");
    Session b1 = openSession("b1", USER_B, "1.6.2");

    consumerCallback().handle("tag", delivery(USER_A, null));

    verify(a1.getAsyncRemote()).sendText(EVENT);
    verify(a2.getAsyncRemote()).sendText(EVENT);
    verify(b1.getAsyncRemote(), never()).sendText(anyString());
  }

  @Test
  void delivery_withQueueId_reachesOnlyThatSession() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");
    Session a2 = openSession("a2", USER_A, "1.6.2");

    consumerCallback()
        .handle("tag", delivery(USER_A, Map.of("queueId", LongStringHelper.asLongString("a2"))));

    verify(a1.getAsyncRemote(), never()).sendText(anyString());
    verify(a2.getAsyncRemote()).sendText(EVENT);
  }

  @Test
  void delivery_withQueueIdOfAnotherInstance_reachesNoLocalSession() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");

    consumerCallback()
        .handle(
            "tag", delivery(USER_A, Map.of("queueId", LongStringHelper.asLongString("remote"))));

    verify(a1.getAsyncRemote(), never()).sendText(anyString());
  }

  @Test
  void delivery_isDowngradedToEachSessionsVersion() throws Exception {
    Session latest = openSession("a1", USER_A, "1.6.2");
    Session legacy = openSession("a2", USER_A, "");
    when(migrator.downgradeIfNeeded(EVENT, "1.6.2")).thenReturn("latest-json");
    when(migrator.downgradeIfNeeded(EVENT, "1.6.0")).thenReturn("legacy-json");

    consumerCallback().handle("tag", delivery(USER_A, null));

    verify(latest.getAsyncRemote()).sendText("latest-json");
    verify(legacy.getAsyncRemote()).sendText("legacy-json");
  }

  @Test
  void brokerConnectionLost_notifiesBrokerDown() throws Exception {
    ArgumentCaptor<ShutdownListener> shutdown = ArgumentCaptor.forClass(ShutdownListener.class);
    verify(connection).addShutdownListener(shutdown.capture());

    shutdown.getValue().shutdownCompleted(new ShutdownSignalException(true, false, null, null));

    verify(healthMonitor).notifyBrokerDown();
  }

  @Test
  void applicationClosingTheConnection_doesNotNotifyBrokerDown() throws Exception {
    ArgumentCaptor<ShutdownListener> shutdown = ArgumentCaptor.forClass(ShutdownListener.class);
    verify(connection).addShutdownListener(shutdown.capture());

    shutdown.getValue().shutdownCompleted(new ShutdownSignalException(true, true, null, null));

    verify(healthMonitor, never()).notifyBrokerDown();
  }

  @Test
  void connectionRecovered_notifiesBrokerRecovered() throws Exception {
    ArgumentCaptor<RecoveryListener> recovery = ArgumentCaptor.forClass(RecoveryListener.class);
    verify((Recoverable) connection).addRecoveryListener(recovery.capture());

    recovery.getValue().handleRecovery((Recoverable) connection);

    verify(healthMonitor).notifyBrokerRecovered();
  }
}
