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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomMemberAdded;
import com.zextras.carbonio.chats.core.data.event.RoomMemberRemoved;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerDemoted;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerPromoted;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.CapabilitiesDto;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class MembersServiceImplTest {

  private final RoomService                roomService;
  private final SubscriptionRepository     subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher            eventDispatcher;
  private final UserService                userService;
  private final MessageDispatcher          messageDispatcher;
  private final MembersService             membersService;
  private final MeetingService             meetingService;
  private final ParticipantService         participantService;
  private final CapabilityService          capabilityService;

  public MembersServiceImplTest(
    SubscriptionMapper subscriptionMapper
  ) {
    this.roomService = mock(RoomService.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.roomUserSettingsRepository = mock(RoomUserSettingsRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.userService = mock(UserService.class);
    this.messageDispatcher = mock(MessageDispatcher.class);
    this.meetingService = mock(MeetingService.class);
    this.participantService = mock(ParticipantService.class);
    this.capabilityService = mock(CapabilityService.class);
    this.membersService = new MembersServiceImpl(
      roomService,
      subscriptionRepository,
      roomUserSettingsRepository,
      eventDispatcher,
      subscriptionMapper,
      userService,
      messageDispatcher,
      meetingService,
      participantService,
      capabilityService);
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID user4Id;
  private static UUID roomId;

  @BeforeAll
  public static void initAll() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
    user4Id = UUID.randomUUID();
    roomId = UUID.randomUUID();
  }

  protected Room generateRoom(RoomTypeDto type) {
    return Room.create()
      .id(roomId.toString())
      .type(type)
      .name("room1")
      .description("Room one")
      .subscriptions(new ArrayList<>());
  }

  @Nested
  @DisplayName("Sets or remove a user as room owner tests")
  public class SetsOwnerTests {

    @Test
    @DisplayName("Correctly set a user as room owner")
    public void setOwner_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(false);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        user2subscription,
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      membersService.setOwner(roomId, user2Id, true, principal);

      verify(subscriptionRepository, times(1)).update(user2subscription.owner(true));
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        RoomOwnerPromoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
      verifyNoMoreInteractions(subscriptionRepository, eventDispatcher);
      verifyNoInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("Correctly remove a user as room owner")
    public void removeOwner_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      Subscription user2subscription = Subscription.create(room, user2Id.toString()).owner(true);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        user2subscription,
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      membersService.setOwner(roomId, user2Id, false, principal);

      verify(subscriptionRepository, times(1)).update(user2subscription.owner(false));
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        RoomOwnerDemoted.create().roomId(UUID.fromString(room.getId())).userId(user2Id));
      verifyNoMoreInteractions(subscriptionRepository, eventDispatcher);
      verifyNoInteractions(messageDispatcher);
    }

    @Test
    @DisplayName("If the user isn't a room member, it throws a 'forbidden' exception")
    public void setRemoveOwner_userNotARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.FORBIDDEN.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.FORBIDDEN.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Forbidden - User '%s' is not a member of the room", user2Id.toString()),
        exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }

    @Test
    @DisplayName("If the room is a one-to-one room, it throws a 'bad request' exception")
    public void setRemoveOwner_roomIsOneToOne() {
      Room room = generateRoom(RoomTypeDto.ONE_TO_ONE);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot set owner privileges on one_to_one rooms", exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }

    @Test
    @DisplayName("If the user is the requester, it throws a 'bad request' exception")
    public void setRemoveOwner_userIsRequester() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user2Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot set owner privileges for itself", exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }
  }

  @Nested
  @DisplayName("Adds a member to a room tests")
  public class InsertsRoomMemberTests {

    @Test
    @DisplayName("Correctly adds a user as a member of group room")
    public void insertGroupRoomMember_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(new ArrayList<>(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      when(subscriptionRepository.insert(any(Subscription.class))).thenReturn(
        Subscription.create(room, user2Id.toString()));
      when(capabilityService.getCapabilities(principal)).thenReturn(CapabilitiesDto.create().maxGroupMembers(128));

      MemberInsertedDto member = membersService.insertRoomMember(roomId,
        MemberToInsertDto.create().userId(user2Id).historyCleared(false),
        principal);
      assertNotNull(member);
      assertEquals(user2Id, member.getUserId());

      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).insert(any(Subscription.class));
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
        RoomMemberAdded.create()
          .roomId(UUID.fromString(room.getId()))
          .userId(user2Id));
      verify(messageDispatcher, times(1)).addRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString());
      verifyNoMoreInteractions(userService, roomService, subscriptionRepository, eventDispatcher, messageDispatcher);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("if the room is a one-to-one room, it throws a 'bad request' exception")
    public void insertOneToOneRoomMember_testNo() {
      Room room = generateRoom(RoomTypeDto.ONE_TO_ONE);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.insertRoomMember(roomId, MemberToInsertDto.create().userId(user2Id), principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot add members to a one_to_one conversation", exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);

      verifyNoMoreInteractions(userService, roomService, roomUserSettingsRepository);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }

    @Test
    @DisplayName("If user is already a room member, it throws a 'bad request' exception")
    public void insertRoomMember_userIsAlreadyARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      when(capabilityService.getCapabilities(principal)).thenReturn(
        CapabilitiesDto.create().maxGroupMembers(128));

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.insertRoomMember(roomId, MemberToInsertDto.create().userId(user2Id), principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Bad Request - User '%s' is already a room member", user2Id.toString()),
        exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);

      verifyNoMoreInteractions(userService, roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher, roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If the user doesn't exist, it throws a 'not found' exception")
    public void insertRoomMember_userNotExists() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(false);
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        membersService.insertRoomMember(roomId, MemberToInsertDto.create().userId(user2Id), principal));

      assertEquals(Status.NOT_FOUND.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.NOT_FOUND.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals(String.format("Not Found - User with id '%s' was not found", user2Id), exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);

      verifyNoMoreInteractions(userService);
      verifyNoInteractions(roomService, subscriptionRepository, eventDispatcher, messageDispatcher,
        roomUserSettingsRepository);
    }

    @Test
    @DisplayName("Correctly adds a user as a member of group room clearing history")
    public void insertRoomMember_historyCleared() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(new ArrayList<>(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false))));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      when(subscriptionRepository.insert(any(Subscription.class))).thenReturn(
        Subscription.create(room, user2Id.toString()));
      when(roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), user2Id.toString()))
        .thenReturn(Optional.of(RoomUserSettings.create().clearedAt(OffsetDateTime.now())));
      when(capabilityService.getCapabilities(principal)).thenReturn(
        CapabilitiesDto.create().maxGroupMembers(128));

      MemberInsertedDto member = membersService.insertRoomMember(roomId,
        MemberToInsertDto.create().userId(user2Id).historyCleared(true).owner(false), principal);
      assertNotNull(member);
      assertEquals(user2Id, member.getUserId());

      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).insert(any(Subscription.class));
      verify(roomUserSettingsRepository, times(1)).getByRoomIdAndUserId(roomId.toString(), user2Id.toString());
      verify(roomUserSettingsRepository, times(1)).save(any(RoomUserSettings.class));
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user3Id.toString(), user2Id.toString()),
        RoomMemberAdded.create()
          .roomId(UUID.fromString(room.getId()))
          .userId(user2Id)
          .isOwner(false)
        );
      verify(messageDispatcher, times(1)).addRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString());
      verifyNoMoreInteractions(userService, roomService, subscriptionRepository, eventDispatcher, messageDispatcher,
        roomUserSettingsRepository);
    }

    @Test
    @DisplayName("Reached max group members when inviting a user, it throws a 'bad request' exception")
    public void insertRoomMember_maxGroupMembers() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(capabilityService.getCapabilities(principal)).thenReturn(CapabilitiesDto.create().maxGroupMembers(3));

      when(userService.userExists(user4Id, principal)).thenReturn(true);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.insertRoomMember(roomId, MemberToInsertDto.create().userId(user4Id), principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot add more members to this group", exception.getMessage());
      verify(userService, times(1)).userExists(user4Id, principal);
      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);
      verify(capabilityService, times(1)).getCapabilities(principal);

      verifyNoMoreInteractions(userService, roomService, capabilityService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher, roomUserSettingsRepository);
    }

  }

  @Nested
  @DisplayName("Removes a member from a room tests")
  public class DeletesRoomMemberTests {

    @Test
    @DisplayName("Correctly removes a member of a group room")
    public void deleteRoomMember_groupTestOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);

      membersService.deleteRoomMember(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        RoomMemberRemoved.create().roomId(roomId).userId(user2Id));
      verify(messageDispatcher, times(1)).removeRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString());
      verifyNoMoreInteractions(roomService, subscriptionRepository, eventDispatcher, messageDispatcher);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("Correctly user removes itself of a group room")
    public void deleteRoomMember_userRemoveItselfTestOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(false),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, false)).thenReturn(room);

      membersService.deleteRoomMember(roomId, user1Id, principal);

      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, false);
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user1Id.toString());
      verify(eventDispatcher, times(1)).sendToUserExchange(
        List.of(user1Id.toString(), user2Id.toString(), user3Id.toString()),
        RoomMemberRemoved.create().roomId(roomId).userId(user1Id));
      verify(messageDispatcher, times(1)).removeRoomMember(roomId.toString(), user1Id.toString(), user1Id.toString());
      verifyNoMoreInteractions(roomService, subscriptionRepository, eventDispatcher, messageDispatcher);
      verifyNoInteractions(roomUserSettingsRepository);
    }

    @Test
    @DisplayName("If user is the last room owner, it throws a 'bad request' exception")
    public void deleteRoomMember_userIsTheLastOwner() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, false)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.deleteRoomMember(roomId, user1Id, principal));
      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Last owner can't leave the room", exception.getMessage());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, false);
      verifyNoMoreInteractions(roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }

    @Test
    @DisplayName("If room is a one-to-one room, it throws a 'bad request' exception")
    public void deleteRoomMember_roomIsAOneToOne() {
      Room room = generateRoom(RoomTypeDto.ONE_TO_ONE);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.deleteRoomMember(roomId, user2Id, principal));

      assertEquals(Status.BAD_REQUEST.getStatusCode(), exception.getHttpStatusCode());
      assertEquals(Status.BAD_REQUEST.getReasonPhrase(), exception.getHttpStatusPhrase());
      assertEquals("Bad Request - Cannot remove a member from a one_to_one conversation", exception.getMessage());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, true);
      verifyNoMoreInteractions(roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageDispatcher);
    }
  }

  @Nested
  @DisplayName("Retrieves all room members list tests")
  public class GetsRoomMembersTest {

    @Test
    @DisplayName("Correctly gets all group members")
    public void getGroupMembers_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomEntityAndCheckUser(roomId, principal, false)).thenReturn(room);
      List<MemberDto> roomMembers = membersService.getRoomMembers(roomId, principal);
      assertNotNull(roomMembers);
      assertEquals(3, roomMembers.size());
      assertEquals(user1Id, roomMembers.get(0).getUserId());
      assertEquals(user2Id, roomMembers.get(1).getUserId());
      assertEquals(user3Id, roomMembers.get(2).getUserId());

      verify(roomService, times(1)).getRoomEntityAndCheckUser(roomId, principal, false);
      verifyNoMoreInteractions(roomService);
    }
  }

  @Nested
  @DisplayName("Initialize the room subscriptions")
  public class InitRoomSubscriptionsTests {

    @Test
    @DisplayName("Correctly initialize a group room subscriptions")
    public void initRoomSubscriptions_groupRoom() {
      List<Subscription> subscriptions = membersService.initRoomSubscriptions(List.of(user1Id, user2Id, user3Id),
        generateRoom(RoomTypeDto.GROUP),
        UserPrincipal.create(user2Id));
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
    }

    @Test
    @DisplayName("Correctly initialize a one-to-one room subscriptions")
    public void initRoomSubscriptions_oneToOneRoom() {
      List<Subscription> subscriptions = membersService.initRoomSubscriptions(List.of(user1Id, user2Id),
        generateRoom(RoomTypeDto.ONE_TO_ONE),
        UserPrincipal.create(user2Id));
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
