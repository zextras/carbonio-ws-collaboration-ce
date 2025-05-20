// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SubscriptionId;
import com.zextras.carbonio.chats.core.data.event.RoomMemberAdded;
import com.zextras.carbonio.chats.core.data.event.RoomMemberRemoved;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerDemoted;
import com.zextras.carbonio.chats.core.data.event.RoomOwnerPromoted;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageType;
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
  private final CapabilityService capabilityService;

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
      ParticipantService participantService,
      CapabilityService capabilityService) {
    this.roomService = roomService;
    this.subscriptionRepository = subscriptionRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.eventDispatcher = eventDispatcher;
    this.subscriptionMapper = subscriptionMapper;
    this.userService = userService;
    this.messageDispatcher = messageDispatcher;
    this.meetingService = meetingService;
    this.participantService = participantService;
    this.capabilityService = capabilityService;
  }

  @Override
  public Optional<Subscription> getSubscription(UUID userId, UUID roomId) {
    return subscriptionRepository.getById(roomId.toString(), userId.toString());
  }

  @Override
  public void setOwner(UUID roomId, UUID userId, boolean isOwner, UserPrincipal currentUser) {
    if (userId.equals(currentUser.getUUID())) {
      throw new BadRequestException("Cannot set owner privileges for itself");
    }
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      throw new BadRequestException(
          String.format("Cannot set owner privileges on %s rooms", room.getType()));
    }
    Subscription subscription =
        room.getSubscriptions().stream()
            .filter(roomMember -> roomMember.getUserId().equals(userId.toString()))
            .findAny()
            .orElseThrow(
                () ->
                    new ForbiddenException(
                        String.format("User '%s' is not a member of the room", userId)));

    subscription.owner(isOwner);
    subscriptionRepository.update(subscription);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        isOwner
            ? RoomOwnerPromoted.create().roomId(roomId).userId(userId)
            : RoomOwnerDemoted.create().roomId(roomId).userId(userId));
  }

  @Override
  public List<MemberInsertedDto> insertRoomMembers(
      UUID roomId, List<MemberToInsertDto> membersToInsert, UserPrincipal currentUser) {
    List<UUID> memberIds = extractUniqueMemberIds(membersToInsert);
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);

    validateInsertRoomMembers(memberIds, room, currentUser);

    OffsetDateTime currentDateTime = OffsetDateTime.now();
    List<MemberInsertedDto> membersInserted = new ArrayList<>();
    for (MemberToInsertDto member : membersToInsert) {
      messageDispatcher.addRoomMember(
          room.getId(), currentUser.getId(), member.getUserId().toString());
      messageDispatcher.sendAffiliationMessage(
          room.getId(),
          currentUser.getId(),
          member.getUserId().toString(),
          MessageType.MEMBER_ADDED);
      MemberInsertedDto memberInsertedDto = processRoomSubscription(room, member, currentDateTime);

      membersInserted.add(memberInsertedDto);
      eventDispatcher.sendToUserExchange(
          room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
          RoomMemberAdded.create()
              .roomId(UUID.fromString(room.getId()))
              .userId(member.getUserId())
              .isOwner(member.isOwner()));
    }
    return membersInserted;
  }

  @Override
  public List<MemberDto> updateRoomOwners(
      UUID roomId, List<MemberDto> members, UserPrincipal currentUser) {
    if (members.stream().anyMatch(member -> member.getUserId().equals(currentUser.getUUID()))) {
      throw new BadRequestException("Cannot update owner privileges for itself");
    }
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, true);
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      throw new BadRequestException(
          String.format("Cannot update owner privileges on %s rooms", room.getType()));
    }

    List<Subscription> subscriptionsToUpdate = new ArrayList<>();
    for (MemberDto member : members) {
      Subscription subscription =
          room.getSubscriptions().stream()
              .filter(roomMember -> roomMember.getUserId().equals(member.getUserId().toString()))
              .findAny()
              .orElseThrow(
                  () ->
                      new ForbiddenException(
                          String.format(
                              "User '%s' is not a member of the room", member.getUserId())));
      subscriptionsToUpdate.add(subscription.owner(member.isOwner()));
    }

    List<Subscription> subscriptionsUpdated =
        subscriptionRepository.updateAll(subscriptionsToUpdate);
    subscriptionsToUpdate.forEach(
        subscription ->
            eventDispatcher.sendToUserExchange(
                room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
                subscription.isOwner()
                    ? RoomOwnerPromoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))
                    : RoomOwnerDemoted.create()
                        .roomId(roomId)
                        .userId(UUID.fromString(subscription.getUserId()))));
    return subscriptionsUpdated.stream()
        .map(
            subscription ->
                new MemberDto()
                    .userId(UUID.fromString(subscription.getUserId()))
                    .owner(subscription.isOwner()))
        .toList();
  }

  private List<UUID> extractUniqueMemberIds(List<MemberToInsertDto> membersToInsert) {
    return new ArrayList<>(
        new HashSet<>(membersToInsert.stream().map(MemberToInsertDto::getUserId).toList()));
  }

  private void validateInsertRoomMembers(
      List<UUID> memberIds, Room room, UserPrincipal currentUser) {
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      throw new BadRequestException(
          String.format("Cannot add members to a %s conversation", room.getType()));
    } else if (RoomTypeDto.GROUP.equals(room.getType())) {
      Integer maxGroupMembers = capabilityService.getCapabilities(currentUser).getMaxGroupMembers();
      if (room.getSubscriptions().size() >= maxGroupMembers) {
        throw new BadRequestException(
            String.format("Cannot add more members to this %s", room.getType()));
      }
    }
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
      roomUserSettingsRepository.save(settings.clearedAt(dateTime));
    }
  }

  @Override
  public void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    Room room =
        roomService.getRoomAndValidateUser(
            roomId, currentUser, !currentUser.getUUID().equals(userId));
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      throw new BadRequestException(
          String.format("Cannot remove a member from a %s conversation", room.getType()));
    }
    if (!currentUser.getUUID().equals(userId)
        && room.getSubscriptions().stream()
            .noneMatch(s -> s.getUserId().equals(userId.toString()))) {
      throw new NotFoundException("The user is not a room member");
    }
    if (room.getMeetingId() != null) {
      meetingService
          .getMeetingEntity(UUID.fromString(room.getMeetingId()))
          .ifPresent(meeting -> participantService.removeMeetingParticipant(meeting, room, userId));
    }
    validateLastRoomOwner(userId.toString(), room);
    String ownerId =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList()
            .get(0);
    messageDispatcher.removeRoomMember(room.getId(), userId.toString());
    messageDispatcher.sendAffiliationMessage(
        room.getId(), ownerId, userId.toString(), MessageType.MEMBER_REMOVED);
    roomUserSettingsRepository
        .getByRoomIdAndUserId(roomId.toString(), userId.toString())
        .ifPresent(roomUserSettingsRepository::delete);
    subscriptionRepository.delete(room.getId(), userId.toString());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomMemberRemoved.create().roomId(UUID.fromString(room.getId())).userId(userId));
    // delete room if it's the last member
    if (room.getSubscriptions().size() == 1) {
      roomService.deleteRoom(roomId, currentUser);
    }
  }

  private void validateLastRoomOwner(String userId, Room room) {
    List<String> owners =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList();
    if (owners.size() == 1 && owners.get(0).equals(userId) && room.getSubscriptions().size() > 1) {
      throw new BadRequestException("Last owner can't leave the room");
    }
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    Room room = roomService.getRoomAndValidateUser(roomId, currentUser, false);
    return subscriptionMapper.ent2memberDto(room.getSubscriptions());
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
