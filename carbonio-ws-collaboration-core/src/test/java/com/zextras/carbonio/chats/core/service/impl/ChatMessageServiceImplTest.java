// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zextras.carbonio.async.model.MessagePinned;
import com.zextras.carbonio.async.model.ReactionChanged;
import com.zextras.carbonio.async.model.WsEvent;
import com.zextras.carbonio.chats.api.RFC3339DateFormat;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Message;
import com.zextras.carbonio.chats.core.data.entity.MessagePin;
import com.zextras.carbonio.chats.core.data.entity.ReadMarker;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.Membership;
import com.zextras.carbonio.chats.core.data.model.MessageContext;
import com.zextras.carbonio.chats.core.data.model.MessageSearchPage;
import com.zextras.carbonio.chats.core.data.model.PinResult;
import com.zextras.carbonio.chats.core.data.model.PinnedMessagesView;
import com.zextras.carbonio.chats.core.data.model.ReadMarkersView;
import com.zextras.carbonio.chats.core.data.model.TimelineItem;
import com.zextras.carbonio.chats.core.data.model.TimelineView;
import com.zextras.carbonio.chats.core.data.model.VisibilityFloor;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.event.mapper.TimelineItemMapper;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageReceivedBroadcaster;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessagePinRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.Removed;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetKind;
import com.zextras.carbonio.chats.core.repository.MessageReactionRepository.SetResult;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.MessageSendDedupRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.DeleteResult;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.DeleteStatus;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.EditResult;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.EditStatus;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.ForwardResult;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.ForwardStatus;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.SendOnceResult;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.SendOnceStatus;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.SendResult;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.SendStatus;
import com.zextras.carbonio.chats.core.repository.MessageWriteRepository.Unpinned;
import com.zextras.carbonio.chats.core.repository.ReadMarkerRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.repository.TimelineRepository;
import com.zextras.carbonio.chats.core.repository.impl.TimelineRepositoryImpl;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.spi.MessageSentListener;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ChatMessageTimelineItemDto;
import com.zextras.carbonio.chats.model.TimelineMessageDto;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.stubbing.OngoingStubbing;

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
  private final MessageWriteRepository messageWriteRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final TimelineRepository timelineRepository;
  private final MessageSendDedupRepository messageSendDedupRepository;
  private final AttachmentService attachmentService;
  private final StoragesService storagesService;
  private final EventDispatcher eventDispatcher;
  private final MessageReceivedBroadcaster messageReceivedBroadcaster;
  private final MessageSentListener messageSentListener;
  private final ObjectMapper objectMapper;
  private final TimelineItemMapper timelineItemMapper;
  private final UserPrincipal user =
      UserPrincipal.create(UUID.fromString("33333333-3333-3333-3333-333333333333"));

  private static final String ROOM_ID = "11111111-1111-1111-1111-111111111111";
  private static final String MESSAGE_ID = "22222222-2222-2222-2222-222222222222";
  private static final String SYSTEM_EVENT_ID = "44444444-4444-4444-4444-444444444444";
  private static final String USER_ID = "33333333-3333-3333-3333-333333333333";
  private static final String OTHER_ROOM_ID = "55555555-5555-5555-5555-555555555555";
  private static final String TARGET_ROOM_ID = "66666666-6666-6666-6666-666666666666";
  private static final UUID ROOM_UUID = UUID.fromString(ROOM_ID);

  ChatMessageServiceImplTest() {
    this.messageRepository = mock(MessageRepository.class);
    this.reactionRepository = mock(MessageReactionRepository.class);
    this.pinRepository = mock(MessagePinRepository.class);
    this.systemEventRepository = mock(SystemEventRepository.class);
    this.readRepository = mock(ReadMarkerRepository.class);
    this.roomRepository = mock(RoomRepository.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    when(subscriptionRepository.getMembership(anyString(), anyString()))
        .thenReturn(Optional.of(new Membership(false, null)));
    this.messageWriteRepository = mock(MessageWriteRepository.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.timelineRepository = new TimelineRepositoryImpl(messageRepository, systemEventRepository);
    this.messageSendDedupRepository = mock(MessageSendDedupRepository.class);
    this.attachmentService = mock(AttachmentService.class);
    this.storagesService = mock(StoragesService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.messageReceivedBroadcaster = mock(MessageReceivedBroadcaster.class);
    this.messageSentListener = mock(MessageSentListener.class);
    this.objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build()
            .setDateFormat(new RFC3339DateFormat());
    this.timelineItemMapper = new TimelineItemMapper(objectMapper);
    this.chatMessageService =
        new ChatMessageServiceImpl(
            messageRepository,
            messageWriteRepository,
            reactionRepository,
            readRepository,
            pinRepository,
            systemEventRepository,
            fileMetadataRepository,
            subscriptionRepository,
            timelineRepository,
            messageSendDedupRepository,
            new RoomAccessImpl(roomRepository, subscriptionRepository),
            attachmentService,
            storagesService,
            eventDispatcher,
            messageReceivedBroadcaster,
            timelineItemMapper,
            objectMapper,
            Set.of(messageSentListener));
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
        subscriptionRepository,
        messageWriteRepository,
        fileMetadataRepository,
        messageSendDedupRepository,
        attachmentService,
        storagesService,
        eventDispatcher,
        messageReceivedBroadcaster,
        messageSentListener);
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
  @DisplayName("setReaction (single per user)")
  class SetReaction {

    @Test
    @DisplayName("Delegates to the repository after the message checks and adds the message time")
    void delegates() {
      OffsetDateTime messageCreatedAt = OffsetDateTime.parse("2025-12-01T10:00:00Z");
      SetResult stored =
          new SetResult(SetKind.ADDED, null, OffsetDateTime.parse("2026-01-01T10:00:00Z"), null);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, messageCreatedAt)));
      when(reactionRepository.set(MESSAGE_ID, USER_ID, "👍")).thenReturn(stored);

      assertEquals(
          stored.withMessageCreatedAt(messageCreatedAt),
          chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍"));
    }

    @Test
    @DisplayName("Throws NotFound when the message does not exist")
    void messageNotFound() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍"));
      verify(reactionRepository, never()).set(any(), any(), any());
    }

    @Test
    @DisplayName("Throws NotFound and stores nothing when the message is deleted")
    void deletedMessage() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message().deleted(true)));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍"));
      verify(reactionRepository, never()).set(any(), any(), any());
    }

    @Test
    @DisplayName("Throws NotFound when the message is in another room")
    void otherRoom() {
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(Message.create().id(MESSAGE_ID).roomId("other-room").senderId(USER_ID)));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍"));
      verify(reactionRepository, never()).set(any(), any(), any());
    }
  }

  @Nested
  @DisplayName("removeReaction (clear the user's single reaction)")
  class RemoveReaction {

    @Test
    @DisplayName("Returns what the repository removed with the message time")
    void removesExisting() {
      OffsetDateTime messageCreatedAt = OffsetDateTime.parse("2025-12-01T10:00:00Z");
      Removed stored = new Removed("👍", OffsetDateTime.parse("2026-01-01T10:00:00Z"), null);
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, messageCreatedAt)));
      when(reactionRepository.remove(MESSAGE_ID, USER_ID)).thenReturn(Optional.of(stored));

      assertEquals(
          Optional.of(stored.withMessageCreatedAt(messageCreatedAt)),
          chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID));
    }

    @Test
    @DisplayName("Is empty when the user has no reaction")
    void noReaction() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(reactionRepository.remove(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

      assertTrue(chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID).isEmpty());
    }

    @Test
    @DisplayName("Throws NotFound and removes nothing when the message is in another room")
    void otherRoom() {
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(Message.create().id(MESSAGE_ID).roomId("other-room").senderId(USER_ID)));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(reactionRepository, never()).remove(any(), any());
    }

    @Test
    @DisplayName("Throws NotFound when the message does not exist")
    void messageNotFound() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID));
    }

    @Test
    @DisplayName("Throws NotFound and removes nothing when the message is at or below the floor")
    void belowVisibilityFloor() {
      OffsetDateTime floor = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID))
          .thenReturn(Optional.of(new Membership(false, floor)));
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(Optional.of(message(MESSAGE_ID, floor)));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(reactionRepository, never()).remove(any(), any());
    }
  }

  @Nested
  @DisplayName("pinMessage (single pin per room)")
  class PinMessage {

    private final OffsetDateTime pinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00.123456Z");

    @Test
    @DisplayName("Upserts the room pin and emits a MESSAGE_PINNED system event")
    void pins() {
      Message message = message();
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message));
      when(pinRepository.pin(ROOM_ID, MESSAGE_ID, USER_ID))
          .thenReturn(
              Optional.of(MessagePin.create(ROOM_ID, MESSAGE_ID, USER_ID).pinnedAt(pinnedAt)));

      SystemEvent stubbed =
          SystemEvent.create()
              .id("stored-event")
              .roomId(ROOM_ID)
              .type("MESSAGE_PINNED")
              .actorId(USER_ID)
              .createdAt(pinnedAt);
      when(systemEventRepository.insertAt(any(SystemEvent.class), eq(pinnedAt)))
          .thenReturn(stubbed);

      PinResult returned = chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      verify(pinRepository).pin(ROOM_ID, MESSAGE_ID, USER_ID);
      ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
      verify(systemEventRepository).insertAt(captor.capture(), eq(pinnedAt));
      assertEquals("MESSAGE_PINNED", captor.getValue().getType());
      assertEquals(USER_ID, captor.getValue().getActorId());
      assertNull(captor.getValue().getCreatedAt());
      assertSame(stubbed, returned.systemEvent());
      assertEquals(pinnedAt, returned.systemEvent().getCreatedAt());
      assertSame(message, returned.message());
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

    @Test
    @DisplayName("Throws NotFound and pins nothing when the message is deleted")
    void deletedMessage() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message().deleted(true)));

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(pinRepository, never()).pin(any(), any(), any());
    }

    @Test
    @DisplayName("Throws NotFound and writes no row when the message is deleted while pinning")
    void deletedWhilePinning() {
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message()));
      when(pinRepository.pin(ROOM_ID, MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class,
          () -> chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      verify(systemEventRepository, never()).insertAt(any(SystemEvent.class), any());
    }
  }

  @Nested
  @DisplayName("unpinMessage")
  class UnpinMessage {

    @Test
    @DisplayName("Removes the room pin and emits a MESSAGE_UNPINNED system event")
    void unpins() {
      when(pinRepository.unpin(ROOM_ID, MESSAGE_ID)).thenReturn(1);

      Optional<SystemEvent> returned =
          chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      verify(pinRepository).unpin(ROOM_ID, MESSAGE_ID);
      ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
      verify(systemEventRepository).insert(captor.capture());
      assertEquals("MESSAGE_UNPINNED", captor.getValue().getType());
      assertEquals(USER_ID, captor.getValue().getActorId());
      assertTrue(returned.isPresent());
      assertEquals(captor.getValue().getId(), returned.get().getId());
      assertEquals(USER_ID, returned.get().getActorId());
    }

    @Test
    @DisplayName("Writes no system event when the message is not the room pin")
    void notPinned() {
      when(pinRepository.unpin(ROOM_ID, MESSAGE_ID)).thenReturn(0);

      Optional<SystemEvent> returned =
          chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      assertTrue(returned.isEmpty());
      verify(systemEventRepository, never()).insert(any(SystemEvent.class));
    }
  }

  @Nested
  @DisplayName("membership checked by the service itself")
  class MembershipChecks {

    private void notAMember() {
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.of(room()));
    }

    private void missingRoom() {
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("unpinMessage by a non-member is Forbidden and writes nothing")
    void unpinNotMember() {
      notAMember();

      assertThrows(
          ForbiddenException.class,
          () -> chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      verifyNoInteractions(pinRepository, systemEventRepository);
    }

    @Test
    @DisplayName("unpinMessage in a missing room is NotFound")
    void unpinMissingRoom() {
      missingRoom();

      NotFoundException exception =
          assertThrows(
              NotFoundException.class,
              () -> chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID));
      assertEquals(
          String.format("Not Found - Room '%s' not found", ROOM_ID), exception.getMessage());
      verifyNoInteractions(pinRepository, systemEventRepository);
    }

    @Test
    @DisplayName("getPinnedMessages by a non-member is Forbidden")
    void pinsNotMember() {
      notAMember();

      assertThrows(
          ForbiddenException.class, () -> chatMessageService.getPinnedMessages(ROOM_ID, USER_ID));
      verifyNoInteractions(pinRepository);
    }

    @Test
    @DisplayName("getPinnedMessages by a member returns the pins of the room")
    void pinsMember() {
      when(pinRepository.getPinnedMessages(ROOM_ID)).thenReturn(List.of());

      assertEquals(List.of(), chatMessageService.getPinnedMessages(ROOM_ID, USER_ID));
      verify(pinRepository).getPinnedMessages(ROOM_ID);
    }

    @Test
    @DisplayName("getRoomReadMarkers by a non-member is Forbidden")
    void markersNotMember() {
      notAMember();

      assertThrows(
          ForbiddenException.class, () -> chatMessageService.getRoomReadMarkers(ROOM_ID, USER_ID));
      verifyNoInteractions(readRepository);
    }

    @Test
    @DisplayName("getRoomReadMarkers in a missing room is NotFound")
    void markersMissingRoom() {
      missingRoom();

      assertThrows(
          NotFoundException.class, () -> chatMessageService.getRoomReadMarkers(ROOM_ID, USER_ID));
      verifyNoInteractions(readRepository);
    }

    @Test
    @DisplayName("getRoomReadMarkers by a member returns the markers of the room")
    void markersMember() {
      when(readRepository.getByRoomId(ROOM_ID)).thenReturn(List.of());

      ReadMarkersView view = chatMessageService.getRoomReadMarkers(ROOM_ID, USER_ID);

      assertEquals(List.of(), view.markers());
      assertEquals(new VisibilityFloor(null), view.floor());
      verify(readRepository).getByRoomId(ROOM_ID);
    }

    @Test
    @DisplayName("removeReaction by a non-member is Forbidden and removes nothing")
    void removeReactionNotMember() {
      notAMember();

      assertThrows(
          ForbiddenException.class,
          () -> chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID));
      verifyNoInteractions(reactionRepository);
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
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID))
          .thenReturn(Optional.of(new Membership(false, at)));
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
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID))
          .thenReturn(Optional.of(new Membership(false, clearedAt)));

      assertEquals(clearedAt, chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID).clearedAt());
    }

    @Test
    @DisplayName("Throws NotFound when the room does not exist")
    void roomNotFound() {
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.empty());

      assertThrows(
          NotFoundException.class, () -> chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID));
    }

    @Test
    @DisplayName("Throws Forbidden when the user is not a member of an existing room")
    void notMember() {
      when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
      when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.of(room()));

      assertThrows(
          ForbiddenException.class, () -> chatMessageService.getVisibilityFloor(ROOM_ID, USER_ID));
    }
  }

  private void givenNonMember() {
    when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
    when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.of(room()));
  }

  private void givenMissingRoom() {
    when(subscriptionRepository.getMembership(ROOM_ID, USER_ID)).thenReturn(Optional.empty());
    when(roomRepository.getById(ROOM_ID)).thenReturn(Optional.empty());
  }

  private void givenFloor(OffsetDateTime clearedAt) {
    when(subscriptionRepository.getMembership(ROOM_ID, USER_ID))
        .thenReturn(Optional.of(new Membership(false, clearedAt)));
  }

  private static void assertRejected(
      Class<? extends ChatsHttpException> type, String debugInfo, Executable call) {
    ChatsHttpException exception = assertThrows(type, call);
    assertEquals(debugInfo, exception.getDebugInfo());
  }

  private static String newId() {
    return UUID.randomUUID().toString();
  }

  @Nested
  @DisplayName("sendMessage")
  class SendMessageTests {

    private final OffsetDateTime sentAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");

    private void plainSendStores(String messageId) {
      when(messageWriteRepository.sendMessage(
              eq(ROOM_ID), eq(USER_ID), eq("hello"), anyString(), isNull(), isNull()))
          .thenReturn(new SendResult(SendStatus.SENT, messageId, sentAt));
    }

    private void plainSendRejected(SendStatus status) {
      when(messageWriteRepository.sendMessage(any(), any(), any(), any(), any(), any()))
          .thenReturn(SendResult.rejected(status));
    }

    private void sendOnceReturns(SendOnceStatus status, String messageId, OffsetDateTime at) {
      when(messageWriteRepository.sendMessageOnce(any(), any(), any(), any(), any(), any()))
          .thenReturn(new SendOnceResult(status, messageId, at));
    }

    private Message original(String id, String attachmentId) {
      return Message.create()
          .id(id)
          .roomId(ROOM_ID)
          .senderId(USER_ID)
          .text("hello")
          .createdAt(sentAt)
          .attachmentId(attachmentId);
    }

    @Test
    @DisplayName("sendMessage notifies the registered message-sent listeners")
    void notifiesMessageSentListeners() throws Exception {
      String messageId = newId();
      plainSendStores(messageId);

      Message sent = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null);

      assertEquals(messageId, sent.getId());
      assertEquals(ROOM_ID, sent.getRoomId());
      assertEquals(USER_ID, sent.getSenderId());
      assertEquals("hello", sent.getText());
      assertEquals(sentAt, sent.getCreatedAt());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, messageId, "hello", sentAt);
      ArgumentCaptor<TimelineMessageDto> broadcast =
          ArgumentCaptor.forClass(TimelineMessageDto.class);
      verify(messageReceivedBroadcaster, times(1))
          .broadcast(eq(ROOM_ID), broadcast.capture(), isNull());
      assertEquals(messageId, broadcast.getValue().getId());
      assertEquals("hello", broadcast.getValue().getText());
    }

    @Test
    @DisplayName("sendMessage rejects a blank text with 400 and stores nothing")
    void blankText_badRequest() {
      assertRejected(
          BadRequestException.class,
          "Message text must not be blank",
          () -> chatMessageService.sendMessage(ROOM_ID, user, " \n\t ", null, null));
      assertRejected(
          BadRequestException.class,
          "Message text must not be blank",
          () -> chatMessageService.sendMessage(ROOM_ID, user, null, null, "t-1"));

      verifyNoInteractions(
          messageWriteRepository, messageSentListener, messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("sendMessage does not notify the listeners when the message is not stored")
    void notStored_doesNotNotifyMessageSentListeners() {
      plainSendRejected(SendStatus.NOT_MEMBER);

      assertThrows(
          ForbiddenException.class,
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null));

      verify(messageSentListener, never()).onMessageSent(any(), any(), any(), any(), any());
      verifyNoInteractions(messageReceivedBroadcaster);
    }

    @Test
    @DisplayName("sendMessage returns the message even when a message-sent listener fails")
    void listenerFails_stillCreated() throws Exception {
      String messageId = newId();
      plainSendStores(messageId);
      doThrow(new RuntimeException("listener failure"))
          .when(messageSentListener)
          .onMessageSent(user, ROOM_UUID, messageId, "hello", sentAt);

      Message sent = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null);

      assertEquals(messageId, sent.getId());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, messageId, "hello", sentAt);
    }

    @Test
    @DisplayName("sendMessage returns the message even when the broadcast fails")
    void broadcastFails_stillCreated() throws Exception {
      String messageId = newId();
      plainSendStores(messageId);
      doThrow(new RuntimeException("broker down"))
          .when(messageReceivedBroadcaster)
          .broadcast(any(), any(), any());

      Message sent = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null);

      assertEquals(messageId, sent.getId());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, messageId, "hello", sentAt);
    }

    @Test
    @DisplayName("Plain send: NOT_MEMBER is Forbidden with the membership/conflict text")
    void plain_notMember_forbidden() {
      plainSendRejected(SendStatus.NOT_MEMBER);

      assertRejected(
          ForbiddenException.class,
          "Not a room member or message ID conflict",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener, eventDispatcher);
    }

    @Test
    @DisplayName("Plain send: ID_CONFLICT is Forbidden with the membership/conflict text")
    void plain_idConflict_forbidden() {
      plainSendRejected(SendStatus.ID_CONFLICT);

      assertRejected(
          ForbiddenException.class,
          "Not a room member or message ID conflict",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, null));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener, eventDispatcher);
    }

    @Test
    @DisplayName("Plain send: REPLY_NOT_FOUND is NotFound 'Reply target not found'")
    void plain_replyNotFound_notFound() {
      plainSendRejected(SendStatus.REPLY_NOT_FOUND);

      assertRejected(
          NotFoundException.class,
          "Reply target not found",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", newId(), null));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener, eventDispatcher);
    }

    @Test
    @DisplayName("A first send with a tempId goes through the claiming query and is broadcast")
    void withTempId_firstSend() throws Exception {
      String messageId = newId();
      when(messageWriteRepository.sendMessageOnce(
              eq(ROOM_ID), eq(USER_ID), eq("hello"), anyString(), isNull(), eq("t-1")))
          .thenReturn(new SendOnceResult(SendOnceStatus.SENT, messageId, sentAt));

      Message sent = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1");

      assertEquals(messageId, sent.getId());
      verify(messageWriteRepository, never()).sendMessage(any(), any(), any(), any(), any(), any());
      verify(messageReceivedBroadcaster, times(1)).broadcast(eq(ROOM_ID), any(), eq("t-1"));
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, messageId, "hello", sentAt);
    }

    @Test
    @DisplayName("A duplicate tempId returns the original, without event or listener")
    void duplicateTempId_returnsOriginal() {
      String originalId = newId();
      sendOnceReturns(SendOnceStatus.DUPLICATE, originalId, null);
      Message original = original(originalId, null);
      when(messageRepository.getById(originalId)).thenReturn(Optional.of(original));

      Message returned = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1");

      assertSame(original, returned);
      assertEquals(sentAt, returned.getCreatedAt());
      assertEquals("hello", returned.getText());
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener, eventDispatcher);
    }

    @Test
    @DisplayName("A duplicate whose first send has not stored its message yet is a Conflict")
    void duplicateInFlight_conflict() {
      sendOnceReturns(SendOnceStatus.DUPLICATE, null, null);

      assertRejected(
          ConflictException.class,
          "The first request with this tempId has not completed yet",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1"));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener);
    }

    @Test
    @DisplayName("A duplicate whose message cannot be read is a Conflict")
    void duplicateMessageMissing_conflict() {
      String originalId = newId();
      sendOnceReturns(SendOnceStatus.DUPLICATE, originalId, null);
      when(messageRepository.getById(originalId)).thenReturn(Optional.empty());

      assertRejected(
          ConflictException.class,
          "The first request with this tempId has not completed yet",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1"));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener);
    }

    @Test
    @DisplayName("A duplicate pointing to a message of another room is a Conflict")
    void duplicateOfOtherRoom_conflict() {
      String originalId = newId();
      sendOnceReturns(SendOnceStatus.DUPLICATE, originalId, null);
      when(messageRepository.getById(originalId))
          .thenReturn(Optional.of(original(originalId, null).roomId(OTHER_ROOM_ID)));

      assertRejected(
          ConflictException.class,
          "The first request with this tempId has not completed yet",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1"));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener);
    }

    @Test
    @DisplayName("With tempId: NOT_MEMBER is Forbidden with the membership/conflict text")
    void withTempId_notMember_forbidden() {
      sendOnceReturns(SendOnceStatus.NOT_MEMBER, null, null);

      assertRejected(
          ForbiddenException.class,
          "Not a room member or message ID conflict",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "t-1"));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener);
    }

    @Test
    @DisplayName("With tempId: REPLY_NOT_FOUND is NotFound 'Reply target not found'")
    void withTempId_replyNotFound_notFound() {
      sendOnceReturns(SendOnceStatus.REPLY_NOT_FOUND, null, null);

      assertRejected(
          NotFoundException.class,
          "Reply target not found",
          () -> chatMessageService.sendMessage(ROOM_ID, user, "hello", newId(), "t-1"));
      verifyNoInteractions(messageReceivedBroadcaster, messageSentListener);
    }

    @Test
    @DisplayName("A blank tempId is not a dedup key: the plain send path is used")
    void blankTempId_plainPath() {
      String messageId = newId();
      when(messageWriteRepository.sendMessage(any(), any(), any(), any(), any(), any()))
          .thenReturn(new SendResult(SendStatus.SENT, messageId, sentAt));

      Message sent = chatMessageService.sendMessage(ROOM_ID, user, "hello", null, "  ");

      assertEquals(messageId, sent.getId());
      verify(messageWriteRepository, never())
          .sendMessageOnce(any(), any(), any(), any(), any(), any());
    }
  }

  @Nested
  @DisplayName("uploadAttachment")
  class UploadAttachment {

    private final InputStream attachment = mock(InputStream.class);
    private final OffsetDateTime attachmentCreatedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");

    private Message createdMessage(String id, String text) {
      return Message.create()
          .id(id)
          .createdAt(attachmentCreatedAt)
          .roomId(ROOM_ID)
          .senderId(USER_ID)
          .text(text)
          .attachmentId(newId());
    }

    @BeforeEach
    void stubAddAttachmentWithACreatedMessage() {
      when(attachmentService.addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenReturn(createdMessage(newId(), ""));
    }

    private Message upload(String description, String replyToId, String area, String tempId) {
      return chatMessageService.uploadAttachment(
          ROOM_ID,
          attachment,
          "image/jpeg",
          1024L,
          "fileName",
          description,
          replyToId,
          area,
          tempId,
          user);
    }

    private void verifyAddAttachment(
        String description, String replyToId, String area, String tempId) {
      verify(attachmentService, times(1))
          .addAttachment(
              eq(ROOM_UUID),
              eq(attachment),
              eq("image/jpeg"),
              eq(1024L),
              eq("fileName"),
              eq(description),
              eq(replyToId),
              eq(area),
              eq(tempId),
              anyString(),
              eq(user));
    }

    private void stubAddAttachment(String description, Message created) {
      when(attachmentService.addAttachment(
              eq(ROOM_UUID),
              eq(attachment),
              eq("image/jpeg"),
              eq(1024L),
              eq("fileName"),
              eq(description),
              any(),
              any(),
              any(),
              anyString(),
              eq(user)))
          .thenReturn(created);
    }

    @Test
    @DisplayName("Upload with all fields stores the attachment and notifies the listeners")
    void withAllFields() {
      String createdId = newId();
      when(messageRepository.getById("reply-id"))
          .thenReturn(Optional.of(message("reply-id", attachmentCreatedAt.minusDays(1))));
      stubAddAttachment("test", createdMessage(createdId, "test"));

      Message created = upload("test", "reply-id", "10x5", null);

      assertEquals(createdId, created.getId());
      verifyAddAttachment("test", "reply-id", "10x5", null);
      verifyNoInteractions(messageSendDedupRepository);
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, createdId, "test", attachmentCreatedAt);
    }

    @Test
    @DisplayName("Upload notifies the message-sent listeners with the caption")
    void notifiesMessageSentListenersWithCaption() {
      String createdId = newId();
      stubAddAttachment("a caption", createdMessage(createdId, "a caption"));

      upload("a caption", null, null, null);

      verifyAddAttachment("a caption", null, null, null);
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, createdId, "a caption", attachmentCreatedAt);
    }

    @Test
    @DisplayName("Upload without a caption notifies the listeners with an empty text")
    void withoutCaption_notifiesMessageSentListenersWithEmptyText() {
      String createdId = newId();
      stubAddAttachment(null, createdMessage(createdId, ""));

      upload(null, null, null, null);

      verifyAddAttachment(null, null, null, null);
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, createdId, "", attachmentCreatedAt);
    }

    @Test
    @DisplayName("Upload returns the created message even when a message-sent listener fails")
    void listenerFails_stillCreated() {
      String createdId = newId();
      Message created = createdMessage(createdId, "");
      stubAddAttachment(null, created);
      doThrow(new RuntimeException("listener failure"))
          .when(messageSentListener)
          .onMessageSent(user, ROOM_UUID, createdId, "", attachmentCreatedAt);

      Message returned = upload(null, null, null, null);

      assertSame(created, returned);
      verifyAddAttachment(null, null, null, null);
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, createdId, "", attachmentCreatedAt);
    }

    @Test
    @DisplayName("Upload does not notify the listeners when the upload fails")
    void uploadFails_doesNotNotifyMessageSentListeners() {
      when(attachmentService.addAttachment(
              eq(ROOM_UUID),
              eq(attachment),
              eq("image/jpeg"),
              eq(1024L),
              eq("fileName"),
              isNull(),
              isNull(),
              isNull(),
              isNull(),
              anyString(),
              eq(user)))
          .thenThrow(new ForbiddenException("not a member"));

      assertThrows(ForbiddenException.class, () -> upload(null, null, null, null));

      verifyAddAttachment(null, null, null, null);
      verify(messageSentListener, never()).onMessageSent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("An upload with a tempId claims it before storing and creates that message id")
    void withTempId_claimsFirst() {
      when(messageSendDedupRepository.claim(eq(ROOM_ID), eq(USER_ID), eq("t-1"), anyString()))
          .thenReturn(new MessageSendDedupRepository.Claim(true, null));
      when(attachmentService.addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenAnswer(invocation -> createdMessage(invocation.getArgument(9), ""));

      Message created = upload(null, null, null, "t-1");

      ArgumentCaptor<String> claimed = ArgumentCaptor.forClass(String.class);
      InOrder order =
          inOrder(subscriptionRepository, messageSendDedupRepository, attachmentService);
      order.verify(subscriptionRepository).getMembership(ROOM_ID, USER_ID);
      order
          .verify(messageSendDedupRepository)
          .claim(eq(ROOM_ID), eq(USER_ID), eq("t-1"), claimed.capture());
      order
          .verify(attachmentService)
          .addAttachment(
              eq(ROOM_UUID),
              any(),
              eq("image/jpeg"),
              eq(1024L),
              eq("fileName"),
              isNull(),
              isNull(),
              isNull(),
              eq("t-1"),
              eq(claimed.getValue()),
              eq(user));
      assertEquals(claimed.getValue(), created.getId());
      verify(messageSendDedupRepository, never()).release(any(), any(), any(), any());
    }

    @Test
    @DisplayName("A duplicate upload returns the original and stores nothing")
    void duplicate_returnsOriginalWithoutStoring() {
      String originalId = newId();
      String attachmentId = newId();
      Message original =
          Message.create()
              .id(originalId)
              .roomId(ROOM_ID)
              .senderId(USER_ID)
              .text("hello")
              .createdAt(attachmentCreatedAt)
              .attachmentId(attachmentId);
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(false, originalId));
      when(messageRepository.getById(originalId)).thenReturn(Optional.of(original));

      Message returned = upload(null, null, null, "t-1");

      assertSame(original, returned);
      verify(attachmentService, never())
          .addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
      verifyNoInteractions(messageSentListener);
    }

    @Test
    @DisplayName("A duplicate upload whose first request has not completed is a Conflict")
    void duplicate_inFlight_conflict() {
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(false, null));

      assertRejected(
          ConflictException.class,
          "The first request with this tempId has not completed yet",
          () -> upload(null, null, null, "t-1"));
      verify(attachmentService, never())
          .addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
      verifyNoInteractions(messageSentListener);
    }

    @Test
    @DisplayName("A storage failure after the claim releases it and keeps the error")
    void storageFailure_releasesTheClaim() {
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(true, null));
      when(attachmentService.addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenThrow(new StorageException("storage down"));

      assertThrows(StorageException.class, () -> upload(null, null, null, "t-1"));

      ArgumentCaptor<String> claimed = ArgumentCaptor.forClass(String.class);
      verify(messageSendDedupRepository)
          .claim(eq(ROOM_ID), eq(USER_ID), eq("t-1"), claimed.capture());
      verify(messageSendDedupRepository).release(ROOM_ID, USER_ID, "t-1", claimed.getValue());
      verifyNoInteractions(messageSentListener);
    }

    @Test
    @DisplayName("An unexpected failure after the claim releases it and propagates")
    void unexpectedFailure_releasesTheClaim() {
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(true, null));
      RuntimeException boom = new RuntimeException("boom");
      when(attachmentService.addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenThrow(boom);

      RuntimeException thrown =
          assertThrows(RuntimeException.class, () -> upload(null, null, null, "t-1"));

      assertSame(boom, thrown);
      verify(messageSendDedupRepository).release(eq(ROOM_ID), eq(USER_ID), eq("t-1"), anyString());
    }

    @Test
    @DisplayName("A failing release of the claim does not hide the upload error")
    void releaseFails_uploadErrorKept() {
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(true, null));
      when(attachmentService.addAttachment(
              any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
          .thenThrow(new StorageException("storage down"));
      doThrow(new RuntimeException("db down"))
          .when(messageSendDedupRepository)
          .release(any(), any(), any(), any());

      assertThrows(StorageException.class, () -> upload(null, null, null, "t-1"));
    }

    @Test
    @DisplayName("An upload without tempId never touches the claims")
    void withoutTempId_noClaim() {
      upload(null, null, null, null);

      verifyNoInteractions(messageSendDedupRepository);
      verifyAddAttachment(null, null, null, null);
    }

    @Test
    @DisplayName("An upload with a blank tempId never touches the claims")
    void blankTempId_noClaim() {
      upload(null, null, null, "  ");

      verifyNoInteractions(messageSendDedupRepository);
      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
    }

    @Test
    @DisplayName("Plain upload checks the membership once")
    void plain_oneMembershipQuery() {
      upload(null, null, null, null);

      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
      verifyAddAttachment(null, null, null, null);
    }

    @Test
    @DisplayName("Upload with a reply checks the membership once")
    void withReply_oneMembershipQuery() {
      when(messageRepository.getById("reply-id"))
          .thenReturn(Optional.of(message("reply-id", attachmentCreatedAt.minusDays(1))));

      upload(null, "reply-id", null, null);

      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
      verifyAddAttachment(null, "reply-id", null, null);
    }

    @Test
    @DisplayName("Upload with a tempId checks the membership once, before the claim")
    void withTempId_oneMembershipQuery() {
      when(messageSendDedupRepository.claim(any(), any(), any(), any()))
          .thenReturn(new MessageSendDedupRepository.Claim(true, null));

      upload(null, null, null, "t-1");

      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
    }

    @Test
    @DisplayName("A non-member uploading with a tempId is Forbidden before any claim")
    void withTempId_notMember_forbiddenBeforeClaim() {
      givenNonMember();

      assertThrows(ForbiddenException.class, () -> upload(null, null, null, "t-1"));

      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A non-member uploading with a reply is Forbidden")
    void withReply_notMember_forbidden() {
      givenNonMember();

      assertThrows(ForbiddenException.class, () -> upload(null, "reply-id", null, null));

      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A non-member uploading a plain attachment is Forbidden")
    void plain_notMember_forbidden() {
      givenNonMember();

      assertThrows(ForbiddenException.class, () -> upload(null, null, null, null));

      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A plain upload to a missing room is NotFound \"Room '<id>'\"")
    void plain_missingRoom_notFound() {
      givenMissingRoom();

      NotFoundException exception =
          assertThrows(NotFoundException.class, () -> upload(null, null, null, null));

      assertEquals(String.format("Room '%s'", ROOM_ID), exception.getDebugInfo());
      assertEquals(String.format("Not Found - Room '%s'", ROOM_ID), exception.getMessage());
      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("An upload with a reply to a missing room is NotFound \"Room '<id>' not found\"")
    void withReply_missingRoom_notFound() {
      givenMissingRoom();

      NotFoundException exception =
          assertThrows(NotFoundException.class, () -> upload(null, "reply-id", null, null));

      assertEquals(String.format("Room '%s' not found", ROOM_ID), exception.getDebugInfo());
      assertEquals(
          String.format("Not Found - Room '%s' not found", ROOM_ID), exception.getMessage());
      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("An upload with a tempId to a missing room is NotFound \"Room '<id>' not found\"")
    void withTempId_missingRoom_notFound() {
      givenMissingRoom();

      NotFoundException exception =
          assertThrows(NotFoundException.class, () -> upload(null, null, null, "t-1"));

      assertEquals(String.format("Room '%s' not found", ROOM_ID), exception.getDebugInfo());
      assertEquals(
          String.format("Not Found - Room '%s' not found", ROOM_ID), exception.getMessage());
      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A reply target that is missing is NotFound before the claim")
    void replyTargetMissing_notFoundBeforeClaim() {
      when(messageRepository.getById("reply-id")).thenReturn(Optional.empty());

      assertRejected(
          NotFoundException.class,
          "Message 'reply-id' not found",
          () -> upload(null, "reply-id", null, "t-1"));

      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A reply target at or before the cleared history is NotFound before the claim")
    void replyTargetBelowFloor_notFoundBeforeClaim() {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      givenFloor(clearedAt);
      when(messageRepository.getById("reply-id"))
          .thenReturn(Optional.of(message("reply-id", clearedAt)));

      assertRejected(
          NotFoundException.class,
          "Message 'reply-id' not found",
          () -> upload(null, "reply-id", null, "t-1"));

      verifyNoInteractions(messageSendDedupRepository, attachmentService, messageSentListener);
    }

    @Test
    @DisplayName("A reply target of another room is NotFound")
    void replyTargetOtherRoom_notFound() {
      when(messageRepository.getById("reply-id"))
          .thenReturn(
              Optional.of(
                  message("reply-id", attachmentCreatedAt.minusDays(1)).roomId(OTHER_ROOM_ID)));

      assertRejected(
          NotFoundException.class,
          "Message 'reply-id' not found",
          () -> upload(null, "reply-id", null, null));

      verifyNoInteractions(attachmentService, messageSentListener);
    }
  }

  @Nested
  @DisplayName("editMessage")
  class EditMessageTests {

    private final OffsetDateTime editedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-10-30T09:59:00Z");

    private void editStores(boolean pinned) {
      when(messageWriteRepository.editMessage(MESSAGE_ID, ROOM_ID, USER_ID, "new text"))
          .thenReturn(
              new EditResult(
                  EditStatus.EDITED, MESSAGE_ID, "new text", editedAt, createdAt, pinned));
    }

    private void editRejected(EditStatus status) {
      when(messageWriteRepository.editMessage(any(), any(), any(), any()))
          .thenReturn(EditResult.rejected(status));
    }

    @Test
    @DisplayName("editMessage notifies the message-sent listeners with the new text")
    void notifiesMessageSentListeners() {
      editStores(false);

      Message edited = chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text");

      assertEquals(MESSAGE_ID, edited.getId());
      assertEquals(ROOM_ID, edited.getRoomId());
      assertEquals(USER_ID, edited.getSenderId());
      assertEquals("new text", edited.getText());
      assertEquals(createdAt, edited.getCreatedAt());
      assertEquals(editedAt, edited.getUpdatedAt());
      assertEquals(Boolean.TRUE, edited.isEdited());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "new text", createdAt);
    }

    @Test
    @DisplayName("editMessage broadcasts MessageEdited to the members who can see the message")
    void broadcastsToMembersWhoCanAccess() throws Exception {
      editStores(false);

      chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text");

      ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
      verify(messageReceivedBroadcaster)
          .broadcastToMembersWhoCanAccess(eq(ROOM_ID), eq(createdAt), json.capture());
      verifyNoInteractions(eventDispatcher);
      JsonNode event = objectMapper.readTree(json.getValue());
      assertEquals("MessageEdited", event.get("type").asText());
      assertEquals(MESSAGE_ID, event.get("messageId").asText());
      assertEquals(ROOM_ID, event.get("roomId").asText());
      assertEquals("new text", event.get("text").asText());
      assertEquals(
          editedAt.toInstant(), OffsetDateTime.parse(event.get("editedAt").asText()).toInstant());
    }

    @Test
    @DisplayName("editMessage of the room pin broadcasts MessageEdited to every room member")
    void pinned_broadcastsToAllMembers() throws Exception {
      List<String> members = List.of(USER_ID, newId());
      when(subscriptionRepository.getUserIdsByRoomId(ROOM_ID)).thenReturn(members);
      editStores(true);

      chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text");

      ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher).sendRawToUserExchange(eq(members), json.capture());
      verify(messageReceivedBroadcaster, never())
          .broadcastToMembersWhoCanAccess(any(), any(), any());
      assertEquals("MessageEdited", objectMapper.readTree(json.getValue()).get("type").asText());
    }

    @Test
    @DisplayName("editMessage returns the message even when the broadcast fails")
    void broadcastFails_stillEdited() throws Exception {
      editStores(false);
      doThrow(new RuntimeException("broker down"))
          .when(messageReceivedBroadcaster)
          .broadcastToMembersWhoCanAccess(any(), any(), any());

      Message edited = chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text");

      assertEquals("new text", edited.getText());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "new text", createdAt);
    }

    @Test
    @DisplayName("editMessage does not notify the listeners when the edit is refused")
    void refused_doesNotNotifyMessageSentListeners() {
      editRejected(EditStatus.NOT_ALLOWED);

      assertThrows(
          ForbiddenException.class,
          () -> chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text"));

      verify(messageSentListener, never()).onMessageSent(any(), any(), any(), any(), any());
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("NOT_ALLOWED is Forbidden with the member/sender/deleted text")
    void notAllowed_forbidden() {
      editRejected(EditStatus.NOT_ALLOWED);

      assertRejected(
          ForbiddenException.class,
          "Not a room member, not the sender, or message is deleted",
          () -> chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text"));
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher, messageSentListener);
    }

    @Test
    @DisplayName("NOT_VISIBLE is NotFound \"Message '<id>' not found\"")
    void notVisible_notFound() {
      editRejected(EditStatus.NOT_VISIBLE);

      assertRejected(
          NotFoundException.class,
          String.format("Message '%s' not found", MESSAGE_ID),
          () -> chatMessageService.editMessage(ROOM_ID, MESSAGE_ID, user, "new text"));
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher, messageSentListener);
    }
  }

  @Nested
  @DisplayName("deleteMessage")
  class DeleteMessageTests {

    private final OffsetDateTime deletedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
    private final OffsetDateTime createdAt = OffsetDateTime.parse("2026-10-30T09:00:00Z");

    private void deleteRejected(DeleteStatus status) {
      when(messageWriteRepository.deleteMessage(any(), any(), any()))
          .thenReturn(DeleteResult.rejected(status));
    }

    @Test
    @DisplayName("deleteMessage leaves the attachment file of the deleted message in place")
    void keepsAttachmentFile() {
      when(messageWriteRepository.deleteMessage(MESSAGE_ID, ROOM_ID, USER_ID))
          .thenReturn(
              new DeleteResult(
                  DeleteStatus.DELETED, MESSAGE_ID, newId(), deletedAt, createdAt, null));

      chatMessageService.deleteMessage(ROOM_ID, MESSAGE_ID, user);

      verifyNoInteractions(storagesService);
      verify(fileMetadataRepository, never()).delete(any());
    }

    @Test
    @DisplayName("NOT_ALLOWED is Forbidden with the member/sender/already-deleted text")
    void notAllowed_forbidden() {
      deleteRejected(DeleteStatus.NOT_ALLOWED);

      assertRejected(
          ForbiddenException.class,
          "Not a room member, not the sender, or message already deleted",
          () -> chatMessageService.deleteMessage(ROOM_ID, MESSAGE_ID, user));
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("NOT_VISIBLE is NotFound \"Message '<id>' not found\"")
    void notVisible_notFound() {
      deleteRejected(DeleteStatus.NOT_VISIBLE);

      assertRejected(
          NotFoundException.class,
          String.format("Message '%s' not found", MESSAGE_ID),
          () -> chatMessageService.deleteMessage(ROOM_ID, MESSAGE_ID, user));
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Pin, unpin and delete events")
  class PinUnpinDeleteEvents {

    @Test
    @DisplayName("unpinMessage of a message that is not the room pin sends nothing")
    void unpinMessage_notPinned_sendsNothing() throws Exception {
      when(pinRepository.unpin(ROOM_ID, MESSAGE_ID)).thenReturn(0);

      Optional<SystemEvent> unpinned =
          chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      assertTrue(unpinned.isEmpty());
      verifyNoInteractions(eventDispatcher, messageReceivedBroadcaster);
    }

    @Test
    @DisplayName("unpinMessage of the room pin sends MessageUnpinned with the persisted row")
    void unpinMessage_pinned_sendsUnpinned() throws Exception {
      OffsetDateTime unpinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      List<String> members = List.of(USER_ID, newId());
      when(subscriptionRepository.getUserIdsByRoomId(ROOM_ID)).thenReturn(members);
      when(pinRepository.unpin(ROOM_ID, MESSAGE_ID)).thenReturn(1);
      when(systemEventRepository.insert(any(SystemEvent.class)))
          .thenAnswer(
              invocation -> {
                SystemEvent persisted = invocation.getArgument(0);
                return persisted.createdAt(unpinnedAt);
              });

      Optional<SystemEvent> unpinned =
          chatMessageService.unpinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      assertTrue(unpinned.isPresent());
      ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher).sendRawToUserExchange(eq(members), payload.capture());
      JsonNode event = objectMapper.readTree(payload.getValue());
      assertEquals("MessageUnpinned", event.get("type").asText());
      assertEquals(MESSAGE_ID, event.get("messageId").asText());
      assertEquals(USER_ID, event.get("unpinnedBy").asText());
      assertEquals(unpinned.get().getId(), event.get("systemEventId").asText());
      assertEquals(
          unpinnedAt.toInstant(),
          OffsetDateTime.parse(event.get("timestamp").asText()).toInstant());
    }

    @Test
    @DisplayName("pinMessage announces the pin it committed without re-reading the room pin")
    void pinMessage_broadcastsCommittedPin() throws Exception {
      OffsetDateTime pinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      Message message =
          Message.create()
              .id(MESSAGE_ID)
              .roomId(ROOM_ID)
              .senderId(newId())
              .text("pinned text")
              .createdAt(pinnedAt.minusMinutes(1));
      SystemEvent systemEvent =
          SystemEvent.create()
              .id("pin-event")
              .roomId(ROOM_ID)
              .type("MESSAGE_PINNED")
              .actorId(USER_ID)
              .createdAt(pinnedAt);
      when(messageRepository.getById(MESSAGE_ID)).thenReturn(Optional.of(message));
      when(pinRepository.pin(ROOM_ID, MESSAGE_ID, USER_ID))
          .thenReturn(
              Optional.of(MessagePin.create(ROOM_ID, MESSAGE_ID, USER_ID).pinnedAt(pinnedAt)));
      when(systemEventRepository.insertAt(any(SystemEvent.class), eq(pinnedAt)))
          .thenReturn(systemEvent);

      PinResult result = chatMessageService.pinMessage(ROOM_ID, MESSAGE_ID, USER_ID);

      assertSame(systemEvent, result.systemEvent());
      assertSame(message, result.message());
      verify(pinRepository, never()).getPinnedMessages(anyString());
      ArgumentCaptor<WsEvent> event = ArgumentCaptor.forClass(WsEvent.class);
      ArgumentCaptor<TimelineMessageDto> broadcastMessage =
          ArgumentCaptor.forClass(TimelineMessageDto.class);
      verify(messageReceivedBroadcaster)
          .broadcastWithReplyVisibility(eq(ROOM_ID), event.capture(), broadcastMessage.capture());
      MessagePinned pinned = (MessagePinned) event.getValue();
      assertEquals(MESSAGE_ID, pinned.getMessageId());
      assertEquals(USER_ID, pinned.getPinnedBy());
      assertEquals("pin-event", pinned.getSystemEventId());
      assertEquals(pinnedAt, pinned.getSystemEventCreatedAt());
      assertEquals("pinned text", broadcastMessage.getValue().getText());
    }

    @Test
    @DisplayName(
        "deleteMessage of the room pin sends MessageDeleted floor-filtered and MessageUnpinned to"
            + " all members")
    void deleteMessage_pinned_sendsDeletedThenUnpinned() throws Exception {
      OffsetDateTime unpinnedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");
      OffsetDateTime deletedAt = OffsetDateTime.parse("2026-10-30T10:00:00.5Z");
      OffsetDateTime messageCreatedAt = OffsetDateTime.parse("2026-10-30T09:00:00Z");
      List<String> members = List.of(USER_ID, newId());
      when(subscriptionRepository.getUserIdsByRoomId(ROOM_ID)).thenReturn(members);
      when(messageWriteRepository.deleteMessage(MESSAGE_ID, ROOM_ID, USER_ID))
          .thenReturn(
              new DeleteResult(
                  DeleteStatus.DELETED,
                  MESSAGE_ID,
                  null,
                  deletedAt,
                  messageCreatedAt,
                  new Unpinned("unpin-event", unpinnedAt)));

      chatMessageService.deleteMessage(ROOM_ID, MESSAGE_ID, user);

      ArgumentCaptor<String> deletedPayload = ArgumentCaptor.forClass(String.class);
      ArgumentCaptor<String> unpinnedPayload = ArgumentCaptor.forClass(String.class);
      InOrder order = inOrder(messageReceivedBroadcaster, eventDispatcher);
      order
          .verify(messageReceivedBroadcaster)
          .broadcastToMembersWhoCanAccess(
              eq(ROOM_ID), eq(messageCreatedAt), deletedPayload.capture());
      order
          .verify(eventDispatcher, times(1))
          .sendRawToUserExchange(eq(members), unpinnedPayload.capture());
      JsonNode deleted = objectMapper.readTree(deletedPayload.getValue());
      JsonNode unpinned = objectMapper.readTree(unpinnedPayload.getValue());
      assertEquals("MessageDeleted", deleted.get("type").asText());
      assertEquals(MESSAGE_ID, deleted.get("messageId").asText());
      assertEquals(USER_ID, deleted.get("deletedBy").asText());
      assertEquals(
          deletedAt.toInstant(),
          OffsetDateTime.parse(deleted.get("deletedAt").asText()).toInstant());
      assertEquals("MessageUnpinned", unpinned.get("type").asText());
      assertEquals(
          unpinnedAt.toInstant(),
          OffsetDateTime.parse(unpinned.get("timestamp").asText()).toInstant());
      assertEquals(MESSAGE_ID, unpinned.get("messageId").asText());
      assertEquals(USER_ID, unpinned.get("unpinnedBy").asText());
      assertEquals("unpin-event", unpinned.get("systemEventId").asText());
      assertEquals(
          unpinnedAt.toInstant(),
          OffsetDateTime.parse(unpinned.get("systemEventCreatedAt").asText()).toInstant());
    }

    @Test
    @DisplayName("deleteMessage of a message that is not the room pin sends only MessageDeleted")
    void deleteMessage_notPinned_sendsOnlyDeleted() throws Exception {
      OffsetDateTime messageCreatedAt = OffsetDateTime.parse("2026-10-30T09:00:00Z");
      when(messageWriteRepository.deleteMessage(MESSAGE_ID, ROOM_ID, USER_ID))
          .thenReturn(
              new DeleteResult(
                  DeleteStatus.DELETED,
                  MESSAGE_ID,
                  null,
                  OffsetDateTime.parse("2026-10-30T10:00:00Z"),
                  messageCreatedAt,
                  null));

      chatMessageService.deleteMessage(ROOM_ID, MESSAGE_ID, user);

      ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
      verify(messageReceivedBroadcaster)
          .broadcastToMembersWhoCanAccess(eq(ROOM_ID), eq(messageCreatedAt), payload.capture());
      verifyNoInteractions(eventDispatcher);
      assertEquals(
          "MessageDeleted", objectMapper.readTree(payload.getValue()).get("type").asText());
    }
  }

  @Nested
  @DisplayName("setReaction and removeReaction listeners and events")
  class ReactionListenersAndEvents {

    private final OffsetDateTime targetCreatedAt = OffsetDateTime.parse("2026-10-30T10:00:00Z");

    private void targetExists(String messageId) {
      when(messageRepository.getById(messageId))
          .thenReturn(Optional.of(message(messageId, targetCreatedAt)));
    }

    private void setStores(
        String messageId,
        String reaction,
        SetKind kind,
        String previous,
        OffsetDateTime updatedAt) {
      when(reactionRepository.set(messageId, USER_ID, reaction))
          .thenReturn(new SetResult(kind, previous, updatedAt, null));
    }

    private ReactionChanged captureReactionBroadcast() throws Exception {
      ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
      verify(messageReceivedBroadcaster)
          .broadcastToMembersWhoCanAccess(eq(ROOM_ID), eq(targetCreatedAt), json.capture());
      verifyNoInteractions(eventDispatcher);
      return objectMapper.readValue(json.getValue(), ReactionChanged.class);
    }

    @Test
    @DisplayName("setReaction ADDED notifies the message-sent listeners with the emoji")
    void added_notifiesMessageSentListeners() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "👍", SetKind.ADDED, null, OffsetDateTime.now());

      SetResult result = chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, user, "👍");

      assertEquals(targetCreatedAt, result.messageCreatedAt());
      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "👍", targetCreatedAt);
    }

    @Test
    @DisplayName("setReaction UPDATED notifies the message-sent listeners with the new emoji")
    void updated_notifiesMessageSentListeners() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "❤️", SetKind.UPDATED, "👍", OffsetDateTime.now());

      chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, user, "❤️");

      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "❤️", targetCreatedAt);
    }

    @Test
    @DisplayName("setReaction UNCHANGED notifies the message-sent listeners with the emoji")
    void unchanged_notifiesMessageSentListeners() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "👍", SetKind.UNCHANGED, "👍", OffsetDateTime.now());

      chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, user, "👍");

      verify(messageSentListener, times(1))
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "👍", targetCreatedAt);
    }

    @Test
    @DisplayName("The userId overload of setReaction does not call the listeners")
    void userIdOverload_doesNotNotifyMessageSentListeners() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "👍", SetKind.ADDED, null, OffsetDateTime.now());

      chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, USER_ID, "👍");

      verifyNoInteractions(messageSentListener);
    }

    @Test
    @DisplayName("setReaction returns the result even when a message-sent listener fails")
    void listenerFails_stillSet() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "👍", SetKind.ADDED, null, OffsetDateTime.now());
      doThrow(new RuntimeException("listener failure"))
          .when(messageSentListener)
          .onMessageSent(user, ROOM_UUID, MESSAGE_ID, "👍", targetCreatedAt);

      SetResult result = chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, user, "👍");

      assertEquals(SetKind.ADDED, result.kind());
    }

    @Test
    @DisplayName("removeReaction never notifies the message-sent listeners")
    void remove_doesNotNotifyMessageSentListeners() {
      targetExists(MESSAGE_ID);
      when(reactionRepository.remove(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.of(new Removed("👍", OffsetDateTime.now(), null)));

      Optional<Removed> removed = chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID);

      assertTrue(removed.isPresent());
      verify(messageSentListener, never()).onMessageSent(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("setReaction ADDED and UPDATED broadcast ReactionChanged with the message floor")
    void broadcastsToMembersWhoCanAccess() throws Exception {
      for (SetKind kind : List.of(SetKind.ADDED, SetKind.UPDATED)) {
        clearInvocations(messageReceivedBroadcaster, eventDispatcher, messageRepository);
        String messageId = newId();
        OffsetDateTime updatedAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
        targetExists(messageId);
        setStores(messageId, "❤️", kind, kind == SetKind.UPDATED ? "👍" : null, updatedAt);

        chatMessageService.setReaction(ROOM_ID, messageId, user, "❤️");

        ReactionChanged sent = captureReactionBroadcast();
        assertEquals(messageId, sent.getMessageId());
        assertEquals(ROOM_ID, sent.getRoomId());
        assertEquals(USER_ID, sent.getUserId());
        assertEquals("❤️", sent.getReaction());
        assertEquals(
            kind == SetKind.ADDED
                ? ReactionChanged.OperationEnum.ADDED
                : ReactionChanged.OperationEnum.UPDATED,
            sent.getOperation());
        assertEquals(kind == SetKind.UPDATED ? "👍" : null, sent.getPreviousReaction());
        verify(messageRepository, times(1)).getById(anyString());
      }
    }

    @Test
    @DisplayName("setReaction UNCHANGED broadcasts nothing")
    void unchanged_broadcastsNothing() {
      targetExists(MESSAGE_ID);
      setStores(MESSAGE_ID, "👍", SetKind.UNCHANGED, "👍", null);

      chatMessageService.setReaction(ROOM_ID, MESSAGE_ID, user, "👍");

      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("removeReaction broadcasts ReactionChanged removed with the message floor")
    void remove_broadcastsToMembersWhoCanAccess() throws Exception {
      OffsetDateTime deletedAt = OffsetDateTime.parse("2026-10-30T11:00:00Z");
      targetExists(MESSAGE_ID);
      when(reactionRepository.remove(MESSAGE_ID, USER_ID))
          .thenReturn(Optional.of(new Removed("👍", deletedAt, null)));

      chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID);

      ReactionChanged sent = captureReactionBroadcast();
      assertEquals(ReactionChanged.OperationEnum.REMOVED, sent.getOperation());
      assertEquals(MESSAGE_ID, sent.getMessageId());
      assertEquals(USER_ID, sent.getUserId());
      assertEquals("👍", sent.getReaction());
      verify(messageRepository, times(1)).getById(anyString());
    }

    @Test
    @DisplayName("removeReaction broadcasts nothing when the user had no reaction")
    void remove_noReaction_broadcastsNothing() {
      targetExists(MESSAGE_ID);
      when(reactionRepository.remove(MESSAGE_ID, USER_ID)).thenReturn(Optional.empty());

      chatMessageService.removeReaction(ROOM_ID, MESSAGE_ID, USER_ID);

      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher, messageSentListener);
    }
  }

  @Nested
  @DisplayName("forwardMessage")
  class ForwardMessageTests {

    private final String attachmentId = newId();
    private final String originalSenderId = newId();
    private FileMetadata video;

    @BeforeEach
    void setUpForward() {
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(
                  Message.create()
                      .id(MESSAGE_ID)
                      .roomId(ROOM_ID)
                      .senderId(originalSenderId)
                      .attachmentId(attachmentId)
                      .createdAt(OffsetDateTime.now())));
      video =
          FileMetadata.create()
              .id(attachmentId)
              .name("clip.mp4")
              .mimeType("video/mp4")
              .originalSize(1024L)
              .type(FileMetadataType.ATTACHMENT)
              .userId(originalSenderId)
              .roomId(ROOM_ID);
      when(fileMetadataRepository.getById(attachmentId)).thenReturn(Optional.of(video));
    }

    private ForwardResult forwarded(String newMessageId, String newAttachmentId, String area) {
      return new ForwardResult(
          ForwardStatus.FORWARDED,
          newMessageId,
          OffsetDateTime.now(),
          "",
          originalSenderId,
          OffsetDateTime.now(),
          newAttachmentId,
          "clip.mp4",
          "video/mp4",
          1024L,
          area);
    }

    private Message forward() {
      return chatMessageService.forwardMessage(ROOM_ID, MESSAGE_ID, TARGET_ROOM_ID, user);
    }

    private OngoingStubbing<ForwardResult> whenForwarding() {
      return when(
          messageWriteRepository.forwardMessage(
              eq(ROOM_ID), eq(TARGET_ROOM_ID), eq(USER_ID), eq(MESSAGE_ID), anyString(), any()));
    }

    private FileMetadata savedCopy() {
      ArgumentCaptor<FileMetadata> copy = ArgumentCaptor.forClass(FileMetadata.class);
      verify(fileMetadataRepository).save(copy.capture());
      return copy.getValue();
    }

    @Test
    @DisplayName("Forwarding a video copies its preview to the forwarded copy")
    void video_copiesPreviewToTheCopy() {
      whenForwarding()
          .thenAnswer(
              invocation -> forwarded(invocation.getArgument(4), invocation.getArgument(5), null));

      Message forwardedMessage = forward();

      FileMetadata copy = savedCopy();
      verify(attachmentService).copyVideoPreview(video, copy.getId(), USER_ID);
      verify(storagesService).copyFile(attachmentId, originalSenderId, copy.getId(), USER_ID);
      assertEquals(copy.getId(), forwardedMessage.getAttachmentId());
      assertEquals(TARGET_ROOM_ID, forwardedMessage.getRoomId());
      assertEquals(USER_ID, forwardedMessage.getSenderId());
      assertEquals(Boolean.TRUE, forwardedMessage.isForwarded());
      assertEquals(originalSenderId, forwardedMessage.getForwardedSenderId());
      verify(fileMetadataRepository, never()).delete(any());
      verify(storagesService, never()).deleteFile(any(), any());
    }

    @Test
    @DisplayName("Forwarding an attachment copies its area to the forwarded copy")
    void copiesTheAreaToTheCopy() {
      video.area("640x480");
      whenForwarding()
          .thenAnswer(
              invocation ->
                  forwarded(invocation.getArgument(4), invocation.getArgument(5), "640x480"));

      forward();

      FileMetadata copy = savedCopy();
      assertEquals("640x480", copy.getArea());
      verify(attachmentService).copyVideoPreview(video, copy.getId(), USER_ID);
    }

    @Test
    @DisplayName("Forwarding broadcasts MessageForwarded to the members of the target room")
    void broadcastsToTargetRoomMembers() throws Exception {
      List<String> members = List.of(USER_ID, newId());
      when(subscriptionRepository.getUserIdsByRoomId(TARGET_ROOM_ID)).thenReturn(members);
      whenForwarding()
          .thenAnswer(
              invocation -> forwarded(invocation.getArgument(4), invocation.getArgument(5), null));

      Message forwardedMessage = forward();

      ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
      verify(eventDispatcher).sendRawToUserExchange(eq(members), payload.capture());
      JsonNode event = objectMapper.readTree(payload.getValue());
      assertEquals("MessageForwarded", event.get("type").asText());
      assertEquals(forwardedMessage.getId(), event.get("message").get("id").asText());
    }

    @Test
    @DisplayName("No preview copy when the forward is rejected and the copied file is removed")
    void forwardFails_noPreviewCopy() {
      whenForwarding().thenReturn(ForwardResult.rejected(ForwardStatus.NOT_MEMBER));

      assertThrows(ForbiddenException.class, this::forward);

      FileMetadata copy = savedCopy();
      verify(attachmentService, never()).copyVideoPreview(any(), anyString(), anyString());
      verify(storagesService).deleteFile(copy.getId(), USER_ID);
      verify(fileMetadataRepository).delete(copy);
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("When the write throws the copied file and its metadata are removed")
    void writeThrows_cleansUpAndRethrows() {
      IllegalStateException failure = new IllegalStateException("db down");
      whenForwarding().thenThrow(failure);

      IllegalStateException thrown = assertThrows(IllegalStateException.class, this::forward);

      assertSame(failure, thrown);
      FileMetadata copy = savedCopy();
      verify(storagesService).deleteFile(copy.getId(), USER_ID);
      verify(fileMetadataRepository).delete(copy);
      verify(attachmentService, never()).copyVideoPreview(any(), anyString(), anyString());
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher, messageSentListener);
    }

    @Test
    @DisplayName("When the write throws and the cleanup fails too the original error is rethrown")
    void writeThrows_cleanupFails_originalRethrown() {
      IllegalStateException failure = new IllegalStateException("db down");
      whenForwarding().thenThrow(failure);
      doThrow(new RuntimeException("storage down")).when(storagesService).deleteFile(any(), any());
      doThrow(new RuntimeException("db down again")).when(fileMetadataRepository).delete(any());

      IllegalStateException thrown = assertThrows(IllegalStateException.class, this::forward);

      assertSame(failure, thrown);
      assertEquals(1, thrown.getSuppressed().length);
      verify(storagesService).deleteFile(any(), eq(USER_ID));
      verify(fileMetadataRepository).delete(any());
    }

    @Test
    @DisplayName("NOT_MEMBER is Forbidden with the source/target/message text")
    void notMember_forbidden() {
      whenForwarding().thenReturn(ForwardResult.rejected(ForwardStatus.NOT_MEMBER));

      assertRejected(
          ForbiddenException.class,
          "Not a member of source/target room or message not found",
          this::forward);
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher, messageSentListener);
    }

    @Test
    @DisplayName(
        "MESSAGE_NOT_FOUND is NotFound \"Message '<id>' not found\" and the copy is removed")
    void messageNotFound_notFound() {
      whenForwarding().thenReturn(ForwardResult.rejected(ForwardStatus.MESSAGE_NOT_FOUND));

      assertRejected(
          NotFoundException.class,
          String.format("Message '%s' not found", MESSAGE_ID),
          this::forward);

      FileMetadata copy = savedCopy();
      verify(storagesService).deleteFile(copy.getId(), USER_ID);
      verify(fileMetadataRepository).delete(copy);
      verifyNoInteractions(messageReceivedBroadcaster, eventDispatcher);
    }

    @Test
    @DisplayName("A rejected forward of a message without attachment removes nothing")
    void rejected_withoutAttachment_removesNothing() {
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(
                  Message.create()
                      .id(MESSAGE_ID)
                      .roomId(ROOM_ID)
                      .senderId(originalSenderId)
                      .createdAt(OffsetDateTime.now())));
      whenForwarding().thenReturn(ForwardResult.rejected(ForwardStatus.NOT_MEMBER));

      assertThrows(ForbiddenException.class, this::forward);

      verifyNoInteractions(storagesService);
      verify(fileMetadataRepository, never()).save(any());
      verify(fileMetadataRepository, never()).delete(any());
    }

    @Test
    @DisplayName("A message of another room is NotFound and nothing is copied or written")
    void messageOfAnotherRoom_notFound() {
      when(messageRepository.getById(MESSAGE_ID))
          .thenReturn(
              Optional.of(
                  Message.create()
                      .id(MESSAGE_ID)
                      .roomId(OTHER_ROOM_ID)
                      .senderId(originalSenderId)
                      .attachmentId(attachmentId)
                      .createdAt(OffsetDateTime.now())));

      assertRejected(
          NotFoundException.class,
          String.format("Message '%s' not found", MESSAGE_ID),
          this::forward);

      verifyNoInteractions(messageWriteRepository, storagesService);
    }

    @Test
    @DisplayName("A message at or before the cleared history is NotFound and nothing is written")
    void messageBelowFloor_notFound() {
      OffsetDateTime clearedAt = OffsetDateTime.now().plusDays(1);
      givenFloor(clearedAt);

      assertRejected(
          NotFoundException.class,
          String.format("Message '%s' not found", MESSAGE_ID),
          this::forward);

      verifyNoInteractions(messageWriteRepository, storagesService);
    }

    @Test
    @DisplayName("A non-member of the target room is Forbidden and nothing is written")
    void notMemberOfTarget_forbidden() {
      when(subscriptionRepository.getMembership(TARGET_ROOM_ID, USER_ID))
          .thenReturn(Optional.empty());
      when(roomRepository.getById(TARGET_ROOM_ID)).thenReturn(Optional.of(room()));

      assertThrows(ForbiddenException.class, this::forward);

      verifyNoInteractions(messageWriteRepository, storagesService);
    }
  }

  @Nested
  @DisplayName("getTimeline")
  class GetTimelineTests {

    private final String otherUserId = newId();

    private TimelineView latest(int limit) {
      return chatMessageService.getTimeline(
          ROOM_ID, USER_ID, null, null, null, null, null, null, limit);
    }

    private List<Message> makeMessages(int count, OffsetDateTime newestTs) {
      List<Message> list = new ArrayList<>();
      for (int i = 0; i < count; i++) {
        list.add(
            Message.create()
                .id("msg-" + String.format("%04d", count - i))
                .roomId(ROOM_ID)
                .senderId(USER_ID)
                .createdAt(newestTs.minusSeconds(i)));
      }
      return list;
    }

    private SystemEvent makeRoomCreatedEvent(OffsetDateTime ts) {
      return SystemEvent.create()
          .id("evt-room-created")
          .roomId(ROOM_ID)
          .type("ROOM_CREATED")
          .actorId(USER_ID)
          .createdAt(ts);
    }

    private SystemEvent makeSe(String id, OffsetDateTime ts) {
      return SystemEvent.create().id(id).roomId(ROOM_ID).type("PIN").createdAt(ts);
    }

    private Message original(OffsetDateTime createdAt) {
      return Message.create()
          .id("orig")
          .roomId(ROOM_ID)
          .senderId(otherUserId)
          .text("original text")
          .createdAt(createdAt);
    }

    private Message reply(OffsetDateTime createdAt) {
      return Message.create()
          .id("reply")
          .roomId(ROOM_ID)
          .senderId(USER_ID)
          .text("the reply")
          .createdAt(createdAt)
          .replyToId("orig");
    }

    private ChatMessageTimelineItemDto firstMessageDto(TimelineView view) {
      MessageContext context = view.context();
      return (ChatMessageTimelineItemDto)
          timelineItemMapper.toTimelineItem(
              view.page().items().get(0), context.attachmentsById(), context.repliedById());
    }

    @Test
    @DisplayName(
        "Default branch: 50 messages + ROOM_CREATED -> 50 items total, hasMoreBefore=true, oldest"
            + " item is MESSAGE")
    void defaultBranch_50MessagesAndRoomCreated_returns50ItemsWithHasMore() {
      OffsetDateTime newestMsg = OffsetDateTime.parse("2026-01-01T12:00:50Z");
      OffsetDateTime roomCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), isNull(), eq(51)))
          .thenReturn(makeMessages(50, newestMsg));
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), isNull(), anyInt()))
          .thenReturn(List.of(makeRoomCreatedEvent(roomCreatedAt)));

      TimelineView view = latest(50);

      assertEquals(50, view.page().items().size(), "merged+trimmed list must be exactly 50");
      assertTrue(
          view.page().hasMoreBefore(), "hasMoreBefore must be true when merged exceeded limit");
      assertInstanceOf(
          TimelineItem.MessageItem.class,
          view.page().items().get(0),
          "oldest kept item must be a MESSAGE (ROOM_CREATED trimmed off)");
      assertEquals(List.of(), view.markers().markers());
    }

    @Test
    @DisplayName(
        "After branch: forward-skip - when more events exist than fetchLimit, oldest events after"
            + " anchor are returned (not newest)")
    void afterBranch_forwardSkip_oldestEventsReturnedNotNewest() {
      OffsetDateTime anchor = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      OffsetDateTime t1 = anchor.plusSeconds(1);
      OffsetDateTime t2 = anchor.plusSeconds(2);
      OffsetDateTime t3 = anchor.plusSeconds(3);
      OffsetDateTime t4 = anchor.plusSeconds(4);
      OffsetDateTime t5 = anchor.plusSeconds(5);
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), eq(anchor), eq(3)))
          .thenReturn(List.of(makeSe("evt-t5", t5), makeSe("evt-t4", t4), makeSe("evt-t3", t3)));
      when(systemEventRepository.findByRoomIdForward(
              eq(ROOM_ID), eq(anchor), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("evt-t1", t1), makeSe("evt-t2", t2), makeSe("evt-t3", t3)));

      TimelineView view =
          chatMessageService.getTimeline(ROOM_ID, USER_ID, null, null, anchor, null, null, null, 2);

      assertEquals(2, view.page().items().size());
      assertEquals(
          t1,
          view.page().items().get(0).createdAt(),
          "oldest event (T1) after anchor must be first - DESC fetch returns newest (T3+) instead");
    }

    @Test
    @DisplayName("After branch: equal-timestamp items are ordered stably by id (ASC tiebreak)")
    void afterBranch_equalTimestamp_idTiebreakDeterminesOrder() {
      OffsetDateTime anchor = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      OffsetDateTime t1 = anchor.plusSeconds(1);
      Message msg = Message.create().id("msg-zzz").roomId(ROOM_ID).senderId(USER_ID).createdAt(t1);
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), eq(anchor), eq(3), eq(true)))
          .thenReturn(List.of(msg));
      SystemEvent se = makeSe("evt-aaa", t1);
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), eq(anchor), eq(3)))
          .thenReturn(List.of(se));
      when(systemEventRepository.findByRoomIdForward(
              eq(ROOM_ID), eq(anchor), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(se));

      TimelineView view =
          chatMessageService.getTimeline(ROOM_ID, USER_ID, null, null, anchor, null, null, null, 2);

      assertEquals(2, view.page().items().size());
      assertInstanceOf(
          TimelineItem.SystemEventItem.class,
          view.page().items().get(0),
          "id 'evt-aaa' < 'msg-zzz' so SYSTEM_EVENT must sort first - fails without id tiebreak");
    }

    @Test
    @DisplayName(
        "Around branch: only events closest to aroundTs are kept on each side, not arbitrary"
            + " room-wide events")
    void aroundBranch_closestEventsKept_notArbitraryRoomWide() {
      OffsetDateTime aroundTs = OffsetDateTime.parse("2026-01-01T12:00:05Z");
      OffsetDateTime t2 = OffsetDateTime.parse("2026-01-01T12:00:02Z");
      OffsetDateTime t3 = OffsetDateTime.parse("2026-01-01T12:00:03Z");
      OffsetDateTime t4 = OffsetDateTime.parse("2026-01-01T12:00:04Z");
      OffsetDateTime t6 = OffsetDateTime.parse("2026-01-01T12:00:06Z");
      OffsetDateTime t7 = OffsetDateTime.parse("2026-01-01T12:00:07Z");
      OffsetDateTime t8 = OffsetDateTime.parse("2026-01-01T12:00:08Z");
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), isNull(), anyInt()))
          .thenReturn(
              List.of(makeSe("e8", t8), makeSe("e7", t7), makeSe("e6", t6), makeSe("e4", t4)));
      when(systemEventRepository.findByRoomIdBounded(
              eq(ROOM_ID), any(OffsetDateTime.class), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("e4", t4), makeSe("e3", t3), makeSe("e2", t2)));
      when(systemEventRepository.findByRoomIdForward(
              eq(ROOM_ID), eq(aroundTs.plusNanos(999_000)), isNull(), isNull(), eq(3)))
          .thenReturn(List.of(makeSe("e6", t6), makeSe("e7", t7), makeSe("e8", t8)));

      TimelineView view =
          chatMessageService.getTimeline(
              ROOM_ID, USER_ID, null, null, null, null, aroundTs, null, 4);

      assertEquals(
          4,
          view.page().items().size(),
          "around(limit=4,half=2): 2 closest older + 2 closest newer = 4 items");
      assertEquals(
          t3,
          view.page().items().get(0).createdAt(),
          "oldest kept older event must be T3 (closest: T4, T3)");
      assertEquals(
          t6,
          view.page().items().get(2).createdAt(),
          "first newer event must be T6 (closest newer to aroundTs)");
    }

    @Test
    @DisplayName(
        "Default branch: 51 messages + ROOM_CREATED -> exactly 50 items total, no over-count")
    void defaultBranch_51MessagesAndRoomCreated_noOverCount() {
      OffsetDateTime newestMsg = OffsetDateTime.parse("2026-01-01T12:00:51Z");
      OffsetDateTime roomCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), isNull(), eq(51)))
          .thenReturn(makeMessages(51, newestMsg));
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), isNull(), anyInt()))
          .thenReturn(List.of(makeRoomCreatedEvent(roomCreatedAt)));

      TimelineView view = latest(50);

      assertEquals(50, view.page().items().size(), "must never return more than limit items");
      assertTrue(view.page().hasMoreBefore());
    }

    @Test
    @DisplayName("Attachments are batch-fetched via getByIds once, never per-message getById")
    void attachments_batchedNotPerMessage() {
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      Message m1 =
          Message.create()
              .id("m1")
              .roomId(ROOM_ID)
              .senderId(USER_ID)
              .createdAt(t)
              .attachmentId("att-1");
      Message m2 =
          Message.create()
              .id("m2")
              .roomId(ROOM_ID)
              .senderId(USER_ID)
              .createdAt(t.plusSeconds(1))
              .attachmentId("att-2");
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), isNull(), eq(51)))
          .thenReturn(List.of(m2, m1));
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), isNull(), anyInt()))
          .thenReturn(List.of());
      when(fileMetadataRepository.getByIds(any()))
          .thenReturn(
              List.of(
                  FileMetadata.create()
                      .id("att-1")
                      .name("a.png")
                      .mimeType("image/png")
                      .originalSize(10L),
                  FileMetadata.create()
                      .id("att-2")
                      .name("b.png")
                      .mimeType("image/png")
                      .originalSize(20L)));

      TimelineView view = latest(50);

      verify(fileMetadataRepository, times(1)).getByIds(any());
      verify(fileMetadataRepository, times(0)).getById(anyString());
      assertEquals(2, view.page().items().size());
      assertEquals(Set.of("att-1", "att-2"), view.context().attachmentsById().keySet());
    }

    @Test
    @DisplayName("Reply is exploded: original embedded depth-1, batch-fetched once")
    void reply_isExplodedDepth1() {
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), isNull(), eq(51)))
          .thenReturn(List.of(reply(t.plusSeconds(5))));
      when(systemEventRepository.findByRoomIdBounded(eq(ROOM_ID), isNull(), isNull(), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(ROOM_ID), any()))
          .thenReturn(List.of(original(t)));

      TimelineView view = latest(50);

      verify(messageRepository, times(1)).getByRoomIdAndIds(eq(ROOM_ID), any());
      ChatMessageTimelineItemDto item = firstMessageDto(view);
      assertEquals("orig", item.getMessage().getReplyToId());
      assertEquals("original text", item.getMessage().getRepliedMessage().getText());
      assertEquals(otherUserId, item.getMessage().getRepliedMessage().getSenderId());
      assertNull(
          item.getMessage().getRepliedMessage().getRepliedMessage(),
          "depth-1: the embedded original must not carry its own repliedMessage");
    }

    @Test
    @DisplayName("Reply target at or before the caller's cleared-at is NOT exploded")
    void reply_belowClearedAt_notExploded() {
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      OffsetDateTime clearedAt = t.plusSeconds(3);
      givenFloor(clearedAt);
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), eq(clearedAt), eq(51)))
          .thenReturn(List.of(reply(t.plusSeconds(10))));
      when(systemEventRepository.findByRoomIdBounded(
              eq(ROOM_ID), isNull(), eq(clearedAt), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(ROOM_ID), any()))
          .thenReturn(List.of(original(t)));

      TimelineView view = latest(50);

      assertEquals(Map.of(), view.context().repliedById());
      ChatMessageTimelineItemDto item = firstMessageDto(view);
      assertNull(item.getMessage().getReplyToId());
      assertNull(item.getMessage().getRepliedMessage());
    }

    @Test
    @DisplayName("Reply target after the caller's cleared-at is exploded")
    void reply_afterClearedAt_exploded() {
      OffsetDateTime t = OffsetDateTime.parse("2026-01-01T12:00:00Z");
      OffsetDateTime clearedAt = t.minusSeconds(3);
      givenFloor(clearedAt);
      when(messageRepository.getByRoomId(eq(ROOM_ID), isNull(), eq(clearedAt), eq(51)))
          .thenReturn(List.of(reply(t.plusSeconds(10))));
      when(systemEventRepository.findByRoomIdBounded(
              eq(ROOM_ID), isNull(), eq(clearedAt), anyInt()))
          .thenReturn(List.of());
      when(messageRepository.getByRoomIdAndIds(eq(ROOM_ID), any()))
          .thenReturn(List.of(original(t)));

      TimelineView view = latest(50);

      assertEquals(Set.of("orig"), view.context().repliedById().keySet());
      ChatMessageTimelineItemDto item = firstMessageDto(view);
      assertEquals("original text", item.getMessage().getRepliedMessage().getText());
    }

    @Test
    @DisplayName("The timeline carries the read markers of the room with the floor of the member")
    void carriesTheReadMarkersWithTheFloor() {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      givenFloor(clearedAt);
      List<ReadMarker> markers = List.of(ReadMarker.create());
      when(readRepository.getByRoomId(ROOM_ID)).thenReturn(markers);

      TimelineView view = latest(50);

      assertSame(markers, view.markers().markers());
      assertEquals(new VisibilityFloor(clearedAt), view.markers().floor());
    }
  }

  @Nested
  @DisplayName("one membership query per request")
  class OneMembershipQuery {

    private void verifyOneMembershipQuery() {
      verify(subscriptionRepository, times(1)).getMembership(ROOM_ID, USER_ID);
    }

    @Test
    @DisplayName("getPinnedMessagesView runs one membership query")
    void pins() {
      PinnedMessagesView view = chatMessageService.getPinnedMessagesView(ROOM_ID, USER_ID);

      assertEquals(List.of(), view.pins());
      verifyOneMembershipQuery();
    }

    @Test
    @DisplayName("getPinnedMessagesView by a non-member is Forbidden")
    void pins_notMember() {
      givenNonMember();

      assertThrows(
          ForbiddenException.class,
          () -> chatMessageService.getPinnedMessagesView(ROOM_ID, USER_ID));
      verifyNoInteractions(pinRepository);
    }

    @Test
    @DisplayName("getRoomReadMarkers runs one membership query")
    void readMarkers() {
      ReadMarkersView view = chatMessageService.getRoomReadMarkers(ROOM_ID, USER_ID);

      assertEquals(List.of(), view.markers());
      verifyOneMembershipQuery();
    }

    @Test
    @DisplayName("getTimeline runs one membership query")
    void timeline() {
      TimelineView view =
          chatMessageService.getTimeline(ROOM_ID, USER_ID, null, null, null, null, null, null, 50);

      assertEquals(List.of(), view.page().items());
      verifyOneMembershipQuery();
    }

    @Test
    @DisplayName("getTimeline by a non-member is Forbidden and reads nothing")
    void timeline_notMember() {
      givenNonMember();

      assertThrows(
          ForbiddenException.class,
          () ->
              chatMessageService.getTimeline(
                  ROOM_ID, USER_ID, null, null, null, null, null, null, 50));
      verifyNoInteractions(messageRepository, systemEventRepository, readRepository);
    }

    @Test
    @DisplayName("getTimeline in a missing room is NotFound")
    void timeline_missingRoom() {
      givenMissingRoom();

      assertThrows(
          NotFoundException.class,
          () ->
              chatMessageService.getTimeline(
                  ROOM_ID, USER_ID, null, null, null, null, null, null, 50));
      verifyNoInteractions(messageRepository, systemEventRepository, readRepository);
    }

    @Test
    @DisplayName("searchVisibleMessages runs one membership query")
    void search() {
      MessageSearchPage page =
          chatMessageService.searchVisibleMessages(ROOM_ID, USER_ID, "hello", null, null, 10);

      assertEquals(List.of(), page.messages());
      assertFalse(page.hasMore());
      verifyOneMembershipQuery();
    }

    @Test
    @DisplayName("searchVisibleMessages by a non-member is Forbidden and searches nothing")
    void search_notMember() {
      givenNonMember();

      assertThrows(
          ForbiddenException.class,
          () ->
              chatMessageService.searchVisibleMessages(ROOM_ID, USER_ID, "hello", null, null, 10));
      verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName(
        "searchVisibleMessages trims the extra hit, flags hasMore and keeps visible replies")
    void search_hasMoreAndVisibleReplyIds() {
      OffsetDateTime clearedAt = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      givenFloor(clearedAt);
      Message first = message("hit-1", clearedAt.plusDays(2)).replyToId("orig-visible");
      Message second = message("hit-2", clearedAt.plusDays(3)).replyToId("orig-hidden");
      Message third = message("hit-3", clearedAt.plusDays(4));
      when(messageRepository.searchByText(
              eq(ROOM_ID), eq(List.of("hello")), isNull(), eq(clearedAt), eq(3)))
          .thenReturn(List.of(first, second, third));
      when(messageRepository.getByRoomIdAndIds(eq(ROOM_ID), any()))
          .thenReturn(
              List.of(
                  message("orig-visible", clearedAt.plusDays(1)),
                  message("orig-hidden", clearedAt)));

      MessageSearchPage page =
          chatMessageService.searchVisibleMessages(ROOM_ID, USER_ID, "hello", null, null, 2);

      assertEquals(List.of(first, second), page.messages());
      assertTrue(page.hasMore());
      assertEquals(Set.of("orig-visible"), page.visibleReplyIds());
      verifyOneMembershipQuery();
    }
  }
}
