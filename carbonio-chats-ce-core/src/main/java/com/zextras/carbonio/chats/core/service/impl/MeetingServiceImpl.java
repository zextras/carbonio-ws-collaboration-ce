package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingDeletedEvent;
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
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository  meetingRepository;
  private final MeetingMapper      meetingMapper;
  private final RoomService        roomService;
  private final MembersService     membersService;
  private final VideoServerService videoServerService;
  private final EventDispatcher    eventDispatcher;

  @Inject
  public MeetingServiceImpl(
    MeetingRepository meetingRepository, MeetingMapper meetingMapper,
    RoomService roomService,
    MembersService membersService, VideoServerService videoServerService,
    EventDispatcher eventDispatcher
  ) {
    this.meetingRepository = meetingRepository;
    this.meetingMapper = meetingMapper;
    this.roomService = roomService;
    this.membersService = membersService;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public List<MeetingDto> getMeetings(UserPrincipal currentUser) {
    List<String> roomsIds = roomService.getRoomsIds(currentUser).stream().map(UUID::toString)
      .collect(Collectors.toList());
    List<Meeting> meetings = meetingRepository.getByRoomsIds(roomsIds);
    return meetingMapper.ent2dto(meetings);
  }

  @Override
  public MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = meetingRepository.getById(meetingId.toString())
      .orElseThrow(() -> new NotFoundException(
        String.format("Meeting with id '%s' not found", meetingId)));
    membersService.getByUserIdAndRoomId(currentUser.getUUID(), UUID.fromString(meeting.getRoomId()))
      .orElseThrow(() -> new ForbiddenException(
        String.format("User '%s' hasn't access to the meeting with id '%s'", currentUser.getId(), meetingId)));
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
    return meetingMapper.ent2dto(getMeetingEntityByRoomId(roomId)
      .orElseThrow(() -> new NotFoundException(
        String.format("Meeting of the room with id '%s' doesn't exist", roomId))));
  }

  @Override
  @Transactional
  public Meeting getsOrCreatesMeetingEntityByRoomId(UUID roomId, UserPrincipal currentUser) {
    Room room = roomService.getRoomEntityAndCheckUser(roomId, currentUser, false);
    return meetingRepository.getByRoomId(roomId.toString()).orElseGet(() -> {
      Meeting meeting = meetingRepository.insert(Meeting.create()
        .id(UUID.randomUUID().toString())
        .roomId(roomId.toString()));
      roomService.setMeetingIntoRoom(room, meeting);
      videoServerService.createMeeting(meeting.getId());
      eventDispatcher.sendToUserQueue(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        MeetingCreatedEvent.create(currentUser.getUUID(), currentUser.getSessionId())
          .meetingId(UUID.fromString(meeting.getId()))
          .roomId(roomId));
      return meeting;
    });
  }

  @Override
  public void deleteMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = meetingRepository.getById(meetingId.toString())
      .orElseThrow(() -> new NotFoundException(
        String.format("Meeting with id '%s' not found", meetingId)));

    deleteMeeting(meeting,
      roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false),
      currentUser.getUUID(), currentUser.getSessionId());
  }

  @Override
  public void deleteMeeting(Meeting meeting, Room room, UUID userId, @Nullable String sessionId) {
    meetingRepository.delete(meeting);
    videoServerService.deleteMeeting(meeting.getId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      MeetingDeletedEvent.create(userId, sessionId)
        .meetingId(UUID.fromString(meeting.getId())));
  }
}
