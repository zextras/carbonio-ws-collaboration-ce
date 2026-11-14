// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
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
    connection = mock(Connection.class);
    channel = mock(Channel.class);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    when(connection.isOpen()).thenReturn(true);
    dispatcher = new EventDispatcherRabbitMq(connection, objectMapper);
  }

  @AfterEach
  void tearDown() {
    dispatcher.stop();
  }

  private List<BasicProperties> published(int times) throws IOException {
    ArgumentCaptor<BasicProperties> props = ArgumentCaptor.forClass(BasicProperties.class);
    verify(channel, timeout(1000).times(times))
        .basicPublish(eq("wsc.events"), eq(""), props.capture(), any());
    return props.getAllValues();
  }

  @Test
  void sendToUserExchange_publishesOneMessageListingTheUser() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);

    ArgumentCaptor<byte[]> body = ArgumentCaptor.forClass(byte[].class);
    verify(channel, timeout(1000)).basicPublish(eq("wsc.events"), eq(""), any(), body.capture());
    assertEquals(Map.of("users", List.of(USER_A)), published(1).getFirst().getHeaders());
    assertArrayEquals(objectMapper.writeValueAsBytes(EVENT), body.getValue());
  }

  @Test
  void sendToUserExchange_withManyUsers_publishesOneMessageListingThem() throws Exception {
    dispatcher.sendToUserExchange(List.of(USER_A, USER_B), EVENT);

    assertEquals(List.of(USER_A, USER_B), published(1).getFirst().getHeaders().get("users"));
  }

  @Test
  void sendToUserExchange_withMoreUsersThanOneMessageHolds_splitsThemAcrossMessages()
      throws Exception {
    List<String> users =
        IntStream.range(0, 2_500).mapToObj(i -> UUID.randomUUID().toString()).toList();

    dispatcher.sendToUserExchange(users, EVENT);

    assertEquals(
        users,
        published(3).stream()
            .flatMap(p -> ((List<?>) p.getHeaders().get("users")).stream())
            .toList());
  }

  @Test
  void sendToUserExchange_withNoUsers_publishesNothing() throws Exception {
    dispatcher.sendToUserExchange(List.of(), EVENT);
    dispatcher.stop();

    verify(channel, never()).basicPublish(anyString(), anyString(), any(), any());
  }

  @Test
  void sendToUserQueue_publishesOneMessageNamingTheSession() throws Exception {
    dispatcher.sendToUserQueue(USER_A, "session-1", EVENT);

    assertEquals(Map.of("queueId", "session-1"), published(1).getFirst().getHeaders());
  }

  @Test
  void publishing_declaresNeitherTheExchangeNorAQueue() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);
    dispatcher.sendToUserQueue(USER_A, "session-1", EVENT);

    published(2);
    verify(channel, never())
        .exchangeDeclare(anyString(), any(BuiltinExchangeType.class), anyBoolean());
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

    verify(reopened, timeout(1000)).basicPublish(eq("wsc.events"), eq(""), any(), any());
  }

  @Test
  void aFullQueue_makesCallersWaitBrieflyForRoom_insteadOfDropping() throws Exception {
    CountDownLatch slowBroker = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              slowBroker.await();
              return null;
            })
        .when(channel)
        .basicPublish(anyString(), anyString(), any(), any());
    int publishingAndQueued = 1 + 10_000;
    for (int i = 0; i < publishingAndQueued; i++) dispatcher.sendToUserExchange(USER_A, EVENT);

    Thread late = Thread.ofPlatform().start(() -> dispatcher.sendToUserExchange(USER_B, EVENT));
    while (late.isAlive() && late.getState() != Thread.State.TIMED_WAITING) Thread.onSpinWait();
    slowBroker.countDown();

    verify(channel, timeout(5000))
        .basicPublish(
            eq("wsc.events"),
            eq(""),
            argThat(props -> List.of(USER_B).equals(props.getHeaders().get("users"))),
            any());
  }

  @Test
  void callersStopWaiting_onceTheBrokerStopsTakingEvents() throws Exception {
    CountDownLatch stalledBroker = new CountDownLatch(1);
    doAnswer(
            invocation -> {
              stalledBroker.await();
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
      stalledBroker.countDown();
    }
  }

  @Test
  void aFailedPublish_doesNotStopLaterPublishes() throws Exception {
    doThrow(new IOException("broker gone"))
        .doNothing()
        .when(channel)
        .basicPublish(anyString(), anyString(), any(), any());

    dispatcher.sendToUserExchange(USER_A, EVENT);
    dispatcher.sendToUserExchange(USER_B, EVENT);

    published(2);
  }

  @Test
  void stop_publishesPendingEventsThenClosesTheConnection() throws Exception {
    dispatcher.sendToUserExchange(USER_A, EVENT);

    dispatcher.stop();

    InOrder order = inOrder(channel, connection);
    order.verify(channel).basicPublish(eq("wsc.events"), eq(""), any(), any());
    order.verify(connection).close();
  }

  @Test
  void isAlive_followsThePublisherChannel() {
    assertTrue(dispatcher.isAlive());

    when(channel.isOpen()).thenReturn(false);

    assertFalse(dispatcher.isAlive());
  }
}
