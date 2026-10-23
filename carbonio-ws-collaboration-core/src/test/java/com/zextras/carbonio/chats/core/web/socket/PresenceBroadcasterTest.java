// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.web.socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class PresenceBroadcasterTest {

  private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
  private final EventDispatcher eventDispatcher = mock(EventDispatcher.class);
  private final ObjectMapper objectMapper = new JacksonConfig().get();
  private final PresenceBroadcaster broadcaster =
      new PresenceBroadcaster(chatMessageService, eventDispatcher, objectMapper);

  @Test
  void broadcastPresence_sendsTransitionWithLastActivityToSubscribersButNotTheUser()
      throws Exception {
    OffsetDateTime lastActivity = OffsetDateTime.parse("2026-10-13T10:15:30Z");
    when(chatMessageService.getPresenceSubscribers("u1")).thenReturn(List.of("u1", "s1"));

    broadcaster.broadcastPresence("u1", false, lastActivity);

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(eventDispatcher).sendRawToUserExchange(eq("s1"), json.capture());
    verify(eventDispatcher, never()).sendRawToUserExchange(eq("u1"), anyString());
    JsonNode event = objectMapper.readTree(json.getValue());
    assertEquals("PresenceChanged", event.get("type").asText());
    assertEquals("u1", event.get("userId").asText());
    assertFalse(event.get("online").asBoolean());
    assertTrue(lastActivity.isEqual(OffsetDateTime.parse(event.get("lastActivity").asText())));
  }

  @Test
  void broadcastPresence_onlineWithLastActivity_sendsBoth() throws Exception {
    OffsetDateTime lastActivity = OffsetDateTime.parse("2026-10-12T18:00:00Z");
    when(chatMessageService.getPresenceSubscribers("u1")).thenReturn(List.of("s1"));

    broadcaster.broadcastPresence("u1", true, lastActivity);

    ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
    verify(eventDispatcher).sendRawToUserExchange(eq("s1"), json.capture());
    JsonNode event = objectMapper.readTree(json.getValue());
    assertTrue(event.get("online").asBoolean());
    assertTrue(lastActivity.isEqual(OffsetDateTime.parse(event.get("lastActivity").asText())));
  }
}
