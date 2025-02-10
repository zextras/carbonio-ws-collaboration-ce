// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingCreated;
import com.zextras.carbonio.chats.core.data.event.MeetingDeleted;
import com.zextras.carbonio.chats.core.data.event.MeetingStarted;
import com.zextras.carbonio.chats.core.data.event.MeetingStopped;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository meetingRepository;
  private final MeetingMapper meetingMapper;
  private final RoomService roomService;
  private final MembersService membersService;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;

  @Inject
  public MeetingServiceImpl(
      MeetingRepository meetingRepository,
      MeetingMapper meetingMapper,
      RoomService roomService,
      MembersService membersService,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      Clock clock) {
    this.meetingRepository = meetingRepository;
    this.meetingMapper = meetingMapper;
    this.roomService = roomService;
    this.membersService = membersService;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
    this.clock = clock;
  }

  @Override
  public MeetingDto createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingTypeDto,
      UUID roomId,
      OffsetDateTime expiration) {
    Room room = roomService.getRoomAndValidateUser(roomId, user, false);
    validateRoomMeeting(room.getId(), room.getMeetingId());

    Meeting meeting = createNewMeeting(name, meetingTypeDto, roomId.toString());
    roomService.setMeetingIntoRoom(room, meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingCreated.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .roomId(UUID.fromString(room.getId())));

    return meetingMapper.ent2dto(meeting);
  }

  private void validateRoomMeeting(String roomId, String meetingId) {
    if (meetingId != null) {
      throw new ConflictException(
          String.format("Room %s has already an associated meeting", roomId));
    }
  }

  private Meeting createNewMeeting(String name, MeetingTypeDto meetingTypeDto, String roomId) {
    return meetingRepository.insert(
        Meeting.create()
            .id(UUID.randomUUID().toString())
            .name(name)
            .meetingType(MeetingType.valueOf(meetingTypeDto.toString().toUpperCase()))
            .active(false)
            .roomId(roomId));
  }

  @Override
  public MeetingDto startMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);

    Meeting updatedMeeting = activateMeeting(meeting);

    notifyMeetingStarted(user, updatedMeeting);

    return meetingMapper.ent2dto(updatedMeeting);
  }

  @Override
  public MeetingDto stopMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);

    Meeting updatedMeeting = deactivateMeeting(meeting);

    notifyMeetingStopped(user, updatedMeeting);

    return meetingMapper.ent2dto(updatedMeeting);
  }

  private Meeting validateMeeting(UUID meetingId) {
    return meetingRepository
        .getById(meetingId.toString())
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
  }

  private Meeting activateMeeting(Meeting meeting) {
    videoServerService.startMeeting(meeting.getId());
    meeting.active(true).startedAt(OffsetDateTime.now(clock));
    return meetingRepository.update(meeting);
  }

  private Meeting deactivateMeeting(Meeting meeting) {
    videoServerService.stopMeeting(meeting.getId());
    meeting.active(false).startedAt(null);
    return meetingRepository.update(meeting);
  }

  private void notifyMeetingStarted(UserPrincipal user, Meeting updatedMeeting) {
    List<String> allReceivers =
        roomService
            .getRoomById(UUID.fromString(updatedMeeting.getRoomId()), user)
            .getMembers()
            .stream()
            .map(m -> m.getUserId().toString())
            .toList();

    eventDispatcher.sendToUserExchange(
        allReceivers,
        MeetingStarted.create()
            .meetingId(UUID.fromString(updatedMeeting.getId()))
            .starterUser(user.getUUID())
            .startedAt(updatedMeeting.getStartedAt()));
  }

  private void notifyMeetingStopped(UserPrincipal user, Meeting updatedMeeting) {
    List<String> allReceivers =
        roomService
            .getRoomById(UUID.fromString(updatedMeeting.getRoomId()), user)
            .getMembers()
            .stream()
            .map(m -> m.getUserId().toString())
            .toList();

    eventDispatcher.sendToUserExchange(
        allReceivers, MeetingStopped.create().meetingId(UUID.fromString(updatedMeeting.getId())));
  }

  @Override
  public List<MeetingDto> getMeetings(UserPrincipal currentUser) {
    List<String> roomsIds =
        roomService.getRoomsIds(currentUser).stream().map(UUID::toString).toList();
    List<Meeting> meetings = meetingRepository.getByRoomsIds(roomsIds);
    return meetingMapper.ent2dto(meetings);
  }

  @Override
  public MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    if (membersService
        .getSubscription(currentUser.getUUID(), UUID.fromString(meeting.getRoomId()))
        .isEmpty()) {
      throw new ForbiddenException(
          String.format(
              "User '%s' hasn't access to the meeting with id '%s'",
              currentUser.getId(), meetingId));
    }
    return meetingMapper.ent2dto(meeting);
  }

  @Override
  public Optional<Meeting> getMeetingEntity(UUID meetingId) {
    return meetingRepository.getById(meetingId.toString());
  }

  @Override
  public Optional<Meeting> getMeetingEntityByRoomId(UUID roomId) {
    return meetingRepository.getByRoomId(roomId.toString());
  }

  @Override
  public MeetingDto getMeetingByRoomId(UUID roomId, UserPrincipal currentUser) {
    roomService.getRoomAndValidateUser(roomId, currentUser, false);
    return meetingMapper.ent2dto(
        getMeetingEntityByRoomId(roomId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting of the room with id '%s' doesn't exist", roomId))));
  }

  @Override
  public void deleteMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);

    Room room =
        roomService.getRoomAndValidateUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    deleteMeeting(currentUser.getId(), meeting, room);
  }

  @Override
  public void deleteMeeting(String userId, Meeting meeting, Room room) {
    videoServerService.stopMeeting(meeting.getId());
    meetingRepository.delete(meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingDeleted.create().meetingId(UUID.fromString(meeting.getId())));
  }

  @Override
  public void updateMeeting(Meeting updatedMeeting) {
    meetingRepository.update(updatedMeeting);
  }
}
