// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.event.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.RoomCreated;
import com.zextras.carbonio.async.model.Typing;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@UnitTest
class EventDispatcherRabbitMqTest {

  private final ObjectMapper objectMapper = spy(new JacksonConfig().get());
  private final Channel channel = mock(Channel.class);
  private final EventDispatcherRabbitMq dispatcher =
      new EventDispatcherRabbitMq(channel, objectMapper);

  @Test
  void wsEventForSeveralUsersIsSerializedOnce() throws Exception {
    when(channel.isOpen()).thenReturn(true);

    dispatcher.sendWsEvent(
        List.of("u1", "u2", "u3"), Typing.create().roomId("r").userId("u0").status(null));

    verify(objectMapper, times(1)).writeValueAsString(any());
    verify(channel, times(3)).basicPublish(anyString(), anyString(), any(), any(byte[].class));
  }

  @Test
  void domainEventForSeveralUsersIsSerializedOnce() throws Exception {
    when(channel.isOpen()).thenReturn(true);

    dispatcher.sendToUserExchange(
        List.of("u1", "u2"),
        RoomCreated.create().roomId(UUID.randomUUID()).type(EventType.ROOM_CREATED));

    verify(objectMapper, times(1)).writeValueAsString(any());
    verify(channel, times(2)).basicPublish(anyString(), anyString(), any(), any(byte[].class));
  }

  @Test
  void aWsEventThatCannotBeSerializedPublishesNothing() throws Exception {
    when(channel.isOpen()).thenReturn(true);
    org.mockito.Mockito.doThrow(new JsonProcessingException("boom") {})
        .when(objectMapper)
        .writeValueAsString(any());

    dispatcher.sendWsEvent(List.of("u1", "u2"), Typing.create().roomId("r").userId("u0"));

    verify(channel, never()).basicPublish(anyString(), anyString(), any(), any(byte[].class));
  }

  @Test
  void aClosedChannelPublishesNothing() throws Exception {
    when(channel.isOpen()).thenReturn(false);

    dispatcher.sendWsEvent("u1", Typing.create().roomId("r").userId("u0"));

    verify(channel, never()).basicPublish(anyString(), anyString(), any(), any(byte[].class));
  }
}
