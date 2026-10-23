// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import com.zextras.carbonio.chats.api.RoomsApi;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.web.socket.action.impl.TypingHandler;
import com.zextras.carbonio.chats.it.annotations.ApiIntegrationTest;
import com.zextras.carbonio.chats.it.tools.ResteasyRequestDispatcher;
import com.zextras.carbonio.chats.it.utils.IntegrationTestUtils;
import com.zextras.carbonio.chats.it.utils.MockedAccount;
import com.zextras.carbonio.chats.it.utils.MockedAccount.MockedAccountType;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.websocket.RemoteEndpoint;
import jakarta.websocket.Session;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Membership changes applied by another instance (written straight to the DB, with no local
 * notification) must be honoured by the very next fan-out.
 */
@ApiIntegrationTest
class RoomMemberFanOutIT {

  private final ResteasyRequestDispatcher dispatcher;
  private final ObjectMapper objectMapper;
  private final IntegrationTestUtils integrationTestUtils;
  private final SubscriptionRepository subscriptionRepository;
  private final TypingHandler typingHandler;
  private final Channel channel;

  private final UUID sender = MockedAccount.getAccount(MockedAccountType.SNOOPY).getUUID();
  private final String senderToken = MockedAccount.getAccount(MockedAccountType.SNOOPY).getToken();
  private final UUID stayer = MockedAccount.getAccount(MockedAccountType.CHARLIE_BROWN).getUUID();
  private final UUID mover = MockedAccount.getAccount(MockedAccountType.LUCY_VAN_PELT).getUUID();

  private Room room;

  public RoomMemberFanOutIT(
      RoomsApi roomsApi,
      ResteasyRequestDispatcher dispatcher,
      ObjectMapper objectMapper,
      IntegrationTestUtils integrationTestUtils,
      SubscriptionRepository subscriptionRepository,
      TypingHandler typingHandler,
      Channel channel) {
    this.dispatcher = dispatcher;
    this.objectMapper = objectMapper;
    this.integrationTestUtils = integrationTestUtils;
    this.subscriptionRepository = subscriptionRepository;
    this.typingHandler = typingHandler;
    this.channel = channel;
    this.dispatcher.getRegistry().addSingletonResource(roomsApi);
  }

  @BeforeEach
  void setUp() {
    room =
        integrationTestUtils.generateAndSaveRoom(
            UUID.randomUUID(), RoomTypeDto.GROUP, "fan-out", List.of(sender, stayer, mover));
  }

  @Test
  @DisplayName("A member removed by another instance does not receive the next message")
  void sendMessage_memberRemovedElsewhere_notDelivered() throws Exception {
    String stayerQueue = bindQueue(stayer);
    String moverQueue = bindQueue(mover);
    send("first");
    nextEvent(stayerQueue);
    nextEvent(moverQueue);

    subscriptionRepository.delete(room.getId(), mover.toString());
    send("second");

    assertEquals("second", nextEvent(stayerQueue).get("message").get("text").asText());
    assertNoEvent(moverQueue);
  }

  @Test
  @DisplayName("A member added by another instance receives the next message")
  void sendMessage_memberAddedElsewhere_delivered() throws Exception {
    subscriptionRepository.delete(room.getId(), mover.toString());
    String moverQueue = bindQueue(mover);
    send("first");
    assertNoEvent(moverQueue);

    subscriptionRepository.insert(
        Subscription.create(room, mover.toString())
            .joinedAt(OffsetDateTime.now())
            .owner(false)
            .temporary(false)
            .external(false));
    send("second");

    assertEquals("second", nextEvent(moverQueue).get("message").get("text").asText());
  }

  @Test
  @DisplayName("A member removed by another instance can no longer relay typing")
  void typing_memberRemovedElsewhere_rejected() throws Exception {
    String stayerQueue = bindQueue(stayer);
    typingHandler.handle(mover.toString(), typing(), openSession());
    nextEvent(stayerQueue);

    subscriptionRepository.delete(room.getId(), mover.toString());
    Session session = openSession();
    typingHandler.handle(mover.toString(), typing(), session);

    assertNoEvent(stayerQueue);
    verify(session.getAsyncRemote())
        .sendObject(argThat(json -> json.toString().contains("ERR_FORBIDDEN")));
  }

  private JsonNode typing() {
    return objectMapper.valueToTree(Map.of("action", "Typing", "roomId", room.getId()));
  }

  private Session openSession() {
    Session session = mock(Session.class);
    when(session.isOpen()).thenReturn(true);
    when(session.getAsyncRemote()).thenReturn(mock(RemoteEndpoint.Async.class));
    return session;
  }

  private void send(String text) throws Exception {
    assertEquals(
        201,
        dispatcher
            .post(
                String.format("/rooms/%s/messages", room.getId()),
                objectMapper.writeValueAsString(Map.of("text", text)),
                senderToken)
            .getStatus());
  }

  private String bindQueue(UUID user) throws Exception {
    channel.exchangeDeclare(user.toString(), BuiltinExchangeType.DIRECT, false, false, null);
    String queue = channel.queueDeclare().getQueue();
    channel.queueBind(queue, user.toString(), "user-events");
    return queue;
  }

  private JsonNode nextEvent(String queue) throws Exception {
    for (int i = 0; i < 50; i++) {
      GetResponse response = channel.basicGet(queue, true);
      if (response != null) {
        return objectMapper.readTree(new String(response.getBody(), StandardCharsets.UTF_8));
      }
      Thread.sleep(100);
    }
    throw new AssertionError("no WS event received on " + queue);
  }

  private void assertNoEvent(String queue) throws Exception {
    Thread.sleep(500);
    assertNull(channel.basicGet(queue, true));
  }
}
