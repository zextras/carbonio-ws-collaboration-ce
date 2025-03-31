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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.ParticipantBuilder;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomCreated;
import com.zextras.carbonio.chats.core.data.event.RoomDeleted;
import com.zextras.carbonio.chats.core.data.event.RoomHistoryCleared;
import com.zextras.carbonio.chats.core.data.event.RoomMuted;
import com.zextras.carbonio.chats.core.data.event.RoomPictureChanged;
import com.zextras.carbonio.chats.core.data.event.RoomPictureDeleted;
import com.zextras.carbonio.chats.core.data.event.RoomUnmuted;
import com.zextras.carbonio.chats.core.data.event.RoomUpdated;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@UnitTest
class RoomServiceImplTest {

  private final RoomService roomService;
  private final AttachmentService attachmentService;
  private final RoomRepository roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final MessageDispatcher messageDispatcher;
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService storagesService;
  private final Clock clock;
  private final CapabilityService capabilityService;

  public RoomServiceImplTest(RoomMapper roomMapper) {
    this.roomRepository = mock(RoomRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.fileMetadataRepository = mock(FileMetadataRepository.class);
    this.userService = mock(UserService.class);
    this.membersService = mock(MembersService.class);
    this.meetingService = mock(MeetingService.class);
    this.storagesService = mock(StoragesService.class);
    this.attachmentService = mock(AttachmentService.class);
    this.capabilityService = mock(CapabilityService.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.clock = mock(Clock.class);
    AppConfig appConfig = mock(AppConfig.class);
    this.roomService =
        new RoomServiceImpl(
            this.roomRepository,
            this.roomUserSettingsRepository,
            this.fileMetadataRepository,
            this.userService,
            this.membersService,
            this.meetingService,
            this.storagesService,
            this.attachmentService,
            this.capabilityService,
            this.eventDispatcher,
            this.messageDispatcher,
            roomMapper,
            this.clock,
            appConfig);
  }

  private UUID user1Id;
  private UUID user2Id;
  private UUID user3Id;
  private UUID user4Id;
  private UUID user5Id;
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
    user4Id = UUID.fromString("c91f0b6d-220e-408f-8575-5bf3633fc7f7");
    user5Id = UUID.fromString("120bbfbe-b97b-44d0-81ac-2f23bc244878");

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
        .type(RoomTypeDto.GROUP)
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
        .type(RoomTypeDto.GROUP)
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
        .type(RoomTypeDto.TEMPORARY)
        .name("temporary1")
        .description("")
        .subscriptions(
            List.of(Subscription.create(roomTemporary1, user1Id.toString()).owner(true)));

    roomTemporary2 = Room.create();
    roomTemporary2
        .id(roomTemporary2Id.toString())
        .type(RoomTypeDto.TEMPORARY)
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
        .type(RoomTypeDto.ONE_TO_ONE)
        .name("")
        .description("")
        .subscriptions(
            List.of(
                Subscription.create(roomOneToOne1, user1Id.toString()).owner(true),
                Subscription.create(roomOneToOne1, user2Id.toString()).owner(false)));

    roomOneToOne2 = Room.create();
    roomOneToOne2
        .id(roomOneToOne2Id.toString())
        .type(RoomTypeDto.ONE_TO_ONE)
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
        roomUserSettingsRepository,
        userService,
        membersService,
        eventDispatcher,
        messageDispatcher,
        fileMetadataRepository,
        storagesService,
        meetingService);
  }

  @Nested
  @DisplayName("Get rooms tests")
  class GetRoomTests {

    @Test
    @DisplayName(
        "Returns all rooms without members or user settings of which the authenticated user is a"
            + " member")
    void getRooms_testOkBasicRooms() {
      when(roomRepository.getByUserId(user1Id.toString(), false))
          .thenReturn(Arrays.asList(roomGroup2, roomOneToOne1));

      List<RoomDto> rooms = roomService.getRooms(null, UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup2Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertEquals(0, rooms.get(0).getMembers().size());
      assertEquals(0, rooms.get(1).getMembers().size());
      assertNull(rooms.get(0).getUserSettings());
      assertNull(rooms.get(1).getUserSettings());

      assertEquals(
          OffsetDateTime.parse("2022-01-01T00:00:00Z"), rooms.get(0).getPictureUpdatedAt());
      assertNull(rooms.get(1).getPictureUpdatedAt());
    }

    @Test
    @DisplayName(
        "Returns all rooms with members and without user settings of which the authenticated user"
            + " is a member")
    void getRooms_testOkWithMembers() {
      when(roomRepository.getByUserId(user1Id.toString(), true))
          .thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));

