// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingCreated;
import com.zextras.carbonio.async.model.MeetingDeclined;
import com.zextras.carbonio.async.model.MeetingDeleted;
import com.zextras.carbonio.async.model.MeetingStarted;
import com.zextras.carbonio.async.model.MeetingStopped;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.Clock;
import java.time.Duration;
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
  private final ParticipantService participantService;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;
  private final MessageDispatcher messageDispatcher;

  @Inject
  public MeetingServiceImpl(
      MeetingRepository meetingRepository,
      MeetingMapper meetingMapper,
      RoomService roomService,
      MembersService membersService,
      ParticipantService participantService,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      Clock clock,
      MessageDispatcher messageDispatcher) {
    this.meetingRepository = meetingRepository;
    this.meetingMapper = meetingMapper;
    this.roomService = roomService;
    this.membersService = membersService;
    this.participantService = participantService;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
    this.clock = clock;
    this.messageDispatcher = messageDispatcher;
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
            .roomId(UUID.fromString(room.getId()))
            .type(EventType.MEETING_CREATED)
            .sentDate(OffsetDateTime.now(clock)));

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
    OffsetDateTime startedAt = meeting.getStartedAt();

    Meeting updatedMeeting = deactivateMeeting(meeting);

    roomService
        .getRoom(UUID.fromString(updatedMeeting.getRoomId()))
        .ifPresent(
            room -> {
              notifyMeetingStopped(updatedMeeting, room);
              notifyMeetingStoppedForOneToOneMeeting(room, user.getId(), startedAt);
              cleanUpRoomMembers(room);
            });

    return meetingMapper.ent2dto(updatedMeeting);
  }

  @Override
  public void declineMeeting(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room =
        roomService.getRoomAndValidateUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    eventDispatcher.sendToUserExchange(
        meeting.getParticipants().stream().map(Participant::getUserId).distinct().toList(),
        MeetingDeclined.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(UUID.fromString(currentUser.getId()))
            .type(EventType.MEETING_DECLINED)
            .sentDate(OffsetDateTime.now()));
    messageDispatcher.sendMeetingDeclined(meeting.getRoomId(), currentUser.getId());

    if (room.getType() == RoomTypeDto.ONE_TO_ONE && meeting.getParticipants().size() == 1) {
      Meeting updatedMeeting = deactivateMeeting(meeting);
      notifyMeetingStopped(updatedMeeting, room);
    }
  }

  private void cleanUpRoomMembers(Room room) {
    List<Subscription> subscriptions = room.getSubscriptions();

    if (RoomTypeDto.TEMPORARY.equals(room.getType())) {
      subscriptions.stream()
          .filter(member -> !member.isOwner())
          .forEach(member -> membersService.deleteRoomMember(member.getUserId(), room));
    }
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
    participantService.clear(UUID.fromString(meeting.getId()));
    meeting.active(false).participants(List.of()).startedAt(null);
    return meetingRepository.update(meeting);
  }

  private void notifyMeetingStarted(UserPrincipal user, Meeting updatedMeeting) {
    RoomDto room = roomService.getRoomById(UUID.fromString(updatedMeeting.getRoomId()), user);
    List<String> allReceivers =
        room.getMembers().stream().map(m -> m.getUserId().toString()).toList();

    eventDispatcher.sendToUserExchange(
        allReceivers,
        MeetingStarted.create()
            .meetingId(UUID.fromString(updatedMeeting.getId()))
            .starterUser(user.getUUID())
            .startedAt(updatedMeeting.getStartedAt())
            .type(EventType.MEETING_STARTED)
            .sentDate(OffsetDateTime.now(clock)));

    notifyMeetingStartedForOneToOneMeeting(user, room);
  }

  private void notifyMeetingStartedForOneToOneMeeting(UserPrincipal user, RoomDto room) {
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      messageDispatcher.sendMeetingStarted(room.getId().toString(), user.getId());
    }
  }

  private void notifyMeetingStopped(Meeting updatedMeeting, Room room) {
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingStopped.create()
            .meetingId(UUID.fromString(updatedMeeting.getId()))
            .type(EventType.MEETING_STOPPED)
            .sentDate(OffsetDateTime.now(clock)));
  }

  private void notifyMeetingStoppedForOneToOneMeeting(
      Room room, String userId, OffsetDateTime startedAt) {
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      long duration = Duration.between(startedAt, OffsetDateTime.now(clock)).toSeconds();
      messageDispatcher.sendMeetingEnded(room.getId(), userId, startedAt, duration);
    }
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
        MeetingDeleted.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .type(EventType.MEETING_DELETED)
            .sentDate(OffsetDateTime.now(clock)));
  }

  @Override
  public void updateMeeting(Meeting updatedMeeting) {
    meetingRepository.update(updatedMeeting);
  }
}
