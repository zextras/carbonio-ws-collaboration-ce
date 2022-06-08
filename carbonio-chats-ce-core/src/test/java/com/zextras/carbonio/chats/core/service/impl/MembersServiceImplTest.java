package com.zextras.carbonio.chats.core.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.zextras.carbonio.chats.core.annotations.UnitTest;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.event.RoomMemberAddedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomMemberRemovedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerChangedEvent;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ChatsHttpException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response.Status;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@UnitTest
public class MembersServiceImplTest {

  private final RoomService            roomService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;
  private final SubscriptionMapper     subscriptionMapper;
  private final UserService            userService;
  private final MessageDispatcher      messageService;
  private final MembersService         membersService;

  public MembersServiceImplTest(
    SubscriptionMapper subscriptionMapper
  ) {
    this.roomService = mock(RoomService.class);
    this.subscriptionRepository = mock(SubscriptionRepository.class);
    this.eventDispatcher = mock(EventDispatcher.class);
    this.subscriptionMapper = subscriptionMapper;
    this.userService = mock(UserService.class);
    this.messageService = mock(MessageDispatcher.class);
    this.membersService = new MembersServiceImpl(
      roomService,
      subscriptionRepository,
      eventDispatcher,
      subscriptionMapper,
      userService,
      messageService
    );
  }

  private static UUID user1Id;
  private static UUID user2Id;
  private static UUID user3Id;
  private static UUID roomId;

  @BeforeAll
  public static void initAll() {
    user1Id = UUID.randomUUID();
    user2Id = UUID.randomUUID();
    user3Id = UUID.randomUUID();
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);
      membersService.setOwner(roomId, user2Id, true, principal);