      List<RoomDto> rooms =
          roomService.getRooms(List.of(RoomExtraFieldDto.MEMBERS), UserPrincipal.create(user1Id));

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
    @DisplayName(
        "Returns all rooms without members and with user settings of which the authenticated user"
            + " is a member")
    void getRooms_testOkWithSettings() {
      when(roomRepository.getByUserId(user1Id.toString(), false))
          .thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));
      when(roomUserSettingsRepository.getMapGroupedByUserId(user1Id.toString()))
          .thenReturn(
              Map.of(
                  roomGroup1.toString(),
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      List<RoomDto> rooms =
          roomService.getRooms(List.of(RoomExtraFieldDto.SETTINGS), UserPrincipal.create(user1Id));

      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id.toString(), rooms.get(0).getId().toString());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id.toString(), rooms.get(1).getId().toString());
      assertEquals(RoomTypeDto.ONE_TO_ONE, rooms.get(1).getType());
      assertEquals(0, rooms.get(0).getMembers().size());
      assertEquals(0, rooms.get(1).getMembers().size());
      assertNotNull(rooms.get(0).getUserSettings());
      assertNotNull(rooms.get(1).getUserSettings());
    }

    @Test
    @DisplayName("Returns all complete rooms of which the authenticated user is a member")
    void getRooms_testOkCompleteRooms() {
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      when(roomRepository.getByUserId(user1Id.toString(), true))
          .thenReturn(Arrays.asList(roomGroup1, roomOneToOne1));
      when(roomUserSettingsRepository.getMapGroupedByUserId(currentUser.getId()))
          .thenReturn(
              Map.of(
                  roomGroup1Id.toString(),
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      List<RoomDto> rooms =
          roomService.getRooms(
              List.of(RoomExtraFieldDto.MEMBERS, RoomExtraFieldDto.SETTINGS), currentUser);

      assertEquals(2, rooms.size());
      assertEquals(roomGroup1Id, rooms.get(0).getId());
      assertEquals(RoomTypeDto.GROUP, rooms.get(0).getType());
      assertEquals(roomOneToOne1Id, rooms.get(1).getId());
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
    @DisplayName("Returns the required group room with all members and room user settings")
    void getRoomById_groupTestOk() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user2Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomGroup2, user2Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      RoomDto room = roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id));

      assertEquals(roomGroup2Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user3Id)));
      assertNotNull(room.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName(
        "Returns the required room with no profile picture with all members and room user settings")
    void getRoomById_testOkWithoutPicture() {
      when(roomRepository.getById(roomOneToOne1Id.toString()))
          .thenReturn(Optional.of(roomOneToOne1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomOneToOne1Id.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomOneToOne1, user1Id.toString())
                      .mutedUntil(OffsetDateTime.now())));
      RoomDto room = roomService.getRoomById(roomOneToOne1Id, UserPrincipal.create(user1Id));

      assertEquals(roomOneToOne1Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
      assertNotNull(room.getUserSettings());
      assertNull(room.getPictureUpdatedAt());
      assertTrue(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName(
        "If the user didn't set anything, it correctly returns the required room with all members"
            + " and default room user settings")
    void getRoomById_testMemberWithoutSettings() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup2Id.toString(), user2Id.toString()))
          .thenReturn(Optional.empty());
      RoomDto room = roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id));

      assertEquals(roomGroup2Id, room.getId());
      assertEquals(2, room.getMembers().size());
      assertTrue(room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
      assertNotNull(room.getUserSettings());
      assertEquals(OffsetDateTime.parse("2022-01-01T00:00:00Z"), room.getPictureUpdatedAt());
      assertFalse(room.getUserSettings().isMuted());
    }

    @Test
    @DisplayName("If the room doesn't exist, it throws a 'not found' exception")
    void getRoomById_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup2Id), exception.getMessage());
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void getRoomById_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.getRoomById(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
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
      void createGroupRoom_testOk() {
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
        when(capabilityService.getCapabilities(mockUserPrincipal))
            .thenReturn(CapabilitiesDto.create().maxGroupMembers(128));

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
        RoomDto room;
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

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertFalse(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomGroup1Id));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1))
            .createRoom(
                roomGroup1Id.toString(),
                user1Id.toString(),
                List.of(user2Id.toString(), user3Id.toString()));
        verify(messageDispatcher, times(0)).addUsersToContacts(anyString(), anyString());
        verifyNoMoreInteractions(messageDispatcher);
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
        when(capabilityService.getCapabilities(mockUserPrincipal))
            .thenReturn(CapabilitiesDto.create().maxGroupMembers(128));

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id).owner(true),
                        MemberDto.create().userId(user3Id)));
        RoomDto room;
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

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomGroup1Id));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1))
            .createRoom(
                roomGroup1Id.toString(),
                user1Id.toString(),
                List.of(user2Id.toString(), user3Id.toString()));
        verify(messageDispatcher, times(0)).addUsersToContacts(anyString(), anyString());
        verifyNoMoreInteractions(messageDispatcher);
      }

      @Test
      @DisplayName(
          "There are less than two members when creating a group, it throws a 'bad request'"
              + " exception")
      void createGroupRoom_errorWhenMembersAreLessThanTwo() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(capabilityService.getCapabilities(mockUserPrincipal))
            .thenReturn(CapabilitiesDto.create().maxGroupMembers(128));

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(List.of(MemberDto.create().userId(user2Id)));
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, mockUserPrincipal));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals("Bad Request - Too few members (required at least 2)", exception.getMessage());
      }

      @Test
      @DisplayName(
          "There are more than max group members when creating a group, it throws a 'bad request'"
              + " exception")
      void createGroupRoom_errorWhenMembersAreMoreThanMaxGroupMembers() {
        UserPrincipal mockUserPrincipal = UserPrincipal.create(user1Id);
        when(capabilityService.getCapabilities(mockUserPrincipal))
            .thenReturn(CapabilitiesDto.create().maxGroupMembers(3));

        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.GROUP)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id),
                        MemberDto.create().userId(user3Id),
                        MemberDto.create().userId(user4Id),
                        MemberDto.create().userId(user5Id)));
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, mockUserPrincipal));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            "Bad Request - Too many members (required less than 2)", exception.getMessage());
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
        when(capabilityService.getCapabilities(mockUserPrincipal))
            .thenReturn(CapabilitiesDto.create().maxGroupMembers(128));

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
        RoomDto room;
        try (MockedStatic<UUID> uuid = Mockito.mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(roomTemporary1Id);
          uuid.when(() -> UUID.fromString(roomTemporary1.getId())).thenReturn(roomTemporary1Id);
          uuid.when(() -> UUID.fromString(user1Id.toString())).thenReturn(user1Id);
          room = roomService.createRoom(creationFields, mockUserPrincipal);
        }
        assertEquals(creationFields.getName(), room.getName());
        assertEquals(creationFields.getDescription(), room.getDescription());
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(1, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString()), RoomCreated.create().roomId(roomTemporary1Id));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1))
            .createRoom(roomTemporary1Id.toString(), user1Id.toString(), List.of());
        verify(messageDispatcher, times(0)).addUsersToContacts(anyString(), anyString());
        verifyNoMoreInteractions(messageDispatcher);
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
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
        RoomDto room;
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
        assertEquals(creationFields.getType(), room.getType());
        assertEquals(3, room.getMembers().size());

        Optional<MemberDto> user1 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findFirst();
        assertTrue(user1.isPresent());
        assertTrue(user1.get().isOwner());
        Optional<MemberDto> user2 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user2Id))
                .findFirst();
        assertTrue(user2.isPresent());
        assertTrue(user2.get().isOwner());
        Optional<MemberDto> user3 =
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user3Id))
                .findFirst();
        assertTrue(user3.isPresent());
        assertFalse(user3.get().isOwner());

        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
                RoomCreated.create().roomId(roomTemporary2Id));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1))
            .createRoom(
                roomTemporary2Id.toString(),
                user1Id.toString(),
                List.of(user2Id.toString(), user3Id.toString()));
        verify(messageDispatcher, times(0)).addUsersToContacts(anyString(), anyString());
        verifyNoMoreInteractions(messageDispatcher);
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
                eq(List.of(MemberDto.create().userId(user2Id))), any(Room.class)))
            .thenReturn(
                Stream.of(user2Id, user1Id)
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
        RoomDto room;
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
        assertTrue(
            room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user1Id)));
        assertTrue(
            room.getMembers().stream().anyMatch(member -> member.getUserId().equals(user2Id)));
        assertTrue(
            room.getMembers().stream()
                .filter(member -> member.getUserId().equals(user1Id))
                .findAny()
                .orElseThrow()
                .isOwner());
        verify(eventDispatcher, times(1))
            .sendToUserExchange(
                List.of(user1Id.toString(), user2Id.toString()),
                RoomCreated.create().roomId(roomOneToOne1Id));
        verifyNoMoreInteractions(eventDispatcher);
        verify(messageDispatcher, times(1))
            .createRoom(
                roomOneToOne1Id.toString(), user1Id.toString(), List.of(user2Id.toString()));
        verify(messageDispatcher, times(1))
            .addUsersToContacts(user1Id.toString(), user2Id.toString());
        verifyNoMoreInteractions(messageDispatcher);
      }

      @Test
      @DisplayName(
          "There are less than two members when creating a one-to-one, it throws a 'bad request'"
              + " exception")
      void createRoomOneToOne_errorWhenMembersAreLessThanTwo() {
        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.ONE_TO_ONE)
                .members(List.of());
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            "Bad Request - Only 2 users can participate in a one-to-one room",
            exception.getMessage());
      }

      @Test
      @DisplayName(
          "There are more than two members when creating a one-to-one with the requester in it, it"
              + " throws a 'bad request' exception")
      void createRoomOneToOne_errorWhenMembersAreMoreThanTwo() {
        RoomCreationFieldsDto creationFields =
            RoomCreationFieldsDto.create()
                .name("room1")
                .description("Room one")
                .type(RoomTypeDto.ONE_TO_ONE)
                .members(
                    List.of(
                        MemberDto.create().userId(user2Id), MemberDto.create().userId(user3Id)));
        ChatsHttpException exception =
            assertThrows(
                BadRequestException.class,
                () -> roomService.createRoom(creationFields, UserPrincipal.create(user1Id)));
        assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
        assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
        assertEquals(
            "Bad Request - Only 2 users can participate in a one-to-one room",
            exception.getMessage());
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
      RoomDto room =
          roomService.updateRoom(
              roomGroup1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(roomGroup1Id, room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomUpdated.create()
                  .roomId(roomGroup1Id)
                  .name("room1-changed")
                  .description("Room one changed"));
      verify(messageDispatcher, times(1))
          .updateRoomName(roomGroup1Id.toString(), user1Id.toString(), "room1-changed");
      verify(messageDispatcher, times(1))
          .updateRoomDescription(roomGroup1Id.toString(), user1Id.toString(), "Room one changed");
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoMoreInteractions(messageDispatcher);
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
      RoomDto room =
          roomService.updateRoom(
              roomGroup1Id, roomEditableFieldsDto, UserPrincipal.create(user1Id));

      assertEquals(roomGroup1Id, room.getId());
      assertEquals("room1-changed", room.getName());
      assertEquals("Room one changed", room.getDescription());

      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomUpdated.create()
                  .roomId(roomGroup1Id)
                  .name("room1-changed")
                  .description("Room one changed"));
      verify(messageDispatcher, times(1))
          .updateRoomName(roomGroup1Id.toString(), user1Id.toString(), "room1-changed");
      verify(messageDispatcher, times(1))
          .updateRoomDescription(roomGroup1Id.toString(), user1Id.toString(), "Room one changed");
      verify(meetingService, times(1)).getMeetingEntity(meeting1Id);
      verify(meetingService, times(1)).updateMeeting(meeting1.name("room1-changed"));
      verifyNoMoreInteractions(eventDispatcher);
      verifyNoMoreInteractions(messageDispatcher);
      verifyNoMoreInteractions(meetingService);
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
  @DisplayName("Delete room tests")
  class DeleteRoomTests {

    @Test
    @DisplayName("Deletes the required group room")
    void deleteRoom_groupTestOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user1Id.toString());
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user2Id.toString());
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user3Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup1Id));
      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName("Deletes the required group room and the related room picture")
    void deleteRoom_groupWithRoomPictureTestOk() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      FileMetadata pfpMetadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup2Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(fileMetadataRepository.find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));

      roomService.deleteRoom(roomGroup2Id, UserPrincipal.create(user2Id));

      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile(pfpMetadata.getId(), user2Id.toString());
      verify(fileMetadataRepository, times(1)).delete(pfpMetadata);
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup2Id.toString(), user2Id.toString());
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup2Id.toString(), user3Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup2Id));
      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName("Deletes the required group room and the associated meeting")
    void deleteRoom_groupWithMeetingTestOk() {
      UUID meetingId = UUID.randomUUID();
      when(roomRepository.getById(roomGroup1Id.toString()))
          .thenReturn(Optional.of(roomGroup1.meetingId(meetingId.toString())));
      Meeting meeting = Meeting.create().id(meetingId.toString()).roomId(roomGroup1Id.toString());
      meeting.participants(
          List.of(
              ParticipantBuilder.create(meeting, "session1User1Id")
                  .userId(user1Id)
                  .audioStreamOn(true)
                  .videoStreamOn(true)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:00:00Z"))
                  .build(),
              ParticipantBuilder.create(meeting, "session1User3Id")
                  .userId(user3Id)
                  .createdAt(OffsetDateTime.parse("2022-01-01T13:15:00Z"))
                  .build()));
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(Optional.of(meeting));
      UserPrincipal currentUser = UserPrincipal.create(user1Id);
      roomService.deleteRoom(roomGroup1Id, currentUser);

      verify(meetingService, times(1)).getMeetingEntity(meetingId);
      verify(meetingService, times(1)).deleteMeeting(user1Id.toString(), meeting, roomGroup1);
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user1Id.toString());
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user2Id.toString());
      verify(messageDispatcher, times(1))
          .removeRoomMember(roomGroup1Id.toString(), user3Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomDeleted.create().roomId(roomGroup1Id));

      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName("Re throws an exception if storage service fails during deleting room picture")
    void deleteRoom_testErrorStorageExceptionDuringDeletingRoomPicture() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));
      FileMetadata pfpMetadata =
          FileMetadata.create()
              .type(FileMetadataType.ROOM_AVATAR)
              .roomId(roomGroup2Id.toString())
              .userId(user2Id.toString())
              .mimeType("mime/type")
              .id(UUID.randomUUID().toString())
              .name("pfp")
              .originalSize(123L);
      when(fileMetadataRepository.find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      doThrow(StorageException.class)
          .when(storagesService)
          .deleteFile(pfpMetadata.getId(), user2Id.toString());

      assertThrows(
          StorageException.class,
          () -> roomService.deleteRoom(roomGroup2Id, UserPrincipal.create(user2Id)));

      verify(fileMetadataRepository, times(1))
          .find(null, roomGroup2Id.toString(), FileMetadataType.ROOM_AVATAR);
      verify(storagesService, times(1)).deleteFile(pfpMetadata.getId(), user2Id.toString());
      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName("If the room doesn't exist then throws a 'not found' exception")
    void deleteRoom_testRoomNotExists() {
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () -> roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - Room '%s'", roomGroup1Id), exception.getMessage());

      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName(
        "If the authenticated user isn't a room member then throws a 'forbidden' exception")
    void deleteRoom_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.deleteRoom(roomGroup2Id, UserPrincipal.create(user1Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());

      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room owner then throws a 'forbidden' exception")
    void deleteRoom_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> roomService.deleteRoom(roomGroup1Id, UserPrincipal.create(user2Id)));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
          exception.getMessage());

      verifyNoMoreInteractions(
          fileMetadataRepository,
          storagesService,
          meetingService,
          messageDispatcher,
          eventDispatcher);
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
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());

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
      RoomUserSettings roomUserSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).mutedUntil(OffsetDateTime.now());
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(roomUserSettings));
      roomService.unmuteRoom(roomGroup1Id, UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
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
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.of(RoomUserSettings.create(roomGroup1, user1Id.toString())));
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
    @DisplayName("Correctly sets the clear date to now when user settings doesn't exist")
    void clearRoom_testOkUserSettingNotExists() {
      OffsetDateTime desiredDate = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
      RoomUserSettings userSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).clearedAt(desiredDate);

      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(Optional.empty());
      when(roomUserSettingsRepository.save(userSettings)).thenReturn(userSettings);

      OffsetDateTime clearedAt =
          roomService.clearRoomHistory(roomGroup1Id, UserPrincipal.create(user1Id));
      assertEquals(desiredDate, clearedAt);

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1)).save(userSettings);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              user1Id.toString(),
              RoomHistoryCleared.create().roomId(roomGroup1Id).clearedAt(clearedAt));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("Correctly sets the clear date to now when user settings exists")
    void clearRoom_testOkUserSettingExists() {
      OffsetDateTime desiredDate = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
      RoomUserSettings userSettings =
          RoomUserSettings.create(roomGroup1, user1Id.toString()).clearedAt(desiredDate);

      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(
              roomGroup1Id.toString(), user1Id.toString()))
          .thenReturn(
              Optional.of(
                  RoomUserSettings.create(roomGroup1, user1Id.toString())
                      .clearedAt(
                          OffsetDateTime.ofInstant(
                              Instant.parse("2021-12-31T00:00:00Z"), ZoneId.systemDefault()))));
      when(roomUserSettingsRepository.save(userSettings)).thenReturn(userSettings);

      OffsetDateTime clearedAt =
          roomService.clearRoomHistory(roomGroup1Id, UserPrincipal.create(user1Id));
      assertEquals(desiredDate, clearedAt);

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomGroup1Id.toString(), user1Id.toString());
      verify(roomUserSettingsRepository, times(1)).save(userSettings);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              user1Id.toString(),
              RoomHistoryCleared.create().roomId(roomGroup1Id).clearedAt(clearedAt));
      verifyNoMoreInteractions(roomRepository, roomUserSettingsRepository, eventDispatcher);
    }

    @Test
    @DisplayName("If the authenticated user isn't a room member, it throws a 'forbidden' exception")
    void clearRoom_testAuthenticatedUserIsNotARoomMember() {
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
      verifyNoInteractions(eventDispatcher, roomUserSettingsRepository);
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
    }
  }

  @Nested
  @DisplayName("Get room and check user tests")
  class GetRoomAndCheckUserTests {

    @Test
    @DisplayName("It returns the requested room")
    void getRoomAndCheckUser_testOk() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      Room room =
          roomService.getRoomAndValidateUser(roomGroup1Id, UserPrincipal.create(user1Id), false);

      assertEquals(roomGroup1, room);
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(roomRepository);
    }

    @Test
    @DisplayName("If the user isn't a room member throws 'forbidden' exception")
    void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomMember() {
      when(roomRepository.getById(roomGroup2Id.toString())).thenReturn(Optional.of(roomGroup2));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.getRoomAndValidateUser(
                      roomGroup2Id, UserPrincipal.create(user1Id), false));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not a member of room '%s'", user1Id, roomGroup2Id),
          exception.getMessage());
    }

    @Test
    @DisplayName("If the user isn't a room owner throws 'forbidden' exception")
    void getRoomAndCheckUser_testAuthenticatedUserIsNotARoomOwner() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  roomService.getRoomAndValidateUser(
                      roomGroup1Id, UserPrincipal.create(user2Id), true));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Forbidden - User '%s' is not an owner of room '%s'", user2Id, roomGroup1Id),
          exception.getMessage());
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
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      InputStream fileStream = mock(InputStream.class);
      when(storagesService.getFileStreamById(pfpMetadata.getId(), user2Id.toString()))
          .thenReturn(fileStream);

      FileContentAndMetadata roomPicture =
          roomService.getRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id));

      assertEquals(fileStream, roomPicture.getFileStream());
      assertEquals(pfpMetadata.getId(), roomPicture.getMetadata().getId());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
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
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.of(pfpMetadata));
      doThrow(StorageException.class)
          .when(storagesService)
          .getFileStreamById(pfpMetadata.getId(), user2Id.toString());

      assertThrows(
          StorageException.class,
          () -> roomService.getRoomPicture(roomGroup1Id, UserPrincipal.create(user1Id)));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
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
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(fileMetadataRepository.find(null, roomGroup1Id.toString(), FileMetadataType.ROOM_AVATAR))
          .thenReturn(Optional.empty());

      InputStream fileStream = mock(InputStream.class);
      roomService.setRoomPicture(
          roomGroup1Id, fileStream, "image/jpeg", 123L, "picture", UserPrincipal.create(user1Id));
      roomGroup1.pictureUpdatedAt(
          OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).update(roomGroup1);
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
      verify(messageDispatcher, times(1))
          .updateRoomPicture(
              roomGroup1Id.toString(), user1Id.toString(), fileMetadata.getId(), "picture");
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomPictureChanged.create()
                  .roomId(roomGroup1Id)
                  .updatedAt(
                      OffsetDateTime.ofInstant(
                          Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verifyNoMoreInteractions(
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
    }

    @Test
    @DisplayName("It update the room picture if it already exists")
    void setRoomPicture_testOkUpdate() {
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
      roomGroup1.pictureUpdatedAt(
          OffsetDateTime.ofInstant(Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault()));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(roomRepository, times(1)).update(roomGroup1);
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
      verify(messageDispatcher, times(1))
          .updateRoomPicture(
              roomGroup1Id.toString(), user1Id.toString(), fileMetadata.getId(), "picture");
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomPictureChanged.create()
                  .roomId(roomGroup1Id)
                  .updatedAt(
                      OffsetDateTime.ofInstant(
                          Instant.parse("2022-01-01T00:00:00Z"), ZoneId.systemDefault())));
      verifyNoMoreInteractions(
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
    }

    @Test
    @DisplayName("If the file is too large, It throws a BadRequestException")
    void setRoomPicture_failsIfPictureIsTooBig() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      InputStream file = mock(InputStream.class);
      BadRequestException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  roomService.setRoomPicture(
                      roomGroup1Id,
                      file,
                      "image/jpeg",
                      600L * 1024,
                      "picture",
                      UserPrincipal.create(user1Id)));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format(
              "Bad Request - The size of the room picture exceeds the maximum value of %d kB", 512),
          exception.getMessage());
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verifyNoMoreInteractions(
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          eventDispatcher,
          messageDispatcher);
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
      verify(messageDispatcher, times(1))
          .deleteRoomPicture(roomGroup1Id.toString(), user1Id.toString());
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              eq(List.of(user1Id.toString(), user2Id.toString(), user3Id.toString())),
              any(RoomPictureDeleted.class));
      verifyNoMoreInteractions(
          roomRepository,
          fileMetadataRepository,
          storagesService,
          messageDispatcher,
          eventDispatcher);
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
          roomRepository,
          fileMetadataRepository,
          storagesService,
          messageDispatcher,
          eventDispatcher);
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
      verifyNoInteractions(
          fileMetadataRepository, storagesService, messageDispatcher, eventDispatcher);
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
      verifyNoInteractions(storagesService, messageDispatcher, eventDispatcher);
    }
  }

  @Nested
  @DisplayName("Forward messages tests")
  class ForwardMessagesTests {

    @Test
    @DisplayName("Forwards a text message")
    void forwardMessages_textMessage() {
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>this is the body of the message to forward!</body>"
              + "</message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body");
      roomService.forwardMessages(
          roomGroup1Id, List.of(forwardMessageDto), UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(messageDispatcher, times(1))
          .forwardMessage(roomGroup1Id.toString(), user1Id.toString(), forwardMessageDto, null);
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(messageToForward);
      verifyNoMoreInteractions(roomRepository, messageDispatcher);
      verifyNoInteractions(
          attachmentService,
          eventDispatcher,
          userService,
          membersService,
          meetingService,
          storagesService);
    }

    @Test
    @DisplayName("Forwards a message describing an attachment")
    void forwardMessages_attachmentMessage() {
      String messageToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><x xmlns=\"urn:xmpp:muclight:0#configuration\">"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>7247c23f-1669-46ef-9a87-e8726fefb7aa</attachment-id>"
              + "<filename>image.jpg</filename><mime-type>image/jpg</mime-type><size>1024</size>"
              + "</x><body/></message>";
      ForwardMessageDto forwardMessageDto =
          ForwardMessageDto.create()
              .originalMessage(messageToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body");
      FileMetadata newAttachment =
          FileMetadata.create()
              .id(UUID.randomUUID().toString())
              .name("image.jpg")
              .originalSize(1024L)
              .mimeType("image/jpg");
      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(messageDispatcher.getAttachmentIdFromMessage(messageToForward))
          .thenReturn(Optional.of("7247c23f-1669-46ef-9a87-e8726fefb7aa"));
      when(attachmentService.copyAttachment(
              roomGroup1,
              UUID.fromString("7247c23f-1669-46ef-9a87-e8726fefb7aa"),
              UserPrincipal.create(user1Id)))
          .thenReturn(newAttachment);

      roomService.forwardMessages(
          roomGroup1Id, List.of(forwardMessageDto), UserPrincipal.create(user1Id));
      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(attachmentService, times(1))
          .copyAttachment(
              roomGroup1,
              UUID.fromString("7247c23f-1669-46ef-9a87-e8726fefb7aa"),
              UserPrincipal.create(user1Id));
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(messageToForward);
      verify(messageDispatcher, times(1))
          .forwardMessage(
              roomGroup1Id.toString(), user1Id.toString(), forwardMessageDto, newAttachment);
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(messageToForward);

      verifyNoMoreInteractions(roomRepository, messageDispatcher, attachmentService);
      verifyNoInteractions(
          eventDispatcher, userService, membersService, meetingService, storagesService);
    }

    @Test
    @DisplayName("Forwards a message describing an attachment")
    void forwardMessages_someMessages() {
      String message1ToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><body>this is the body of the message to forward!</body>"
              + "</message>";
      ForwardMessageDto forwardMessage1Dto =
          ForwardMessageDto.create()
              .originalMessage(message1ToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body");

      String message2ToForward =
          "<message xmlns=\"jabber:client\" from=\"sender-id\" to=\"recipient-id\""
              + " type=\"groupchat\"><x xmlns=\"urn:xmpp:muclight:0#configuration\">"
              + "<operation>attachmentAdded</operation>"
              + "<attachment-id>7247c23f-1669-46ef-9a87-e8726fefb7aa</attachment-id>"
              + "<filename>image.jpg</filename><mime-type>image/jpg</mime-type><size>1024</size>"
              + "</x><body/></message>";
      ForwardMessageDto forwardMessage2Dto =
          ForwardMessageDto.create()
              .originalMessage(message2ToForward)
              .originalMessageSentAt(OffsetDateTime.parse("2023-01-01T00:00:00Z"))
              .description("this is my body");
      FileMetadata newAttachment =
          FileMetadata.create()
              .id(UUID.randomUUID().toString())
              .name("image.jpg")
              .originalSize(1024L)
              .mimeType("image/jpg");

      when(roomRepository.getById(roomGroup1Id.toString())).thenReturn(Optional.of(roomGroup1));
      when(messageDispatcher.getAttachmentIdFromMessage(message2ToForward))
          .thenReturn(Optional.of("7247c23f-1669-46ef-9a87-e8726fefb7aa"));
      when(attachmentService.copyAttachment(
              roomGroup1,
              UUID.fromString("7247c23f-1669-46ef-9a87-e8726fefb7aa"),
              UserPrincipal.create(user1Id)))
          .thenReturn(newAttachment);

      roomService.forwardMessages(
          roomGroup1Id,
          List.of(forwardMessage1Dto, forwardMessage2Dto),
          UserPrincipal.create(user1Id));

      verify(roomRepository, times(1)).getById(roomGroup1Id.toString());
      verify(attachmentService, times(1))
          .copyAttachment(
              roomGroup1,
              UUID.fromString("7247c23f-1669-46ef-9a87-e8726fefb7aa"),
              UserPrincipal.create(user1Id));
      verify(messageDispatcher, times(1))
          .forwardMessage(roomGroup1Id.toString(), user1Id.toString(), forwardMessage1Dto, null);
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(message1ToForward);
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(message2ToForward);
      verify(messageDispatcher, times(1))
          .forwardMessage(
              roomGroup1Id.toString(), user1Id.toString(), forwardMessage2Dto, newAttachment);
      verify(messageDispatcher, times(1)).getAttachmentIdFromMessage(message2ToForward);

      verifyNoMoreInteractions(roomRepository, messageDispatcher, attachmentService);
      verifyNoInteractions(
          eventDispatcher, userService, membersService, meetingService, storagesService);
    }
  }
}
