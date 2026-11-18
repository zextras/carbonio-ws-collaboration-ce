// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.MeetingDeleted;
import com.zextras.carbonio.async.model.RoomDeleted;
import com.zextras.carbonio.async.model.RoomMemberRemoved;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.RoomLifecycleService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

@UnitTest
class RoomLifecycleServiceImplTest {

  private static final OffsetDateTime PERSISTED_AT =
      OffsetDateTime.parse("2026-10-23T09:00:00.123456Z");

  private final RoomRepository roomRepository = mock(RoomRepository.class);
  private final SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
  private final RoomUserSettingsRepository roomUserSettingsRepository =
      mock(RoomUserSettingsRepository.class);
  private final MeetingRepository meetingRepository = mock(MeetingRepository.class);
  private final FileMetadataRepository fileMetadataRepository = mock(FileMetadataRepository.class);
  private final MeetingService meetingService = mock(MeetingService.class);
  private final ParticipantService participantService = mock(ParticipantService.class);
  private final AttachmentService attachmentService = mock(AttachmentService.class);
  private final StoragesService storagesService = mock(StoragesService.class);
  private final EventDispatcher eventDispatcher = mock(EventDispatcher.class);
  private final ChatMessageService chatMessageService = mock(ChatMessageService.class);
  private final RoomLifecycleService roomLifecycleService;

  private final UUID user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
  private final UUID user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
  private final UUID user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");
  private final UUID roomId = UUID.fromString("cdc44826-23b0-4e99-bec2-7fb2f00b6b13");
  private final UUID roomGroup1Id = roomId;
  private final UUID roomGroup2Id = UUID.fromString("0471809c-e0bb-4bfd-85b6-b7b9a1eca597");
  private final UUID meetingId = UUID.fromString("aaa32dbf-e6ff-4032-864c-6298abaf5607");

  RoomLifecycleServiceImplTest() {
    RoomAccess roomAccess = new RoomAccessImpl(roomRepository, subscriptionRepository);
    roomLifecycleService =
        new RoomLifecycleServiceImpl(
            roomAccess,
            roomRepository,
            meetingRepository,
            fileMetadataRepository,
            meetingService,
            participantService,
            new MembersServiceImpl(
                roomAccess,
                subscriptionRepository,
                roomUserSettingsRepository,
                eventDispatcher,
                mock(UserService.class),
                chatMessageService,
                new SystemEventContentFactory(new ObjectMapper())),
            attachmentService,
            storagesService,
            eventDispatcher);
  }

  private Room generateRoom(RoomType type) {
    return Room.create()
        .id(roomId.toString())
        .type(type)
        .name("room1")
        .description("Room one")
        .subscriptions(new java.util.ArrayList<>());
  }

