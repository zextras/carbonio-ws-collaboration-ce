// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class UserEventListenerTest {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String USER_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String USER_C = "cccccccc-cccc-cccc-cccc-cccccccccccc";
  private static final String EVENT = "{\"type\":\"roomCreated\"}";

  private Connection connection;
  private Channel channel;
  private EventWebSocketSessions sessions;
  private WebsocketVersionMigrator migrator;
  private MessageBrokerVideoserverHealthMonitor healthMonitor;
  private UserEventListener listener;

  @BeforeEach
  void setUp() throws Exception {
    connection = mock(Connection.class, withSettings().extraInterfaces(Recoverable.class));
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.queueDeclare(eq(""), eq(false), eq(true), eq(true), any()))
        .thenReturn(new AMQP.Queue.DeclareOk.Builder().queue("amq.gen-1").build());
    sessions = new EventWebSocketSessions(mock(SessionPingManager.class));
    migrator = mock(WebsocketVersionMigrator.class);
    when(migrator.downgradeIfNeeded(anyString(), anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    healthMonitor = mock(MessageBrokerVideoserverHealthMonitor.class);
    listener = new UserEventListener(connection, sessions, migrator, healthMonitor);
    listener.start();
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

  private CancelCallback cancelCallback() throws Exception {
    ArgumentCaptor<CancelCallback> callback = ArgumentCaptor.forClass(CancelCallback.class);
    verify(channel)
        .basicConsume(eq("amq.gen-1"), eq(true), any(DeliverCallback.class), callback.capture());
    return callback.getValue();
  }

  private static Delivery delivery(Map<String, Object> headers) {
    return new Delivery(
        new Envelope(1, false, "wsc.events", ""),
        new BasicProperties.Builder().headers(headers).build(),
        EVENT.getBytes(StandardCharsets.UTF_8));
  }

  private static Delivery forUsers(String... userIds) {
    return delivery(
        Map.of("users", Arrays.stream(userIds).map(LongStringHelper::asLongString).toList()));
  }

  private static Delivery forSession(String sessionId) {
    return delivery(Map.of("queueId", LongStringHelper.asLongString(sessionId)));
  }

  @Test
  void construction_subscribesNothing_untilStarted() throws Exception {
    Connection idle = mock(Connection.class);

    new UserEventListener(idle, sessions, migrator, healthMonitor);

    verify(idle, never()).createChannel();
  }

  @Test
  void start_bindsAServerNamedExclusiveQueueToEveryUserEvent() throws Exception {
    verify(channel).exchangeDeclare("wsc.events", BuiltinExchangeType.FANOUT, true);
    verify(channel).queueDeclare("", false, true, true, Map.of("x-max-length", 10_000));
    verify(channel).queueBind("amq.gen-1", "wsc.events", "");
  }

  @Test
  void delivery_reachesEveryLocalSessionOfTheListedUserOnly() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");
    Session a2 = openSession("a2", USER_A, "1.6.2");
    Session b1 = openSession("b1", USER_B, "1.6.2");

    consumerCallback().handle("tag", forUsers(USER_A));

    verify(a1.getAsyncRemote()).sendText(EVENT);
    verify(a2.getAsyncRemote()).sendText(EVENT);
    verify(b1.getAsyncRemote(), never()).sendText(anyString());
  }

  @Test
  void delivery_listingSeveralUsers_reachesEveryLocalSessionOfEachOfThem() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");
    Session b1 = openSession("b1", USER_B, "1.6.2");
    Session c1 = openSession("c1", USER_C, "1.6.2");

    consumerCallback().handle("tag", forUsers(USER_A, USER_B));

    verify(a1.getAsyncRemote()).sendText(EVENT);
    verify(b1.getAsyncRemote()).sendText(EVENT);
    verify(c1.getAsyncRemote(), never()).sendText(anyString());
  }

  @Test
  void delivery_triesEverySession_evenWhenSendingFails() throws Exception {
    RemoteEndpoint.Async toA = openSession("a1", USER_A, "1.6.2").getAsyncRemote();
    RemoteEndpoint.Async toB = openSession("b1", USER_B, "1.6.2").getAsyncRemote();
    doThrow(new IllegalStateException("closing")).when(toA).sendText(EVENT);
    doThrow(new IllegalStateException("closing")).when(toB).sendText(EVENT);

    consumerCallback().handle("tag", forUsers(USER_A, USER_B));

    verify(toA).sendText(EVENT);
    verify(toB).sendText(EVENT);
  }

  @Test
  void delivery_withQueueId_reachesOnlyThatSession() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");
    Session a2 = openSession("a2", USER_A, "1.6.2");

    consumerCallback().handle("tag", forSession("a2"));

    verify(a1.getAsyncRemote(), never()).sendText(anyString());
    verify(a2.getAsyncRemote()).sendText(EVENT);
  }

  @Test
  void delivery_withQueueIdOfAnotherInstance_reachesNoLocalSession() throws Exception {
    Session a1 = openSession("a1", USER_A, "1.6.2");

    consumerCallback().handle("tag", forSession("remote"));

    verify(a1.getAsyncRemote(), never()).sendText(anyString());
  }

  @Test
  void delivery_isDowngradedToEachSessionsVersion() throws Exception {
    Session latest = openSession("a1", USER_A, "1.6.2");
    Session legacy = openSession("a2", USER_A, "");
    when(migrator.downgradeIfNeeded(EVENT, "1.6.2")).thenReturn("latest-json");
    when(migrator.downgradeIfNeeded(EVENT, "1.6.0")).thenReturn("legacy-json");

    consumerCallback().handle("tag", forUsers(USER_A));

    verify(latest.getAsyncRemote()).sendText("latest-json");
    verify(legacy.getAsyncRemote()).sendText("legacy-json");
  }

  @Test
  void delivery_isDowngradedOncePerVersion() throws Exception {
    openSession("a1", USER_A, "");
    openSession("a2", USER_A, "");
    openSession("b1", USER_B, "");

    consumerCallback().handle("tag", forUsers(USER_A, USER_B));

    verify(migrator, times(1)).downgradeIfNeeded(EVENT, "1.6.0");
  }

  @Test
  void delivery_withAnUnreadableUserList_isSkippedInsteadOfClosingTheChannel() throws Exception {
    DeliverCallback callback = consumerCallback();

    assertDoesNotThrow(
        () -> callback.handle("tag", delivery(Map.of("users", Arrays.asList((Object) null)))));
  }

  @Test
  void queueCancelledWhileTheConnectionCloses_doesNotSubscribeAgain() throws Exception {
    when(connection.isOpen()).thenReturn(false);

    cancelCallback().handle("tag");

    verify(connection, times(1)).createChannel();
  }

  @Test
  void queueDeletedByTheBroker_subscribesAgainOnAFreshChannel() throws Exception {
    when(connection.isOpen()).thenReturn(true);
    Channel fresh = mock(Channel.class);
    when(connection.createChannel()).thenReturn(fresh);
    when(fresh.queueDeclare(eq(""), eq(false), eq(true), eq(true), any()))
        .thenReturn(new AMQP.Queue.DeclareOk.Builder().queue("amq.gen-2").build());

    cancelCallback().handle("tag");

    verify(channel).close();
    verify(fresh).queueBind("amq.gen-2", "wsc.events", "");
    verify(fresh)
        .basicConsume(
            eq("amq.gen-2"), eq(true), any(DeliverCallback.class), any(CancelCallback.class));
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
