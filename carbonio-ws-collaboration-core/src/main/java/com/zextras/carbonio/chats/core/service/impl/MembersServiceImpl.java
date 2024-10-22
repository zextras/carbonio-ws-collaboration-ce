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
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
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
  private final MessageDispatcher messageService;
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
    this.messageService = messageDispatcher;
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
    Room room = roomService.getRoomEntityAndCheckUser(roomId, currentUser, true);
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
  public MemberInsertedDto insertRoomMember(
      UUID roomId, MemberToInsertDto memberToInsertDto, UserPrincipal currentUser) {
    if (!userService.userExists(memberToInsertDto.getUserId(), currentUser)) {
      throw new NotFoundException(
          String.format("User with id '%s' was not found", memberToInsertDto.getUserId()));
    }
    Room room = roomService.getRoomEntityAndCheckUser(roomId, currentUser, true);
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      throw new BadRequestException(
          String.format("Cannot add members to a %s conversation", room.getType()));
    } else if (RoomTypeDto.GROUP.equals(room.getType())) {
      Integer maxGroupMembers = capabilityService.getCapabilities(currentUser).getMaxGroupMembers();
      if (room.getSubscriptions().size() == maxGroupMembers) {
        throw new BadRequestException(
            String.format("Cannot add more members to this %s", room.getType()));
      }
    }
    if (room.getSubscriptions().stream()
        .anyMatch(member -> memberToInsertDto.getUserId().toString().equals(member.getUserId()))) {
      throw new BadRequestException(
          String.format("User '%s' is already a room member", memberToInsertDto.getUserId()));
    }
    Subscription subscription =
        subscriptionRepository.insert(
            Subscription.create()
                .room(room)
                .userId(memberToInsertDto.getUserId().toString())
                .owner(memberToInsertDto.isOwner())
                .temporary(false)
                .external(false)
                .joinedAt(OffsetDateTime.now()));
    room.getSubscriptions().add(subscription);
    RoomUserSettings settings = null;
    if (memberToInsertDto.isHistoryCleared()) {
      settings =
          roomUserSettingsRepository
              .getByRoomIdAndUserId(roomId.toString(), memberToInsertDto.getUserId().toString())
              .orElseGet(
                  () -> RoomUserSettings.create(room, memberToInsertDto.getUserId().toString()));
      roomUserSettingsRepository.save(settings.clearedAt(OffsetDateTime.now()));
    }
    messageService.addRoomMember(
        room.getId(), currentUser.getId(), memberToInsertDto.getUserId().toString());

    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomMemberAdded.create()
            .roomId(UUID.fromString(room.getId()))
            .userId(memberToInsertDto.getUserId())
            .isOwner(memberToInsertDto.isOwner()));
    return subscriptionMapper.ent2memberInsertedDto(subscription, settings);
  }

  @Override
  @Transactional
  public void deleteRoomMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    Room room =
        roomService.getRoomEntityAndCheckUser(
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
    List<String> owners =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList();
    if (owners.size() == 1
        && owners.get(0).equals(userId.toString())
        && room.getSubscriptions().size() > 1) {
      throw new BadRequestException("Last owner can't leave the room");
    }

    subscriptionRepository.delete(room.getId(), userId.toString());

    messageService.removeRoomMember(
        room.getId(),
        room.getSubscriptions().stream().filter(Subscription::isOwner).toList().get(0).getUserId(),
        userId.toString());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomMemberRemoved.create().roomId(UUID.fromString(room.getId())).userId(userId));
    // the room isn't updated with subscriptions table. It still has the deleted subscription,
    // so if room has only one subscription, but actually it is empty
    if (room.getSubscriptions().size() == 1) {
      roomService.deleteRoom(roomId, currentUser);
    }
  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, UserPrincipal currentUser) {
    return subscriptionMapper.ent2memberDto(
        roomService.getRoomEntityAndCheckUser(roomId, currentUser, false).getSubscriptions());
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
                    // When we have a one to one, both members are owners
                    .owner(member.isOwner() || RoomTypeDto.ONE_TO_ONE.equals(room.getType()))
                    .temporary(false)
                    .external(false)
                    .joinedAt(OffsetDateTime.now()))
        .toList();
  }
}
