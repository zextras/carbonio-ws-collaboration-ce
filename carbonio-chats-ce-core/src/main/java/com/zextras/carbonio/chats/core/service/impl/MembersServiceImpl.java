// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.event.RoomMemberAddedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomMemberRemovedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerChangedEvent;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
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
  private final UserService            userService;
  private final MessageDispatcher      messageService;

  @Inject
  public MembersServiceImpl(
    RoomService roomService, SubscriptionRepository subscriptionRepository,
    EventDispatcher eventDispatcher,
    SubscriptionMapper subscriptionMapper,
    UserService userService,
    MessageDispatcher messageDispatcher
  ) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.userService = userService;
    this.messageService = messageDispatcher;
  }

  @Override
  public void setOwner(UUID roomId, UUID userId, boolean isOwner, UserPrincipal currentUser) {
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
    eventDispatcher.sendToTopic(currentUser.getUUID(), room.getId(),
      RoomOwnerChangedEvent.create(userId).memberId(userId).isOwner(false)
    );
    // send to server XMPP
    messageService.setMemberRole(room.getId(), currentUser.getId(), userId.toString(), isOwner);
  }

  @Override
  public MemberDto insertRoomMember(UUID roomId, MemberDto memberDto, UserPrincipal currentUser) {
    // gets room and check if current user is owner
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    // room cannot be one to one
    if (room.getType().equals(RoomTypeDto.ONE_TO_ONE)) {
      throw new BadRequestException("Can't add members to a one to one conversation");
    }
    // check that user isn't duplicated
    if (room.getSubscriptions().stream()
      .anyMatch(member -> memberDto.getUserId().toString().equals(member.getUserId()))) {
      throw new BadRequestException(String.format("User '%s' is already a room member", memberDto.getUserId()));
    }

    if(!userService.userExists(memberDto.getUserId(), currentUser)) {
      throw new NotFoundException(String.format("User with id '%s' was not found", memberDto.getUserId()));
    }
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
      currentUser.getUUID(),
      room.getId(),
      RoomMemberAddedEvent
        .create(UUID.fromString(room.getId()))
        .memberId(memberDto.getUserId())
        .isOwner(memberDto.isOwner())
        .temporary(false)
        .external(false)
    );
    // send to server xmpp
    messageService.addRoomMember(room.getId(), currentUser.getId(), memberDto.getUserId().toString());
    return subscriptionMapper.ent2memberDto(subscription);
  }

  @Override
  public void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    // gets room and check if current user is owner
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    if (room.getType().equals(RoomTypeDto.ONE_TO_ONE)) {
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
      currentUser.getUUID(),
      room.getId(),
      RoomMemberRemovedEvent.create(UUID.fromString(room.getId())).memberId(userId)
    );
    // sent to server XMPP
    messageService.removeRoomMember(room.getId(), currentUser.getId(), userId.toString());
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    // gets room and check if user is a member
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, false);
    return subscriptionMapper.ent2memberDto(room.getSubscriptions());
  }

  @Override
  public List<Subscription> initRoomSubscriptions(List<UUID> membersIds, Room room, UserPrincipal requester) {
    List<Subscription> result = membersIds.stream().map(userId ->
      Subscription.create()
        .id(new SubscriptionId(room.getId(), userId.toString()))
        .userId(userId.toString())
        .room(room)
        .owner(RoomTypeDto.ONE_TO_ONE.equals(room.getType())) //When we have a one to one, both members are owners
        .temporary(false)
        .external(false)
        .joinedAt(OffsetDateTime.now())
    ).collect(Collectors.toList());
    result.add(Subscription.create()
      .id(new SubscriptionId(room.getId(), requester.getId()))
      .userId(requester.getId())
      .room(room)
      .owner(true)
      .temporary(false)
      .external(false)
      .joinedAt(OffsetDateTime.now()));

    return result;
  }
}
