// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.RoomCreated;
import com.zextras.carbonio.async.model.RoomDescriptionChanged;
import com.zextras.carbonio.async.model.RoomHistoryCleared;
import com.zextras.carbonio.async.model.RoomMuted;
import com.zextras.carbonio.async.model.RoomNameChanged;
import com.zextras.carbonio.async.model.RoomPictureChanged;
import com.zextras.carbonio.async.model.RoomPictureDeleted;
import com.zextras.carbonio.async.model.RoomUnmuted;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.Membership;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.ws.rs.core.Response.Status;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
class RoomServiceImplTest {

  private final RoomService roomService;
  private final AttachmentService attachmentService;
  private final RoomRepository roomRepository;
  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final FileMetadataRepository fileMetadataRepository;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final StoragesService storagesService;
  private final Clock clock;
  private final ChatMessageService chatMessageService;

  public RoomServiceImplTest() {
    this.roomRepository = mock(RoomRepository.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.messageRepository = mock(MessageRepository.class);
    this.systemEventRepository = mock(SystemEventRepository.class);
    this.userService = mock(UserService.class);
    this.membersService = mock(MembersService.class);
    this.meetingService = mock(MeetingService.class);
    this.storagesService = mock(StoragesService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.clock = mock(Clock.class);
    this.chatMessageService = mock(ChatMessageService.class);
    this.roomService =
        new RoomServiceImpl(
            this.roomRepository,
            this.roomUserSettingsRepository,
            this.fileMetadataRepository,
            this.messageRepository,
            this.systemEventRepository,
            new RoomAccessImpl(this.roomRepository, this.subscriptionRepository),
            this.userService,
            this.membersService,
            this.meetingService,
            this.storagesService,
            this.attachmentService,
            this.eventDispatcher,
            this.chatMessageService,
            new ObjectMapper(),
            new SystemEventContentFactory(new ObjectMapper()));
  }

  private void insertIncreasesCreatedAt() {
    AtomicInteger persistedLines = new AtomicInteger();
    when(chatMessageService.insertSystemEvent(any(SystemEvent.class)))
        .thenAnswer(
            i ->
                i.<SystemEvent>getArgument(0)
                    .createdAt(
                        OffsetDateTime.parse("2026-01-01T10:00:00Z")
                            .plusNanos(1_000L * persistedLines.incrementAndGet())));
  }

  private List<SystemEvent> insertedSystemEvents(int expected) {
    ArgumentCaptor<SystemEvent> captor = ArgumentCaptor.forClass(SystemEvent.class);
    verify(chatMessageService, times(expected)).insertSystemEvent(captor.capture());
    return captor.getAllValues();
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID roomGroup1Id;
  private UUID roomGroup2Id;
  private UUID roomTemporary1Id;
  private UUID roomTemporary2Id;
  private UUID roomOneToOne1Id;
  private UUID roomOneToOne2Id;

  private UUID meeting1Id;

  private Room roomGroup1;
  private Room roomGroup2;
  private Room roomTemporary1;
  private Room roomTemporary2;
  private Room roomOneToOne1;
  private Room roomOneToOne2;

  private Meeting meeting1;

  @BeforeEach
  public void init() {
    when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
    when(clock.getZone()).thenReturn(ZoneId.systemDefault());

    user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
    user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");

    roomGroup1Id = UUID.fromString("cdc44826-23b0-4e99-bec2-7fb2f00b6b13");
    roomGroup2Id = UUID.fromString("0471809c-e0bb-4bfd-85b6-b7b9a1eca597");
    roomTemporary1Id = UUID.fromString("823379f7-4cd6-4513-85fe-66494a233e1f");
    roomTemporary2Id = UUID.fromString("ebbd8c5f-12f3-413d-92f5-6da1cdb8c644");
    roomOneToOne1Id = UUID.fromString("86327874-40f4-47cb-914d-f0ce706d1611");
    roomOneToOne2Id = UUID.fromString("19e5717e-652d-409e-b4fa-87e8dff790c1");

    meeting1Id = UUID.fromString("aaa32dbf-e6ff-4032-864c-6298abaf5607");

    roomGroup1 = Room.create();
    roomGroup1
        .id(roomGroup1Id.toString())
        .type(RoomType.GROUP)
        .name("room1")
        .description("Room one")
        .subscriptions(
            List.of(
                Subscription.create(roomGroup1, user1Id.toString()).owner(true),
                Subscription.create(roomGroup1, user2Id.toString()).owner(false),
                Subscription.create(roomGroup1, user3Id.toString()).owner(false)));

    roomGroup2 = Room.create();
    roomGroup2
        .id(roomGroup2Id.toString())
        .type(RoomType.GROUP)
        .name("room2")
        .description("Room two")
        .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        .subscriptions(
            List.of(
                Subscription.create(roomGroup2, user2Id.toString()).owner(true),
                Subscription.create(roomGroup2, user3Id.toString()).owner(false)));

    roomTemporary1 = Room.create();
    roomTemporary1
        .id(roomTemporary1Id.toString())
        .type(RoomType.TEMPORARY)
        .name("temporary1")
        .description("")
        .subscriptions(
            List.of(Subscription.create(roomTemporary1, user1Id.toString()).owner(true)));

    roomTemporary2 = Room.create();
    roomTemporary2
        .id(roomTemporary2Id.toString())
        .type(RoomType.TEMPORARY)
        .name("temporary2")
        .description("")
        .subscriptions(
            List.of(
                Subscription.create(roomTemporary2, user1Id.toString()).owner(true),
                Subscription.create(roomTemporary2, user2Id.toString()).owner(true),
                Subscription.create(roomTemporary2, user3Id.toString())));

    roomOneToOne1 = Room.create();
    roomOneToOne1
        .id(roomOneToOne1Id.toString())
        .type(RoomType.ONE_TO_ONE)
        .name("")
        .description("")
        .subscriptions(
            List.of(
                Subscription.create(roomOneToOne1, user1Id.toString()).owner(true),
                Subscription.create(roomOneToOne1, user2Id.toString()).owner(false)));

    roomOneToOne2 = Room.create();
    roomOneToOne2
        .id(roomOneToOne2Id.toString())
        .type(RoomType.ONE_TO_ONE)
        .name("")
        .description("")
        .subscriptions(
            List.of(
                Subscription.create(roomOneToOne2, user1Id.toString()).owner(true),
                Subscription.create(roomOneToOne2, user2Id.toString()).owner(true)));

    meeting1 = Meeting.create().id(meeting1Id.toString());
  }

  @AfterEach
  public void afterEach() {
    reset(
        roomRepository,
        subscriptionRepository,
        roomUserSettingsRepository,
        userService,
        membersService,
        eventDispatcher,
        fileMetadataRepository,
        storagesService,
        meetingService,
        chatMessageService);
  }

  @Nested
  @DisplayName("Get rooms tests")
  class GetRoomTests {

    @Test
    @DisplayName("Returns all rooms of which the authenticated user is a member without members")
    void getRooms_testOkBasicRooms() {
      when(roomRepository.getByUserId(user1Id.toString(), false))
          .thenReturn(Arrays.asList(roomGroup2, roomOneToOne1));

      List<Room> rooms = roomService.getRooms(null, UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup2Id.toString(), rooms.get(0).getId());
      assertEquals(RoomType.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId());
      assertEquals(RoomType.ONE_TO_ONE, rooms.get(1).getType());
      assertEquals(
          OffsetDateTime.parse("2022-01-01T00:00:00Z"), rooms.get(0).getPictureUpdatedAt());
      assertNull(rooms.get(1).getPictureUpdatedAt());
      verify(roomRepository, times(1)).getByUserId(user1Id.toString(), false);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("Requests the members of every room when the members extra field is set")
    void getRooms_testOkWithMembers() {
      when(roomRepository.getByUserId(user1Id.toString(), true))
          .thenReturn(Arrays.asList(roomGroup1, roomOneToOne1, roomTemporary1));

      List<Room> rooms =
          roomService.getRooms(List.of(RoomExtraFieldDto.MEMBERS), UserPrincipal.create(user1Id));

      assertEquals(3, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId());
      assertEquals(RoomType.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId());
      assertEquals(RoomType.ONE_TO_ONE, rooms.get(1).getType());
      assertEquals(roomTemporary1Id.toString(), rooms.get(2).getId());
      assertEquals(RoomType.TEMPORARY, rooms.get(2).getType());
      assertNotNull(rooms.get(0).getSubscriptions());
      assertNotNull(rooms.get(1).getSubscriptions());
      assertNotNull(rooms.get(2).getSubscriptions());
      verify(roomRepository, times(1)).getByUserId(user1Id.toString(), true);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("The settings extra field alone does not request the members nor the settings")
    void getRooms_testOkWithSettings() {
      when(roomRepository.getByUserId(user1Id.toString(), false))
          .thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));

      List<Room> rooms =
          roomService.getRooms(List.of(RoomExtraFieldDto.SETTINGS), UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId());
      assertEquals(RoomType.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId());
      assertEquals(RoomType.ONE_TO_ONE, rooms.get(1).getType());
      verify(roomRepository, times(1)).getByUserId(user1Id.toString(), false);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("Returns the settings of the user for every room grouped by room identifier")
    void getRoomsUserSettings_testOk() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      Map<String, RoomUserSettings> settings =
          Map.of(
              roomGroup1Id.toString(),
              RoomUserSettings.create(roomGroup1, user1Id.toString())
                  .mutedUntil(OffsetDateTime.now()));
      when(roomUserSettingsRepository.getMapGroupedByUserId(currentUser.getId()))
          .thenReturn(settings);

      assertEquals(settings, roomService.getRoomsUserSettings(currentUser));
      verify(roomUserSettingsRepository, times(1)).getMapGroupedByUserId(currentUser.getId());
    }
  }

  @Nested
  @DisplayName("Get room user settings tests")
  class GetRoomUserSettingsTests {

    @Test
    @DisplayName("Returns the settings of the user for the required room")
    void getRoomUserSettings_testOk() {
      RoomUserSettings settings =
          RoomUserSettings.create(roomGroup2, user2Id.toString()).mutedUntil(OffsetDateTime.now());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user2Id.toString()))
          .thenReturn(Optional.of(settings));

      assertEquals(
          Optional.of(settings),
          roomService.getRoomUserSettings(roomGroup2Id, UserPrincipal.create(user2Id)));
    }

    @Test
    @DisplayName("If the user didn't set anything, the room user settings are empty")
    void getRoomUserSettings_testMemberWithoutSettings() {
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user2Id.toString()))
          .thenReturn(Optional.empty());

      assertTrue(
          roomService.getRoomUserSettings(roomGroup2Id, UserPrincipal.create(user2Id)).isEmpty());
    }
  }

  @Nested
  @DisplayName("Create room tests")
  class CreateRoomTests {

    @Nested
    @DisplayName("Create group room tests")
    class CreateGroupRoomTests {

      @Test
      @DisplayName("It creates the room and returns it")
      void createGroupRoom_testOk() {
        insertIncreasesCreatedAt();
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id).queueId(UUID.randomUUID());
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal)).thenReturn(true);
        when(membersService.initRoomSubscriptions(
                eq(
                    List.of(
                        MemberDto.create().userId(user1Id).owner(true),
                        MemberDto.create().userId(user2Id),
                        MemberDto.create().userId(user3Id))),
                any(Room.class)))
            .thenReturn(
                List.of(
                    Subscription.create(roomGroup1, user1Id.toString()).owner(true),
                    Subscription.create(roomGroup1, user2Id.toString()),
                    Subscription.create(roomGroup1, user3Id.toString())));
        when(roomRepository.insert(roomGroup1)).thenReturn(roomGroup1);

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
        Room room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomGroup1Id);
          uuid.when(() -> UUID.fromString(roomGroup1.getId())).thenReturn(roomGroup1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(RoomType.valueOf(creationFields.getType().name()), room.getType());
        assertEquals(3, room.getSubscriptions().size());

        Optional<Subscription> user1 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user1Id.toString()))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<Subscription> user2 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user2Id.toString()))
                .findFirst();
        assertTrue(user2.isPresent());
        assertFalse(user2.get().isOwner());
        Optional<Subscription> user3 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user3Id.toString()))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        ArgumentCaptor<SystemEvent> eventCaptor = ArgumentCaptor.forClass(SystemEvent.class);
        verify(chatMessageService, times(3)).insertSystemEvent(eventCaptor.capture());
        List<SystemEvent> events = eventCaptor.getAllValues();
        assertEquals("ROOM_CREATED", events.get(0).getType());
        assertEquals("MEMBER_ADDED", events.get(1).getType());
        assertEquals("MEMBER_ADDED", events.get(2).getType());
        verify(membersService).initRoomSubscriptions(any(), any(Room.class));
        assertEquals(user1Id.toString(), events.get(0).getActorId());
        assertEquals(user1Id.toString(), events.get(1).getActorId());
        assertEquals(user1Id.toString(), events.get(2).getActorId());
        assertEquals(
            "{\"addedByUserId\":\"" + user1Id + "\",\"addedUserIds\":[\"" + user2Id + "\"]}",
            events.get(1).getContent());
        assertEquals(
            "{\"addedByUserId\":\"" + user1Id + "\",\"addedUserIds\":[\"" + user3Id + "\"]}",
            events.get(2).getContent());
        assertTrue(events.get(0).getCreatedAt().isBefore(events.get(1).getCreatedAt()));
        assertTrue(events.get(1).getCreatedAt().isBefore(events.get(2).getCreatedAt()));

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomGroup1Id));
        verifyNoMoreInteractions(eventDispatcher);
      }

      @Test
      @DisplayName("It creates the room setting the owners and returns it")
      void createGroupRoom_testOkWithOwners() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id).queueId(UUID.randomUUID());
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal)).thenReturn(true);
        when(membersService.initRoomSubscriptions(
                eq(
                    List.of(
                        MemberDto.create().userId(user1Id).owner(true),
                        MemberDto.create().userId(user2Id).owner(true),
                        MemberDto.create().userId(user3Id))),
                any(Room.class)))
            .thenReturn(
                List.of(
                    Subscription.create(roomGroup1, user1Id.toString()).owner(true),
                    Subscription.create(roomGroup1, user2Id.toString()).owner(true),
                    Subscription.create(roomGroup1, user3Id.toString())));
        when(roomRepository.insert(roomGroup1))
            .thenReturn(
                roomGroup1.subscriptions(
                    List.of(
                        Subscription.create(roomGroup1, user1Id.toString()).owner(true),
                        Subscription.create(roomGroup1, user2Id.toString()).owner(true),
                        Subscription.create(roomGroup1, user3Id.toString()).owner(false))));

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id).owner(true),
                        MemberDto.create().userId(user3Id)));
        Room room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomGroup1Id);
          uuid.when(() -> UUID.fromString(roomGroup1.getId())).thenReturn(roomGroup1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(RoomType.valueOf(creationFields.getType().name()), room.getType());
        assertEquals(3, room.getSubscriptions().size());

        Optional<Subscription> user1 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user1Id.toString()))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<Subscription> user2 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user2Id.toString()))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<Subscription> user3 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user3Id.toString()))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomGroup1Id));
        verifyNoMoreInteractions(eventDispatcher);
      }

      @Test
      @DisplayName("If there are duplicate invites, it throws a 'bad request' exception")
      void createGroupRoom_testRoomToCreateWithDuplicateInvites() {
        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user2Id)));
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals("Bad Request - Members cannot be duplicated", exception.getMessage());
      }

      @Test
      @DisplayName("If the current user is invited, it throws a 'bad request' exception")
      void createGroupRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() {
        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user1Id), MemberDto.create().userId(user2Id)));
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            "Bad Request - Requester can't be invited to the room", exception.getMessage());
      }

      @Test
      @DisplayName("If there is an invitee without account, it throws a 'not found' exception")
      void createGroupRoom_testInvitedUserWithoutAccount() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user3Id, mockUserPrincipal)).thenReturn(true);
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(false);

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
        ChatsHttpException exception =
            assertThrows(
                NotFoundException.class,
                () -> roomService.createRoom(creationFields, mockUserPrincipal));
        assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            String.format("Not Found - User with id '%s' not found", user2Id),
            exception.getMessage());
      }
    }

    @Nested
    @DisplayName("Create temporary room tests")
    class CreateTemporaryRoomTests {

      @Test
      @DisplayName("It creates the room and returns it")
      void createTemporaryRoom_testOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id).queueId(UUID.randomUUID());
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal)).thenReturn(true);
        when(membersService.initRoomSubscriptions(
                eq(List.of(MemberDto.create().userId(user1Id).owner(true))), any(Room.class)))
            .thenReturn(
                List.of(Subscription.create(roomTemporary1, user1Id.toString()).owner(true)));
        when(roomRepository.insert(roomTemporary1)).thenReturn(roomTemporary1);

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("temporary1")
                .description("")
                .type(RoomTypeDto.TEMPORARY);
        Room room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomTemporary1Id);
          uuid.when(() -> UUID.fromString(roomTemporary1.getId())).thenReturn(roomTemporary1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(RoomType.valueOf(creationFields.getType().name()), room.getType());
        assertEquals(1, room.getSubscriptions().size());

        Optional<Subscription> user1 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user1Id.toString()))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString()), RoomCreated.create().roomId(roomTemporary1Id));
        verifyNoMoreInteractions(eventDispatcher);
      }

      @Test
      @DisplayName("It creates the room setting the owners and returns it")
      void createTemporaryRoom_testOkWithOwners() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id).queueId(UUID.randomUUID());
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal)).thenReturn(true);
        when(membersService.initRoomSubscriptions(
                eq(
                    List.of(
                        MemberDto.create().userId(user1Id).owner(true),
                        MemberDto.create().userId(user2Id).owner(true),
                        MemberDto.create().userId(user3Id))),
                any(Room.class)))
            .thenReturn(
                List.of(
                    Subscription.create(roomTemporary2, user1Id.toString()).owner(true),
                    Subscription.create(roomTemporary2, user2Id.toString()).owner(true),
                    Subscription.create(roomTemporary2, user3Id.toString())));
        when(roomRepository.insert(roomTemporary2)).thenReturn(roomTemporary2);

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("temporary2")
                .description("")
                .type(RoomTypeDto.TEMPORARY)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id).owner(true),
                        MemberDto.create().userId(user3Id)));
        Room room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomTemporary2Id);
          uuid.when(() -> UUID.fromString(roomTemporary2.getId())).thenReturn(roomTemporary2Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(RoomType.valueOf(creationFields.getType().name()), room.getType());
        assertEquals(3, room.getSubscriptions().size());

        Optional<Subscription> user1 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user1Id.toString()))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<Subscription> user2 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user2Id.toString()))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<Subscription> user3 =
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user3Id.toString()))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomTemporary2Id));
        verifyNoMoreInteractions(eventDispatcher);
      }
    }

    @Nested
    @DisplayName("Create one-to-one room tests")
    class CreateOneToOneRoomTests {

      @Test
      @DisplayName("It creates a one-to-one room and returns it")
      void createRoomOneToOne_testOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(membersService.initRoomSubscriptions(
                eq(
                    List.of(
                        MemberDto.create().userId(user1Id).owner(true),
                        MemberDto.create().userId(user2Id))),
                any(Room.class)))
            .thenReturn(
                Stream.of(user1Id, user2Id)
                    .map(
                        userId ->
                            Subscription.create(roomOneToOne1, userId.toString())
                                .owner(userId.equals(user1Id)))
                    .toList());
        when(roomRepository.insert(roomOneToOne1)).thenReturn(roomOneToOne1);

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room2")
                .description("Room one")
                .type(RoomTypeDto.ONE_TO_ONE)
                .members(List.of(MemberDto.create().userId(user2Id)));
        Room room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomOneToOne1Id);
          uuid.when(() -> UUID.fromString(roomOneToOne1.getId())).thenReturn(roomOneToOne1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals("", room.getName());
        assertEquals("", room.getDescription());
        assertEquals(RoomType.valueOf(creationFields.getType().name()), room.getType());
        assertEquals(2, room.getSubscriptions().size());
        assertNull(room.getPictureUpdatedAt());
        assertTrue(
            room.getSubscriptions().stream()
                .anyMatch(member -> member.getUserId().equals(user1Id.toString())));
        assertTrue(
            room.getSubscriptions().stream()
                .anyMatch(member -> member.getUserId().equals(user2Id.toString())));
        assertTrue(
            room.getSubscriptions().stream()
                .filter(member -> member.getUserId().equals(user1Id.toString()))
                .findAny()
                .orElseThrow()
                .isOwner());
        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString()),
                RoomCreated.create().roomId(roomOneToOne1Id));
        verifyNoMoreInteractions(eventDispatcher);
      }

      @Test
      @DisplayName(
          "Given creation fields for a one-to-one room, if there is a room with those users returns"
              + " a status code 409")
      void createRoomOneToOne_testOneToOneAlreadyExists() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal)).thenReturn(true);
        when(roomRepository.getOneToOneByAllUserIds(user1Id.toString(), user2Id.toString()))
            .thenReturn(Optional.of(roomOneToOne1));
        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.ONE_TO_ONE)
                .members(List.of(MemberDto.create().userId(user2Id)));
        ChatsHttpException exception =
            assertThrows(
                ConflictException.class,
                () -> roomService.createRoom(creationFields, mockUserPrincipal));
        assertEquals(Status.CONFLICT.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.CONFLICT.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            "Conflict - The one-to-one room already exists for these users",
            exception.getMessage());
      }
    }
  }

  @Nested
  @DisplayName("Update room tests")
  class UpdateRoomTest {

    @Test
    @DisplayName("It correctly updates the room")
    void updateRoom_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(
              Optional.of(roomGroup1.name("room1-to-change").description("Room one to change")));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));

      RoomEditableFieldsDto roomEditableFieldsDto =
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed");
      Room room =
          roomService.updateRoom(
              roomGroup1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(roomGroup1Id.toString(), room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      List<SystemEvent> events = insertedSystemEvents(2);
      List<String> receivers = List.of(user1Id.toString(), user2Id.toString(), user3Id.toString());
      InOrder inOrder = inOrder(eventDispatcher);
      inOrder
          .verify(eventDispatcher, times(1))
          .sendToUserExchange(
              receivers,
              RoomNameChanged.create()
                  .roomId(roomGroup1Id)
                  .name("room1-changed")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(0).getId())
                  .systemEventCreatedAt(events.get(0).getCreatedAt()));
      inOrder
          .verify(eventDispatcher, times(1))
          .sendToUserExchange(
              receivers,
              RoomDescriptionChanged.create()
                  .roomId(roomGroup1Id)
                  .description("Room one changed")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(1).getId())
                  .systemEventCreatedAt(events.get(1).getCreatedAt()));
      verifyNoMoreInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("It correctly updates the room name and also the associated meeting name")
    void updateRoomWithMeeting_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(
              Optional.of(
                  roomGroup1
                      .meetingId(meeting1Id.toString())
                      .name("room1-to-change")
                      .description("Room one to change")));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      when(meetingService.getMeetingEntity(meeting1Id)).thenReturn(Optional.of(meeting1));

      RoomEditableFieldsDto roomEditableFieldsDto =
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed");
      Room room =
          roomService.updateRoom(
              roomGroup1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(roomGroup1Id.toString(), room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      List<SystemEvent> events = insertedSystemEvents(2);
      List<String> receivers = List.of(user1Id.toString(), user2Id.toString(), user3Id.toString());
      InOrder inOrder = inOrder(eventDispatcher);
      inOrder
          .verify(eventDispatcher, times(1))
          .sendToUserExchange(
              receivers,
              RoomNameChanged.create()
                  .roomId(roomGroup1Id)
                  .name("room1-changed")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(0).getId())
                  .systemEventCreatedAt(events.get(0).getCreatedAt()));
      inOrder
          .verify(eventDispatcher, times(1))
          .sendToUserExchange(
              receivers,
              RoomDescriptionChanged.create()
                  .roomId(roomGroup1Id)
                  .description("Room one changed")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(1).getId())
                  .systemEventCreatedAt(events.get(1).getCreatedAt()));
      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(meetingService, times(1)).updateMeeting(meeting1.name("room1-changed"));
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoMoreInteractions(meetingService);
    }

    @Test
    @DisplayName("It writes name and description events in order")
    void updateRoom_testWritesNameThenDescriptionEvents() {
      insertIncreasesCreatedAt();
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(Optional.of(roomGroup1.name("old").description("old description")));

      roomService.updateRoom(
          roomGroup1Id,
          RoomEditableFieldsDto.create().name("new").description("new description"),
          UserPrincipal.create(user1Id));

      List<SystemEvent> events = insertedSystemEvents(2);
      SystemEvent nameEvent = events.get(0);
      SystemEvent descriptionEvent = events.get(1);
      assertEquals("ROOM_NAME_CHANGED", nameEvent.getType());
      assertEquals(user1Id.toString(), nameEvent.getActorId());
      assertEquals(roomGroup1Id.toString(), nameEvent.getRoomId());
      assertEquals("{\"changedBy\":\"" + user1Id + "\",\"name\":\"new\"}", nameEvent.getContent());
      assertEquals("ROOM_DESCRIPTION_CHANGED", descriptionEvent.getType());
      assertEquals(user1Id.toString(), descriptionEvent.getActorId());
      assertEquals(
          "{\"changedBy\":\"" + user1Id + "\",\"description\":\"new description\"}",
          descriptionEvent.getContent());
      assertTrue(nameEvent.getCreatedAt().isBefore(descriptionEvent.getCreatedAt()));
    }

    @Test
    @DisplayName("It writes only the name event when only the name changes")
    void updateRoom_testOnlyNameChanged() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(Optional.of(roomGroup1.name("old").description("same")));

      roomService.updateRoom(
          roomGroup1Id,
          RoomEditableFieldsDto.create().name("new").description("same"),
          UserPrincipal.create(user1Id));

      List<SystemEvent> events = insertedSystemEvents(1);
      assertEquals("ROOM_NAME_CHANGED", events.get(0).getType());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomNameChanged.create()
                  .roomId(roomGroup1Id)
                  .name("new")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(0).getId())
                  .systemEventCreatedAt(events.get(0).getCreatedAt()));
      verifyNoMoreInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("It writes a description event when the description is cleared")
    void updateRoom_testDescriptionClearedToEmpty() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(Optional.of(roomGroup1.name("same").description("old description")));

      roomService.updateRoom(
          roomGroup1Id,
          RoomEditableFieldsDto.create().description(""),
          UserPrincipal.create(user1Id));

      List<SystemEvent> events = insertedSystemEvents(1);
      assertEquals("ROOM_DESCRIPTION_CHANGED", events.get(0).getType());
      assertEquals(
          "{\"changedBy\":\"" + user1Id + "\",\"description\":\"\"}", events.get(0).getContent());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomDescriptionChanged.create()
                  .roomId(roomGroup1Id)
                  .description("")
                  .updatedBy(user1Id)
                  .systemEventId(events.get(0).getId())
                  .systemEventCreatedAt(events.get(0).getCreatedAt()));
      verifyNoMoreInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("It writes no event and sends nothing when nothing changes")
    void updateRoom_testNothingChanged() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(Optional.of(roomGroup1.name("same").description("same description")));

      roomService.updateRoom(
          roomGroup1Id,
          RoomEditableFieldsDto.create().name("same").description("same description"),
          UserPrincipal.create(user1Id));

      verifyNoInteractions(chatMessageService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void updateRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  roomService.updateRoom(
                      roomGroup1Id,
                      RoomEditableFieldsDto.create()
                          .name("room1-changed")
                          .description("Room one changed"),
                      UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't member of required room, it throws a 'forbidden'"
            + " exception")
    void updateRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.updateRoom(
                      roomGroup2Id,
                      RoomEditableFieldsDto.create()
                          .name("room1-changed")
                          .description("Room one changed"),
                      UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't owner of required room, it throws a 'forbidden' exception")
    void updateRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(
              Optional.of(roomGroup1.name("room1-changed").description("Room one changed")));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.updateRoom(
                      roomGroup1Id,
                      RoomEditableFieldsDto.create()
                          .name("room1-changed")
                          .description("Room one changed"),
                      UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
          exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Mute room tests")
  class MuteRoomTests {

    @Test
    @DisplayName("Mute the current user in a specific room when user settings not exists")
    void muteRoom_testOkUserSettingNotExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());
      roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .save(
              RoomUserSettings.create(roomGroup1, user1Id.toString())
                  .mutedUntil(
                      OffsetDateTime.ofInstant(
                          Instant.parse("0001-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(user1Id.toString(), RoomMuted.create().roomId(roomGroup1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings exists")
    void muteRoom_testOkUserSettingExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      RoomUserSettings roomUserSettings = RoomUserSettings.create(roomGroup1, user1Id.toString());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(roomUserSettings));
      roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .save(
              roomUserSettings.mutedUntil(
                  OffsetDateTime.ofInstant(
                      Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(user1Id.toString(), RoomMuted.create().roomId(roomGroup1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Correctly does nothing if the user is already muted")
    void muteRoom_testOkUserAlreadyMuted() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository);
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void muteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.muteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void muteRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Unmute room tests")
  class UnmuteRoomTests {

    @Test
    @DisplayName("Correctly does nothing if user settings not exists")
    void unmuteRoom_testOkUserSettingNotExists() {
      when(subscriptionRepository.getMembership(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(new Membership(false, null)));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(subscriptionRepository, times(1))
          .getMembership(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository);
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("Unmute the current user in a specific room")
    void unmuteRoom_testOkUserSettingExists() {
      when(subscriptionRepository.getMembership(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(new Membership(false, null)));
      RoomUserSettings roomUserSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).mutedUntil(OffsetDateTime.now());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(roomUserSettings));
      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(subscriptionRepository, times(1))
          .getMembership(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1)).save(roomUserSettings.mutedUntil(null));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(user1Id.toString(), RoomUnmuted.create().roomId(roomGroup1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Correctly does nothing if the user has already unmuted")
    void unmuteRoom_testOkUserAlreadyUnmuted() {
      when(subscriptionRepository.getMembership(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(new Membership(false, null)));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(RoomUserSettings.create(roomGroup1, user1Id.toString())));
      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(subscriptionRepository, times(1))
          .getMembership(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository);
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void unmuteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.unmuteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void unmuteRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Clear room tests")
  class ClearRoomTests {

    @Test
    @DisplayName(
        "Correctly sets the clear date to now when user settings doesn't exist on permanent room")
    void clearRoom_testOkUserSettingNotExistsOnPermanentRoom() {
      OffsetDateTime desiredDate = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
      RoomUserSettings userSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).clearedAt(desiredDate);

      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.setClearedAtNow(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(desiredDate);

      OffsetDateTime clearedAt =
          roomService.clearRoomHistory(roomGroup1Id, UserPrincipal.create(user1Id));
      assertEquals(desiredDate, clearedAt);

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .setClearedAtNow(roomGroup1Id.toString(), user1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              user1Id.toString(),
              RoomHistoryCleared.create().roomId(roomGroup1Id).clearedAt(clearedAt));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
      verifyNoInteractions(attachmentService, chatMessageService);
    }

    @Test
    @DisplayName("Correctly sets the clear date to now when user settings exists on permanent room")
    void clearRoom_testOkUserSettingExistsOnPermanentRoom() {
      OffsetDateTime desiredDate = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
      RoomUserSettings userSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).clearedAt(desiredDate);

      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.setClearedAtNow(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(desiredDate);

      OffsetDateTime clearedAt =
          roomService.clearRoomHistory(roomGroup1Id, UserPrincipal.create(user1Id));
      assertEquals(desiredDate, clearedAt);

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .setClearedAtNow(roomGroup1Id.toString(), user1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              user1Id.toString(),
              RoomHistoryCleared.create().roomId(roomGroup1Id).clearedAt(clearedAt));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
      verifyNoInteractions(attachmentService, chatMessageService);
    }

    @Test
    @DisplayName("Correctly clears the temporary room performed by an owner")
    void clearRoom_testOkTemporaryRoom() {
      OffsetDateTime rowCreatedAt = OffsetDateTime.parse("2026-01-01T10:00:00.123456Z");
      when(chatMessageService.insertSystemEvent(any(SystemEvent.class)))
          .thenAnswer(invocation -> invocation.<SystemEvent>getArgument(0).createdAt(rowCreatedAt));

      when(roomRepository.getById(roomTemporary2Id.toString()))
          .thenReturn(Optional.of(roomTemporary2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomTemporary2Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());
      List<String> attachmentIds = List.of("attachment-1", "attachment-2");
      when(fileMetadataRepository.getIdsByRoomIdAndType(
              roomTemporary2Id.toString(), FileMetadataType.ATTACHMENT))
          .thenReturn(attachmentIds);

      OffsetDateTime clearedAt =
          roomService.clearRoomHistory(roomTemporary2Id, UserPrincipal.create(user1Id));
      assertEquals(rowCreatedAt, clearedAt);

      verify(roomRepository, times(1)).getById(roomTemporary2Id.toString());
      verify(fileMetadataRepository, times(1)).deleteByIds(attachmentIds);
      verify(attachmentService, times(1))
          .deleteAttachmentFiles(attachmentIds, UserPrincipal.create(user1Id));
      verify(messageRepository, times(1)).deleteByRoomId(roomTemporary2Id.toString());
      verify(systemEventRepository, times(1)).deleteByRoomId(roomTemporary2Id.toString());
      List<SystemEvent> events = insertedSystemEvents(1);
      SystemEvent event = events.get(0);
      assertEquals(rowCreatedAt, event.getCreatedAt());
      assertEquals("ROOM_HISTORY_CLEARED", event.getType());
      assertEquals(user1Id.toString(), event.getActorId());
      assertEquals(roomTemporary2Id.toString(), event.getRoomId());
      assertEquals("{\"clearedBy\":\"" + user1Id + "\"}", event.getContent());
      InOrder inOrder =
          inOrder(
              fileMetadataRepository,
              messageRepository,
              systemEventRepository,
              chatMessageService,
              eventDispatcher,
              attachmentService);
      inOrder.verify(fileMetadataRepository).deleteByIds(attachmentIds);
      inOrder.verify(messageRepository).deleteByRoomId(roomTemporary2Id.toString());
      inOrder.verify(systemEventRepository).deleteByRoomId(roomTemporary2Id.toString());
      inOrder.verify(chatMessageService).insertSystemEvent(event);
      inOrder
          .verify(eventDispatcher)
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomHistoryCleared.create()
                  .roomId(roomTemporary2Id)
                  .clearedAt(rowCreatedAt)
                  .clearedBy(user1Id)
                  .systemEventId(event.getId())
                  .systemEventCreatedAt(rowCreatedAt));
      inOrder
          .verify(attachmentService)
          .deleteAttachmentFiles(attachmentIds, UserPrincipal.create(user1Id));
      verifyNoMoreInteractions(
          roomRepository,
          roomUserSettingsRepository,
          eventDispatcher,
          attachmentService,
          messageRepository,
          systemEventRepository);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't a room member of a permanent room, it throws a 'forbidden'"
            + " exception")
    void clearRoom_testAuthenticatedUserIsNotARoomMemberOfPermanentRoom() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.clearRoomHistory(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository, attachmentService);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't an owner of the temporary room, it throws a 'forbidden'"
            + " exception")
    void clearRoom_testAuthenticatedUserIsNotAnOwnerOfTemporaryRoom() {
      when(roomRepository.getById(roomTemporary2Id.toString()))
          .thenReturn(Optional.of(roomTemporary2));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.clearRoomHistory(roomTemporary2Id, UserPrincipal.create(user3Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user3Id, roomTemporary2Id),
          exception.getMessage());

      verify(roomRepository, times(1)).getById(roomTemporary2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository, attachmentService);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void clearRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.clearRoomHistory(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository, attachmentService);
    }
  }

  @Nested
  @DisplayName("Get room picture tests")
  class GetRoomPictureTests {

    @Test
    @DisplayName("It returns the room picture")
    void getRoomPicture_testOk() {
      FileMetadata pfpMetadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(subscriptionRepository.getMembership(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(new Membership(false, null)));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      InputStream fileStream = mock(InputStream.class);
      when(storagesService.getFileStreamById(pfpMetadata.getId(), user2Id.toString()))
          .thenReturn(fileStream);

      FileContentAndMetadata roomPicture =
          roomService.getRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id));

      assertEquals(fileStream, roomPicture.getFileStream());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(subscriptionRepository, times(1))
          .getMembership(roomGroup1Id.toString(), user1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).getFileStreamById(pfpMetadata.getId(), user2Id.toString());
      verifyNoMoreInteractions(roomRepository, fileMetadataRepository, storagesService);
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void getRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ForbiddenException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.getRoomPicture(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository, fileMetadataRepository, storagesService);
    }

    @Test
    @DisplayName("It throws an exception if storage service fails")
    void getRoomPicture_testErrorStorageException() {
      FileMetadata pfpMetadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(subscriptionRepository.getMembership(roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(new Membership(false, null)));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      doThrow(StorageException.class)
          .when(storagesService)
          .getFileStreamById(pfpMetadata.getId(), user2Id.toString());

      assertThrows(
          StorageException.class,
          () -> roomService.getRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id)));

      verify(subscriptionRepository, times(1))
          .getMembership(roomGroup1Id.toString(), user1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).getFileStreamById(pfpMetadata.getId(), user2Id.toString());
      verifyNoMoreInteractions(roomRepository, fileMetadataRepository, storagesService);
    }
  }

  @Nested
  @DisplayName("Set room picture tests")
  class SetRoomPictureTests {

    @Test
    @DisplayName("It sets the room picture if it didn't exists")
    void setRoomPicture_testOkInsert() {
      when(roomRepository.markPictureUpdated(roomGroup1))
          .thenAnswer(
              i ->
                  i.<Room>getArgument(0)
                      .pictureUpdatedAt(
                          OffsetDateTime.ofInstant(
                              Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.empty());

      InputStream fileStream = mock(InputStream.class);
      roomService.setRoomPicture(
          roomGroup1Id, fileStream, "image/jpeg", 123L, "picture", UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).markPictureUpdated(roomGroup1);
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1))
          .saveFile(eq(fileStream), anyString(), eq(user1Id.toString()), eq(123L));
      ArgumentCaptor<FileMetadata> fileMetadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
      verify(fileMetadataRepository, times(1)).save(fileMetadataCaptor.capture());
      FileMetadata fileMetadata = fileMetadataCaptor.getValue();
      assertEquals("image/jpeg", fileMetadata.getMimeType());
      assertEquals(FileMetadataType.ROOM_AVATAR, fileMetadata.getType());
      assertEquals("picture", fileMetadata.getName());
      assertEquals(123L, fileMetadata.getOriginalSize());
      assertEquals(user1Id.toString(), fileMetadata.getUserId());
      assertEquals(roomGroup1Id.toString(), fileMetadata.getRoomId());
      SystemEvent pictureEvent = insertedSystemEvents(1).get(0);
      assertEquals("ROOM_PICTURE_UPDATED", pictureEvent.getType());
      assertEquals(user1Id.toString(), pictureEvent.getActorId());
      assertEquals(roomGroup1Id.toString(), pictureEvent.getRoomId());
      assertEquals("{\"updatedBy\":\"" + user1Id + "\"}", pictureEvent.getContent());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomPictureChanged.create()
                  .roomId(roomGroup1Id)
                  .updatedAt(
                      OffsetDateTime.ofInstant(
                          Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()))
                  .updatedBy(user1Id)
                  .systemEventId(pictureEvent.getId()));
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("It update the room picture if it already exists")
    void setRoomPicture_testOkUpdate() {
      when(roomRepository.markPictureUpdated(roomGroup1))
          .thenAnswer(
              i ->
                  i.<Room>getArgument(0)
                      .pictureUpdatedAt(
                          OffsetDateTime.ofInstant(
                              Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      FileMetadata existingMetadata =
          FileMetadata.create()
              .id("123")
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId("fake-old-user");
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(existingMetadata));

      InputStream fileStream = mock(InputStream.class);
      roomService.setRoomPicture(
          roomGroup1Id, fileStream, "image/jpeg", 123L, "picture", UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).markPictureUpdated(roomGroup1);
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile("123", "fake-old-user");
      verify(fileMetadataRepository, times(1)).delete(existingMetadata);
      verify(storagesService, times(1))
          .saveFile(eq(fileStream), anyString(), eq(user1Id.toString()), eq(123L));
      ArgumentCaptor<FileMetadata> fileMetadataCaptor = ArgumentCaptor.forClass(FileMetadata.class);
      verify(fileMetadataRepository, times(1)).save(fileMetadataCaptor.capture());
      FileMetadata fileMetadata = fileMetadataCaptor.getValue();
      assertNotEquals(existingMetadata.getId(), fileMetadata.getId());
      assertEquals("image/jpeg", fileMetadata.getMimeType());
      assertEquals(FileMetadataType.ROOM_AVATAR, fileMetadata.getType());
      assertEquals("picture", fileMetadata.getName());
      assertEquals(123L, fileMetadata.getOriginalSize());
      assertEquals(user1Id.toString(), fileMetadata.getUserId());
      assertEquals(roomGroup1.getId(), fileMetadata.getRoomId());
      SystemEvent pictureEvent = insertedSystemEvents(1).get(0);
      assertEquals("ROOM_PICTURE_UPDATED", pictureEvent.getType());
      assertEquals(user1Id.toString(), pictureEvent.getActorId());
      assertEquals(roomGroup1Id.toString(), pictureEvent.getRoomId());
      assertEquals("{\"updatedBy\":\"" + user1Id + "\"}", pictureEvent.getContent());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomPictureChanged.create()
                  .roomId(roomGroup1Id)
                  .updatedAt(
                      OffsetDateTime.ofInstant(
                          Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()))
                  .updatedBy(user1Id)
                  .systemEventId(pictureEvent.getId()));
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("Throws an exception if storage service fails uploading new picture")
    void setRoomPicture_testErrorStorageExceptionUploadingNewPicture() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.empty());
      InputStream fileStream = mock(InputStream.class);
      doThrow(StorageException.class)
          .when(storagesService)
          .saveFile(eq(fileStream), anyString(), eq(user1Id.toString()), eq(123L));

      assertThrows(
          StorageException.class,
          () ->
              roomService.setRoomPicture(
                  roomGroup1Id,
                  fileStream,
                  "image/jpeg",
                  123L,
                  "picture",
                  UserPrincipal.create(user1Id)));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1))
          .saveFile(eq(fileStream), anyString(), eq(user1Id.toString()), eq(123L));
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("Throws an exception if storage service fails updating existing picture")
    void setRoomPicture_testErrorStorageExceptionUpdatingExistingPicture() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      FileMetadata existingMetadata =
          FileMetadata.create()
              .id("123")
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId("fake-old-user");
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(existingMetadata));
      doThrow(StorageException.class).when(storagesService).deleteFile("123", "fake-old-user");

      InputStream fileStream = mock(InputStream.class);
      assertThrows(
          StorageException.class,
          () ->
              roomService.setRoomPicture(
                  roomGroup1Id,
                  fileStream,
                  "image/jpeg",
                  123L,
                  "picture",
                  UserPrincipal.create(user1Id)));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile("123", "fake-old-user");
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void setRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      InputStream file = mock(InputStream.class);
      ForbiddenException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.setRoomPicture(
                      roomGroup2Id,
                      file,
                      "image/jpeg",
                      123L,
                      "picture",
                      UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the user is not a room owner, it throws a ForbiddenException")
    void setRoomPicture_failsIfUserIsNotOwner() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      InputStream file = mock(InputStream.class);
      ForbiddenException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.setRoomPicture(
                      roomGroup2Id,
                      file,
                      "image/jpeg",
                      123L,
                      "picture",
                      UserPrincipal.create(user3Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user3Id, roomGroup2Id),
          exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room is not a group, It throws a BadRequestException")
    void setRoomPicture_failsIfRoomIsNotAGroup() {
      when(roomRepository.getById(roomOneToOne2Id.toString()))
          .thenReturn(Optional.of(roomOneToOne2));
      InputStream file = mock(InputStream.class);
      BadRequestException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  roomService.setRoomPicture(
                      roomOneToOne2Id,
                      file,
                      "image/jpeg",
                      123L,
                      "picture",
                      UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Bad Request - The room picture can only be set for groups", exception.getMessage());
      verify(roomRepository, times(1)).getById(roomOneToOne2Id.toString());
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the picture is not an image, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsNotAnImage() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      InputStream file = mock(InputStream.class);
      BadRequestException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  roomService.setRoomPicture(
                      roomGroup1Id,
                      file,
                      "text/html",
                      123L,
                      "picture",
                      UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - The room picture must be an image", exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Delete room picture tests")
  class DeleteRoomPictureTests {

    @Test
    @DisplayName("Correctly deletes the room picture")
    void deleteRoomPicture_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      FileMetadata metadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(metadata));

      roomService.deleteRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).update(roomGroup1.pictureUpdatedAt(null));
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(fileMetadataRepository, times(1)).delete(metadata);
      verify(storagesService, times(1)).deleteFile(metadata.getId(), user2Id.toString());
      SystemEvent deletedEvent = insertedSystemEvents(1).get(0);
      assertEquals("ROOM_PICTURE_DELETED", deletedEvent.getType());
      assertEquals(user1Id.toString(), deletedEvent.getActorId());
      assertEquals(roomGroup1Id.toString(), deletedEvent.getRoomId());
      assertEquals("{\"deletedBy\":\"" + user1Id + "\"}", deletedEvent.getContent());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomPictureDeleted.create()
                  .roomId(roomGroup1Id)
                  .deletedBy(user1Id)
                  .systemEventId(deletedEvent.getId()));
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("Throws an exception if storage service fails")
    void deleteRoomPicture_testErrorStorageException() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      FileMetadata metadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup1Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(metadata));
      doThrow(StorageException.class)
          .when(storagesService)
          .deleteFile(metadata.getId(), user2Id.toString());

      assertThrows(
          StorageException.class,
          () -> roomService.deleteRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id)));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile(metadata.getId(), user2Id.toString());
      verifyNoMoreInteractions(
          roomRepository, fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If user is not the room owner, it throws a ForbiddenException")
    void deleteRoomPicture_userNotRoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.deleteRoomPicture(roomGroup1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Forbidden - User '82735f6d-4c6c-471e-99d9-4eef91b1ec45' "
              + "is not an owner of room 'cdc44826-23b0-4e99-bec2-7fb2f00b6b13'",
          exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(fileMetadataRepository, storagesService, eventDispatcher);
    }

    @Test
    @DisplayName("If the room hasn't its picture, it throws a BadRequestException")
    void deleteRoomPicture_fileNotFound() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.empty());

      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.deleteRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - Room picture '%s' not found", roomGroup1Id),
          exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR);
      verifyNoMoreInteractions(roomRepository, fileMetadataRepository);
      verifyNoInteractions(storagesService, eventDispatcher);
    }
  }
}
