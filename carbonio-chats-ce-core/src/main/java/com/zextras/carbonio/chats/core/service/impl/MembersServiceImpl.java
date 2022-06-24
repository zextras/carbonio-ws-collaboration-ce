// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
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
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MembersServiceImpl implements MembersService {

  private final RoomService                roomService;
  private final SubscriptionRepository     subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher            eventDispatcher;
  private final SubscriptionMapper         subscriptionMapper;
  private final UserService                userService;
  private final MessageDispatcher          messageService;

  @Inject
  public MembersServiceImpl(
    RoomService roomService, SubscriptionRepository subscriptionRepository,
    RoomUserSettingsRepository roomUserSettingsRepository,
    EventDispatcher eventDispatcher,
    SubscriptionMapper subscriptionMapper,
    UserService userService,
    MessageDispatcher messageDispatcher
  ) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.userService = userService;
    this.messageService = messageDispatcher;
  }

  @Override
  public void setOwner(UUID roomId, UUID userId, boolean isOwner, UserPrincipal currentUser) {
    if (userId.equals(currentUser.getUUID())) {
      throw new BadRequestException("Cannot set owner privileges for itself");
    }
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    if (List.of(RoomTypeDto.ONE_TO_ONE, RoomTypeDto.CHANNEL).contains(room.getType())) {
      throw new BadRequestException(String.format("Cannot set owner privileges on %s rooms", room.getType()));
    }
    Subscription subscription = room.getSubscriptions().stream()
      .filter(roomMember -> roomMember.getUserId().equals(userId.toString()))
      .findAny()
      .orElseThrow(
        () -> new ForbiddenException(String.format("User '%s' is not a member of the room", userId)));

    subscription.owner(isOwner);
    subscriptionRepository.update(subscription);
    messageService.setMemberRole(room.getId(), currentUser.getId(), userId.toString(), isOwner);
    eventDispatcher.sendToTopic(currentUser.getUUID(), room.getId(),
      RoomOwnerChangedEvent.create(userId).memberId(userId).isOwner(isOwner)
    );
  }

  @Override
  @Transactional
  public MemberDto insertRoomMember(UUID roomId, MemberDto memberDto, UserPrincipal currentUser) {
    if (!userService.userExists(memberDto.getUserId(), currentUser)) {
      throw new NotFoundException(String.format("User with id '%s' was not found", memberDto.getUserId()));
    }
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    if (List.of(RoomTypeDto.ONE_TO_ONE, RoomTypeDto.CHANNEL).contains(room.getType())) {
      throw new BadRequestException(String.format("Cannot add members to a %s conversation", room.getType()));
    }
    if (room.getSubscriptions().stream()
      .anyMatch(member -> memberDto.getUserId().toString().equals(member.getUserId()))) {
      throw new BadRequestException(String.format("User '%s' is already a room member", memberDto.getUserId()));
    }
    Subscription subscription = subscriptionRepository.insert(
      Subscription.create()
        .room(room)
        .userId(memberDto.getUserId().toString())
        .owner(memberDto.isOwner())
        .temporary(false)
        .external(false)
        .joinedAt(OffsetDateTime.now())
    );
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      roomUserSettingsRepository.save(
        RoomUserSettings.create(room, memberDto.getUserId().toString())
          .rank(roomUserSettingsRepository.getWorkspaceMaxRank(memberDto.getUserId().toString()).orElse(0) + 1));
      room.getChildren().forEach(child -> {
        try {
          messageService.addRoomMember(child.getId(), currentUser.getId(), memberDto.getUserId().toString());
        } catch (Exception e) {
          ChatsLogger.warn(String.format(
            "An error occurred during a room user addition notification to message dispatcher. RoomId: '%s', UserId: '%s'",
            child.getId(), memberDto.getUserId()));
        }
      });
    } else {
      messageService.addRoomMember(room.getId(), currentUser.getId(), memberDto.getUserId().toString());
    }

    eventDispatcher.sendToTopic(
      currentUser.getUUID(),
      room.getId(),
      RoomMemberAddedEvent
        .create(UUID.fromString(room.getId()))
        .memberId(memberDto.getUserId())
        .isOwner(memberDto.isOwner())
        .temporary(false)
        .external(false));

    return subscriptionMapper.ent2memberDto(subscription);
  }

  @Override
  @Transactional
  public void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, true);
    if (List.of(RoomTypeDto.ONE_TO_ONE, RoomTypeDto.CHANNEL).contains(room.getType())) {
      throw new BadRequestException(String.format("Cannot remove a member from a %s conversation", room.getType()));
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
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), userId.toString())
        .ifPresent(roomUserSettingsRepository::delete);
      room.getChildren().forEach(child -> {
        try {
          messageService.removeRoomMember(child.getId(), currentUser.getId(), userId.toString());
        } catch (Exception e) {
          ChatsLogger.warn(String.format(
            "An error occurred during a room user removal notification to message dispatcher. RoomId: '%s', UserId: '%s'",
            child.getId(), userId));
        }
      });
    } else {
      messageService.removeRoomMember(room.getId(), currentUser.getId(), userId.toString());
    }
    eventDispatcher.sendToTopic(
      currentUser.getUUID(),
      room.getId(),
      RoomMemberRemovedEvent.create(UUID.fromString(room.getId())).memberId(userId)
    );
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, false);
    return subscriptionMapper.ent2memberDto(room.getSubscriptions());
  }

  @Override
  public List<Subscription> initRoomSubscriptions(List<UUID> membersIds, Room room, UserPrincipal requester) {
    return membersIds.stream().map(userId ->
      Subscription.create()
        .id(new SubscriptionId(room.getId(), userId.toString()))
        .userId(userId.toString())
        .room(room)
        //When we have a one to one, both members are owners
        .owner(userId.equals(requester.getUUID()) || RoomTypeDto.ONE_TO_ONE.equals(room.getType()))
        .temporary(false)
        .external(false)
        .joinedAt(OffsetDateTime.now())
    ).collect(Collectors.toList());
  }
}
