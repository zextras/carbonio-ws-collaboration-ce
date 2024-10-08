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
import io.vavr.control.Option;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
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
      MeetingTypeDto meetingType,
      UUID roomId,
      OffsetDateTime expiration) {
    return meetingMapper.ent2dto(
        Option.of(roomService.getRoomEntityAndCheckUser(roomId, user, false))
            .map(
                room -> {
                  if (room.getMeetingId() == null) {
                    Meeting meeting =
                        meetingRepository.insert(
                            Meeting.create()
                                .id(UUID.randomUUID().toString())
                                .name(name)
                                .meetingType(
                                    MeetingType.valueOf(meetingType.toString().toUpperCase()))
                                .active(false)
                                .roomId(roomId.toString()));
                    roomService.setMeetingIntoRoom(room, meeting);
                    eventDispatcher.sendToUserExchange(
                        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
                        MeetingCreated.create()
                            .meetingId(UUID.fromString(meeting.getId()))
                            .roomId(roomId));
                    return meeting;
                  } else {
                    throw new ConflictException(
                        String.format("Room %s has already an associated meeting", roomId));
                  }
                })
            .getOrElseThrow(
                () -> new NotFoundException(String.format("Room %s not found", roomId))));
  }

  @Override
  public MeetingDto updateMeeting(UserPrincipal user, UUID meetingId, Boolean active) {
    return meetingMapper.ent2dto(
        meetingRepository
            .getById(meetingId.toString())
            .map(
                meeting -> {
                  Option.of(active)
                      .peek(
                          s -> {
                            if (!Objects.equals(s, meeting.getActive())) {
                              if (Boolean.TRUE.equals(s)) {
                                videoServerService.startMeeting(meeting.getId());
                                meeting.startedAt(OffsetDateTime.now(clock));
                              } else {
                                videoServerService.stopMeeting(meeting.getId());
                                meeting.startedAt(null);
                              }
                              meeting.active(s);
                            }
                          });
                  Meeting updatedMeeting = meetingRepository.update(meeting);
                  eventDispatcher.sendToUserExchange(
                      roomService
                          .getRoomById(UUID.fromString(meeting.getRoomId()), user)
                          .getMembers()
                          .stream()
                          .map(m -> m.getUserId().toString())
                          .toList(),
                      Boolean.TRUE.equals(active)
                          ? MeetingStarted.create()
                              .meetingId(UUID.fromString(updatedMeeting.getId()))
                              .starterUser(user.getUUID())
                              .startedAt(updatedMeeting.getStartedAt())
                          : MeetingStopped.create()
                              .meetingId(UUID.fromString(updatedMeeting.getId())));
                  return updatedMeeting;
                })
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId))));
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
    Meeting meeting =
        meetingRepository
            .getById(meetingId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));
    if (membersService
        .getByUserIdAndRoomId(currentUser.getUUID(), UUID.fromString(meeting.getRoomId()))
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
    roomService.getRoomEntityAndCheckUser(roomId, currentUser, false);
    return meetingMapper.ent2dto(
        getMeetingEntityByRoomId(roomId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting of the room with id '%s' doesn't exist", roomId))));
  }

  @Override
  public void deleteMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting =
        meetingRepository
            .getById(meetingId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));

    deleteMeeting(
        meeting,
        roomService.getRoomEntityAndCheckUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false),
        currentUser.getUUID());
  }

  @Override
  public void deleteMeeting(Meeting meeting, Room room, UUID userId) {
    videoServerService.stopMeeting(meeting.getId());
    meetingRepository.delete(meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingDeleted.create().meetingId(UUID.fromString(meeting.getId())));
  }
}
