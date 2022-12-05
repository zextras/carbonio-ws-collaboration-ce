package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantJoinedEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantLeftEvent;
import com.zextras.carbonio.chats.meeting.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
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
      MeetingParticipantJoinedEvent.create(currentUser.getUUID(), currentUser.getSessionId()).meetingId(meetingId));
  }

  @Override
  @Transactional
  public void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = meetingService.getMeetingEntity(meetingId);
    Participant participant = meeting.getParticipants().stream()
      .filter(p -> currentUser.getId().equals(p.getUserId()) && currentUser.getSessionId().equals(p.getSessionId()))
      .findAny().orElseThrow(() -> new NotFoundException("Session not found"));
    Room room = roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    participantRepository.removeParticipant(participant);
    videoServerService.leaveSession(currentUser.getSessionId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      MeetingParticipantLeftEvent.create(currentUser.getUUID(), currentUser.getSessionId()).meetingId(meetingId));
    if (meeting.getParticipants().size() == 1) {
      meetingService.deleteMeetingById(meetingId, currentUser);
    }
  }
}
