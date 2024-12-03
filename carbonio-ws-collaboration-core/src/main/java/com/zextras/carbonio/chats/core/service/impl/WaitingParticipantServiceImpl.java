// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.WaitingParticipant;
import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingWaitingParticipantAccepted;
import com.zextras.carbonio.chats.core.data.event.MeetingWaitingParticipantRejected;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.WaitingParticipantService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.meeting.model.QueueUpdateStatusDto;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class WaitingParticipantServiceImpl implements WaitingParticipantService {

  private final MeetingService meetingService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final WaitingParticipantRepository waitingParticipantRepository;
  private final EventDispatcher eventDispatcher;

  @Inject
  public WaitingParticipantServiceImpl(
      MeetingService meetingService,
      RoomService roomService,
      MembersService membersService,
      WaitingParticipantRepository waitingParticipantRepository,
      EventDispatcher eventDispatcher) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.membersService = membersService;
    this.waitingParticipantRepository = waitingParticipantRepository;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void addQueuedUser(String meetingId, String userId, String queueId) {
    waitingParticipantRepository.insert(
        WaitingParticipant.create()
            .id(UUID.randomUUID().toString())
            .meetingId(meetingId)
            .userId(userId)
            .queueId(queueId)
            .status(JoinStatus.WAITING));
  }

  @Override
  public void removeQueuedUser(WaitingParticipant wp) {
    waitingParticipantRepository.remove(wp);
  }

  public void updateQueuedUser(WaitingParticipant wp) {
    waitingParticipantRepository.update(wp);
  }

  @Override
  public Optional<WaitingParticipant> getWaitingParticipant(String meetingId, String userId) {
    return waitingParticipantRepository.getWaitingParticipant(meetingId, userId);
  }

  @Override
  public List<UUID> getQueue(UUID meetingId) {
    return waitingParticipantRepository.getWaitingList(meetingId.toString()).stream()
        .map(WaitingParticipant::getUserId)
        .map(UUID::fromString)
        .toList();
  }

  @Override
  public void clearQueue(UUID meetingId) {
    waitingParticipantRepository.clear(meetingId.toString());
  }

  @Override
  public void removeFromQueue(UUID queueId) {
    waitingParticipantRepository
        .getByQueueId(queueId.toString())
        .ifPresent(
            wp ->
                meetingService
                    .getMeetingEntity(UUID.fromString(wp.getMeetingId()))
                    .ifPresent(
                        meeting ->
                            roomService
                                .getRoom(UUID.fromString(meeting.getRoomId()))
                                .ifPresent(
                                    room ->
                                        handleRejection(
                                            wp, UUID.fromString(meeting.getId()), room))));
  }

  @Override
  public void updateQueue(
      UUID meetingId, UUID userId, QueueUpdateStatusDto status, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room = validateMeetingRoom(meeting);

    // Check if the current user has permission to update the queue
    validateQueueUpdatePermissions(status, userId, currentUser, room);

    // Process the waiting participant based on the provided status
    waitingParticipantRepository
        .getWaitingParticipant(meetingId.toString(), userId.toString())
        .ifPresentOrElse(
            wp -> processQueueUpdate(status, wp, meetingId, userId, meeting, room, currentUser),
            () -> {
              throw new NotFoundException(
                  String.format("User '%s' not in the meeting '%s' queue", userId, meetingId));
            });
  }

  // Validates and retrieves the meeting
  private Meeting validateMeeting(UUID meetingId) {
    return meetingService
        .getMeetingEntity(meetingId)
        .orElseThrow(
            () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
  }

  // Validates and retrieves the associated room
  private Room validateMeetingRoom(Meeting meeting) {
    return roomService
        .getRoom(UUID.fromString(meeting.getRoomId()))
        .orElseThrow(
            () -> new NotFoundException(String.format("Room '%s' not found", meeting.getRoomId())));
  }

  private boolean isOwner(Room room, String userId) {
    return room.getSubscriptions().stream()
        .filter(u -> u.getUserId().equals(userId))
        .findFirst()
        .map(Subscription::isOwner)
        .orElse(false);
  }

  private void validateQueueUpdatePermissions(
      QueueUpdateStatusDto status, UUID userId, UserPrincipal currentUser, Room room) {
    boolean isModerator = isOwner(room, currentUser.getId());
    boolean unauthorizedAcceptance = QueueUpdateStatusDto.ACCEPTED.equals(status) && !isModerator;
    boolean unauthorizedRejection =
        QueueUpdateStatusDto.REJECTED.equals(status)
            && (!isModerator && !userId.toString().equals(currentUser.getId()));

    if (unauthorizedAcceptance || unauthorizedRejection) {
      throw new ForbiddenException("User cannot accept or reject a queued user");
    }
  }

  private void processQueueUpdate(
      QueueUpdateStatusDto status,
      WaitingParticipant wp,
      UUID meetingId,
      UUID userId,
      Meeting meeting,
      Room room,
      UserPrincipal currentUser) {
    switch (status) {
      case ACCEPTED -> handleAcceptance(wp, meeting, userId, room, currentUser);
      case REJECTED -> handleRejection(wp, meetingId, room);
    }
  }

  private void handleAcceptance(
      WaitingParticipant wp, Meeting meeting, UUID userId, Room room, UserPrincipal currentUser) {
    wp.status(JoinStatus.ACCEPTED);
    waitingParticipantRepository.update(wp);

    // Insert user as a room member if not already present
    try {
      membersService.insertRoomMembers(
          UUID.fromString(meeting.getRoomId()),
          List.of(MemberToInsertDto.create().userId(userId).owner(false).historyCleared(false)),
          currentUser);
    } catch (BadRequestException ignored) {
      // Member insertion failure is ignored as the user might already be in the room
    }

    DomainEvent acceptedEvent =
        MeetingWaitingParticipantAccepted.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(UUID.fromString(wp.getUserId()));

    notifyUsers(room, wp, acceptedEvent);
  }

  private void handleRejection(WaitingParticipant wp, UUID meetingId, Room room) {
    waitingParticipantRepository.remove(wp);

    DomainEvent rejectedEvent =
        MeetingWaitingParticipantRejected.create()
            .meetingId(meetingId)
            .userId(UUID.fromString(wp.getUserId()));

    notifyUsers(room, wp, rejectedEvent);
  }

  private void notifyUsers(Room room, WaitingParticipant wp, DomainEvent event) {
    // Notify all room owners
    List<String> ownerIds = getOwnerIds(room);
    eventDispatcher.sendToUserExchange(ownerIds, event);

    // Notify the user in the queue
    eventDispatcher.sendToUserQueue(wp.getUserId(), wp.getQueueId(), event);
  }

  private List<String> getOwnerIds(Room room) {
    return room.getSubscriptions().stream()
        .filter(Subscription::isOwner)
        .map(Subscription::getUserId)
        .toList();
  }
}
