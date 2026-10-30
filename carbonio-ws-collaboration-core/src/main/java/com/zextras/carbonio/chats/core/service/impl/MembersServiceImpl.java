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
import com.zextras.carbonio.chats.core.exception.BadRequestException;
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
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MembersServiceImpl implements MembersService {

  private final RoomService roomService;
  private final SubscriptionRepository subscriptionRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final EventDispatcher eventDispatcher;
  private final SubscriptionMapper subscriptionMapper;
  private final UserService userService;
  private final MeetingService meetingService;
  private final ParticipantService participantService;
  private final ChatMessageService chatMessageService;
  private final DatabaseClock databaseClock;
  private final SystemEventContentFactory systemEventContentFactory;

  @Inject
  public MembersServiceImpl(
      RoomService roomService,
      SubscriptionRepository subscriptionRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      EventDispatcher eventDispatcher,
      SubscriptionMapper subscriptionMapper,
      UserService userService,
      MeetingService meetingService,
      ParticipantService participantService,
      ChatMessageService chatMessageService,
      DatabaseClock databaseClock,
      SystemEventContentFactory systemEventContentFactory) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.userService = userService;
    this.meetingService = meetingService;
    this.participantService = participantService;
    this.chatMessageService = chatMessageService;
    this.databaseClock = databaseClock;
    this.systemEventContentFactory = systemEventContentFactory;
  }

  @Override
  public List<MemberInsertedDto> insertRoomMembers(
      UUID roomId, List<MemberToInsertDto> membersToInsert, UserPrincipal currentUser) {
    List<MemberToInsertDto> uniqueMembers = deduplicate(membersToInsert);
    List<UUID> memberIds = uniqueMembers.stream().map(MemberToInsertDto::getUserId).toList();
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);

    validateMemberPresence(memberIds, room);
    validateUserExistence(memberIds, currentUser);

    OffsetDateTime currentDateTime = databaseClock.now();
    List<AddedMember> addedMembers =
        persistAddedMembers(room, uniqueMembers, currentUser, currentDateTime);

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
              .type(EventType.ROOM_MEMBER_ADDED)
              .sentDate(OffsetDateTime.now()));
    }

    return addedMembers.stream().map(AddedMember::inserted).toList();
  }

  private record AddedMember(
      MemberToInsertDto member, MemberInsertedDto inserted, SystemEvent systemEvent) {}

  @Transactional
  private List<AddedMember> persistAddedMembers(
      Room room,
      List<MemberToInsertDto> members,
      UserPrincipal currentUser,
      OffsetDateTime joinedAt) {
    List<MemberInsertedDto> inserted = new ArrayList<>();
    for (MemberToInsertDto member : members) {
      inserted.add(processRoomSubscription(room, member, joinedAt));
    }
    OffsetDateTime firstEventAt = databaseClock.now();
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
                      currentUser.getId(), List.of(member.getUserId().toString())))
              .createdAt(firstEventAt.plus(i, ChronoUnit.MILLIS));
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

  private MemberInsertedDto processRoomSubscription(
      Room room, MemberToInsertDto member, OffsetDateTime dateTime) {
    Subscription subscription =
        subscriptionRepository.insert(
            Subscription.create()
                .room(room)
                .userId(member.getUserId().toString())
                .owner(member.isOwner())
                .external(member.isExternal())
                .temporary(member.isTemporary())
                .joinedAt(dateTime));
    room.getSubscriptions().add(subscription);

    MemberInsertedDto memberInsertedDto =
        MemberInsertedDto.create().userId(member.getUserId()).owner(member.isOwner());

    if (member.isHistoryCleared()) {
      return memberInsertedDto.clearedAt(
          roomUserSettingsRepository.setClearedAtNow(room.getId(), member.getUserId().toString()));
    }
    return memberInsertedDto;
  }

  @Override
  public void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    Room room =
        roomService.getRoomAndValidateUser(
            roomId, currentUser, !currentUser.getUUID().equals(userId));
    validateUserMemberShip(userId, currentUser, room);
    if (room.getMeetingId() != null) {
      meetingService
          .getMeetingEntity(UUID.fromString(room.getMeetingId()))
          .ifPresent(meeting -> participantService.removeMeetingParticipant(meeting, room, userId));
    }
    validateLastRoomOwner(userId.toString(), room);
    List<String> receivers = room.getSubscriptions().stream().map(Subscription::getUserId).toList();
    removeMemberData(userId.toString(), room);
    persistAndDispatchMemberRemoved(receivers, room, userId.toString(), currentUser.getId());

    // delete room if it's the last member
    if (room.getSubscriptions().size() == 1) {
      roomService.deleteRoom(roomId, currentUser);
    }
  }

  private static void validateUserMemberShip(UUID userId, UserPrincipal currentUser, Room room) {
    if (!currentUser.getUUID().equals(userId)
        && room.getSubscriptions().stream()
            .noneMatch(s -> s.getUserId().equals(userId.toString()))) {
      throw new NotFoundException("The user is not a room member");
    }
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
            .type(EventType.ROOM_MEMBER_REMOVED)
            .sentDate(OffsetDateTime.now()));
  }

  private void validateLastRoomOwner(String userId, Room room) {
    List<String> owners =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList();
    if (owners.size() == 1
        && owners.getFirst().equals(userId)
        && room.getSubscriptions().size() > 1) {
      throw new BadRequestException("Last owner can't leave the room");
    }
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, false);
    return subscriptionMapper.ent2memberDto(room.getSubscriptions());
  }

  @Override
  public void promoteMemberToOwner(UUID roomId, UUID userId, UserPrincipal currentUser) {
    validateUserRequest(userId, currentUser);
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);
    Subscription subscription = getSubscriptionFromRoom(userId, room);
    subscription.owner(true);
    subscriptionRepository.update(subscription);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomOwnerPromoted.create()
            .roomId(roomId)
            .userId(userId)
            .type(EventType.ROOM_OWNER_PROMOTED)
            .sentDate(OffsetDateTime.now()));
  }

  @Override
  public void demoteOwnerToMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    validateUserRequest(userId, currentUser);
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);
    Subscription subscription = getSubscriptionFromRoom(userId, room);
    subscription.owner(false);
    subscriptionRepository.update(subscription);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomOwnerDemoted.create()
            .roomId(roomId)
            .userId(userId)
            .type(EventType.ROOM_OWNER_DEMOTED)
            .sentDate(OffsetDateTime.now()));
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
  public List<MemberDto> updateRoomOwners(
      UUID roomId, List<MemberDto> members, UserPrincipal currentUser) {
    validateUserRequest(members, currentUser);
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);

    List<Subscription> subscriptionsToUpdate = new ArrayList<>();
    for (MemberDto member : members) {
      Subscription subscription = getSubscriptionFromRoom(member.getUserId(), room);
      subscriptionsToUpdate.add(subscription.owner(member.isOwner()));
    }

    List<Subscription> subscriptionsUpdated =
        subscriptionRepository.updateAll(subscriptionsToUpdate);
    sendOwnersUpdates(roomId, subscriptionsToUpdate, room);
    return subscriptionsUpdated.stream()
        .map(
            subscription ->
                new MemberDto()
                    .userId(UUID.fromString(subscription.getUserId()))
                    .owner(subscription.isOwner()))
        .toList();
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
                        .sentDate(OffsetDateTime.now())
                    : RoomOwnerDemoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))
                        .type(EventType.ROOM_OWNER_DEMOTED)
                        .sentDate(OffsetDateTime.now())));
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
  public List<Subscription> initRoomSubscriptions(
      List<MemberDto> members, Room room, OffsetDateTime joinedAt) {
    return members.stream()
        .map(
            member ->
                Subscription.create()
                    .id(new SubscriptionId(room.getId(), member.getUserId().toString()))
                    .userId(member.getUserId().toString())
                    .room(room)
                    // When we have a one-to-one, both members are owners
                    .owner(member.isOwner() || RoomTypeDto.ONE_TO_ONE.equals(room.getType()))
                    .joinedAt(joinedAt))
        .toList();
  }
}