      verify(subscriptionRepository, times(1)).update(user2subscription.owner(true));
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(roomId.toString()),
        any(RoomOwnerChangedEvent.class)
      );
      verify(messageService, times(1)).setMemberRole(roomId.toString(), user1Id.toString(), user2Id.toString(), true);
      verifyNoMoreInteractions(subscriptionRepository, eventDispatcher, messageService);
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);
      membersService.setOwner(roomId, user2Id, false, principal);

      verify(subscriptionRepository, times(1)).update(user2subscription.owner(false));
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(roomId.toString()),
        any(RoomOwnerChangedEvent.class)
      );
      verify(messageService, times(1)).setMemberRole(roomId.toString(), user1Id.toString(), user2Id.toString(), false);
      verifyNoMoreInteractions(subscriptionRepository, eventDispatcher, messageService);
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals(String.format("Forbidden - User '%s' is not a member of the room", user2Id.toString()),
        exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Cannot set owner privileges on one-to-one rooms", exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.setOwner(roomId, user2Id, true, principal));

      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Cannot set owner privileges for itself", exception.getMessage());
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
    }
  }

  @Nested
  @DisplayName("Adds a member to a room tests")
  public class InsertsRoomMemberTests {

    @Test
    @DisplayName("Correctly adds a user as a room member ")
    public void insertRoomMember_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);
      when(subscriptionRepository.insert(any(Subscription.class))).thenReturn(
        Subscription.create(room, user2Id.toString()));

      MemberDto member = membersService.insertRoomMember(roomId, MemberDto.create().userId(user2Id), principal);
      assertNotNull(member);
      assertEquals(user2Id, member.getUserId());

      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).insert(any(Subscription.class));
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(roomId.toString()),
        any(RoomMemberAddedEvent.class));
      verify(messageService, times(1)).addRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString());
      verifyNoMoreInteractions(userService, roomService, subscriptionRepository, eventDispatcher, messageService);
    }

    @Test
    @DisplayName("if user is already a room member, it throws a 'bad request' exception")
    public void insertRoomMember_userIsAlreadyARoomMember() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.insertRoomMember(roomId, MemberDto.create().userId(user2Id), principal));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals(String.format("Bad Request - User '%s' is already a room member", user2Id.toString()),
        exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);

      verifyNoMoreInteractions(userService, roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
    }

    @Test
    @DisplayName("if the room is a one-to-one room, it throws a 'bad request' exception")
    public void insertRoomMember_roomIsAOneToOne() {
      Room room = generateRoom(RoomTypeDto.ONE_TO_ONE);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(true);
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);
      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.insertRoomMember(roomId, MemberDto.create().userId(user2Id), principal));

      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Can't add members to a one to one conversation", exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);
      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);

      verifyNoMoreInteractions(userService, roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
    }

    @Test
    @DisplayName("if the user doesn't exist, it throws a 'not found' exception")
    public void insertRoomMember_userNotExists() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);

      when(userService.userExists(user2Id, principal)).thenReturn(false);
      ChatsHttpException exception = assertThrows(NotFoundException.class, () ->
        membersService.insertRoomMember(roomId, MemberDto.create().userId(user2Id), principal));

      assertEquals(Status.NOT_FOUND, exception.getHttpStatus());
      assertEquals(String.format("Not Found - User with id '%s' was not found", user2Id), exception.getMessage());
      verify(userService, times(1)).userExists(user2Id, principal);

      verifyNoMoreInteractions(userService);
      verifyNoInteractions(roomService, subscriptionRepository, eventDispatcher, messageService);
    }

  }

  @Nested
  @DisplayName("Removes a member from a room tests")
  public class DeletesRoomMemberTests {

    @Test
    @DisplayName("Correctly removes a room member")
    public void deleteRoomMember_test() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      membersService.deleteRoomMember(roomId, user2Id, principal);

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);
      verify(subscriptionRepository, times(1)).delete(roomId.toString(), user2Id.toString());
      verify(eventDispatcher, times(1)).sendToTopic(eq(user1Id), eq(roomId.toString()),
        any(RoomMemberRemovedEvent.class));
      verify(messageService, times(1)).removeRoomMember(roomId.toString(), user1Id.toString(), user2Id.toString());
      verifyNoMoreInteractions(roomService, subscriptionRepository, eventDispatcher, messageService);
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
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(BadRequestException.class, () ->
        membersService.deleteRoomMember(roomId, user1Id, principal));
      assertEquals(Status.BAD_REQUEST, exception.getHttpStatus());
      assertEquals("Bad Request - Last owner can't leave the room", exception.getMessage());

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);
      verifyNoMoreInteractions(roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
    }

    @Test
    @DisplayName("If room is a one-yo-one room, it throws a 'forbidden' exception")
    public void deleteRoomMember_roomIsAOneToOne() {
      Room room = generateRoom(RoomTypeDto.ONE_TO_ONE);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndCheckUser(roomId, principal, true)).thenReturn(room);

      ChatsHttpException exception = assertThrows(ForbiddenException.class, () ->
        membersService.deleteRoomMember(roomId, user2Id, principal));

      assertEquals(Status.FORBIDDEN, exception.getHttpStatus());
      assertEquals("Forbidden - Can't remove members from a one to one conversation", exception.getMessage());

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, true);
      verifyNoMoreInteractions(roomService);
      verifyNoInteractions(subscriptionRepository, eventDispatcher, messageService);
    }
  }

  @Nested
  @DisplayName("Retrieves all room members list tests")
  public class GetsRoomMembersTest {

    @Test
    @DisplayName("Correctly gets all room members")
    public void getRoomMembers_testOk() {
      Room room = generateRoom(RoomTypeDto.GROUP);
      room.subscriptions(List.of(
        Subscription.create(room, user1Id.toString()).owner(true),
        Subscription.create(room, user2Id.toString()).owner(false),
        Subscription.create(room, user3Id.toString()).owner(false)
      ));
      UserPrincipal principal = UserPrincipal.create(user1Id);
      when(roomService.getRoomAndCheckUser(roomId, principal, false)).thenReturn(room);
      List<MemberDto> roomMembers = membersService.getRoomMembers(roomId, principal);
      assertNotNull(roomMembers);
      assertEquals(3, roomMembers.size());
      assertEquals(user1Id, roomMembers.get(0).getUserId());
      assertEquals(user2Id, roomMembers.get(1).getUserId());
      assertEquals(user3Id, roomMembers.get(2).getUserId());

      verify(roomService, times(1)).getRoomAndCheckUser(roomId, principal, false);
      verifyNoMoreInteractions(roomService);
    }
  }

  @Nested
  @DisplayName("Initialize the room subscriptions")
  public class InitRoomSubscriptionsTests {

    @Test
    @DisplayName("Correctly initialize a group room subscriptions")
    public void initRoomSubscriptions_groupRoom() {
      List<Subscription> subscriptions = membersService.initRoomSubscriptions(List.of(user1Id, user2Id, user3Id), generateRoom(RoomTypeDto.GROUP),
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
      List<Subscription> subscriptions = membersService.initRoomSubscriptions(List.of(user1Id, user2Id), generateRoom(RoomTypeDto.ONE_TO_ONE),
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