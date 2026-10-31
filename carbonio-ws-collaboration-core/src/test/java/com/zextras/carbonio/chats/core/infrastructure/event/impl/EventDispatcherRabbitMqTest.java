// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@UnitTest
class EventDispatcherRabbitMqTest {

  private static final String USER_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String USER_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final DomainEvent EVENT = DomainEvent.create().type(EventType.PONG);

  private final ObjectMapper objectMapper = new ObjectMapper();
  private Connection connection;
  private Channel channel;
  private EventDispatcherRabbitMq dispatcher;

  @BeforeEach
  void setUp() throws Exception {
    ConnectionFactory factory = mock(ConnectionFactory.class);
    connection = mock(Connection.class);
    channel = mock(Channel.class);
    when(factory.newConnection(anyString())).thenReturn(connection);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    when(connection.isOpen()).thenReturn(true);
    dispatcher = new EventDispatcherRabbitMq(factory, objectMapper);
  }

  @AfterEach
  void tearDown() {
    dispatcher.stop();
  }

  private byte[] json() throws Exception {
    return objectMapper.writeValueAsString(EVENT).getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void sendToUserExchange_publishesToTheEventsExchangeRoutedByUser() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);

    ArgumentCaptor<BasicProperties> props = ArgumentCaptor.forClass(BasicProperties.class);
    ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
    verify(channel, timeout(1000))
        .basicPublish(eq("wsc.events"), eq(USER_A), props.capture(), body.capture());
    assertNull(props.getValue().getHeaders());
    assertEquals(new String(json(), StandardCharsets.UTF_8), new String(body.getValue()));
  }

  @Test
  void sendToUserExchange_withManyUsers_publishesOncePerUserInOrder() throws Exception {
    dispatcher.sendToUserExchange(List.of(USER_A, USER_B), EVENT);

    InOrder order = inOrder(channel);
    order.verify(channel, timeout(1000)).basicPublish(eq("wsc.events"), eq(USER_A), any(), any());
    order.verify(channel, timeout(1000)).basicPublish(eq("wsc.events"), eq(USER_B), any(), any());
  }

  @Test
  void sendToUserQueue_addsTheTargetSessionAsHeader() throws Exception {
    dispatcher.sendToUserQueue(USER_A, "session-1", EVENT);

    ArgumentCaptor<BasicProperties> props = ArgumentCaptor.forClass(BasicProperties.class);
    verify(channel, timeout(1000))
        .basicPublish(eq("wsc.events"), eq(USER_A), props.capture(), any());
    assertEquals("session-1", props.getValue().getHeaders().get("queueId"));
  }

  @Test
  void publishing_declaresTheExchangeOnceAndNeverAQueue() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);
    dispatcher.sendToUserQueue(USER_A, "session-1", EVENT);

    verify(channel, timeout(1000).times(2)).basicPublish(anyString(), anyString(), any(), any());
    verify(channel).exchangeDeclare("wsc.events", BuiltinExchangeType.TOPIC, true);
    verify(channel, never())
        .queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any());
  }

  @Test
  void aChannelClosedByTheBroker_isReopenedForTheNextPublish() throws Exception {
    Channel reopened = mock(Channel.class);
    when(reopened.isOpen()).thenReturn(true);
    when(connection.createChannel()).thenReturn(reopened);
    when(channel.isOpen()).thenReturn(false);

    dispatcher.sendToUserExchange(USER_A, EVENT);

    verify(reopened, timeout(1000)).basicPublish(eq("wsc.events"), eq(USER_A), any(), any());
    verify(reopened).exchangeDeclare("wsc.events", BuiltinExchangeType.TOPIC, true);
  }

  @Test
  void callersNeverBlock_whileTheBrokerBlocksPublishing() throws Exception {
    CountDownLatch brokerBlocked = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              brokerBlocked.await();
              return null;
            })
        .when(channel)
        .basicPublish(anyString(), anyString(), any(), any());

    try {
      assertTimeoutPreemptively(
          Duration.ofSeconds(5),
          () -> {
            for (int i = 0; i < 50_000; i++) dispatcher.sendToUserExchange(USER_A, EVENT);
          });
    } finally {
      brokerBlocked.countDown();
    }
  }

  @Test
  void aFailedPublish_doesNotStopLaterPublishes() throws Exception {
    doThrow(new IOException("broker gone"))
        .when(channel)
        .basicPublish(eq("wsc.events"), eq(USER_A), any(), any());

    dispatcher.sendToUserExchange(USER_A, EVENT);
    dispatcher.sendToUserExchange(USER_B, EVENT);

    verify(channel, timeout(1000)).basicPublish(eq("wsc.events"), eq(USER_B), any(), any());
  }

  @Test
  void stop_publishesPendingEventsThenClosesTheConnection() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);

    dispatcher.stop();

    InOrder order = inOrder(channel, connection);
    order.verify(channel).basicPublish(eq("wsc.events"), eq(USER_A), any(), any());
    order.verify(connection).close();
  }

  @Test
  void isAlive_followsThePublisherChannel() {
    assertTrue(dispatcher.isAlive());

    when(channel.isOpen()).thenReturn(false);

    assertFalse(dispatcher.isAlive());
  }
}
