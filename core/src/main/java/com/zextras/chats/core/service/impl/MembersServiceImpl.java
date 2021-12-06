package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.data.entity.SubscriptionId;
import com.zextras.chats.core.data.event.RoomMemberAddedEvent;
import com.zextras.chats.core.data.event.RoomMemberRemovedEvent;
import com.zextras.chats.core.data.event.RoomOwnerChangedEvent;
import com.zextras.chats.core.exception.BadRequestException;
import com.zextras.chats.core.exception.ForbiddenException;
import com.zextras.chats.core.exception.NotFoundException;
import com.zextras.chats.core.mapper.SubscriptionMapper;
import com.zextras.chats.core.model.MemberDto;
import com.zextras.chats.core.model.RoomTypeDto;
import com.zextras.chats.core.repository.SubscriptionRepository;
import com.zextras.chats.core.service.MembersService;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.security.AccountService;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MembersServiceImpl implements MembersService {

  private final SubscriptionRepository subscriptionRepository;
  private final EventDispatcher        eventDispatcher;
  private final MockSecurityContext    mockSecurityContext;
  private final SubscriptionMapper     subscriptionMapper;
  private final AccountService         accountService;

  @Inject
  public MembersServiceImpl(
    SubscriptionRepository subscriptionRepository,
    EventDispatcher eventDispatcher,
    MockSecurityContext mockSecurityContext,
    SubscriptionMapper subscriptionMapper,
    AccountService accountService
  ) {
    this.subscriptionRepository = subscriptionRepository;
    this.eventDispatcher = eventDispatcher;
    this.mockSecurityContext = mockSecurityContext;
    this.subscriptionMapper = subscriptionMapper;
    this.accountService = accountService;
  }

  @Override
  public void setOwner(Room room, UUID userId, boolean isOwner) {
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
    eventDispatcher.sendToTopic(
      ((MockUserPrincipal) mockSecurityContext.getUserPrincipal().get()).getId(), room.getId(),
      RoomOwnerChangedEvent.create(userId).memberId(userId).isOwner(false)
    );
  }

  @Override
  public MemberDto addRoomMember(Room room, UUID userId, boolean asOwner) {
    if (room.getType().equals(RoomTypeDto.ONETOONE)) {
      throw new ForbiddenException("Can't add members to a one to one conversation");
    }

    accountService.getById(userId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("User with id '%s' was not found", userId)));

    //if the user is already a member, don't insert it
    return subscriptionMapper.ent2memberDto(room.getSubscriptions().stream()
      .filter(member -> member.getUserId().equals(userId.toString()))
      .findFirst()
      .orElseGet(() -> {
        Subscription newSubscription = subscriptionRepository.insert(
          Subscription.create()
            .room(room)
            .userId(userId.toString())
            .owner(asOwner)
            .temporary(false)
            .external(false)
            .joinedAt(OffsetDateTime.now())
        );

        eventDispatcher.sendToTopic(
          ((MockUserPrincipal) mockSecurityContext.getUserPrincipal().get()).getId(),
          room.getId(),
          RoomMemberAddedEvent
            .create(UUID.fromString(room.getId()))
            .memberId(userId)
            .isOwner(asOwner)
            .temporary(false)
            .external(false)
        );
        return newSubscription;
      }));
  }

  @Override
  public void removeRoomMember(Room room, UUID userId) {
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

    //TODO do we need to delete the room?
    subscriptionRepository.delete(room.getId(), userId.toString());
    eventDispatcher.sendToTopic(
      ((MockUserPrincipal) mockSecurityContext.getUserPrincipal().get()).getId(),
      room.getId(),
      RoomMemberRemovedEvent.create(UUID.fromString(room.getId())).memberId(userId)
    );
  }

  @Override
  public List<MemberDto> getRoomMembers(Room room) {
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
