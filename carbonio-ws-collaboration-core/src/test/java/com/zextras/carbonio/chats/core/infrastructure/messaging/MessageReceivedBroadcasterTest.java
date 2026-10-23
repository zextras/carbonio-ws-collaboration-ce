// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.async.model.MessagePinned;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.JacksonConfig;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MessageReceivedBroadcasterTest {

  private static final String ROOM = "room";
  private static final OffsetDateTime T = OffsetDateTime.parse("2026-01-01T10:00:00Z");

  private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
  private final RoomUserSettingsRepository settingsRepository =
      mock(RoomUserSettingsRepository.class);
  private final EventDispatcher eventDispatcher = mock(EventDispatcher.class);
  private final MessageReceivedBroadcaster broadcaster =
      new MessageReceivedBroadcaster(
          subscriptionRepository, settingsRepository, eventDispatcher, new JacksonConfig().get());

  private TimelineMessageDto messageReplyingTo(OffsetDateTime originalAt) {
    return TimelineMessageDto.create()
        .id("m")
        .roomId(ROOM)
        .senderId("a")
        .text("t")
        .createdAt(T.plusDays(1))
        .replyToId("orig")
        .repliedMessage(
            TimelineMessageDto.create()
                .id("orig")
                .roomId(ROOM)
                .senderId("a")
                .text("secret")
                .createdAt(originalAt));
  }

  @Test
  void splitsRecipientsInTwoPayloadsWithoutMutatingTheMessage() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("early", "cleared"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of("cleared"));
    TimelineMessageDto message = messageReplyingTo(T);

    broadcaster.broadcast(ROOM, message, "tmp");

    ArgumentCaptor<List<String>> users = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
    verify(eventDispatcher, times(2)).sendRawToUserExchange(users.capture(), payloads.capture());
    int withIdx = users.getAllValues().get(0).contains("early") ? 0 : 1;
    assertEquals(List.of("early"), users.getAllValues().get(withIdx));
    assertEquals(List.of("cleared"), users.getAllValues().get(1 - withIdx));
    assertTrue(payloads.getAllValues().get(withIdx).contains("secret"));
    String without = payloads.getAllValues().get(1 - withIdx);
    assertFalse(without.contains("secret"));
    assertFalse(without.contains("replyToId"));
    assertTrue(without.contains("\"tempId\":\"tmp\""));
    assertNotNull(message.getRepliedMessage());
  }

  @Test
  void genericEventKeepsTheTypeAndSplitsTheReplyReference() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("early", "cleared"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of("cleared"));
    TimelineMessageDto message = messageReplyingTo(T);

    broadcaster.broadcastWithReplyVisibility(
        ROOM,
        MessagePinned.create()
            .messageId("m")
            .roomId(ROOM)
            .pinnedBy("a")
            .timestamp(T)
            .message(message)
            .sentDate(T),
        message);

    ArgumentCaptor<List<String>> users = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<String> payloads = ArgumentCaptor.forClass(String.class);
    verify(eventDispatcher, times(2)).sendRawToUserExchange(users.capture(), payloads.capture());
    int withIdx = users.getAllValues().get(0).contains("early") ? 0 : 1;
    assertTrue(payloads.getAllValues().get(withIdx).contains("secret"));
    String without = payloads.getAllValues().get(1 - withIdx);
    assertFalse(without.contains("secret"));
    assertFalse(without.contains("replyToId"));
    assertTrue(without.contains("\"type\":\"MessagePinned\""));
  }

  @Test
  void everyoneAllowedSendsOnePayload() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("a", "b"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of());

    broadcaster.broadcast(ROOM, messageReplyingTo(T), null);

    verify(eventDispatcher, times(1)).sendRawToUserExchange(eq(List.of("a", "b")), anyString());
  }

  @Test
  void messageWithoutReplySendsSinglePayloadWithoutTouchingTheDb() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("a", "b"));

    broadcaster.broadcast(
        ROOM,
        TimelineMessageDto.create().id("m").roomId(ROOM).senderId("a").text("t").createdAt(T),
        null);

    verify(eventDispatcher, times(1)).sendRawToUserExchange(eq(List.of("a", "b")), anyString());
    verify(settingsRepository, never()).getUserIdsClearedAtOrAfter(anyString(), any());
  }

  @Test
  void editEventGoesOnlyToMembersWhoCanAccessTheMessage() {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM))
        .thenReturn(List.of("early", "cleared", "other"));
    when(settingsRepository.getUserIdsClearedAtOrAfter(ROOM, T)).thenReturn(List.of("cleared"));

    broadcaster.broadcastToMembersWhoCanAccess(ROOM, T, "{}");

    verify(eventDispatcher, times(1))
        .sendRawToUserExchange(eq(List.of("early", "other")), eq("{}"));
  }

  @Test
  void quoteWithoutCreationDateIsStrippedForEveryone() throws Exception {
    when(subscriptionRepository.getUserIdsByRoomId(ROOM)).thenReturn(List.of("a", "b"));
    TimelineMessageDto message = messageReplyingTo(T);
    message.getRepliedMessage().createdAt(null);

    broadcaster.broadcast(ROOM, message, null);

    ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
    verify(eventDispatcher, times(1))
        .sendRawToUserExchange(eq(List.of("a", "b")), payload.capture());
    assertFalse(payload.getValue().contains("secret"));
  }
}
