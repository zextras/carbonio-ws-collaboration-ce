// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.MessageReaction;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.UserContactRepository;
import com.zextras.carbonio.chats.core.repository.WsPresenceRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class ChatMessageServiceImplTest {

  private final ChatMessageService chatMessageService;
  private final MessageRepository messageRepository;
  private final MessageReactionRepository reactionRepository;
  private final MessagePinRepository pinRepository;
  private final SystemEventRepository systemEventRepository;
  private final ReadMarkerRepository readRepository;
  private final RoomRepository roomRepository;
  private final SubscriptionRepository subscriptionRepository;

  private static final String ROOM_ID = "11111111-1111-1111-1111-111111111111";
  private static final String MESSAGE_ID = "22222222-2222-2222-2222-222222222222";
  private static final String SYSTEM_EVENT_ID = "44444444-4444-4444-4444-444444444444";
  private static final String USER_ID = "33333333-3333-3333-3333-333333333333";

  ChatMessageServiceImplTest() {
    this.messageRepository = mock(MessageRepository.class);
    this.reactionRepository = mock(MessageReactionRepository.class);
    this.pinRepository = mock(MessagePinRepository.class);
    this.systemEventRepository = mock(SystemEventRepository.class);
    this.readRepository = mock(ReadMarkerRepository.class);
    this.roomRepository = mock(RoomRepository.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    when(subscriptionRepository.getVisibilityFloor(anyString(), anyString()))
        .thenReturn(Optional.of(new VisibilityFloor(null)));
    ObjectMapper objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat());
    this.chatMessageService =
        new ChatMessageServiceImpl(
            messageRepository,
            reactionRepository,
            readRepository,
            pinRepository,
            systemEventRepository,
            mock(WsPresenceRepository.class),
            mock(UserContactRepository.class),
            roomRepository,
            subscriptionRepository,
            objectMapper);
  }

  @AfterEach
  void tearDown() {
    reset(
        messageRepository,
        reactionRepository,
        pinRepository,
        systemEventRepository,
        readRepository,
        roomRepository,
        subscriptionRepository);
  }

  private Room room() {
    return Room.create().id(ROOM_ID).name("room").subscriptions(List.of());
  }

  private Message message() {
    return Message.create().id(MESSAGE_ID).roomId(ROOM_ID).senderId(USER_ID).text("hi");
  }

  private Message message(String id, java.time.OffsetDateTime createdAt) {
    return Message.create()
        .id(id)
        .roomId(ROOM_ID)
        .senderId(USER_ID)
        .text("hi")
        .createdAt(createdAt);
  }

  private SystemEvent systemEvent(String id, java.time.OffsetDateTime createdAt) {
    return SystemEvent.create().id(id).roomId(ROOM_ID).type("MESSAGE_PINNED").createdAt(createdAt);
  }

  @Nested
  @DisplayName("addReaction (single per user)")
  class AddReaction {

    @Test
    @DisplayName("Inserts a fresh reaction when the user has none; reports no replacement")
    void fresh() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(reactionRepository.getByUserAndMessage(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.empty());

      Optional<String> replaced =
          chatMessageService.addReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍");

      assertTrue(replaced.isEmpty());
      verify(reactionRepository).insert(any(MessageReaction.class));
      verify(reactionRepository, never()).update(any(MessageReaction.class));
      verify(reactionRepository, never()).delete(any(), any());
    }

    @Test
    @DisplayName("Re-sending the same emoji is a no-op; reports no replacement")
    void sameEmojiNoOp() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(reactionRepository.getByUserAndMessage(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.of(MessageReaction.create(message(), USER_ID, "👍")));

      Optional<String> replaced =
          chatMessageService.addReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍");

      assertTrue(replaced.isEmpty());
      verify(reactionRepository, never()).insert(any(MessageReaction.class));
      verify(reactionRepository, never()).update(any(MessageReaction.class));
      verify(reactionRepository, never()).delete(any(), any());
    }

    @Test
    @DisplayName("A different emoji replaces the previous one and reports the replaced emoji")
    void replacesDifferentEmoji() {
      MessageReaction existing = MessageReaction.create(message(), USER_ID, "👍");
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(reactionRepository.getByUserAndMessage(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.of(existing));

      Optional<String> replaced =
          chatMessageService.addReaction(ROOM_ID, MESSAGE_ID, USER_ID, "❤️");

      assertTrue(replaced.isPresent());
      assertEquals("👍", replaced.get());
      ArgumentCaptor<MessageReaction> captor = ArgumentCaptor.forClass(MessageReaction.class);
      verify(reactionRepository).update(captor.capture());
      assertEquals("❤️", captor.getValue().getReaction());
      verify(reactionRepository, never()).insert(any(MessageReaction.class));
    }

    @Test
    @DisplayName("Throws NotFound when the message does not exist")
    void messageNotFound() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.addReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍"));
    }
  }

  @Nested
  @DisplayName("removeReaction (clear the user's single reaction)")
  class RemoveReaction {

    @Test
    @DisplayName("Removes the user's reaction and reports the removed emoji")
    void removesExisting() {
      when(reactionRepository.getByUserAndMessage(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.of(MessageReaction.create(message(), USER_ID, "👍")));

      Optional<String> removed = chatMessageService.removeReaction(MESSAGE_ID, USER_ID);

      assertTrue(removed.isPresent());
      assertEquals("👍", removed.get());
      verify(reactionRepository).delete(MESSAGE_ID, USER_ID);
    }

    @Test
    @DisplayName("Is a no-op when the user has no reaction")
    void noReaction() {
      when(reactionRepository.getByUserAndMessage(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.empty());

      Optional<String> removed = chatMessageService.removeReaction(MESSAGE_ID, USER_ID);

      assertTrue(removed.isEmpty());
      verify(reactionRepository, never()).delete(any(), any());
    }
  }

  @Nested
  @DisplayName("pinMessage (single pin per room)")
  class PinMessage {

    @Test
    @DisplayName("Replaces the room pin and emits a MESSAGE_PINNED system event")
    void pins() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(pinRepository.pin(ROOM_ID, MESSAGE_ID, USER_ID))
          .thenReturn(MessagePin.create(ROOM_ID, MESSAGE_ID, USER_ID));

      SystemEvent returned = chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      verify(pinRepository).pin(ROOM_ID, MESSAGE_ID, USER_ID);
      ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
      verify(systemEventRepository).insert(captor.capture());
      assertEquals("MESSAGE_PINNED", captor.getValue().getType());
      assertEquals(USER_ID, captor.getValue().getActorId());
      assertEquals(captor.getValue().getId(), returned.getId());
      assertEquals(USER_ID, returned.getActorId());
    }

    @Test
    @DisplayName("Throws NotFound when the message belongs to another room")
    void wrongRoom() {
      Message other = Message.create().id(MESSAGE_ID).roomId("other-room").senderId(USER_ID);
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(other));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(pinRepository, never()).pin(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("unpinMessage")
  class UnpinMessage {

    @Test
    @DisplayName("Removes the room pin and emits a MESSAGE_UNPINNED system event")
    void unpins() {
      SystemEvent returned = chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      verify(pinRepository).unpin(ROOM_ID, MESSAGE_ID);
      ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
      verify(systemEventRepository).insert(captor.capture());
      assertEquals("MESSAGE_UNPINNED", captor.getValue().getType());
      assertEquals(USER_ID, captor.getValue().getActorId());
      assertEquals(captor.getValue().getId(), returned.getId());
      assertEquals(USER_ID, returned.getActorId());
    }
  }

  @Nested
  @DisplayName("setReadMarker")
  class SetReadMarker {

    @Test
    @DisplayName("Marks a text message of the room and returns the new marker time")
    void textMessage_success() {
      OffsetDateTime ts = OffsetDateTime.now().minusMinutes(5);
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message(MESSAGE_ID, ts)));
      when(readRepository.advance(USER_ID, ROOM_ID, ts)).thenReturn(Optional.of(ts));

      Optional<OffsetDateTime> result =
          chatMessageService.setReadMarker(ROOM_ID, USER_ID, MESSAGE_ID);

      assertEquals(Optional.of(ts), result);
    }

    @Test
    @DisplayName("Marks a system event of the room")
    void systemEvent_success() {
      OffsetDateTime ts = OffsetDateTime.now().minusMinutes(2);
      when(messageRepository.getById(SYSTEM_EVENT_ID)).thenReturn(Optional.empty());
      when(systemEventRepository.getById(SYSTEM_EVENT_ID))
          .thenReturn(Optional.of(systemEvent(SYSTEM_EVENT_ID, ts)));
      when(readRepository.advance(USER_ID, ROOM_ID, ts)).thenReturn(Optional.of(ts));

      Optional<OffsetDateTime> result =
          chatMessageService.setReadMarker(ROOM_ID, USER_ID, SYSTEM_EVENT_ID);

      assertEquals(Optional.of(ts), result);
    }

    @Test
    @DisplayName("Returns empty when an older item does not move the marker")
    void olderItem_returnsEmpty() {
      OffsetDateTime older = OffsetDateTime.now().minusMinutes(6);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, older)));
      when(readRepository.advance(USER_ID, ROOM_ID, older)).thenReturn(Optional.empty());

      Optional<OffsetDateTime> result =
          chatMessageService.setReadMarker(ROOM_ID, USER_ID, MESSAGE_ID);

      assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Throws NotFoundException when id is neither a message nor a system event")
    void unknownId_throws() {
      when(messageRepository.getById("unknown-id")).thenReturn(Optional.empty());
      when(systemEventRepository.getById("unknown-id")).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReadMarker(ROOM_ID, USER_ID, "unknown-id"));
      verify(readRepository, never()).advance(any(), any(), any());
    }

    @Test
    @DisplayName("Throws NotFoundException when the message belongs to another room")
    void messageOfAnotherRoom_throws() {
      Message foreign = message(MESSAGE_ID, OffsetDateTime.now()).roomId("other-room");
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(foreign));
      when(systemEventRepository.getById(MESSAGE_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReadMarker(ROOM_ID, USER_ID, MESSAGE_ID));
      verify(readRepository, never()).advance(any(), any(), any());
    }

    @Test
    @DisplayName("Throws NotFoundException when the system event belongs to another room")
    void systemEventOfAnotherRoom_throws() {
      SystemEvent foreign = systemEvent(SYSTEM_EVENT_ID, OffsetDateTime.now()).roomId("other-room");
      when(messageRepository.getById(SYSTEM_EVENT_ID)).thenReturn(Optional.empty());
      when(systemEventRepository.getById(SYSTEM_EVENT_ID)).thenReturn(Optional.of(foreign));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReadMarker(ROOM_ID, USER_ID, SYSTEM_EVENT_ID));
      verify(readRepository, never()).advance(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("visibility floor (cleared_at)")
  class VisibilityFloorChecks {

    private final OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");

    private void clearedAt(OffsetDateTime at) {
      when(subscriptionRepository.getVisibilityFloor(ROOM_ID, USER_ID))
          .thenReturn(Optional.of(new VisibilityFloor(at)));
    }

    @Test
    @DisplayName("reply target at or before cleared_at is NotFound, after it is accepted")
    void replyTarget() {
      clearedAt(clearedAt);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, clearedAt)));
      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.validateReplyTarget(ROOM_ID, USER_ID, MESSAGE_ID));

      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, clearedAt.plusNanos(1000))));
      chatMessageService.validateReplyTarget(ROOM_ID, USER_ID, MESSAGE_ID);
    }

    @Test
    @DisplayName("reply target of another room is NotFound")
    void replyTargetOtherRoom() {
      clearedAt(null);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(Message.create().id(MESSAGE_ID).roomId("other").senderId(USER_ID)));
      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.validateReplyTarget(ROOM_ID, USER_ID, MESSAGE_ID));
    }

    @Test
    @DisplayName("pin of a message at or before cleared_at is NotFound")
    void pinBelowFloor() {
      clearedAt(clearedAt);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, clearedAt)));
      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(pinRepository, never()).pin(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("getVisibilityFloor")
  class GetVisibilityFloor {

    @Test
    @DisplayName("Returns the floor of a member")
    void member() {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(subscriptionRepository.getVisibilityFloor(ROOM_ID, USER_ID))
          .thenReturn(Optional.of(new VisibilityFloor(clearedAt)));

      assertEquals(clearedAt, chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID).clearedAt());
    }

    @Test
    @DisplayName("Throws NotFound when the room does not exist")
    void roomNotFound() {
      when(subscriptionRepository.getVisibilityFloor(ROOM_ID, USER_ID))
          .thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class, () -> chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID));
    }

    @Test
    @DisplayName("Throws Forbidden when the user is not a member of an existing room")
    void notMember() {
      when(subscriptionRepository.getVisibilityFloor(ROOM_ID, USER_ID))
          .thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.of(room()));

      assertThrows(
          ForbiddenException.class, () -> chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID));
    }
  }
}
