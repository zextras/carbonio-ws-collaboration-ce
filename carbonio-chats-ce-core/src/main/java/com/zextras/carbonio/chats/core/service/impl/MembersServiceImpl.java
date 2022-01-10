package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.model.MemberDto;
import com.zextras.carbonio.chats.core.model.RoomTypeDto;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.carbonio.chats.core.web.security.AccountService;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import com.zextras.carbonio.chats.core.data.event.RoomMemberAddedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomMemberRemovedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerChangedEvent;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MembersServiceImpl implements MembersService {

  private final RoomService            roomService;
  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;
  private final SubscriptionMapper     subscriptionMapper;
  private final AccountService         accountService;
  private final MessageService         messageService;

  @Inject
  public MembersServiceImpl(
    RoomService roomService, SubscriptionRepository subscriptionRepository,
    EventDispatcher eventDispatcher,
    SubscriptionMapper subscriptionMapper,
    AccountService accountService,
    MessageService messageService
  ) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.accountService = accountService;
    this.messageService = messageService;
  }

  @Override
  public void setOwner(UUID roomId, UUID userId, boolean isOwner, MockUserPrincipal currentUser) {
    // gets room and check if current user is owner
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    // find member
    Subscription subscription = room.getSubscriptions().stream()
      .filter(roomMember -> roomMember.getUserId().equals(userId.toString()))
      .findAny()
      .orElseThrow(
        () -> new ForbiddenException(String.format("User '%s' is not a member of the room", userId.toString())));
    // change owner value
    subscription.owner(isOwner);
    // update row
    subscriptionRepository.update(subscription);
    // send event at all room members
    eventDispatcher.sendToTopic(currentUser.getId(), room.getId(),
      RoomOwnerChangedEvent.create(userId).memberId(userId).isOwner(false)
    );
    // send to server XMPP
    messageService.setMemberRole(room.getId(), currentUser.getId().toString(), userId.toString(), isOwner);
  }

  @Override
  public MemberDto addRoomMember(UUID roomId, MemberDto memberDto, MockUserPrincipal currentUser) {
    // gets room and check if current user is owner
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    // room cannot be one to one
    if (room.getType().equals(RoomTypeDto.ONETOONE)) {
      throw new ForbiddenException("Can't add members to a one to one conversation");
    }
    // check that user isn't duplicated
    if (room.getSubscriptions().stream().anyMatch(member -> memberDto.getUserId().toString().equals(member.getUserId()))) {
      throw new BadRequestException(String.format("User '%s' is already a room member", memberDto.getUserId()));
    }
    // check the users existence
    accountService.getById(memberDto.getUserId().toString())
      .orElseThrow(() -> new NotFoundException(String.format("User with id '%s' was not found", memberDto.getUserId())));
    // insert the new member
    Subscription subscription = subscriptionRepository.insert(
      Subscription.create()
        .room(room)
        .userId(memberDto.getUserId().toString())
        .owner(memberDto.isOwner())
        .temporary(false)
        .external(false)
        .joinedAt(OffsetDateTime.now())
    );

    eventDispatcher.sendToTopic(
      currentUser.getId(),
      room.getId(),
      RoomMemberAddedEvent
        .create(UUID.fromString(room.getId()))
        .memberId(memberDto.getUserId())
        .isOwner(memberDto.isOwner())
        .temporary(false)
        .external(false)
    );
    // send to server xmpp
    messageService.addRoomMember(room.getId(), currentUser.getId().toString(), memberDto.getUserId().toString());
    return subscriptionMapper.ent2memberDto(subscription);
  }

  @Override
  public void removeRoomMember(UUID roomId, UUID userId, MockUserPrincipal currentUser) {
    // gets room and check if current user is owner
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    if (room.getType().equals(RoomTypeDto.ONETOONE)) {
      throw new ForbiddenException("Can't remove members from a one to one conversation");
    }

    List<String> owners = room.getSubscriptions().stream()
      .filter(Subscription::isOwner)
      .map(Subscription::getUserId)
      .collect(Collectors.toList());

    if (owners.size() == 1 && owners.get(0).equals(userId.toString()) && room.getSubscriptions().size() > 1) {
      throw new BadRequestException("Last owner can't leave the room");
    }

    // TODO do we need to delete the room?
    subscriptionRepository.delete(room.getId(), userId.toString());
    eventDispatcher.sendToTopic(
      currentUser.getId(),
      room.getId(),
      RoomMemberRemovedEvent.create(UUID.fromString(room.getId())).memberId(userId)
    );
    // sent to server XMPP
    messageService.removeRoomMember(room.getId(), currentUser.getId().toString(), userId.toString());
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, MockUserPrincipal currentUser) {
    // gets room and check if user is a member
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, false);
    return subscriptionMapper.ent2memberDto(room.getSubscriptions());
  }

  @Override
  public List<Subscription> initRoomSubscriptions(List<String> membersIds, Room room, MockUserPrincipal requester) {
    List<Subscription> result = membersIds.stream().map(userId ->
      Subscription.create()
        .id(new SubscriptionId(room.getId(), userId))
        .userId(userId)
        .room(room)
        .owner(false)
        .temporary(false)
        .external(false)
        .joinedAt(OffsetDateTime.now())
    ).collect(Collectors.toList());
    result.add(Subscription.create()
      .id(new SubscriptionId(room.getId(), requester.getId().toString()))
      .userId(requester.getId().toString())
      .room(room)
      .owner(true)
      .temporary(false)
      .external(false)
      .joinedAt(OffsetDateTime.now()));

    return result;
  }
}
