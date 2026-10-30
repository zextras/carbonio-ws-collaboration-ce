// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.async.model.DomainEvent;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.RoomMemberAdded;
import com.zextras.carbonio.async.model.RoomMemberRemoved;
import com.zextras.carbonio.async.model.RoomOwnerDemoted;
import com.zextras.carbonio.async.model.RoomOwnerPromoted;
import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.SystemEventContentFactory;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.ws.rs.core.Response.Status;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@UnitTest
class MembersServiceImplTest {

  private final RoomService roomService;
  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final ParticipantService participantService;
  private final ChatMessageService chatMessageService;
  private final DatabaseClock databaseClock;

  public MembersServiceImplTest(SubscriptionMapper subscriptionMapper) {
    this.roomService = mock(RoomService.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.userService = mock(UserService.class);
    this.meetingService = mock(MeetingService.class);
    this.participantService = mock(ParticipantService.class);
    this.chatMessageService = mock(ChatMessageService.class);
    this.databaseClock = mock(DatabaseClock.class);
    when(databaseClock.now()).thenReturn(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
    this.membersService =
        new MembersServiceImpl(
            roomService,
            subscriptionRepository,
            roomUserSettingsRepository,
            eventDispatcher,
            subscriptionMapper,
            userService,
            meetingService,
            participantService,
            chatMessageService,
            databaseClock,
            new SystemEventContentFactory(new ObjectMapper()));
  }

  private static final OffsetDateTime JOINED_AT = OffsetDateTime.parse("2026-01-01T10:00:00Z");
  private static final OffsetDateTime PERSISTED_AT =
      OffsetDateTime.parse("2026-10-23T09:00:00.123456Z");
  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID roomId;
  private static UUID meetingId;

  @BeforeAll
  static void initAll() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    roomId = UUID.randomUUID();
    meetingId = UUID.randomUUID();
  }

  @AfterEach
  void verifyAll() {
    verifyNoMoreInteractions(
        roomService,
        subscriptionRepository,
        roomUserSettingsRepository,
        eventDispatcher,
        userService,
        meetingService,
        participantService,
        chatMessageService);
  }

  protected Room generateRoom(RoomTypeDto type) {
    return Room.create()
        .id(roomId.toString())
        .type(type)
        .name("room1")
        .description("Room one")
        .subscriptions(new ArrayList<>());
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
  @DisplayName("Promote a member as room owner tests")
  class PromoteMemberToOwnerTests {

    @Test
    @DisplayName("Correctly promote a member as room owner")
    void promoteMemberToOwner_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(false);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              user2subscription,
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      membersService.promoteMemberToOwner(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).update(user2subscription.owner(true));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerPromoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
    }

    @Test
    @DisplayName("If the target user isn't a room member, it throws a 'forbidden' exception")
    void promoteMemberToUser_userNotARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> membersService.promoteMemberToOwner(roomId, user2Id, principal));

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Forbidden - User '%s' is not a member of the room", user2Id.toString()),
          exception.getMessage());
    }

    @Test
    @DisplayName("If the target member is the requester, it throws a 'bad request' exception")
    void promoteMemberToOwner_targetMemberIsRequester() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () -> membersService.promoteMemberToOwner(roomId, user2Id, principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot set owner privileges for itself", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Demote a owner to a room member tests")
  class DemoteOwnerToMemberTests {

    @Test
    @DisplayName("Correctly demote an owner to a room member")
    void demoteOwnerToMember_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(true);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              user2subscription,
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      membersService.demoteOwnerToMember(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).update(user2subscription.owner(false));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerDemoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
    }

    @Test
    @DisplayName("If the target user isn't a room member, it throws a 'forbidden' exception")
    void demoteOwnerToMember_userNotARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              ForbiddenException.class,
              () -> membersService.demoteOwnerToMember(roomId, user2Id, principal));

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Forbidden - User '%s' is not a member of the room", user2Id.toString()),
          exception.getMessage());
    }

    @Test
    @DisplayName("If the target owner is the requester, it throws a 'bad request' exception")
    void demoteOwnerToMember_targetOwnerIsRequester() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () -> membersService.demoteOwnerToMember(roomId, user2Id, principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot set owner privileges for itself", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Adds a member to a room tests")
  class InsertsRoomMemberTests {

    @Test
    @DisplayName("Correctly adds a user as a member of group room")
    void insertGroupRoomMember_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          new ArrayList<>(
              List.of(
                  Subscription.create(room, user1Id.toString()).owner(true),
                  Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(subscriptionRepository.insert(any(Subscription.class)))
          .thenReturn(Subscription.create(room, user2Id.toString()));
      insertSetsCreatedAt();

      List<MemberInsertedDto> members =
          membersService.insertRoomMembers(
              roomId,
              List.of(MemberToInsertDto.create().userId(user2Id).historyCleared(false)),
              principal);
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertNotNull(memberInsertedDto);
      assertEquals(user2Id, memberInsertedDto.getUserId());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(1)).userExists(user2Id, principal);
      verify(subscriptionRepository, times(1)).insert(any(Subscription.class));
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      SystemEvent added = capturedSystemEvent();
      assertEquals("MEMBER_ADDED", added.getType());
      assertEquals(user1Id.toString(), added.getActorId());
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
              RoomMemberAdded.create()
                  .roomId(UUID.fromString(room.getId()))
                  .userId(user2Id)
                  .isOwner(false)
                  .addedBy(user1Id)
                  .systemEventId(systemEventId)
                  .systemEventCreatedAt(PERSISTED_AT));
    }

    @Test
    @DisplayName("If user is already a room member, it throws a 'bad request' exception")
    void insertRoomMember_userIsAlreadyARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  membersService.insertRoomMembers(
                      roomId, List.of(MemberToInsertDto.create().userId(user2Id)), principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Bad Request - User '%s' is already a room member", user2Id.toString()),
          exception.getMessage());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
    }

    @Test
    @DisplayName("If the user doesn't exist, it throws a 'not found' exception")
    void insertRoomMember_userNotExists() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(user2Id, principal)).thenReturn(false);
      ChatsHttpException exception =
          assertThrows(
              NotFoundException.class,
              () ->
                  membersService.insertRoomMembers(
                      roomId, List.of(MemberToInsertDto.create().userId(user2Id)), principal));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Not Found - User with id '%s' not found", user2Id),
          exception.getMessage());
      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(1)).userExists(user2Id, principal);
    }

    @Test
    @DisplayName("Correctly adds a user as a member of group room clearing history")
    void insertRoomMember_historyCleared() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          new ArrayList<>(
              List.of(
                  Subscription.create(room, user1Id.toString()).owner(true),
                  Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(subscriptionRepository.insert(any(Subscription.class)))
          .thenReturn(Subscription.create(room, user2Id.toString()));
      OffsetDateTime stored = OffsetDateTime.parse("2026-01-01T10:00:00Z");
      when(roomUserSettingsRepository.setClearedAtNow(roomId.toString(), user2Id.toString()))
          .thenReturn(stored);

      List<MemberInsertedDto> members =
          membersService.insertRoomMembers(
              roomId,
              List.of(MemberToInsertDto.create().userId(user2Id).owner(false).historyCleared(true)),
              principal);
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertNotNull(memberInsertedDto);
      assertEquals(user2Id, memberInsertedDto.getUserId());
      assertEquals(stored, memberInsertedDto.getClearedAt());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(1)).userExists(user2Id, principal);

      ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository, times(1)).insert(subscriptionCaptor.capture());
      Subscription capturedSubscription = subscriptionCaptor.getValue();
      assertEquals(user2Id.toString(), capturedSubscription.getUserId());
      assertFalse(capturedSubscription.isOwner());
      assertFalse(capturedSubscription.isExternal());
      assertFalse(capturedSubscription.isTemporary());
      assertNotNull(capturedSubscription.getJoinedAt());

      verify(roomUserSettingsRepository, times(1))
          .setClearedAtNow(roomId.toString(), user2Id.toString());
      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
              RoomMemberAdded.create()
                  .roomId(UUID.fromString(room.getId()))
                  .userId(user2Id)
                  .isOwner(false)
                  .addedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("Correctly adds a temporary member to a group room")
    void insertRoomMember_temporaryMember() {
      Room room = generateRoom(RoomTypeDto.TEMPORARY);
      room.subscriptions(
          new ArrayList<>(
              List.of(
                  Subscription.create(room, user1Id.toString()).owner(true),
                  Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(subscriptionRepository.insert(any(Subscription.class)))
          .thenReturn(Subscription.create(room, user2Id.toString()).temporary(true));

      List<MemberInsertedDto> members =
          membersService.insertRoomMembers(
              roomId,
              List.of(
                  MemberToInsertDto.create().userId(user2Id).temporary(true).historyCleared(false)),
              principal);
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertNotNull(memberInsertedDto);
      assertEquals(user2Id, memberInsertedDto.getUserId());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(1)).userExists(user2Id, principal);

      ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository, times(1)).insert(subscriptionCaptor.capture());
      Subscription capturedSubscription = subscriptionCaptor.getValue();
      assertEquals(user2Id.toString(), capturedSubscription.getUserId());
      assertFalse(capturedSubscription.isOwner());
      assertFalse(capturedSubscription.isExternal());
      assertTrue(capturedSubscription.isTemporary());
      assertNotNull(capturedSubscription.getJoinedAt());

      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
              RoomMemberAdded.create()
                  .roomId(UUID.fromString(room.getId()))
                  .userId(user2Id)
                  .isOwner(false)
                  .addedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("Correctly adds an external member to a group room")
    void insertRoomMember_externalMember() {
      Room room = generateRoom(RoomTypeDto.TEMPORARY);
      room.subscriptions(
          new ArrayList<>(
              List.of(
                  Subscription.create(room, user1Id.toString()).owner(true),
                  Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(subscriptionRepository.insert(any(Subscription.class)))
          .thenReturn(Subscription.create(room, user2Id.toString()).external(true));

      List<MemberInsertedDto> members =
          membersService.insertRoomMembers(
              roomId,
              List.of(
                  MemberToInsertDto.create().userId(user2Id).external(true).historyCleared(false)),
              principal);
      assertEquals(1, members.size());
      MemberInsertedDto memberInsertedDto = members.get(0);
      assertNotNull(memberInsertedDto);
      assertEquals(user2Id, memberInsertedDto.getUserId());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(1)).userExists(user2Id, principal);

      ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
      verify(subscriptionRepository, times(1)).insert(subscriptionCaptor.capture());
      Subscription capturedSubscription = subscriptionCaptor.getValue();
      assertEquals(user2Id.toString(), capturedSubscription.getUserId());
      assertFalse(capturedSubscription.isOwner());
      assertTrue(capturedSubscription.isExternal());
      assertFalse(capturedSubscription.isTemporary());
      assertNotNull(capturedSubscription.getJoinedAt());

      verify(chatMessageService, times(1)).insertSystemEvent(any(SystemEvent.class));
      String systemEventId = capturedSystemEvent().getId();
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
              RoomMemberAdded.create()
                  .roomId(UUID.fromString(room.getId()))
                  .userId(user2Id)
                  .isOwner(false)
                  .addedBy(user1Id)
                  .systemEventId(systemEventId));
    }

    @Test
    @DisplayName("Bulk add sends one event per member, all with the id of the single system event")
    void insertRoomMembers_bulkSharesSystemEventId() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          new ArrayList<>(List.of(Subscription.create(room, user1Id.toString()).owner(true))));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      when(userService.userExists(any(UUID.class), any(UserPrincipal.class))).thenReturn(true);
      when(subscriptionRepository.insert(any(Subscription.class)))
          .thenReturn(Subscription.create(room, user2Id.toString()));

      membersService.insertRoomMembers(
          roomId,
          List.of(
              MemberToInsertDto.create().userId(user2Id).historyCleared(false),
              MemberToInsertDto.create().userId(user3Id).historyCleared(false)),
          principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(userService, times(2)).userExists(any(UUID.class), any(UserPrincipal.class));
      verify(subscriptionRepository, times(2)).insert(any(Subscription.class));
      SystemEvent added = capturedSystemEvent();
      ArgumentCaptor<RoomMemberAdded> events = ArgumentCaptor.forClass(RoomMemberAdded.class);
      verify(eventDispatcher, times(2)).sendToUserExchange(anyList(), events.capture());
      assertEquals(2, events.getAllValues().size());
      events
          .getAllValues()
          .forEach(
              e -> {
                assertEquals(added.getId(), e.getSystemEventId());
                assertEquals(added.getActorId(), e.getAddedBy().toString());
              });
    }
  }

  @Nested
  @DisplayName("Updates owners status of a room tests")
  class UpdateOwnersTests {

    @Test
    @DisplayName("Correctly promotes members to owners of a room")
    void updateOwners_testOkPromote() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(false);
      Subscription user3subscription = Subscription.create(room, user3Id.toString()).owner(false);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              user2subscription,
              user3subscription));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      membersService.updateRoomOwners(
          roomId,
          List.of(
              MemberDto.create().userId(user2Id).owner(true),
              MemberDto.create().userId(user3Id).owner(true)),
          principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(subscriptionRepository, times(1))
          .updateAll(List.of(user2subscription, user3subscription));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerPromoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerPromoted.create().roomId(UUID.fromString(room.getId())).userId(user3Id));
    }

    @Test
    @DisplayName("Owner update events carry their type and sent date")
    void updateOwners_eventsCarryTypeAndSentDate() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(false);
      Subscription user3subscription = Subscription.create(room, user3Id.toString()).owner(true);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              user2subscription,
              user3subscription));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      membersService.updateRoomOwners(
          roomId,
          List.of(
              MemberDto.create().userId(user2Id).owner(true),
              MemberDto.create().userId(user3Id).owner(false)),
          principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(subscriptionRepository, times(1))
          .updateAll(List.of(user2subscription, user3subscription));
      ArgumentCaptor<DomainEvent> events = ArgumentCaptor.forClass(DomainEvent.class);
      verify(eventDispatcher, times(2)).sendToUserExchange(anyList(), events.capture());
      assertEquals(EventType.ROOM_OWNER_PROMOTED, events.getAllValues().get(0).getType());
      assertNotNull(events.getAllValues().get(0).getSentDate());
      assertEquals(EventType.ROOM_OWNER_DEMOTED, events.getAllValues().get(1).getType());
      assertNotNull(events.getAllValues().get(1).getSentDate());
    }

    @Test
    @DisplayName("Correctly demote owners to members of a room")
    void updateOwners_testOkDemote() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(true);
      Subscription user3subscription = Subscription.create(room, user3Id.toString()).owner(true);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              user2subscription,
              user3subscription));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      membersService.updateRoomOwners(
          roomId,
          List.of(
              MemberDto.create().userId(user2Id).owner(false),
              MemberDto.create().userId(user3Id).owner(false)),
          principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
      verify(subscriptionRepository, times(1))
          .updateAll(List.of(user2subscription, user3subscription));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerDemoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
              RoomOwnerDemoted.create().roomId(UUID.fromString(room.getId())).userId(user3Id));
    }

    @Test
    @DisplayName("It throws exception if a user is not a room member")
    void updateOwners_testErrorUserIsNotAMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user3subscription = Subscription.create(room, user3Id.toString()).owner(true);
      room.subscriptions(
          List.of(Subscription.create(room, user1Id.toString()).owner(true), user3subscription));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ForbiddenException exception =
          assertThrows(
              ForbiddenException.class,
              () ->
                  membersService.updateRoomOwners(
                      roomId,
                      List.of(
                          MemberDto.create().userId(user2Id).owner(false),
                          MemberDto.create().userId(user3Id).owner(false)),
                      principal));

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          String.format("Forbidden - User '%s' is not a member of the room", user2Id.toString()),
          exception.getMessage());
    }

    @Test
    @DisplayName("If the user is the requester, it throws a 'bad request' exception")
    void updateOwners_testErrorUserIsRequester() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user2Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () ->
                  membersService.updateRoomOwners(
                      roomId, List.of(MemberDto.create().userId(user2Id).owner(true)), principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(
          "Bad Request - Cannot update owner privileges for itself", exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Removes a member from a room tests")
  class DeletesRoomMemberTests {

    @Test
    @DisplayName("Correctly removes a member of a group room")
    void deleteRoomMember_groupTestOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);

      membersService.deleteRoomMember(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
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
    void deleteRoomMember_groupTestOkWithMeeting() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      room.meetingId(meetingId.toString());
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, true)).thenReturn(room);
      Meeting meeting =
          Meeting.create()
              .id(meetingId.toString())
              .roomId(roomId.toString())
              .participants(
                  List.of(
                      Participant.create().userId(user1Id.toString()),
                      Participant.create().userId(user2Id.toString())));
      when(meetingService.getMeetingEntity(meetingId)).thenReturn(Optional.of(meeting));

      membersService.deleteRoomMember(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, true);
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
    @DisplayName("Correctly user removes itself of a group room")
    void deleteRoomMember_userRemoveItselfTestOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(false),
              Subscription.create(room, user2Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, false)).thenReturn(room);

      membersService.deleteRoomMember(roomId, user1Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, false);
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
    void deleteRoomMember_userRemoveItselfTestOkWithMuteAndHistoryCleared() {
      Room room = generateRoom(RoomTypeDto.GROUP);
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
      when(roomService.getRoomAndValidateUser(roomId, principal, false)).thenReturn(room);
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), user1Id.toString()))
          .thenReturn(Optional.of(roomUserSettings));

      membersService.deleteRoomMember(roomId, user1Id, principal);

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, false);
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
    @DisplayName("Automatic removal persists MEMBER_REMOVED authored by the first room owner")
    void deleteRoomMember_automaticRemovalAuthoredByFirstOwner() throws Exception {
      Room room = generateRoom(RoomTypeDto.TEMPORARY);
      room.subscriptions(
          List.of(
              Subscription.create(room, user3Id.toString()).owner(false),
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false)));
      insertSetsCreatedAt();

      membersService.deleteRoomMember(user2Id.toString(), room);

      SystemEvent removed = capturedSystemEvent();
      assertEquals("MEMBER_REMOVED", removed.getType());
      assertEquals(roomId.toString(), removed.getRoomId());
      assertEquals(user1Id.toString(), removed.getActorId());
      assertEquals(
          Map.of("removedByUserId", user1Id.toString(), "removedUserId", user2Id.toString()),
          new ObjectMapper()
              .readValue(removed.getContent(), new TypeReference<Map<String, String>>() {}));
      ArgumentCaptor<RoomMemberRemoved> event = ArgumentCaptor.forClass(RoomMemberRemoved.class);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              eq(List.of(user3Id.toString(), user1Id.toString(), user2Id.toString())),
              event.capture());
      assertEquals(user2Id, event.getValue().getUserId());
      assertEquals(user1Id, event.getValue().getRemovedBy());
      assertEquals(removed.getId(), event.getValue().getSystemEventId());
      assertEquals(PERSISTED_AT, event.getValue().getSystemEventCreatedAt());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user2Id.toString());
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("A removal on behalf of a user persists MEMBER_REMOVED authored by that user")
    void deleteRoomMember_removalAuthoredByGivenUser() throws Exception {
      Room room = generateRoom(RoomTypeDto.TEMPORARY);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user3Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).temporary(true)));
      insertSetsCreatedAt();

      membersService.deleteRoomMember(user2Id.toString(), room, user3Id.toString());

      SystemEvent removed = capturedSystemEvent();
      assertEquals("MEMBER_REMOVED", removed.getType());
      assertEquals(roomId.toString(), removed.getRoomId());
      assertEquals(user3Id.toString(), removed.getActorId());
      assertEquals(
          Map.of("removedByUserId", user3Id.toString(), "removedUserId", user2Id.toString()),
          new ObjectMapper()
              .readValue(removed.getContent(), new TypeReference<Map<String, String>>() {}));
      ArgumentCaptor<RoomMemberRemoved> event = ArgumentCaptor.forClass(RoomMemberRemoved.class);
      verify(eventDispatcher, times(1))
          .sendToUserExchange(
              eq(List.of(user1Id.toString(), user3Id.toString(), user2Id.toString())),
              event.capture());
      assertEquals(user2Id, event.getValue().getUserId());
      assertEquals(user3Id, event.getValue().getRemovedBy());
      assertEquals(removed.getId(), event.getValue().getSystemEventId());
      assertEquals(PERSISTED_AT, event.getValue().getSystemEventCreatedAt());
      verify(roomUserSettingsRepository, times(1))
          .getByRoomIdAndUserId(roomId.toString(), user2Id.toString());
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
    }

    @Test
    @DisplayName("If user is the last room owner, it throws a 'bad request' exception")
    void deleteRoomMember_userIsTheLastOwner() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, false)).thenReturn(room);

      ChatsHttpException exception =
          assertThrows(
              BadRequestException.class,
              () -> membersService.deleteRoomMember(roomId, user1Id, principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Last owner can't leave the room", exception.getMessage());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, false);
    }
  }

  @Nested
  @DisplayName("Retrieves all room members list tests")
  class GetsRoomMembersTest {

    @Test
    @DisplayName("Correctly gets all group members")
    void getGroupMembers_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(
          List.of(
              Subscription.create(room, user1Id.toString()).owner(true),
              Subscription.create(room, user2Id.toString()).owner(false),
              Subscription.create(room, user3Id.toString()).owner(false)));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndValidateUser(roomId, principal, false)).thenReturn(room);
      List<MemberDto> roomMembers = membersService.getRoomMembers(roomId, principal);
      assertNotNull(roomMembers);
      assertEquals(3, roomMembers.size());
      assertEquals(user1Id, roomMembers.get(0).getUserId());
      assertEquals(user2Id, roomMembers.get(1).getUserId());
      assertEquals(user3Id, roomMembers.get(2).getUserId());

      verify(roomService, times(1)).getRoomAndValidateUser(roomId, principal, false);
    }
  }

  @Nested
  @DisplayName("Initialize the room subscriptions")
  class InitRoomSubscriptionsTests {

    @Test
    @DisplayName("Correctly initialize a group room subscriptions")
    void initRoomSubscriptions_groupRoom() {
      List<Subscription> subscriptions =
          membersService.initRoomSubscriptions(
              List.of(
                  MemberDto.create().userId(user1Id),
                  MemberDto.create().userId(user2Id).owner(true),
                  MemberDto.create().userId(user3Id)),
              generateRoom(RoomTypeDto.GROUP),
              JOINED_AT);
      assertNotNull(subscriptions);
      assertEquals(3, subscriptions.size());

      assertEquals(user1Id.toString(), subscriptions.get(0).getUserId());
      assertEquals(roomId.toString(), subscriptions.get(0).getRoom().getId());
      assertFalse(subscriptions.get(0).isOwner());

      assertEquals(user2Id.toString(), subscriptions.get(1).getUserId());
      assertEquals(roomId.toString(), subscriptions.get(1).getRoom().getId());
      assertTrue(subscriptions.get(1).isOwner());

      assertEquals(user3Id.toString(), subscriptions.get(2).getUserId());
      assertEquals(roomId.toString(), subscriptions.get(2).getRoom().getId());
      assertFalse(subscriptions.get(2).isOwner());
      subscriptions.forEach(sub -> assertEquals(JOINED_AT, sub.getJoinedAt()));
    }

    @Test
    @DisplayName("Correctly initialize a one-to-one room subscriptions")
    void initRoomSubscriptions_oneToOneRoom() {
      List<Subscription> subscriptions =
          membersService.initRoomSubscriptions(
              List.of(MemberDto.create().userId(user1Id), MemberDto.create().userId(user2Id)),
              generateRoom(RoomTypeDto.ONE_TO_ONE),
              JOINED_AT);
      assertNotNull(subscriptions);
      assertEquals(2, subscriptions.size());

      assertEquals(user1Id.toString(), subscriptions.get(0).getUserId());
      assertEquals(roomId.toString(), subscriptions.get(0).getRoom().getId());
      assertTrue(subscriptions.get(0).isOwner());

      assertEquals(user2Id.toString(), subscriptions.get(1).getUserId());
      assertEquals(roomId.toString(), subscriptions.get(1).getRoom().getId());
      assertTrue(subscriptions.get(1).isOwner());
    }
  }
}
