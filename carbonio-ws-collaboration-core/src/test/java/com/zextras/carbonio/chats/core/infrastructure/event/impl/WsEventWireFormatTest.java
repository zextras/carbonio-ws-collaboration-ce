// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.RoomCreated;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.presence.PresenceBroadcaster;
import com.zextras.carbonio.chats.core.service.presence.PresenceService;
import com.zextras.carbonio.chats.core.web.socket.action.impl.TypingHandler;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class WsEventWireFormatTest {

  private static final String ROOM = "room";
  private static final OffsetDateTime T = OffsetDateTime.parse("2026-01-01T10:00:00Z");

  private final ObjectMapper objectMapper = new JacksonConfig().get();
  private final Channel channel = mock(Channel.class);
  private final List<String> publishedPerRecipient = new ArrayList<>();
  private final EventDispatcherRabbitMq dispatcher;
  private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
  private final RoomUserSettingsRepository settingsRepository =
      mock(RoomUserSettingsRepository.class);
  private final MessageReceivedBroadcaster broadcaster;

  WsEventWireFormatTest() throws Exception {
    Connection connection = mock(Connection.class);
    when(connection.createChannel()).thenReturn(channel);
    when(channel.isOpen()).thenReturn(true);
    org.mockito.Mockito.doAnswer(
            invocation -> {
              BasicProperties properties = invocation.getArgument(2);
              String body = new String(invocation.<byte[]>getArgument(3), StandardCharsets.UTF_8);
              ((List<?>) properties.getHeaders().get("users"))
                  .forEach(user -> publishedPerRecipient.add(user + " <- " + body));
              return null;
            })
        .when(channel)
        .basicPublish(anyString(), anyString(), any(), any(byte[].class));
    dispatcher = new EventDispatcherRabbitMq(connection, objectMapper);
    broadcaster =
        new MessageReceivedBroadcaster(subscriptionRepository, settingsRepository, dispatcher);
  }

  private List<String> published() {
    dispatcher.stop();
    return publishedPerRecipient;
  }

  private static TimelineMessageDto message(String id) {
    return TimelineMessageDto.create()
        .id(id)
        .roomId(ROOM)
        .senderId("a")
        .text("t")
        .createdAt(T.plusDays(1));
  }

  private static TimelineMessageDto reply(String id, OffsetDateTime originalAt) {
    return message(id)
        .replyToId("orig")
        .repliedMessage(message("orig").text("quoted").createdAt(originalAt));
  }

  @Test
  void messageReceived_plainWithTempId() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("u1", "u2"));

    broadcaster.broadcast(ROOM, message("m"), "tmp");

    assertEquals(
        List.of(
            "u1 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]},\"tempId\":\"tmp\"}",
            "u2 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]},\"tempId\":\"tmp\"}"),
        published());
  }

  @Test
  void messageReceived_plainWithoutTempId() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("u1"));

    broadcaster.broadcast(ROOM, message("m"), null);

    assertEquals(
        List.of(
            "u1 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]}}"),
        published());
  }

  @Test
  void messageReceived_replyQuotedForEveryone() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("u1", "u2"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of());

    broadcaster.broadcast(ROOM, reply("m", T), "tmp");

    assertEquals(
        List.of(
            "u1 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"replyToId\":\"orig\",\"reactions\":[],\"repliedMessage\":{\"id\":\"orig\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"quoted\",\"createdAt\":\"2026-01-01T10:00:00Z\",\"reactions\":[]}},\"tempId\":\"tmp\"}",
            "u2 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"replyToId\":\"orig\",\"reactions\":[],\"repliedMessage\":{\"id\":\"orig\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"quoted\",\"createdAt\":\"2026-01-01T10:00:00Z\",\"reactions\":[]}},\"tempId\":\"tmp\"}"),
        published());
  }

  @Test
  void messageReceived_replySplitBetweenQuotedAndUnquotedRecipients() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("early", "cleared"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of("cleared"));

    broadcaster.broadcast(ROOM, reply("m", T), "tmp");

    assertEquals(
        List.of(
            "early <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"replyToId\":\"orig\",\"reactions\":[],\"repliedMessage\":{\"id\":\"orig\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"quoted\",\"createdAt\":\"2026-01-01T10:00:00Z\",\"reactions\":[]}},\"tempId\":\"tmp\"}",
            "cleared <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]},\"tempId\":\"tmp\"}"),
        published());
  }

  @Test
  void messageReceived_replyWithOriginalWithoutTimestampLosesTheQuoteForEveryone()
      throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("u1", "u2"));

    broadcaster.broadcast(ROOM, reply("m", null), null);

    assertEquals(
        List.of(
            "u1 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]}}",
            "u2 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]}}"),
        published());
  }

  @Test
  void messageReceived_replyIdWithoutQuotedMessageLosesTheReplyId() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("u1"));

    broadcaster.broadcast(ROOM, message("m").replyToId("orig"), null);

    assertEquals(
        List.of(
            "u1 <-"
                + " {\"type\":\"MessageReceived\",\"message\":{\"id\":\"m\",\"roomId\":\"room\",\"senderId\":\"a\",\"text\":\"t\",\"createdAt\":\"2026-01-02T10:00:00Z\",\"reactions\":[]}}"),
        published());
  }

  @Test
  void presenceChanged_offlineWithLastActivityAndOnlineWithout() {
    PresenceService presenceService = mock(PresenceService.class);
    when(presenceService.getPresenceSubscribers("u1")).thenReturn(List.of("u1", "s1", "s2"));
    PresenceBroadcaster presenceBroadcaster = new PresenceBroadcaster(presenceService, dispatcher);

    presenceBroadcaster.broadcastPresence(
        "u1", false, OffsetDateTime.parse("2026-10-13T10:15:30Z"));
    presenceBroadcaster.broadcastPresence("u1", true, null);

    assertEquals(
        List.of(
            "s1 <-"
                + " {\"type\":\"PresenceChanged\",\"userId\":\"u1\",\"online\":false,\"lastActivity\":\"2026-10-13T10:15:30Z\"}",
            "s2 <-"
                + " {\"type\":\"PresenceChanged\",\"userId\":\"u1\",\"online\":false,\"lastActivity\":\"2026-10-13T10:15:30Z\"}",
            "s1 <- {\"type\":\"PresenceChanged\",\"userId\":\"u1\",\"online\":true}",
            "s2 <- {\"type\":\"PresenceChanged\",\"userId\":\"u1\",\"online\":true}"),
        published());
  }

  @Test
  void typing_startedAndStoppedAreRelayedToTheOtherMembers() {
    RoomAccess roomAccess = mock(RoomAccess.class);
    when(roomAccess.getMemberIdsIfMember(ROOM, "u1")).thenReturn(List.of("u1", "u2", "u3"));
    TypingHandler typingHandler = new TypingHandler(dispatcher, roomAccess, objectMapper);

    typingHandler.handle("u1", payload("started"), null);
    typingHandler.handle("u1", payload("stopped"), null);
    typingHandler.handle("u1", payload("anything"), null);

    assertEquals(
        List.of(
            "u2 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"started\"}",
            "u3 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"started\"}",
            "u2 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"stopped\"}",
            "u3 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"stopped\"}",
            "u2 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"started\"}",
            "u3 <-"
                + " {\"type\":\"Typing\",\"roomId\":\"room\",\"userId\":\"u1\",\"status\":\"started\"}"),
        published());
  }

  @Test
  void domainEvent_fanOutToSeveralUsersAndToOne() {
    UUID roomId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    DomainEvent event = RoomCreated.create().roomId(roomId).type(EventType.ROOM_CREATED);

    dispatcher.sendToUserExchange(List.of("u1", "u2"), event);
    dispatcher.sendToUserExchange("u3", event);

    String json = "{\"type\":\"RoomCreated\",\"roomId\":\"00000000-0000-0000-0000-0000000000a1\"}";
    assertEquals(List.of("u1 <- " + json, "u2 <- " + json, "u3 <- " + json), published());
  }

  private JsonNode payload(String status) {
    return objectMapper.valueToTree(
        java.util.Map.of("action", "Typing", "roomId", ROOM, "status", status));
  }
}
