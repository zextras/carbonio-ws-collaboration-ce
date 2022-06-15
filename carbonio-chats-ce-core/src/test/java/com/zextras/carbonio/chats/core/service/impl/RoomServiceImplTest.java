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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.FileMetadataBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomDeletedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomHashResetEvent;
import com.zextras.carbonio.chats.core.data.event.RoomPictureChangedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomUpdatedEvent;
import com.zextras.carbonio.chats.core.data.event.UserMutedEvent;
import com.zextras.carbonio.chats.core.data.event.UserUnmutedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.io.File;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
class RoomServiceImplTest {

  private final RoomService                roomService;
  private final RoomRepository             roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher            eventDispatcher;
  private final MessageDispatcher          messageDispatcher;
  private final UserService                userService;
  private final MembersService             membersService;
  private final FileMetadataRepository     fileMetadataRepository;
  private final StoragesService            storagesService;
  private final Clock                      clock;

  public RoomServiceImplTest(RoomMapper roomMapper) {
    this.roomRepository = mock(RoomRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.userService = mock(UserService.class);
    this.membersService = mock(MembersService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.clock = mock(Clock.class);
    this.roomService = new RoomServiceImpl(
      this.roomRepository,
      this.roomUserSettingsRepository,
      roomMapper,
      this.eventDispatcher,
      this.messageDispatcher,
      this.userService,
      this.membersService,
      this.fileMetadataRepository,
      this.storagesService,
      this.clock
    );
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID roomGroup1Id;
  private UUID roomGroup2Id;
  private UUID roomOneToOne1Id;
  private UUID roomOneToOne2Id;
  private UUID roomWorkspace1Id;
  private UUID roomWorkspace2Id;

  private Room roomGroup1;
  private Room roomGroup2;
  private Room roomOneToOne1;
  private Room roomOneToOne2;
  private Room roomWorkspace1;
  private Room roomWorkspace2;

  @BeforeEach
  public void init() {
    user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
    user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");

    roomGroup1Id = UUID.fromString("cdc44826-23b0-4e99-bec2-7fb2f00b6b13");
    roomGroup2Id = UUID.fromString("0471809c-e0bb-4bfd-85b6-b7b9a1eca597");
    roomOneToOne1Id = UUID.fromString("86327874-40f4-47cb-914d-f0ce706d1611");
    roomOneToOne2Id = UUID.fromString("19e5717e-652d-409e-b4fa-87e8dff790c1");
    roomWorkspace1Id = UUID.fromString("a4196800-ae80-48d9-a878-6d6cc2072282");
    roomWorkspace2Id = UUID.fromString("0fd274e6-34fd-4379-8908-5f9d9d6d537d");

    roomGroup1 = Room.create();
    roomGroup1
      .id(roomGroup1Id.toString())
      .type(RoomTypeDto.GROUP)
      .name("room1")
      .description("Room one")
      .subscriptions(List.of(
        Subscription.create(roomGroup1, user1Id.toString()).owner(true),
        Subscription.create(roomGroup1, user2Id.toString()).owner(false),
        Subscription.create(roomGroup1, user3Id.toString()).owner(false)));

    roomGroup2 = Room.create();
    roomGroup2
      .id(roomGroup2Id.toString())
      .type(RoomTypeDto.GROUP)
      .name("room3")
      .description("Room three")
      .pictureUpdatedAt(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
      .subscriptions(List.of(
        Subscription.create(roomGroup2, user2Id.toString()).owner(true),
        Subscription.create(roomGroup2, user3Id.toString()).owner(false)));

    roomOneToOne1 = Room.create();
    roomOneToOne1
      .id(roomOneToOne1Id.toString())
      .type(RoomTypeDto.ONE_TO_ONE)
      .name("")
      .description("")
      .subscriptions(List.of(
        Subscription.create(roomOneToOne1, user1Id.toString()).owner(true),
        Subscription.create(roomOneToOne1, user2Id.toString()).owner(false)));

    roomOneToOne2 = Room.create();
    roomOneToOne2
      .id(roomOneToOne2Id.toString())
      .type(RoomTypeDto.ONE_TO_ONE)
      .name("")
      .description("")
      .subscriptions(List.of(
        Subscription.create(roomOneToOne2, user1Id.toString()).owner(true),
        Subscription.create(roomOneToOne2, user2Id.toString()).owner(true)));

    roomWorkspace1 = Room.create();
    roomWorkspace1
      .id(roomWorkspace1Id.toString())
      .type(RoomTypeDto.WORKSPACE)
      .name("workspace1")
      .description("Workspace one")
      .subscriptions(List.of(
        Subscription.create(roomWorkspace1, user1Id.toString()).owner(true),
        Subscription.create(roomWorkspace1, user2Id.toString()).owner(false),
        Subscription.create(roomWorkspace1, user3Id.toString()).owner(false)))
      .userSettings(List.of(
        RoomUserSettings.create(roomWorkspace1, user1Id.toString()).rank(1),
        RoomUserSettings.create(roomWorkspace1, user2Id.toString()).rank(1),
        RoomUserSettings.create(roomWorkspace1, user3Id.toString()).rank(1)
      ));

    roomWorkspace2 = Room.create();
    roomWorkspace2
      .id(roomWorkspace2Id.toString())
      .type(RoomTypeDto.WORKSPACE)
      .name("workspace2")
      .description("Workspace two")
      .subscriptions(List.of(
        Subscription.create(roomWorkspace2, user1Id.toString()).owner(true),
        Subscription.create(roomWorkspace2, user2Id.toString()).owner(false),
        Subscription.create(roomWorkspace2, user3Id.toString()).owner(false)))
      .userSettings(List.of(
        RoomUserSettings.create(roomWorkspace1, user1Id.toString()).rank(10),
        RoomUserSettings.create(roomWorkspace1, user2Id.toString()).rank(9),
        RoomUserSettings.create(roomWorkspace1, user3Id.toString()).rank(8)
      ));
  }

  @AfterEach
  public void afterEach() {
    reset(
      this.roomRepository,
      this.roomUserSettingsRepository,
      this.userService,
      this.membersService,
      this.eventDispatcher,
      this.messageDispatcher,
      this.fileMetadataRepository,
      this.storagesService
    );

  }

  @Nested
  @DisplayName("Get rooms tests")
  class GetRoomTests {

    @Test
    @DisplayName("Returns all rooms without members or user settings of which the authenticated user is a member")
    public void getRooms_testOkBasicRooms() {
      when(roomRepository.getByUserId(user1Id.toString(), false)).thenReturn(Arrays.asList(roomGroup2, roomOneToOne1));

      List<RoomDto> rooms = roomService.getRooms(null, UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup2Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertNull(rooms.get(0).getMembers());
      assertNull(rooms.get(1).getMembers());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), rooms.get(0).getPictureUpdatedAt());
      assertNull(rooms.get(1).getPictureUpdatedAt());
    }

    @Test
    @DisplayName("Returns all rooms with members and without user settings of which the authenticated user is a member")
    public void getRooms_testOkWithMembers() {
      when(roomRepository.getByUserId(user1Id.toString(), true)).thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));
      List<RoomDto> rooms = roomService.getRooms(List.of(RoomExtraFieldDto.MEMBERS), UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertNotNull(rooms.get(0).getMembers());
      assertNotNull(rooms.get(1).getMembers());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());
    }

    @Test
    @DisplayName("Returns all rooms without members and with user settings of which the authenticated user is a member")
    public void getRooms_testOkWithSettings() {
      when(roomRepository.getByUserId(user1Id.toString(), false)).thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));
      when(roomUserSettingsRepository.getByUserId(user1Id.toString())).thenReturn(Collections.emptyList());
      List<RoomDto> rooms = roomService.getRooms(List.of(RoomExtraFieldDto.SETTINGS), UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertNull(rooms.get(0).getMembers());
      assertNull(rooms.get(1).getMembers());
      assertNotNull(rooms.get(0).getUserSettings());
      assertNotNull(rooms.get(1).getUserSettings());
    }

    @Test
    @DisplayName("Returns all complete rooms of which the authenticated user is a member")
    public void getRooms_testOkCompleteRooms() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomRepository.getByUserId(user1Id.toString(), true)).thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));
      when(roomUserSettingsRepository.getByUserId(currentUser.getId())).thenReturn(
        List.of(RoomUserSettings.create(roomGroup1, user1Id.toString()).mutedUntil(OffsetDateTime.now()))
      );
      List<RoomDto> rooms = roomService.getRooms(List.of(RoomExtraFieldDto.MEMBERS, RoomExtraFieldDto.SETTINGS),
        currentUser);
      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertNotNull(rooms.get(0).getMembers());
      assertNotNull(rooms.get(1).getMembers());
      assertNotNull(rooms.get(0).getUserSettings());
      assertNotNull(rooms.get(1).getUserSettings());
      assertTrue(rooms.get(0).getUserSettings().isMuted());
      assertFalse(rooms.get(1).getUserSettings().isMuted());

    }
  }

  @Nested
  @DisplayName("Get room by id tests")
  class GetRoomByIdTests {

    @Test
    @DisplayName("Returns the required room with all members and room user settings")
    public void getRoomById_testOk() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup2Id.toString(), user2Id.toString()))
        .thenReturn(
          Optional.of(RoomUserSettings.create(roomGroup2, user2Id.toString()).mutedUntil(OffsetDateTime.now())));
      RoomInfoDto room = roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id));

      assertEquals(roomGroup2Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
      assertNotNull(room.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("Returns the required room with no profile picture with all members and room user settings")
    public void getRoomById_testOkWithoutPicture() {
      when(roomRepository.getById(roomOneToOne1Id.toString())).thenReturn(Optional.of(roomOneToOne1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomOneToOne1Id.toString(), user1Id.toString()))
        .thenReturn(
          Optional.of(RoomUserSettings.create(roomOneToOne1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));
      RoomInfoDto room = roomService.getRoomById(roomOneToOne1Id, UserPrincipal.create(user1Id));

      assertEquals(roomOneToOne1Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertNotNull(room.getUserSettings());
      assertNull(room.getPictureUpdatedAt());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the user is a system user, it returns the required room with all members and room user settings")
    public void getRoomById_testWithSystemUser() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup2Id.toString(), user2Id.toString()))
        .thenReturn(
          Optional.of(RoomUserSettings.create(roomGroup2, user2Id.toString()).mutedUntil(OffsetDateTime.now())));
      RoomInfoDto room = roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id).systemUser(true));

      assertEquals(roomGroup2Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
      assertNotNull(room.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the user didn't set anything, it correctly returns the required room with all members and default room user settings")
    public void getRoomById_testMemberWithoutSettings() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup2Id.toString(), user2Id.toString()))
        .thenReturn(Optional.empty());
      RoomInfoDto room = roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id));

      assertEquals(roomGroup2Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertNotNull(room.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertFalse(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void getRoomById_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup2Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void getRoomById_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
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
      public void createGroupRoom_testOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal))
          .thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal))
          .thenReturn(true);
        when(
          membersService.initRoomSubscriptions(eq(Arrays.asList(user2Id, user3Id)), any(Room.class),
            eq(mockUserPrincipal)))
          .thenReturn(Stream.of(user2Id, user3Id, user1Id).map(userId ->
            Subscription.create(roomGroup1, userId.toString()).owner(userId.equals(user1Id))
          ).collect(Collectors.toList()));
        when(roomRepository.insert(roomGroup1)).thenReturn(roomGroup1);

        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room1")
          .description("Room one")
          .type(RoomTypeDto.GROUP)
          .membersIds(List.of(user2Id, user3Id));
        RoomInfoDto room;
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
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
        assertTrue(
          room.getMembers().stream().filter(member -> member.getUserId().equals(user1Id)).findAny().get().isOwner());

        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user1Id.toString()),
          eq(RoomCreatedEvent.create(roomGroup1Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user2Id.toString()),
          eq(RoomCreatedEvent.create(roomGroup1Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user3Id.toString()),
          eq(RoomCreatedEvent.create(roomGroup1Id).from(user1Id)));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1)).createRoom(roomGroup1, user1Id.toString());
        verify(messageDispatcher, times(0)).addUsersToContacts(anyString(), anyString());
        verifyNoMoreInteractions(messageDispatcher);
      }

      @Test
      @DisplayName("There are less than two members when creating a group, it throws a 'bad request' exception")
      public void createGroupRoom_errorWhenMembersAreLessThanTwo() {
        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room1")
          .description("Room one")
          .type(RoomTypeDto.GROUP)
          .membersIds(List.of(user2Id));
        ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
          roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
        assertEquals("Bad Request - Too few members (required at least 3)", exception.getMessage());
      }
    }

    @Nested
    @DisplayName("Create one-to-one room tests")
    class CreateOneToOneRoomTests {

      @Test
      @DisplayName("It creates a one to one room and returns it")
      public void createRoomOneToOne_testOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal))
          .thenReturn(true);
        when(
          membersService.initRoomSubscriptions(eq(List.of(user2Id)), any(Room.class),
            eq(mockUserPrincipal)))
          .thenReturn(Stream.of(user2Id, user1Id).map(userId ->
            Subscription.create(roomOneToOne1, userId.toString()).owner(userId.equals(user1Id))
          ).collect(Collectors.toList()));
        when(roomRepository.insert(roomOneToOne1)).thenReturn(roomOneToOne1);

        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room2")
          .description("Room one")
          .type(RoomTypeDto.ONE_TO_ONE)
          .membersIds(List.of(user2Id));
        RoomInfoDto room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomOneToOne1Id);
          uuid.when(() -> UUID.fromString(roomOneToOne1.getId())).thenReturn(roomOneToOne1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals("", room.getName());
        assertEquals("", room.getDescription());
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(2, room.getMembers().size());
        assertNull(room.getPictureUpdatedAt());
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
        assertTrue(
          room.getMembers().stream().filter(member -> member.getUserId().equals(user1Id)).findAny().get().isOwner());
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user1Id.toString()),
          eq(RoomCreatedEvent.create(roomOneToOne1Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user2Id.toString()),
          eq(RoomCreatedEvent.create(roomOneToOne1Id).from(user1Id)));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1)).createRoom(roomOneToOne1, user1Id.toString());
        verify(messageDispatcher, times(1)).addUsersToContacts(user1Id.toString(), user2Id.toString());
        verifyNoMoreInteractions(messageDispatcher);
      }

      @Test
      @DisplayName("There are less than two members when creating a one to one, it throws a 'bad request' exception")
      public void createRoomOneToOne_errorWhenMembersAreLessThanTwo() {
        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room1")
          .description("Room one")
          .type(RoomTypeDto.ONE_TO_ONE)
          .membersIds(List.of());
        ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
          roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
        assertEquals("Bad Request - Only 2 users can participate to a one-to-one room", exception.getMessage());
      }

      @Test
      @DisplayName("There are more than two members when creating a one to one with the requester in it, it throws a 'bad request' exception")
      public void createRoomOneToOne_errorWhenMembersAreMoreThanTwo() {
        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room1")
          .description("Room one")
          .type(RoomTypeDto.ONE_TO_ONE)
          .membersIds(List.of(user2Id, user3Id));
        ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
          roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
        assertEquals("Bad Request - Only 2 users can participate to a one-to-one room", exception.getMessage());
      }

      @Test
      @DisplayName("Given creation fields for a one to one room, if there is a room with those users returns a status code 409")
      public void createRoomOneToOne_testOneToOneAlreadyExists() throws Exception {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal))
          .thenReturn(true);
        when(roomRepository.getOneToOneByAllUserIds(user1Id.toString(), user2Id.toString())).thenReturn(
          Optional.of(roomOneToOne1));
        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("room1")
          .description("Room one")
          .type(RoomTypeDto.ONE_TO_ONE)
          .membersIds(List.of(user2Id));
        ChatsHttpException exception = assertThrows(ConflictException.class, () ->
          roomService.createRoom(creationFields, mockUserPrincipal));
        assertEquals(Status.CONFLICT, exception.getHttpStatus());
        assertEquals("Conflict - The one to one room already exists for these users", exception.getMessage());
      }
    }

    @Nested
    @DisplayName("Create workspace room tests")
    class CreateWorkspaceRoomTests {

      @Test
      @DisplayName("It creates the first workspace room and returns it")
      public void createWorkspaceRoom_insertFirstWorkspaceTestOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal))
          .thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal))
          .thenReturn(true);
        when(
          membersService.initRoomSubscriptions(eq(Arrays.asList(user2Id, user3Id)), any(Room.class),
            eq(mockUserPrincipal)))
          .thenReturn(Stream.of(user2Id, user3Id, user1Id).map(userId ->
            Subscription.create(roomWorkspace1, userId.toString()).owner(userId.equals(user1Id))
          ).collect(Collectors.toList()));
        when(roomUserSettingsRepository.getWorkspaceMaxRanksMapByUsers(
          List.of(user2Id.toString(), user3Id.toString(), user1Id.toString())))
          .thenReturn(new HashMap<>());
        when(roomRepository.insert(roomWorkspace1)).thenReturn(roomWorkspace1);

        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("workspace1")
          .description("Workspace one")
          .type(RoomTypeDto.WORKSPACE)
          .membersIds(List.of(user2Id, user3Id));
        RoomInfoDto room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomWorkspace1Id);
          uuid.when(() -> UUID.fromString(roomWorkspace1.getId())).thenReturn(roomWorkspace1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));

        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
        assertTrue(
          room.getMembers().stream().filter(member -> member.getUserId().equals(user1Id)).findAny().get().isOwner());

        assertEquals(1, room.getUserSettings().getRank());

        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user1Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace1Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user2Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace1Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user3Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace1Id).from(user1Id)));
        verifyNoMoreInteractions(eventDispatcher);
        verifyNoInteractions(messageDispatcher);
      }


      @Test
      @DisplayName("It creates the nth workspace room and returns it")
      public void createWorkspaceRoom_insertNthWorkspaceTestOk() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(userService.userExists(user2Id, mockUserPrincipal))
          .thenReturn(true);
        when(userService.userExists(user3Id, mockUserPrincipal))
          .thenReturn(true);
        when(
          membersService.initRoomSubscriptions(eq(Arrays.asList(user2Id, user3Id)), any(Room.class),
            eq(mockUserPrincipal)))
          .thenReturn(Stream.of(user2Id, user3Id, user1Id).map(userId ->
            Subscription.create(roomWorkspace2, userId.toString()).owner(userId.equals(user1Id))
          ).collect(Collectors.toList()));
        when(roomUserSettingsRepository.getWorkspaceMaxRanksMapByUsers(
          List.of(user2Id.toString(), user3Id.toString(), user1Id.toString())))
          .thenReturn(Map.of(
            user1Id.toString(), RoomUserSettings.create(Room.create().id(UUID.randomUUID().toString()), user1Id.toString()).rank(9),
            user2Id.toString(), RoomUserSettings.create(Room.create().id(UUID.randomUUID().toString()), user2Id.toString()).rank(8),
            user3Id.toString(), RoomUserSettings.create(Room.create().id(UUID.randomUUID().toString()), user3Id.toString()).rank(7)));

        when(roomRepository.insert(roomWorkspace2)).thenReturn(roomWorkspace2);

        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("workspace2")
          .description("Workspace two")
          .type(RoomTypeDto.WORKSPACE)
          .membersIds(List.of(user2Id, user3Id));
        RoomInfoDto room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomWorkspace2Id);
          uuid.when(() -> UUID.fromString(roomWorkspace2.getId())).thenReturn(roomWorkspace2Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          uuid.when(() -> UUID.fromString(user2Id.toString())).thenReturn(user2Id);
          uuid.when(() -> UUID.fromString(user3Id.toString())).thenReturn(user3Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(3, room.getMembers().size());
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
        assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
        assertTrue(
          room.getMembers().stream().filter(member -> member.getUserId().equals(user1Id)).findAny().get().isOwner());

        assertEquals(10, room.getUserSettings().getRank());

        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user1Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace2Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user2Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace2Id).from(user1Id)));
        verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user3Id.toString()),
          eq(RoomCreatedEvent.create(roomWorkspace2Id).from(user1Id)));
        verifyNoMoreInteractions(eventDispatcher);
        verifyNoInteractions(messageDispatcher);
      }

      @Test
      @DisplayName("There are less than two members when creating a group, it throws a 'bad request' exception")
      public void createWorkspaceRoom_errorWhenMembersAreLessThanTwo() {
        RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
          .name("workspace1")
          .description("Workspace one")
          .type(RoomTypeDto.WORKSPACE)
          .membersIds(List.of(user2Id));
        ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
          roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
        assertEquals("Bad Request - Too few members (required at least 3)", exception.getMessage());
      }
    }

    @Test
    @DisplayName("If there are duplicate invites, it throws a 'bad request' exception")
    public void createRoom_testRoomToCreateWithDuplicateInvites() {
      RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
        .name("room1")
        .description("Room one")
        .type(RoomTypeDto.GROUP)
        .membersIds(List.of(user2Id, user2Id));
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Members cannot be duplicated", exception.getMessage());
    }

    @Test
    @DisplayName("If the current user is invited, it throws a 'bad request' exception")
    public void createRoom_testRoomToCreateWithInvitedUsersListContainsCurrentUser() {
      RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
        .name("room1")
        .description("Room one")
        .type(RoomTypeDto.GROUP)
        .membersIds(List.of(user1Id, user2Id));
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Requester can't be invited to the room", exception.getMessage());
    }

    @Test
    @DisplayName("If there is an invitee without account, it throws a 'not found' exception")
    public void createRoom_testInvitedUserWithoutAccount() {
      UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
      when(userService.userExists(user2Id, mockUserPrincipal))
        .thenReturn(false);

      RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
        .name("room1")
        .description("Room one")
        .type(RoomTypeDto.GROUP)
        .membersIds(List.of(user2Id, user3Id));
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.createRoom(creationFields, mockUserPrincipal));
      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - User with identifier '%s' not found", user2Id), exception.getMessage());
    }

  }

  @Nested
  @DisplayName("Update room tests")
  class UpdateRoomTest {

    @Test
    @DisplayName("It correctly updates the room")
    public void updateRoom_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(
        Optional.of(roomGroup1.name("room1-to-change").description("Room one to change")));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString()))
        .thenReturn(
          Optional.of(RoomUserSettings.create(roomGroup1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("room1-changed")
        .description("Room one changed");
      RoomDto room = roomService.updateRoom(roomGroup1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(roomGroup1Id, room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        RoomUpdatedEvent.create(roomGroup1Id).from(user1Id));
      verify(messageDispatcher, times(1)).updateRoomName(roomGroup1Id.toString(), user1Id.toString(), "room1-changed");
      verify(messageDispatcher, times(1)).updateRoomDescription(roomGroup1Id.toString(), user1Id.toString(),
        "Room one changed");
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoMoreInteractions(messageDispatcher);

      reset(roomRepository, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void updateRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.updateRoom(roomGroup1Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't member of required room, it throws a 'forbidden' exception")
    public void updateRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.updateRoom(roomGroup2Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't owner of required room, it throws a 'forbidden' exception")
    public void updateRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(
        Optional.of(roomGroup1.name("room1-changed").description("Room one changed")));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.updateRoom(roomGroup1Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Delete room tests")
  class DeleteRoomTests {

    @Test
    @DisplayName("Deletes the required room")
    public void deleteRoom_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        RoomDeletedEvent.create(roomGroup1Id));
      verifyNoMoreInteractions(eventDispatcher);
      verify(messageDispatcher, times(1)).deleteRoom(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("If the room doesn't exist then throws a 'not found' exception")
    public void deleteRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member then throws a 'forbidden' exception")
    public void deleteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.deleteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner then throws a 'forbidden' exception")
    public void deleteRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Reset room hash tests")
  class ResetRoomHashTests {

    @Test
    @DisplayName("It changes the room hash and returns it")
    public void resetRoomHash_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      String oldHash = roomGroup1.getHash();
      when(roomRepository.update(roomGroup1)).thenReturn(roomGroup1);

      HashDto hashDto = roomService.resetRoomHash(roomGroup1Id, UserPrincipal.create(user1Id));

      assertNotNull(hashDto);
      assertNotEquals(oldHash, hashDto.getHash());
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(roomGroup1Id.toString()),
        eq(RoomHashResetEvent.create(roomGroup1Id).hash(hashDto.getHash())));
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'not found' exception")
    public void resetRoomHash_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.resetRoomHash(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner, it throws a 'not found' exception")
    public void resetRoomHash_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.resetRoomHash(roomGroup1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void resetRoomHash_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.resetRoomHash(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Mute room tests")
  class MuteRoomTests {

    @Test
    @DisplayName("Mute the current user in a specific room when user settings not exists")
    void muteRoom_testOkUserSettingNotExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.empty());
      when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
      when(clock.getZone()).thenReturn(ZoneId.systemDefault());
      roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .save(RoomUserSettings.create(roomGroup1, user1Id.toString())
          .mutedUntil(OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        UserMutedEvent.create(roomGroup1Id).memberId(user1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Mute the current user in a specific room when user settings exists")
    void muteRoom_testOkUserSettingExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      RoomUserSettings roomUserSettings = RoomUserSettings.create(roomGroup1, user1Id.toString());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.of(roomUserSettings));
      when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
      when(clock.getZone()).thenReturn(ZoneId.systemDefault());
      roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .save(roomUserSettings
          .mutedUntil(OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        UserMutedEvent.create(roomGroup1Id).memberId(user1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Correctly does nothing if the user is already muted")
    void muteRoom_testOkUserAlreadyMuted() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.of(RoomUserSettings
          .create(roomGroup1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));
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
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup2Id.toString(), user1Id.toString())).thenReturn(
        Optional.empty());

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.muteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void muteRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Unmute room tests")
  class UnmuteRoomTests {

    @Test
    @DisplayName("Correctly does nothing if user settings not exists")
    void unmuteRoom_testOkUserSettingNotExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.empty());

      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository);
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("Unmute the current user in a specific room")
    void unmuteRoom_testOkUserSettingExists() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      RoomUserSettings roomUserSettings = RoomUserSettings.create(roomGroup1, user1Id.toString())
        .mutedUntil(OffsetDateTime.now());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.of(roomUserSettings));
      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .save(roomUserSettings.mutedUntil(null));
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        UserUnmutedEvent.create(roomGroup1Id).memberId(user1Id));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Correctly does nothing if the user has already unmuted")
    void unmuteRoom_testOkUserAlreadyUnmuted() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString())).thenReturn(
        Optional.of(RoomUserSettings.create(roomGroup1, user1Id.toString())));
      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
        .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository);
      verifyNoInteractions(eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void unmuteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomGroup2Id.toString(), user1Id.toString())).thenReturn(
        Optional.empty());

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.unmuteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void unmuteRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.muteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Get room and check user tests")
  class GetRoomAndCheckUserTests {

    @Test
    @DisplayName("It returns the requested room")
    public void getRoomAndCheckUser_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      Room room = roomService.getRoomAndCheckUser(roomGroup1Id, UserPrincipal.create(user1Id), false);

      assertEquals(roomGroup1, room);
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(roomRepository);
    }

    @Test
    @DisplayName("If the user is a system user, it returns the requested room")
    public void getRoomAndCheckUser_testOkSystemUserAndNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      Room room = roomService.getRoomAndCheckUser(roomGroup2Id, UserPrincipal.create(user1Id).systemUser(true), false);

      assertEquals(roomGroup2.getId(), room.getId());
      assertEquals(roomGroup2.getName(), room.getName());

      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verifyNoMoreInteractions(roomRepository);
    }

    @Test
    @DisplayName("If the user isn't a room member throws 'forbidden' exception")
    public void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomAndCheckUser(roomGroup2Id, UserPrincipal.create(user1Id), false));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the user isn't a room owner throws 'forbidden' exception")
    public void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomAndCheckUser(roomGroup1Id, UserPrincipal.create(user2Id), true));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Get room picture tests")
  class GetRoomPictureTests {

    @Test
    @DisplayName("It returns the room picture")
    void getRoomPicture_testOk() {
      FileMetadata pfpMetadata = FileMetadata.create().type(FileMetadataType.ROOM_AVATAR)
        .roomId(roomGroup1Id.toString())
        .userId(user2Id.toString()).mimeType("mime/type").id(roomGroup1Id.toString()).name("pfp").originalSize(123L);
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(roomGroup1Id.toString(), user2Id.toString())).thenReturn(file);

      FileContentAndMetadata roomPicture = roomService.getRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id));

      assertEquals(file, roomPicture.getFile());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1)).getById(roomGroup1Id.toString());
      verify(storagesService, times(1)).getFileById(roomGroup1Id.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("If the user is a system user, it returns the room picture")
    void getRoomPicture_testOkWithSystemUser() {
      FileMetadata pfpMetadata = FileMetadata.create().type(FileMetadataType.ROOM_AVATAR)
        .roomId(roomGroup2Id.toString())
        .userId(user2Id.toString()).mimeType("mime/type").id(roomGroup2Id.toString()).name("pfp").originalSize(123L);
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(fileMetadataRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(roomGroup2Id.toString(), user2Id.toString())).thenReturn(file);

      FileContentAndMetadata roomPicture = roomService.getRoomPicture(roomGroup2Id,
        UserPrincipal.create(user1Id).systemUser(true));

      assertEquals(file, roomPicture.getFile());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verify(fileMetadataRepository, times(1)).getById(roomGroup2Id.toString());
      verify(storagesService, times(1)).getFileById(roomGroup2Id.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void getRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> roomService.getRoomPicture(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Set room picture tests")
  class SetRoomPictureTests {

    @Test
    @DisplayName("It sets the room picture if it didn't exists")
    void setRoomPicture_testOkInsert() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.empty());
      when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
      when(clock.getZone()).thenReturn(ZoneId.systemDefault());
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(roomGroup1Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(roomGroup1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id));

      roomGroup1.pictureUpdatedAt(
        OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()));
      FileMetadata expectedMetadata = FileMetadataBuilder.create().id(roomGroup1.getId()).roomId(roomGroup1.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).update(roomGroup1);
      verify(fileMetadataRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(storagesService, times(0)).deleteFile(anyString(), anyString());
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        RoomPictureChangedEvent.create(roomGroup1Id).from(user1Id));
      verify(messageDispatcher, times(1)).updateRoomPictures(roomGroup1Id.toString(), user1Id.toString(),
        roomGroup1Id.toString(), "picture");
    }

    @Test
    @DisplayName("It update the room picture if it already exists")
    void setRoomPicture_testOkUpdate() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.getById(roomGroup1Id.toString()))
        .thenReturn(Optional.of(FileMetadata.create().id("123").type(FileMetadataType.ROOM_AVATAR)
          .roomId(roomGroup1Id.toString()).userId("fake-old-user")));
      when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
      when(clock.getZone()).thenReturn(ZoneId.systemDefault());
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(roomGroup1Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(roomGroup1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id));

      roomGroup1.pictureUpdatedAt(
        OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()));
      FileMetadata expectedMetadata = FileMetadataBuilder.create().id("123").roomId(roomGroup1.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).update(roomGroup1);
      verify(fileMetadataRepository, times(1)).getById(roomGroup1Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(storagesService, times(1)).deleteFile("123", "fake-old-user");
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup1Id.toString(),
        RoomPictureChangedEvent.create(roomGroup1Id).from(user1Id));
      verify(messageDispatcher, times(1)).updateRoomPictures(roomGroup1Id.toString(), user1Id.toString(),
        "123", "picture");
    }

    @Test
    @DisplayName("If the user is a system user and not a member, it sets the room picture")
    void setRoomPicture_testOkWithSystemUser() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(fileMetadataRepository.getById(roomGroup2Id.toString()))
        .thenReturn(Optional.of(FileMetadata.create().id("123").type(FileMetadataType.ROOM_AVATAR)
          .roomId(roomGroup2Id.toString())));
      when(clock.instant()).thenReturn(Instant.parse("2022-01-01T00:00:00Z"));
      when(clock.getZone()).thenReturn(ZoneId.systemDefault());
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(roomGroup2Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(roomGroup2Id, file, "image/jpeg", "picture",
        UserPrincipal.create(user1Id).systemUser(true));

      roomGroup2.pictureUpdatedAt(
        OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()));
      FileMetadata expectedMetadata = FileMetadataBuilder.create().id("123").roomId(roomGroup2.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(roomGroup2Id.toString());
      verify(roomRepository, times(1)).update(roomGroup2);
      verify(fileMetadataRepository, times(1)).getById(roomGroup2Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, roomGroup2Id.toString(),
        RoomPictureChangedEvent.create(roomGroup2Id).from(user1Id));
      verify(messageDispatcher, times(1)).updateRoomPictures(roomGroup2Id.toString(), user1Id.toString(),
        "123", "picture");
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void setRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      File file = mock(File.class);
      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> roomService.setRoomPicture(roomGroup2Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the user is not a room owner, it throws a ForbiddenException")
    void setRoomPicture_failsIfUserIsNotOwner() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      File file = mock(File.class);
      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> roomService.setRoomPicture(roomGroup2Id, file, "image/jpeg", "picture", UserPrincipal.create(user3Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user3Id, roomGroup2Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the file is too large, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsTooBig() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      File file = mock(File.class);
      when(file.length()).thenReturn(257L * 1024);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(roomGroup1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals(String.format("Bad Request - The room picture cannot be greater than %d KB", 256),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the room is not a group, It throws a BadRequestException")
    void setRoomPicture_failsIfRoomIsNotAGroup() {
      when(roomRepository.getById(roomOneToOne2Id.toString())).thenReturn(Optional.of(roomOneToOne2));
      File file = mock(File.class);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(roomOneToOne2Id, file, "image/jpeg", "picture",
          UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - The room picture can only be set to group type rooms",
        exception.getMessage());
    }

    @Test
    @DisplayName("If the picture is not an image, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsNotAnImage() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      File file = mock(File.class);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(roomGroup1Id, file, "text/html", "picture", UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - The room picture must be an image",
        exception.getMessage());
    }
  }
}