  private Room groupRoom1() {
    Room room = Room.create();
    room.id(roomGroup1Id.toString())
        .type(RoomType.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(room, user1Id.toString()).owner(true),
                Subscription.create(room, user2Id.toString()).owner(false),
                Subscription.create(room, user3Id.toString()).owner(false)));
    return room;
  }

  private Room groupRoom2() {
    Room room = Room.create();
    room.id(roomGroup2Id.toString())
        .type(RoomType.GROUP)
        .name("room2")
        .description("Room two")
        .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        .subscriptions(
            List.of(
                Subscription.create(room, user2Id.toString()).owner(true),
                Subscription.create(room, user3Id.toString()).owner(false)));
    return room;
  }

  private FileMetadata avatarOf(Room room, UUID ownerId) {
    return FileMetadata.create()
        .type(FileMetadataType.ROOM_AVATAR)
        .roomId(room.getId())
        .userId(ownerId.toString())
        .mimeType("mime/type")
        .id(UUID.randomUUID().toString())
        .name("pfp")
        .originalSize(123L);
  }

  private void insertSetsCreatedAt() {
    when(chatMessageService.insertSystemEvent(any(SystemEvent.class)))
        .thenAnswer(i -> i.<SystemEvent>getArgument(0).createdAt(PERSISTED_AT));
  }

  private SystemEvent capturedSystemEvent() {
    ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
    verify(chatMessageService).insertSystemEvent(captor.capture());
    return captor.getValue();
  }

  @Nested
  @DisplayName("Removes a member from a room tests")
  class RemoveMemberTests {

    @AfterEach
    void noUnexpectedInteractions() {
      verifyNoMoreInteractions(
          subscriptionRepository,
          roomUserSettingsRepository,
          eventDispatcher,
          chatMessageService,
          meetingService,
          participantService,
          attachmentService);
    }

    @Test
    @DisplayName("Correctly removes a member of a group room")
    void removeMember_groupTestOk() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      roomLifecycleService.removeMember(roomId, user2Id, principal);

      verify(roomRepository, times(1)).getById(roomId.toString());
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      SystemEvent removed = capturedSystemEvent();
      assertEquals("MEMBER_REMOVED", removed.getType());
      assertEquals(user1Id.toString(), removed.getActorId());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user2Id.toString());
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomMemberRemoved.create()
                  .roomId(roomId)
                  .userId(user2Id)
                  .removedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("Correctly removes a member of a group room during a meeting")
    void removeMember_groupTestOkWithMeeting() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      room.meetingId(meetingId.toString());
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));
      Meeting meeting =
          Meeting.create()
              .id(meetingId.toString())
              .roomId(roomId.toString())
              .participants(
                  List.of(
                      Participant.create().userId(user1Id.toString()),
                      Participant.create().userId(user2Id.toString())));
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(Optional.of(meeting));

      roomLifecycleService.removeMember(roomId, user2Id, principal);

      verify(roomRepository, times(1)).getById(roomId.toString());
      verify(meetingService, times(1)).getMeetingEntity(meetingId);
      verify(participantService, times(1)).removeMeetingParticipant(meeting, room, user2Id);
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user2Id.toString());
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomMemberRemoved.create()
                  .roomId(roomId)
                  .userId(user2Id)
                  .removedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("When the last member leaves, the room is deleted and no member removal is done")
    void removeMember_lastMemberLeaves_deletesRoom() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(List.of(Subscription.create(room, user1Id.toString()).owner(true)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      roomLifecycleService.removeMember(roomId, user1Id, principal);

      verify(roomRepository, times(1)).getById(roomId.toString());
      verify(roomRepository, times(1)).delete(roomId.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(List.of(user1Id.toString()), RoomDeleted.create().roomId(roomId));
      verify(attachmentService, times(1))
          .deleteAttachmentsByIds(List.of(), UserPrincipal.create(user1Id));
      verifyNoInteractions(subscriptionRepository, chatMessageService);
      verifyNoMoreInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("Correctly user removes itself of a group room")
    void removeMember_userRemoveItselfTestOk() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(false),
              Subscription.create(room, user2Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      roomLifecycleService.removeMember(roomId, user1Id, principal);

      verify(roomRepository, times(1)).getById(roomId.toString());
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      SystemEvent left = capturedSystemEvent();
      assertEquals("MEMBER_REMOVED", left.getType());
      assertEquals(user1Id.toString(), left.getActorId());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user1Id.toString());
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user1Id.toString());
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomMemberRemoved.create()
                  .roomId(roomId)
                  .userId(user1Id)
                  .removedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("Correctly user removes itself of a group room muted and with history cleared")
    void removeMember_userRemoveItselfTestOkWithMuteAndHistoryCleared() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(false),
              Subscription.create(room, user2Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      RoomUserSettings roomUserSettings =
          RoomUserSettings.create()
              .userId(user1Id.toString())
              .mutedUntil(OffsetDateTime.parse("0001-01-01T00:00:00Z"))
              .clearedAt(OffsetDateTime.now());
      room.userSettings(List.of(roomUserSettings));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), user1Id.toString()))
          .thenReturn(Optional.of(roomUserSettings));

      roomLifecycleService.removeMember(roomId, user1Id, principal);

      verify(roomRepository, times(1)).getById(roomId.toString());
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1)).delete(roomUserSettings);
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user1Id.toString());
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomMemberRemoved.create()
                  .roomId(roomId)
                  .userId(user1Id)
                  .removedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("If user is the last room owner, it throws a 'bad request' exception")
    void removeMember_userIsTheLastOwner() {
      Room room = generateRoom(RoomType.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () -> roomLifecycleService.removeMember(roomId, user1Id, principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Last owner can't leave the room", exception.getMessage());

      verify(roomRepository, times(1)).getById(roomId.toString());
      verifyNoInteractions(
          subscriptionRepository,
          roomUserSettingsRepository,
          eventDispatcher,
          chatMessageService,
          participantService);
    }

    @Test
    @DisplayName("A non-owner removing another member gets 'forbidden' and nothing is changed")
    void removeMember_nonOwnerRemovesAnotherMember_isForbidden() {
      Room room = groupRoom1();
      UserPrincipal principal = UserPrincipal.create(user2Id);
      when(roomRepository.getById(roomId.toString())).thenReturn(Optional.of(room));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomLifecycleService.removeMember(roomId, user3Id, principal));
      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());

      verify(roomRepository, times(1)).getById(roomId.toString());
      verifyNoInteractions(
          subscriptionRepository,
          roomUserSettingsRepository,
          eventDispatcher,
          chatMessageService,
          participantService);
    }
  }

  @Nested
  @DisplayName("Delete room tests")
  class DeleteRoomTests {

    @Test
    @DisplayName("Deletes the required group room")
    void deleteRoom_groupTestOk() {
      Room room = groupRoom1();
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(room));

      roomLifecycleService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).delete(roomGroup1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup1Id));
      verify(attachmentService, times(1))
          .deleteAttachmentsByIds(List.of(), UserPrincipal.create(user1Id));
      verifyNoMoreInteractions(storagesService, meetingService, eventDispatcher);
      verifyNoInteractions(meetingRepository);
    }

    @Test
    @DisplayName("Deletes the required group room and the related room picture")
    void deleteRoom_groupWithRoomPictureTestOk() {
      Room room = groupRoom2();
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(room));
      FileMetadata pfpMetadata = avatarOf(room, user2Id);
      when(fileMetadataRepository.find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));

      roomLifecycleService.deleteRoom(roomGroup2Id, UserPrincipal.create(user2Id));

      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile(pfpMetadata.getId(), user2Id.toString());
      verify(fileMetadataRepository, times(1)).delete(pfpMetadata);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup2Id));
      verifyNoMoreInteractions(storagesService, meetingService, eventDispatcher);
    }

    @Test
    @DisplayName("Deletes the required group room and the associated meeting")
    void deleteRoom_groupWithMeetingTestOk() {
      Room room = groupRoom1().meetingId(meetingId.toString());
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(room));
      Meeting meeting = Meeting.create().id(meetingId.toString()).roomId(roomGroup1Id.toString());
      meeting.participants(
          List.of(
              Participant.create().userId(user1Id.toString()),
              Participant.create().userId(user3Id.toString())));
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(Optional.of(meeting));

      roomLifecycleService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(meetingService, times(1)).getMeetingEntity(meetingId);
      verify(meetingService, times(1)).releaseMeetingResources(user1Id.toString(), meeting);
      verify(meetingRepository, times(1)).delete(meeting);
      List<String> receivers = List.of(user1Id.toString(), user2Id.toString(), user3Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(receivers, MeetingDeleted.create().meetingId(meetingId));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(receivers, RoomDeleted.create().roomId(roomGroup1Id));
      verifyNoMoreInteractions(storagesService, meetingService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "A storage failure on the room picture after the commit is logged: the room stays deleted"
            + " and its events are sent")
    void deleteRoom_storageExceptionDuringDeletingRoomPicture() {
      Room room = groupRoom2();
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(room));
      FileMetadata pfpMetadata = avatarOf(room, user2Id);
      when(fileMetadataRepository.find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      doThrow(StorageException.class)
          .when(storagesService)
          .deleteFile(pfpMetadata.getId(), user2Id.toString());

      roomLifecycleService.deleteRoom(roomGroup2Id, UserPrincipal.create(user2Id));

      verify(roomRepository, times(1)).delete(roomGroup2Id.toString());
      verify(storagesService, times(1)).deleteFile(pfpMetadata.getId(), user2Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup2Id));
    }

    @Test
    @DisplayName(
        "A failure deleting the attachment rows after the commit is logged: the room stays deleted"
            + " and its events are sent")
    void deleteRoom_attachmentCleanupFails_isLogged() {
      Room room = groupRoom1();
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(room));
      doThrow(new IllegalStateException("db"))
          .when(attachmentService)
          .deleteAttachmentsByIds(List.of(), UserPrincipal.create(user1Id));

      roomLifecycleService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).delete(roomGroup1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup1Id));
      verify(attachmentService, times(1))
          .deleteAttachmentsByIds(List.of(), UserPrincipal.create(user1Id));
    }

    @Test
    @DisplayName("If the room doesn't exist then throws a 'not found' exception")
    void deleteRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomLifecycleService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());

      verifyNoInteractions(
          fileMetadataRepository, storagesService, meetingService, eventDispatcher);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't a room member then throws a 'forbidden' exception")
    void deleteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(groupRoom2()));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomLifecycleService.deleteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verifyNoInteractions(
          fileMetadataRepository, storagesService, meetingService, eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner then throws a 'forbidden' exception")
    void deleteRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(groupRoom1()));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomLifecycleService.deleteRoom(roomGroup1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
          exception.getMessage());

      verifyNoInteractions(
          fileMetadataRepository, storagesService, meetingService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Delete cascade ordering")
  class CascadeOrderingTests {

    private final UserPrincipal actor = UserPrincipal.create(user2Id);
    private final List<String> attachmentIds = List.of("att-1", "att-2");
    private Room room;
    private Meeting meeting;
    private FileMetadata avatar;

    private void roomWithMeetingPictureAndAttachments() {
      room = groupRoom2().meetingId(meetingId.toString());
      meeting = Meeting.create().id(meetingId.toString()).roomId(room.getId());
      avatar = avatarOf(room, user2Id);
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(Optional.of(meeting));
      when(fileMetadataRepository.getIdsByRoomIdAndType(room.getId(), FileMetadataType.ATTACHMENT))
          .thenReturn(attachmentIds);
      when(fileMetadataRepository.find(null, room.getId(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(avatar));
    }

    @Test
    @DisplayName("Every database step runs before any storage call or event, which follow in order")
    void databaseStepsBeforeIo() {
      roomWithMeetingPictureAndAttachments();

      roomLifecycleService.deleteRoom(room, actor);

      List<String> receivers = List.of(user2Id.toString(), user3Id.toString());
      InOrder order =
          inOrder(
              meetingService,
              meetingRepository,
              fileMetadataRepository,
              roomRepository,
              eventDispatcher,
              attachmentService,
              storagesService);
      order.verify(meetingService).releaseMeetingResources(user2Id.toString(), meeting);
      order.verify(meetingRepository).delete(meeting);
      order
          .verify(fileMetadataRepository)
          .getIdsByRoomIdAndType(room.getId(), FileMetadataType.ATTACHMENT);
      order.verify(fileMetadataRepository).find(null, room.getId(), FileMetadataType.ROOM_AVATAR);
      order.verify(fileMetadataRepository).delete(avatar);
      order.verify(roomRepository).delete(room.getId());
      order
          .verify(eventDispatcher)
          .sendToUserExchange(receivers, MeetingDeleted.create().meetingId(meetingId));
      order
          .verify(eventDispatcher)
          .sendToUserExchange(receivers, RoomDeleted.create().roomId(roomGroup2Id));
      order.verify(attachmentService).deleteAttachmentsByIds(attachmentIds, actor);
      order.verify(storagesService).deleteFile(avatar.getId(), user2Id.toString());
      order.verifyNoMoreInteractions();
    }

    @Test
    @DisplayName("No storage call, event or video-server call happens while the rows are deleted")
    void noIoInsideTheDatabaseStep() {
      roomWithMeetingPictureAndAttachments();
      AtomicBoolean checked = new AtomicBoolean();
      org.mockito.stubbing.Answer<Object> noIoYet =
          invocation -> {
            verifyNoInteractions(eventDispatcher, storagesService, attachmentService);
            verify(meetingService).releaseMeetingResources(user2Id.toString(), meeting);
            checked.set(true);
            return null;
          };
      doAnswer(noIoYet).when(meetingRepository).delete(meeting);
      doAnswer(noIoYet).when(fileMetadataRepository).delete(avatar);
      doAnswer(noIoYet).when(roomRepository).delete(room.getId());

      roomLifecycleService.deleteRoom(room, actor);

      assertTrue(checked.get());
      verify(eventDispatcher, times(2)).sendToUserExchange(any(List.class), any());
    }

    @Test
    @DisplayName(
        "A failure of the video-server release aborts the cascade before any row is touched")
    void releaseFailureAbortsBeforeTheDatabase() {
      roomWithMeetingPictureAndAttachments();
      doThrow(new StorageException())
          .when(meetingService)
          .releaseMeetingResources(user2Id.toString(), meeting);

      assertThrows(StorageException.class, () -> roomLifecycleService.deleteRoom(room, actor));

      verifyNoInteractions(
          meetingRepository,
          fileMetadataRepository,
          roomRepository,
          eventDispatcher,
          storagesService,
          attachmentService);
    }

    @Test
    @DisplayName("A failure of a database step sends no event and calls no storage")
    void databaseFailureSendsNothing() {
      roomWithMeetingPictureAndAttachments();
      doThrow(new IllegalStateException("db")).when(roomRepository).delete(room.getId());

      assertThrows(IllegalStateException.class, () -> roomLifecycleService.deleteRoom(room, actor));

      verifyNoInteractions(eventDispatcher, storagesService, attachmentService);
    }

    @Test
    @DisplayName("A room without meeting or picture touches neither the meeting nor the picture")
    void plainRoom() {
      Room plain = groupRoom1();
      when(fileMetadataRepository.getIdsByRoomIdAndType(plain.getId(), FileMetadataType.ATTACHMENT))
          .thenReturn(attachmentIds);

      roomLifecycleService.deleteRoom(plain, UserPrincipal.create(user1Id));

      verifyNoInteractions(meetingService, meetingRepository, storagesService);
      verify(fileMetadataRepository, never()).find(any(), any(), any());
      verify(attachmentService)
          .deleteAttachmentsByIds(attachmentIds, UserPrincipal.create(user1Id));
    }
  }
}
