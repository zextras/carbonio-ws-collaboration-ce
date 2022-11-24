package com.zextras.carbonio.chats.meeting.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.event.MeetingCreatedEvent;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.model.MeetingDto;
import com.zextras.carbonio.chats.meeting.repository.MeetingRepository;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import io.ebean.annotation.Transactional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository  meetingRepository;
  private final RoomService        roomService;
  private final VideoServerService videoServerService;
  private final EventDispatcher    eventDispatcher;

  @Inject
  public MeetingServiceImpl(
    MeetingRepository meetingRepository, RoomService roomService,
    VideoServerService videoServerService,
    EventDispatcher eventDispatcher
  ) {
    this.meetingRepository = meetingRepository;
    this.roomService = roomService;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  public MeetingDto createMeetingByRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = roomService.getRoomEntityAndCheckUser(roomId, currentUser, false);
    if (meetingRepository.getMeetingByRoomId(roomId.toString()).isPresent()) {
      throw new ConflictException(String.format("Meeting for room '%s' exists", roomId));
    }
    Meeting meeting = meetingRepository.insert(Meeting.create()
      .id(UUID.randomUUID().toString())
      .roomId(roomId.toString()));
    videoServerService.createMeeting(meeting.getId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      MeetingCreatedEvent.create(currentUser.getUUID(), currentUser.getSessionId())
        .meetingId(UUID.fromString(meeting.getId()))
        .roomId(roomId));
    return MeetingDto.create()
      .id(UUID.fromString(meeting.getId()))
      .roomId(roomId)
      .createdAt(meeting.getCreatedAt());
  }
}
