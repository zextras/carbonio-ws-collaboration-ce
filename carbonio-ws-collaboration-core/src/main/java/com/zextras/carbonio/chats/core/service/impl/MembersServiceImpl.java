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
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
import com.zextras.carbonio.chats.core.mapper.SubscriptionMapper;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.MemberInsertedDto;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
  private final MessageDispatcher messageDispatcher;
  private final MeetingService meetingService;
  private final ParticipantService participantService;

  @Inject
  public MembersServiceImpl(
      RoomService roomService,
      SubscriptionRepository subscriptionRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      EventDispatcher eventDispatcher,
      SubscriptionMapper subscriptionMapper,
      UserService userService,
      MessageDispatcher messageDispatcher,
      MeetingService meetingService,
      ParticipantService participantService) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.userService = userService;
    this.messageDispatcher = messageDispatcher;
    this.meetingService = meetingService;
    this.participantService = participantService;
  }

  @Override
  public List<MemberInsertedDto> insertRoomMembers(
      UUID roomId, List<MemberToInsertDto> membersToInsert, UserPrincipal currentUser) {
    List<UUID> memberIds = extractUniqueMemberIds(membersToInsert);
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);

    validateMemberPresence(memberIds, room);
    validateUserExistence(memberIds, currentUser);

    OffsetDateTime currentDateTime = OffsetDateTime.now();
    List<MemberInsertedDto> membersInserted = new ArrayList<>();
    for (MemberToInsertDto member : membersToInsert) {
      addMemberToRoom(member.getUserId().toString(), room, currentUser.getId());
      MemberInsertedDto memberInsertedDto = processRoomSubscription(room, member, currentDateTime);

      membersInserted.add(memberInsertedDto);
      eventDispatcher.sendToUserExchange(
          room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
          RoomMemberAdded.create()
              .roomId(UUID.fromString(room.getId()))
              .userId(member.getUserId())
              .isOwner(member.isOwner())
              .type(EventType.ROOM_MEMBER_ADDED)
              .sentDate(OffsetDateTime.now()));
    }
    return membersInserted;
  }

  private List<UUID> extractUniqueMemberIds(List<MemberToInsertDto> membersToInsert) {
    return new ArrayList<>(
        new HashSet<>(membersToInsert.stream().map(MemberToInsertDto::getUserId).toList()));
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

  private void addMemberToRoom(String memberId, Room room, String currentUserId) {
    messageDispatcher.addRoomMember(room.getId(), currentUserId, memberId);
    messageDispatcher.sendAffiliationMessage(
        room.getId(), currentUserId, memberId, MessageType.MEMBER_ADDED);
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

    processRoomUserSettings(room, member, dateTime);

    MemberInsertedDto memberInsertedDto =
        MemberInsertedDto.create().userId(member.getUserId()).owner(member.isOwner());

    return member.isHistoryCleared() ? memberInsertedDto.clearedAt(dateTime) : memberInsertedDto;
  }

  private void processRoomUserSettings(
      Room room, MemberToInsertDto member, OffsetDateTime dateTime) {
    if (member.isHistoryCleared()) {
      RoomUserSettings settings =
          roomUserSettingsRepository
              .getByRoomIdAndUserId(room.getId(), member.getUserId().toString())
              .orElseGet(() -> RoomUserSettings.create(room, member.getUserId().toString()));

      settings.clearedAt(dateTime);
      roomUserSettingsRepository.save(settings);
    }
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
    deleteRoomMember(userId.toString(), room);
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
    removeRoomMember(userId, room);
    roomUserSettingsRepository
        .getByRoomIdAndUserId(room.getId(), userId)
        .ifPresent(roomUserSettingsRepository::delete);
    subscriptionRepository.delete(room.getId(), userId);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomMemberRemoved.create()
            .roomId(UUID.fromString(room.getId()))
            .userId(UUID.fromString(userId))
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

  private void removeRoomMember(String memberId, Room room) {
    String ownerId =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList()
            .getFirst();

    messageDispatcher.removeRoomMember(room.getId(), memberId);
    messageDispatcher.sendAffiliationMessage(
        room.getId(), ownerId, memberId, MessageType.MEMBER_REMOVED);
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
                    : RoomOwnerDemoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))));
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
                    .owner(member.isOwner() || RoomTypeDto.ONE_TO_ONE.equals(room.getType()))
                    .joinedAt(OffsetDateTime.now()))
        .toList();
  }
}
