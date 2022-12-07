package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantJoinedEvent;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantLeftEvent;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
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
public class ParticipantServiceImpl implements ParticipantService {

  private final MeetingService        meetingService;
  private final RoomService           roomService;
  private final ParticipantRepository participantRepository;
  private final MeetingMapper         meetingMapper;
  private final VideoServerService    videoServerService;
  private final EventDispatcher       eventDispatcher;

  @Inject
  public ParticipantServiceImpl(
    MeetingService meetingService,
    RoomService roomService, ParticipantRepository participantRepository,
    MeetingMapper meetingMapper, VideoServerService videoServerService,
    EventDispatcher eventDispatcher
  ) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.participantRepository = participantRepository;
    this.meetingMapper = meetingMapper;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  public Optional<MeetingDto> insertMeetingParticipantByRoomId(
    UUID roomId, JoinSettingsDto joinSettings, UserPrincipal currentUser
  ) {
    Meeting meeting = meetingService.getsOrCreatesMeetingEntityByRoomId(roomId, currentUser);
    insertMeetingParticipant(meeting, joinSettings, currentUser);

    return Optional.ofNullable(meeting.getParticipants().size() > 1 ? null : meetingMapper.ent2dto(meeting));
  }

  @Override
  @Transactional
  public void insertMeetingParticipant(UUID meetingId, JoinSettingsDto joinSettings, UserPrincipal currentUser) {
    Meeting meeting = meetingService.getMeetingEntity(meetingId)
      .orElseThrow(() -> new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
    if (meeting.getParticipants().stream().anyMatch(participant ->
      participant.getUserId().equals(currentUser.getId()) && participant.getSessionId()
        .equals(currentUser.getSessionId()))) {
      throw new ConflictException("Session is already inserted into the meeting");
    }
    insertMeetingParticipant(meeting, joinSettings, currentUser);
  }

  private void insertMeetingParticipant(Meeting meeting, JoinSettingsDto joinSettings, UserPrincipal currentUser) {
    Room room = roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    Participant participant = participantRepository.insert(
      Participant.create(currentUser.getId(), meeting, currentUser.getSessionId())
        .microphoneOn(joinSettings.isMicrophoneOn())
        .cameraOn(joinSettings.isCameraOn()));
    videoServerService.joinSession(currentUser.getSessionId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      MeetingParticipantJoinedEvent.create(currentUser.getUUID(), currentUser.getSessionId())
        .meetingId(UUID.fromString(meeting.getId())));
    meeting.getParticipants().add(participant);
  }

  @Override
  @Transactional
  public void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = meetingService.getMeetingEntity(meetingId)
      .orElseThrow(() -> new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
    Room room = roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    removeMeetingParticipant(meeting, room, currentUser.getUUID(), currentUser.getSessionId());
  }

  @Override
  public void removeMeetingParticipant(Meeting meeting, Room room, UUID userId, @Nullable String sessionId) {
    List<Participant> participants = meeting.getParticipants().stream()
      .filter(p -> userId.toString().equals(p.getUserId()) && (sessionId == null || sessionId.equals(p.getSessionId())))
      .collect(Collectors.toList());
    if (sessionId != null && participants.isEmpty()) {
      throw new NotFoundException("Session not found");
    }
    participants.forEach(participant -> {
      participantRepository.remove(participant);
      videoServerService.leaveSession(participant.getSessionId());
      eventDispatcher.sendToUserQueue(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        MeetingParticipantLeftEvent.create(userId, participant.getSessionId())
          .meetingId(UUID.fromString(meeting.getId())));
      meeting.getParticipants().remove(participant);
      if (meeting.getParticipants().isEmpty()) {
        meetingService.deleteMeeting(meeting, room, userId, participant.getSessionId());
      }
    });
  }

}
