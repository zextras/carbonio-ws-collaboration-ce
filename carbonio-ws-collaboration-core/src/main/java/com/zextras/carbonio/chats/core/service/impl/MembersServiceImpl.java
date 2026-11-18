// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.RoomMemberAdded;
import com.zextras.carbonio.async.model.RoomMemberRemoved;
import com.zextras.carbonio.async.model.RoomOwnerDemoted;
import com.zextras.carbonio.async.model.RoomOwnerPromoted;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.InsertedMember;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import io.ebean.annotation.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MembersServiceImpl implements MembersService {

  private final RoomAccess roomAccess;
  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final UserService userService;
  private final ChatMessageService chatMessageService;
  private final SystemEventContentFactory systemEventContentFactory;

  @Inject
  public MembersServiceImpl(
      RoomAccess roomAccess,
      SubscriptionRepository subscriptionRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      EventDispatcher eventDispatcher,
      UserService userService,
      ChatMessageService chatMessageService,
      SystemEventContentFactory systemEventContentFactory) {
    this.roomAccess = roomAccess;
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.userService = userService;
    this.chatMessageService = chatMessageService;
    this.systemEventContentFactory = systemEventContentFactory;
  }

  @Override
  public List<InsertedMember> insertRoomMembers(
      UUID roomId, List<MemberToInsertDto> membersToInsert, UserPrincipal currentUser) {
    List<MemberToInsertDto> uniqueMembers = deduplicate(membersToInsert);
    List<UUID> memberIds = uniqueMembers.stream().map(MemberToInsertDto::getUserId).toList();
    Room room = roomAccess.loadForOwner(roomId, currentUser);

    validateMemberPresence(memberIds, room);
    validateUserExistence(memberIds, currentUser);

    List<AddedMember> addedMembers = persistAddedMembers(room, uniqueMembers, currentUser);

    List<String> receivers = room.getSubscriptions().stream().map(Subscription::getUserId).toList();
    for (AddedMember added : addedMembers) {
      eventDispatcher.sendToUserExchange(
          receivers,
          RoomMemberAdded.create()
              .roomId(UUID.fromString(room.getId()))
              .userId(added.member().getUserId())
              .isOwner(added.member().isOwner())
              .addedBy(UUID.fromString(added.systemEvent().getActorId()))
              .systemEventId(added.systemEvent().getId())
              .systemEventCreatedAt(added.systemEvent().getCreatedAt())
              .type(EventType.ROOM_MEMBER_ADDED));
    }

    return addedMembers.stream().map(AddedMember::inserted).toList();
  }

  private record AddedMember(
      MemberToInsertDto member, InsertedMember inserted, SystemEvent systemEvent) {}

  @Transactional
  private List<AddedMember> persistAddedMembers(
      Room room, List<MemberToInsertDto> members, UserPrincipal currentUser) {
    List<InsertedMember> inserted = new ArrayList<>();
    for (MemberToInsertDto member : members) {
      inserted.add(processRoomSubscription(room, member));
    }
    List<AddedMember> addedMembers = new ArrayList<>();
    for (int i = 0; i < members.size(); i++) {
      MemberToInsertDto member = members.get(i);
      SystemEvent systemEvent =
          SystemEvent.create()
              .id(UUID.randomUUID().toString())
              .roomId(room.getId())
              .type("MEMBER_ADDED")
              .actorId(currentUser.getId())
              .content(
                  systemEventContentFactory.memberAdded(
                      currentUser.getId(), List.of(member.getUserId().toString())));
      chatMessageService.insertSystemEvent(systemEvent);
      addedMembers.add(new AddedMember(member, inserted.get(i), systemEvent));
    }
    return addedMembers;
  }

  private static List<MemberToInsertDto> deduplicate(List<MemberToInsertDto> membersToInsert) {
    Map<UUID, MemberToInsertDto> byUserId = new LinkedHashMap<>();
    membersToInsert.forEach(member -> byUserId.putIfAbsent(member.getUserId(), member));
    return List.copyOf(byUserId.values());
  }

  private static void validateMemberPresence(List<UUID> memberIds, Room room) {
    room.getSubscriptions().stream()
        .map(Subscription::getUserId)
        .map(UUID::fromString)
        .filter(memberIds::contains)
        .findFirst()
        .ifPresent(
            memberId -> {
              throw new BadRequestException(
                  String.format("User '%s' is already a room member", memberId));
            });
  }

  private void validateUserExistence(List<UUID> memberIds, UserPrincipal currentUser) {
    memberIds.stream()
        .filter(memberId -> !userService.userExists(memberId, currentUser))
        .findFirst()
        .ifPresent(
            uuid -> {
              throw new NotFoundException(String.format("User with id '%s' not found", uuid));
            });
  }

  private InsertedMember processRoomSubscription(Room room, MemberToInsertDto member) {
    Subscription subscription =
        subscriptionRepository.insert(
            Subscription.create()
                .room(room)
                .userId(member.getUserId().toString())
                .owner(member.isOwner())
                .external(member.isExternal())
                .temporary(member.isTemporary()));
    room.getSubscriptions().add(subscription);

    if (member.isHistoryCleared()) {
      return new InsertedMember(
          member.getUserId(),
          member.isOwner(),
          roomUserSettingsRepository.setClearedAtNow(room.getId(), member.getUserId().toString()));
    }
    return new InsertedMember(member.getUserId(), member.isOwner(), null);
  }

  @Override
  public void deleteRoomMember(String userId, Room room) {
    String firstOwnerId =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .findFirst()
            .orElseThrow();
    deleteRoomMember(userId, room, firstOwnerId);
  }

  @Override
  public void deleteRoomMember(String userId, Room room, String removedBy) {
    List<String> receivers = room.getSubscriptions().stream().map(Subscription::getUserId).toList();
    removeMemberData(userId, room);
    persistAndDispatchMemberRemoved(receivers, room, userId, removedBy);
  }

  private void persistAndDispatchMemberRemoved(
      List<String> receivers, Room room, String removedUserId, String actorId) {
    SystemEvent systemEvent =
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(room.getId())
            .type("MEMBER_REMOVED")
            .actorId(actorId)
            .content(systemEventContentFactory.memberRemoved(actorId, removedUserId));
    chatMessageService.insertSystemEvent(systemEvent);
    dispatchMemberRemoved(receivers, room, removedUserId, systemEvent);
  }

  private void removeMemberData(String userId, Room room) {
    roomUserSettingsRepository
        .getByRoomIdAndUserId(room.getId(), userId)
        .ifPresent(roomUserSettingsRepository::delete);
    subscriptionRepository.delete(room.getId(), userId);
  }

  private void dispatchMemberRemoved(
      List<String> receivers, Room room, String userId, SystemEvent systemEvent) {
    eventDispatcher.sendToUserExchange(
        receivers,
        RoomMemberRemoved.create()
            .roomId(UUID.fromString(room.getId()))
            .userId(UUID.fromString(userId))
            .removedBy(UUID.fromString(systemEvent.getActorId()))
            .systemEventId(systemEvent.getId())
            .systemEventCreatedAt(systemEvent.getCreatedAt())
            .type(EventType.ROOM_MEMBER_REMOVED));
  }

  @Override
  public List<Subscription> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    return roomAccess.loadForMember(roomId, currentUser).getSubscriptions();
  }

  @Override
  public void promoteMemberToOwner(UUID roomId, UUID userId, UserPrincipal currentUser) {
    validateUserRequest(userId, currentUser);
    Room room = roomAccess.loadForOwner(roomId, currentUser);
    Subscription subscription = getSubscriptionFromRoom(userId, room);
    subscription.owner(true);
    subscriptionRepository.update(subscription);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomOwnerPromoted.create()
            .roomId(roomId)
            .userId(userId)
            .type(EventType.ROOM_OWNER_PROMOTED));
  }

  @Override
  public void demoteOwnerToMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    validateUserRequest(userId, currentUser);
    Room room = roomAccess.loadForOwner(roomId, currentUser);
    Subscription subscription = getSubscriptionFromRoom(userId, room);
    subscription.owner(false);
    subscriptionRepository.update(subscription);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomOwnerDemoted.create().roomId(roomId).userId(userId).type(EventType.ROOM_OWNER_DEMOTED));
  }

  private static Subscription getSubscriptionFromRoom(UUID userId, Room room) {
    return room.getSubscriptions().stream()
        .filter(roomMember -> roomMember.getUserId().equals(userId.toString()))
        .findAny()
        .orElseThrow(
            () ->
                new ForbiddenException(
                    String.format("User '%s' is not a member of the room", userId)));
  }

  private static void validateUserRequest(UUID userId, UserPrincipal currentUser) {
    if (userId.equals(currentUser.getUUID())) {
      throw new BadRequestException("Cannot set owner privileges for itself");
    }
  }

  @Override
  public List<Subscription> updateRoomOwners(
      UUID roomId, List<MemberDto> members, UserPrincipal currentUser) {
    validateUserRequest(members, currentUser);
    Room room = roomAccess.loadForOwner(roomId, currentUser);

    List<Subscription> subscriptionsToUpdate = new ArrayList<>();
    for (MemberDto member : members) {
      Subscription subscription = getSubscriptionFromRoom(member.getUserId(), room);
      subscriptionsToUpdate.add(subscription.owner(member.isOwner()));
    }

    List<Subscription> subscriptionsUpdated =
        subscriptionRepository.updateAll(subscriptionsToUpdate);
    sendOwnersUpdates(roomId, subscriptionsToUpdate, room);
    return subscriptionsUpdated;
  }

  private void sendOwnersUpdates(UUID roomId, List<Subscription> subscriptionsToUpdate, Room room) {
    subscriptionsToUpdate.forEach(
        subscription ->
            eventDispatcher.sendToUserExchange(
                room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
                Boolean.TRUE.equals(subscription.isOwner())
                    ? RoomOwnerPromoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))
                        .type(EventType.ROOM_OWNER_PROMOTED)
                    : RoomOwnerDemoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))
                        .type(EventType.ROOM_OWNER_DEMOTED)));
  }

  private static void validateUserRequest(List<MemberDto> members, UserPrincipal currentUser) {
    if (members.stream().anyMatch(member -> member.getUserId().equals(currentUser.getUUID()))) {
      throw new BadRequestException("Cannot update owner privileges for itself");
    }
  }

  @Override
  public Optional<Subscription> getSubscription(UUID userId, UUID roomId) {
    return subscriptionRepository.getById(roomId.toString(), userId.toString());
  }

  @Override
  public List<Subscription> initRoomSubscriptions(List<MemberDto> members, Room room) {
    return members.stream()
        .map(
            member ->
                Subscription.create()
                    .id(new SubscriptionId(room.getId(), member.getUserId().toString()))
                    .userId(member.getUserId().toString())
                    .room(room)
                    // When we have a one-to-one, both members are owners
                    .owner(member.isOwner() || RoomType.ONE_TO_ONE.equals(room.getType())))
        .map(subscriptionRepository::insert)
        .toList();
  }
}
