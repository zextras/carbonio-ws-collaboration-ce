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
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
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

import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.io.File;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
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

  public RoomServiceImplTest(RoomMapper roomMapper) {
    this.roomRepository = mock(RoomRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.userService = mock(UserService.class);
    this.membersService = mock(MembersService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.storagesService = mock(StoragesService.class);
    this.roomService = new RoomServiceImpl(
      this.roomRepository,
      this.roomUserSettingsRepository,
      roomMapper,
      this.eventDispatcher,
      this.messageDispatcher,
      this.userService,
      this.membersService,
      this.fileMetadataRepository,
      this.storagesService);
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID room1Id;
  private UUID room2Id;
  private UUID room3Id;
  private UUID room4Id;
  private Room room1;
  private Room room2;
  private Room room3;
  private Room room4;

  @BeforeEach
  public void init() {
    user1Id = UUID.fromString("332a9527-3388-4207-be77-6d7e2978a723");
    user2Id = UUID.fromString("82735f6d-4c6c-471e-99d9-4eef91b1ec45");
    user3Id = UUID.fromString("ea7b9b61-bef5-4cf4-80cb-19612c42593a");

    room1Id = UUID.fromString("cdc44826-23b0-4e99-bec2-7fb2f00b6b13");
    room2Id = UUID.fromString("86327874-40f4-47cb-914d-f0ce706d1611");
    room3Id = UUID.fromString("0471809c-e0bb-4bfd-85b6-b7b9a1eca597");
    room4Id = UUID.fromString("19e5717e-652d-409e-b4fa-87e8dff790c1");

    room1 = Room.create();
    room1
      .id(room1Id.toString())
      .type(RoomTypeDto.GROUP)
      .name("room1")
      .description("Room one")
      .subscriptions(List.of(
        Subscription.create(room1, user1Id.toString()).owner(true),
        Subscription.create(room1, user2Id.toString()).owner(false),
        Subscription.create(room1, user3Id.toString()).owner(false)));

    room2 = Room.create();
    room2
      .id(room2Id.toString())
      .type(RoomTypeDto.ONE_TO_ONE)
      .name("room2")
      .description("Room two")
      .subscriptions(List.of(
        Subscription.create(room2, user1Id.toString()).owner(true),
        Subscription.create(room2, user2Id.toString()).owner(false)));

    room3 = Room.create();
    room3
      .id(room3Id.toString())
      .type(RoomTypeDto.GROUP)
      .name("room3")
      .description("Room three")
      .subscriptions(List.of(
        Subscription.create(room3, user2Id.toString()).owner(true),
        Subscription.create(room3, user3Id.toString()).owner(false)));

    room4 = Room.create();
    room4
      .id(room4Id.toString())
      .type(RoomTypeDto.ONE_TO_ONE)
      .name("room4")
      .description("One to one")
      .subscriptions(List.of(
        Subscription.create(room4, user1Id.toString()).owner(false),
        Subscription.create(room4, user2Id.toString()).owner(false)));
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
    @DisplayName("Returns all rooms of which the authenticated user is a member")
    public void getRooms_testOk() {
      when(roomRepository.getByUserId(user1Id.toString(), false)).thenReturn(Arrays.asList(room1, room2));
      List<RoomDto> rooms = roomService.getRooms(null, UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(room1Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(room2Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
    }
  }

  @Nested
  @DisplayName("Get room by id tests")
  class GetRoomByIdTests {

    @Test
    @DisplayName("Returns the required room with all members and room user settings")
    public void getRoomById_testOk() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(room1Id.toString(), user1Id.toString()))
        .thenReturn(Optional.of(RoomUserSettings.create(room1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));
      RoomInfoDto room = roomService.getRoomById(room1Id, UserPrincipal.create(user1Id));

      assertEquals(room1Id, room.getId());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertNotNull(room.getUserSettings());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the user is a system user, it returns the required room with all members and room user settings")
    public void getRoomById_testWithSystemUser() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(room1Id.toString(), user1Id.toString()))
        .thenReturn(Optional.of(RoomUserSettings.create(room1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));
      RoomInfoDto room = roomService.getRoomById(room1Id, UserPrincipal.create(user1Id).systemUser(true));

      assertEquals(room1Id, room.getId());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertNotNull(room.getUserSettings());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the user didn't set anything, it correctly returns the required room with all members and default room user settings")
    public void getRoomById_testMemberWithoutSettings() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(room1Id.toString(), user1Id.toString()))
        .thenReturn(Optional.empty());
      RoomInfoDto room = roomService.getRoomById(room1Id, UserPrincipal.create(user1Id));

      assertEquals(room1Id, room.getId());
      assertEquals(3, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertNotNull(room.getUserSettings());
      assertFalse(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void getRoomById_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.getRoomById(room1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", room1Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    public void getRoomById_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomById(room3Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Create room tests")
  class CreateRoomTests {

    @Test
    @DisplayName("It creates the room and returns it")
    public void createRoom_testOk() {
      UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
      when(userService.userExists(user2Id, mockUserPrincipal))
        .thenReturn(true);
      when(userService.userExists(user3Id, mockUserPrincipal))
        .thenReturn(true);
      when(
        membersService.initRoomSubscriptions(eq(Arrays.asList(user2Id, user3Id)), any(Room.class),
          eq(mockUserPrincipal)))
        .thenReturn(Stream.of(user2Id, user3Id, user1Id).map(userId ->
          Subscription.create(room1, userId.toString())
        ).collect(Collectors.toList()));
      when(roomRepository.insert(any(Room.class))).thenReturn(room1);

      RoomCreationFieldsDto creationFields = RoomCreationFieldsDto.create()
        .name("room1")
        .description("Room one")
        .type(RoomTypeDto.GROUP)
        .membersIds(List.of(user2Id, user3Id));
      RoomInfoDto room = roomService.createRoom(creationFields, mockUserPrincipal);
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
        eq(RoomCreatedEvent.create(room1Id).from(user1Id)));
      verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user2Id.toString()),
        eq(RoomCreatedEvent.create(room1Id).from(user1Id)));
      verify(eventDispatcher, times(1)).sendToQueue(eq(user1Id), eq(user3Id.toString()),
        eq(RoomCreatedEvent.create(room1Id).from(user1Id)));
      verifyNoMoreInteractions(eventDispatcher);
      verify(messageDispatcher, times(1)).createRoom(room1, user1Id.toString());
      verifyNoMoreInteractions(messageDispatcher);
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
      when(roomRepository.getById(room1Id.toString())).thenReturn(
        Optional.of(room1.name("room1-changed").description("Room one changed")));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(room1Id.toString(), user1Id.toString()))
        .thenReturn(Optional.of(RoomUserSettings.create(room1, user1Id.toString()).mutedUntil(OffsetDateTime.now())));

      RoomEditableFieldsDto roomEditableFieldsDto = RoomEditableFieldsDto.create().name("room1-changed")
        .description("Room one changed");
      RoomDto room = roomService.updateRoom(room1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(room1Id, room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      verify(eventDispatcher, times(1)).sendToTopic(user1Id, room1Id.toString(),
        RoomUpdatedEvent.create(room1Id).from(user1Id));
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoInteractions(messageDispatcher);

      reset(roomRepository, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void updateRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.updateRoom(room1Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", room1Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't member of required room, it throws a 'forbidden' exception")
    public void updateRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.updateRoom(room3Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't owner of required room, it throws a 'forbidden' exception")
    public void updateRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(
        Optional.of(room1.name("room1-changed").description("Room one changed")));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.updateRoom(room1Id,
          RoomEditableFieldsDto.create().name("room1-changed").description("Room one changed"),
          UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, room1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Delete room tests")
  class DeleteRoomTests {

    @Test
    @DisplayName("Deletes the required room")
    public void deleteRoom_testOk() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));

      roomService.deleteRoom(room1Id, UserPrincipal.create(user1Id));

      verify(eventDispatcher, times(1)).sendToTopic(user1Id, room1Id.toString(),
        RoomDeletedEvent.create(room1Id));
      verifyNoMoreInteractions(eventDispatcher);
      verify(messageDispatcher, times(1)).deleteRoom(room1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("If the room doesn't exist then throws a 'not found' exception")
    public void deleteRoom_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.deleteRoom(room1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", room1Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member then throws a 'forbidden' exception")
    public void deleteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.deleteRoom(room3Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner then throws a 'forbidden' exception")
    public void deleteRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.deleteRoom(room1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, room1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Reset room hash tests")
  class ResetRoomHashTests {

    @Test
    @DisplayName("It changes the room hash and returns it")
    public void resetRoomHash_testOk() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      String oldHash = room1.getHash();
      when(roomRepository.update(room1)).thenReturn(room1);

      HashDto hashDto = roomService.resetRoomHash(room1Id, UserPrincipal.create(user1Id));

      assertNotNull(hashDto);
      assertNotEquals(oldHash, hashDto.getHash());
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(room1Id.toString()),
        eq(RoomHashResetEvent.create(room1Id).hash(hashDto.getHash())));
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'not found' exception")
    public void resetRoomHash_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.resetRoomHash(room3Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner, it throws a 'not found' exception")
    public void resetRoomHash_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.resetRoomHash(room1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, room1Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    public void resetRoomHash_testRoomNotExists() {
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        roomService.resetRoomHash(room1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - Room '%s'", room1Id), exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Mute room tests")
  class MuteRoomTests {

    @Test
    void muteRoom() {
    }

    @Test
    void unmuteRoom() {
    }
  }

  @Nested
  @DisplayName("Get room and check user tests")
  class GetRoomAndCheckUserTests {

    @Test
    @DisplayName("It returns the requested room")
    public void getRoomAndCheckUser_testOk() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      Room room = roomService.getRoomAndCheckUser(room1Id, UserPrincipal.create(user1Id), false);

      assertEquals(room1, room);
      verify(roomRepository, times(1)).getById(room1Id.toString());
      verifyNoMoreInteractions(roomRepository);
    }

    @Test
    @DisplayName("If the user is a system user, it returns the requested room")
    public void getRoomAndCheckUser_testOkSystemUserAndNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));
      Room room = roomService.getRoomAndCheckUser(room3Id, UserPrincipal.create(user1Id).systemUser(true), false);

      assertEquals(room3.getId(), room.getId());
      assertEquals(room3.getName(), room.getName());

      verify(roomRepository, times(1)).getById(room3Id.toString());
      verifyNoMoreInteractions(roomRepository);
    }

    @Test
    @DisplayName("If the user isn't a room member throws 'forbidden' exception")
    public void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomAndCheckUser(room3Id, UserPrincipal.create(user1Id), false));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the user isn't a room owner throws 'forbidden' exception")
    public void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        roomService.getRoomAndCheckUser(room1Id, UserPrincipal.create(user2Id), true));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not an owner of room '%s'", user2Id, room1Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Get room picture tests")
  class GetRoomPictureTests {

    @Test
    @DisplayName("It returns the room picture")
    void getRoomPicture_testOk() {
      FileMetadata pfpMetadata = FileMetadata.create().type(FileMetadataType.ROOM_AVATAR).roomId(room1Id.toString())
        .userId(user2Id.toString()).mimeType("mime/type").id(room1Id.toString()).name("pfp").originalSize(123L);
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(fileMetadataRepository.getById(room1Id.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(room1Id.toString(), user2Id.toString())).thenReturn(file);

      FileContentAndMetadata roomPicture = roomService.getRoomPicture(room1Id, UserPrincipal.create(user1Id));

      assertEquals(file, roomPicture.getFile());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(roomRepository, times(1)).getById(room1Id.toString());
      verify(fileMetadataRepository, times(1)).getById(room1Id.toString());
      verify(storagesService, times(1)).getFileById(room1Id.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("If the user is a system user, it returns the room picture")
    void getRoomPicture_testOkWithSystemUser() {
      FileMetadata pfpMetadata = FileMetadata.create().type(FileMetadataType.ROOM_AVATAR).roomId(room3Id.toString())
        .userId(user2Id.toString()).mimeType("mime/type").id(room3Id.toString()).name("pfp").originalSize(123L);
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));
      when(fileMetadataRepository.getById(room3Id.toString())).thenReturn(Optional.of(pfpMetadata));
      File file = new File("test");
      when(storagesService.getFileById(room3Id.toString(), user2Id.toString())).thenReturn(file);

      FileContentAndMetadata roomPicture = roomService.getRoomPicture(room3Id,
        UserPrincipal.create(user1Id).systemUser(true));

      assertEquals(file, roomPicture.getFile());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(roomRepository, times(1)).getById(room3Id.toString());
      verify(fileMetadataRepository, times(1)).getById(room3Id.toString());
      verify(storagesService, times(1)).getFileById(room3Id.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void getRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));

      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> roomService.getRoomPicture(room3Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Set room picture tests")
  class SetRoomPictureTests {

    @Test
    @DisplayName("It sets the room picture if it didn't exists")
    void setRoomPicture_testOkInsert() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(fileMetadataRepository.getById(room1Id.toString())).thenReturn(Optional.empty());
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(room1Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(room1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id));

      FileMetadata expectedMetadata = FileMetadataBuilder.create().id(room1.getId()).roomId(room1.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(room1Id.toString());
      verify(fileMetadataRepository, times(1)).getById(room1Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(storagesService, times(0)).deleteFile(anyString(), anyString());
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, room1Id.toString(),
        RoomPictureChangedEvent.create(room1Id).from(user1Id));
      verify(messageDispatcher, times(1)).sendMessageToRoom(room1Id.toString(), user1Id.toString(),
        "Updated room picture");
    }

    @Test
    @DisplayName("It update the room picture if it already exists")
    void setRoomPicture_testOkUpdate() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      when(fileMetadataRepository.getById(room1Id.toString()))
        .thenReturn(Optional.of(FileMetadata.create().id("123").type(FileMetadataType.ROOM_AVATAR)
          .roomId(room1Id.toString()).userId("fake-old-user")));
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(room1Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(room1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id));

      FileMetadata expectedMetadata = FileMetadataBuilder.create().id("123").roomId(room1.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(room1Id.toString());
      verify(fileMetadataRepository, times(1)).getById(room1Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(storagesService, times(1)).deleteFile("123", "fake-old-user");
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, room1Id.toString(),
        RoomPictureChangedEvent.create(room1Id).from(user1Id));
      verify(messageDispatcher, times(1)).sendMessageToRoom(room1Id.toString(), user1Id.toString(),
        "Updated room picture");
    }

    @Test
    @DisplayName("If the user is a system user and not a member, it sets the room picture")
    void setRoomPicture_testOkWithSystemUser() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));
      when(fileMetadataRepository.getById(room3Id.toString()))
        .thenReturn(Optional.of(FileMetadata.create().id("123").type(FileMetadataType.ROOM_AVATAR)
          .roomId(room3Id.toString())));
      File file = mock(File.class);
      when(file.length()).thenReturn(123L);
      when(storagesService.getFileById(room3Id.toString(), user2Id.toString())).thenReturn(file);

      roomService.setRoomPicture(room3Id, file, "image/jpeg", "picture",
        UserPrincipal.create(user1Id).systemUser(true));

      FileMetadata expectedMetadata = FileMetadataBuilder.create().id("123").roomId(room3.getId())
        .mimeType("image/jpeg").type(FileMetadataType.ROOM_AVATAR).name("picture").originalSize(123L)
        .userId(user1Id.toString());
      verify(roomRepository, times(1)).getById(room3Id.toString());
      verify(fileMetadataRepository, times(1)).getById(room3Id.toString());
      verify(fileMetadataRepository, times(1)).save(expectedMetadata);
      verify(storagesService, times(1)).saveFile(file, expectedMetadata, user1Id.toString());
      verify(eventDispatcher, times(1)).sendToTopic(user1Id, room3Id.toString(),
        RoomPictureChangedEvent.create(room3Id).from(user1Id));
      verify(messageDispatcher, times(1)).sendMessageToRoom(room3Id.toString(), user1Id.toString(),
        "Updated room picture");
    }

    @Test
    @DisplayName("If the user is not a room member, It throws a ForbiddenException")
    void setRoomPicture_failsIfUserIsNotPartOfTheRoom() {
      when(roomRepository.getById(room3Id.toString())).thenReturn(Optional.of(room3));
      File file = mock(File.class);
      ForbiddenException exception = assertThrows(ForbiddenException.class,
        () -> roomService.setRoomPicture(room3Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of room '%s'", user1Id, room3Id),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the file is too large, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsTooBig() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      File file = mock(File.class);
      when(file.length()).thenReturn(257L * 1024);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(room1Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals(String.format("Bad Request - The room picture cannot be greater than %d KB", 256),
        exception.getMessage());
    }

    @Test
    @DisplayName("If the room is not a group, It throws a BadRequestException")
    void setRoomPicture_failsIfRoomIsNotAGroup() {
      when(roomRepository.getById(room4Id.toString())).thenReturn(Optional.of(room4));
      File file = mock(File.class);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(room4Id, file, "image/jpeg", "picture", UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - The room picture can only be set to group type rooms",
        exception.getMessage());
    }

    @Test
    @DisplayName("If the picture is not an image, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsNotAnImage() {
      when(roomRepository.getById(room1Id.toString())).thenReturn(Optional.of(room1));
      File file = mock(File.class);
      BadRequestException exception = assertThrows(BadRequestException.class,
        () -> roomService.setRoomPicture(room1Id, file, "text/html", "picture", UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - The room picture must be an image",
        exception.getMessage());
    }
  }
}