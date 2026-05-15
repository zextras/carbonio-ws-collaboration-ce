// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.CancelCallback;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Recoverable;
import com.rabbitmq.client.RecoveryListener;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.exception.EventDispatcherException;
import com.zextras.carbonio.chats.core.web.socket.MessageBrokerHealthMonitor;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class RabbitConnectionPoolServiceTest {

  private static final String USER_ROUTING_KEY = "user-events";

  private final ConnectionFactory factory;
  private final Connection connection;
  private final Channel channel;
  private final MessageBrokerHealthMonitor monitor;

  public RabbitConnectionPoolServiceTest() {
    this.factory = mock(ConnectionFactory.class);
    this.connection = mock(Connection.class, withSettings().extraInterfaces(Recoverable.class));
    this.channel = mock(Channel.class);
    this.monitor = mock(MessageBrokerHealthMonitor.class);
  }

  @BeforeEach
  void setUp() throws Exception {
    when(factory.newConnection()).thenReturn(connection);
    when(connection.isOpen()).thenReturn(true);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    when(channel.basicConsume(
            anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class)))
        .thenReturn("tag-1");
  }

  @AfterEach
  void afterEach() {
    reset(factory, connection, channel, monitor);
  }

  // ---- Constructor ----

  @Test
  void constructor_createsCorrectPoolSize() throws Exception {
    new RabbitConnectionPoolService(factory, 3, monitor);
    verify(factory, times(3)).newConnection();
  }

  @Test
  void constructor_throwsEventDispatcherException_onIOException() throws Exception {
    when(factory.newConnection()).thenThrow(new IOException("refused"));
    assertThrows(
        EventDispatcherException.class, () -> new RabbitConnectionPoolService(factory, 1, monitor));
  }

  @Test
  void constructor_throwsEventDispatcherException_onTimeoutException() throws Exception {
    when(factory.newConnection()).thenThrow(new TimeoutException("timeout"));
    assertThrows(
        EventDispatcherException.class, () -> new RabbitConnectionPoolService(factory, 1, monitor));
  }

  // ---- setupSession ----

  @Test
  void setupSession_declaresExchangeQueueAndBinding() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);
    DeliverCallback cb = mock(DeliverCallback.class);

    pool.setupSession("user-1", "session-1", cb, msg -> {});

    verify(channel).exchangeDeclare("user-1", BuiltinExchangeType.DIRECT, false, false, null);
    verify(channel).queueDeclare("session-1", false, false, true, null);
    verify(channel).queueBind("session-1", "user-1", USER_ROUTING_KEY);
    verify(channel).basicConsume(eq("session-1"), eq(true), eq(cb), any(CancelCallback.class));
  }

  // ---- teardownSession ----

  @Test
  void teardownSession_cancelsConsumerUnbindsAndDeletesQueue() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);
    pool.setupSession("user-1", "session-1", mock(DeliverCallback.class), msg -> {});

    pool.teardownSession("session-1");

    verify(channel).basicCancel("tag-1");
    verify(channel).queueUnbind("session-1", "user-1", USER_ROUTING_KEY);
    verify(channel).queueDeleteNoWait("session-1", false, false);
    verify(channel).close();
  }

  @Test
  void teardownSession_isNoOp_whenSessionNotFound() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);

    assertDoesNotThrow(() -> pool.teardownSession("unknown-session"));
  }

  @Test
  void teardownSession_handlesChannelCloseException_gracefully() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);
    pool.setupSession("user-1", "session-1", mock(DeliverCallback.class), msg -> {});
    doThrow(new IOException("close failed")).when(channel).close();

    assertDoesNotThrow(() -> pool.teardownSession("session-1"));
  }

  // ---- Shutdown listener delegates to monitor ----

  @Test
  void shutdownListener_notifiesMonitor_onBrokerDown() {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);

    triggerShutdown(false);

    verify(monitor).notifyBrokerDown();
  }

  @Test
  void shutdownListener_doesNotNotifyMonitor_whenInitiatedByApplication() {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);

    triggerShutdown(true);

    verify(monitor, never()).notifyBrokerDown();
  }

  @Test
  void shutdownListener_isIdempotent_perOutage() {
    new RabbitConnectionPoolService(factory, 1, monitor);

    triggerShutdown(false);
    triggerShutdown(false);
    triggerShutdown(false);

    verify(monitor, times(1)).notifyBrokerDown();
  }

  @Test
  void shutdownListener_reArms_afterFullRecovery() {
    new RabbitConnectionPoolService(factory, 1, monitor);

    triggerShutdown(false);
    triggerRecovery();
    triggerShutdown(false);

    verify(monitor, times(2)).notifyBrokerDown();
    verify(monitor, times(1)).notifyBrokerRecovered();
  }

  // ---- Recovery listener delegates to monitor ----

  @Test
  void recoveryListener_rebuildsTopologyForSession() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);
    DeliverCallback cb = mock(DeliverCallback.class);
    pool.setupSession("user-1", "session-1", cb, msg -> {});

    triggerShutdown(false);
    clearInvocations(channel);
    when(channel.basicConsume(
            anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class)))
        .thenReturn("tag-2");

    triggerRecovery();

    verify(channel).exchangeDeclare("user-1", BuiltinExchangeType.DIRECT, false, false, null);
    verify(channel).queueDeclare("session-1", false, false, true, null);
    verify(channel).queueBind("session-1", "user-1", USER_ROUTING_KEY);
    verify(channel).basicConsume(eq("session-1"), eq(true), eq(cb), any(CancelCallback.class));
  }

  @Test
  void recoveryListener_notifiesMonitor_onceAllConnectionsHaveRecovered() throws Exception {
    Connection connection2 =
        mock(Connection.class, withSettings().extraInterfaces(Recoverable.class));
    Channel channel2 = mock(Channel.class);
    when(connection2.isOpen()).thenReturn(true);
    when(connection2.createChannel()).thenReturn(channel2);
    when(channel2.basicConsume(
            anyString(), anyBoolean(), any(DeliverCallback.class), any(CancelCallback.class)))
        .thenReturn("tag-2");
    when(factory.newConnection()).thenReturn(connection).thenReturn(connection2);

    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 2, monitor);

    ArgumentCaptor<ShutdownListener> shutdownCaptor =
        ArgumentCaptor.forClass(ShutdownListener.class);
    verify(connection).addShutdownListener(shutdownCaptor.capture());
    shutdownCaptor
        .getValue()
        .shutdownCompleted(new ShutdownSignalException(true, false, null, connection));

    ArgumentCaptor<RecoveryListener> recoveryCaptor =
        ArgumentCaptor.forClass(RecoveryListener.class);
    verify((Recoverable) connection).addRecoveryListener(recoveryCaptor.capture());
    recoveryCaptor.getValue().handleRecovery((Recoverable) connection);
    // Only one connection has recovered; monitor must not be notified yet.
    verify(monitor, never()).notifyBrokerRecovered();

    ArgumentCaptor<RecoveryListener> recovery2Captor =
        ArgumentCaptor.forClass(RecoveryListener.class);
    verify((Recoverable) connection2).addRecoveryListener(recovery2Captor.capture());
    recovery2Captor.getValue().handleRecovery((Recoverable) connection2);
    verify(monitor, times(1)).notifyBrokerRecovered();
  }

  // ---- stop ----

  @Test
  void stop_closesAllOpenConnections() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);

    pool.stop();

    verify(connection).close();
  }

  @Test
  void stop_handlesCloseException_gracefully() throws Exception {
    RabbitConnectionPoolService pool = new RabbitConnectionPoolService(factory, 1, monitor);
    doThrow(new IOException("close failed")).when(connection).close();

    assertDoesNotThrow(pool::stop);
  }

  // ---- helpers ----

  private void triggerShutdown(boolean initiatedByApplication) {
    ArgumentCaptor<ShutdownListener> captor = ArgumentCaptor.forClass(ShutdownListener.class);
    verify(connection).addShutdownListener(captor.capture());
    captor
        .getValue()
        .shutdownCompleted(
            new ShutdownSignalException(true, initiatedByApplication, null, connection));
  }

  private void triggerRecovery() {
    ArgumentCaptor<RecoveryListener> captor = ArgumentCaptor.forClass(RecoveryListener.class);
    verify((Recoverable) connection).addRecoveryListener(captor.capture());
    captor.getValue().handleRecovery((Recoverable) connection);
  }
}
