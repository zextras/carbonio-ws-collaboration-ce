package com.zextras.carbonio.chats.meeting.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.meeting.data.entity.Meeting;
import com.zextras.carbonio.chats.meeting.data.entity.Participant;
import com.zextras.carbonio.chats.meeting.data.event.ParticipantJoinedEvent;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.chats.meeting.repository.ParticipantRepository;
import com.zextras.carbonio.chats.meeting.service.MeetingService;
import com.zextras.carbonio.chats.meeting.service.ParticipantService;
import io.ebean.annotation.Transactional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipantServiceImpl implements ParticipantService {

  private final MeetingService        meetingService;
  private final RoomService           roomService;
  private final ParticipantRepository participantRepository;
  private final VideoServerService    videoServerService;
  private final EventDispatcher       eventDispatcher;

  @Inject
  public ParticipantServiceImpl(
    MeetingService meetingService,
    RoomService roomService, ParticipantRepository participantRepository,
    VideoServerService videoServerService,
    EventDispatcher eventDispatcher
  ) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.participantRepository = participantRepository;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }


  @Override
  @Transactional
  public void insertMeetingParticipant(UUID meetingId, JoinSettingsDto joinSettings, UserPrincipal currentUser) {
    Meeting meeting = meetingService.getMeetingEntity(meetingId);
    if (meeting.getParticipants().stream().anyMatch(participant ->
      participant.getUserId().equals(currentUser.getId()) && participant.getSessionId()
        .equals(currentUser.getSessionId()))) {
      throw new ConflictException("Session is already inserted into the meeting");
    }
    Room room = roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    participantRepository.insertParticipant(
      Participant.create(currentUser.getId(), meeting, currentUser.getSessionId())
        .microphoneOn(joinSettings.isMicrophoneOn())
        .cameraOn(joinSettings.isCameraOn()));
    videoServerService.joinSession(currentUser.getSessionId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      ParticipantJoinedEvent.create(currentUser.getUUID(), currentUser.getSessionId()).meetingId(meetingId));
  }
}
