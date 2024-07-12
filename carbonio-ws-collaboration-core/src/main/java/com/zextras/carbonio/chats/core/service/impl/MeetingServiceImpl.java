// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Recording;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingCreated;
import com.zextras.carbonio.chats.core.data.event.MeetingDeleted;
import com.zextras.carbonio.chats.core.data.event.MeetingRecordingStarted;
import com.zextras.carbonio.chats.core.data.event.MeetingRecordingStopped;
import com.zextras.carbonio.chats.core.data.event.MeetingStarted;
import com.zextras.carbonio.chats.core.data.event.MeetingStopped;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.RecordingRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.MeetingTypeDto;
import io.vavr.control.Option;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository meetingRepository;
  private final RecordingRepository recordingRepository;
  private final MeetingMapper meetingMapper;
  private final RoomService roomService;
  private final MembersService membersService;
  private final ParticipantService participantService;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;

  @Inject
  public MeetingServiceImpl(
      MeetingRepository meetingRepository,
      RecordingRepository recordingRepository,
      MeetingMapper meetingMapper,
      RoomService roomService,
      MembersService membersService,
      ParticipantService participantService,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      Clock clock) {
    this.meetingRepository = meetingRepository;
    this.recordingRepository = recordingRepository;
    this.meetingMapper = meetingMapper;
    this.roomService = roomService;
    this.membersService = membersService;
    this.participantService = participantService;
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
                  List<String> queuedReceivers =
                      Option.of(active)
                          .map(
                              s -> {
                                List<String> addedReceivers = Collections.emptyList();
                                if (!Objects.equals(s, meeting.getActive())) {
                                  meeting.active(s);
                                  if (Boolean.TRUE.equals(s)) {
                                    videoServerService.startMeeting(meeting.getId());
                                    meeting.startedAt(OffsetDateTime.now(clock));
                                  } else {
                                    meeting.getRecordings().stream()
                                        .filter(r -> RecordingStatus.STARTED.equals(r.getStatus()))
                                        .findFirst()
                                        .ifPresent(
                                            recording -> {
                                              videoServerService.updateRecording(
                                                  meeting.getId(), false);
                                              videoServerService.startRecordingPostProcessing(
                                                  meeting.getId(),
                                                  meeting.getName(),
                                                  null,
                                                  null,
                                                  recording.getToken());
                                              recordingRepository.update(
                                                  recording.status(RecordingStatus.STOPPED));
                                            });
                                    videoServerService.stopMeeting(meeting.getId());
                                    meeting.startedAt(null);
                                    addedReceivers =
                                        participantService.getQueue(meetingId).stream()
                                            .map(UUID::toString)
                                            .toList();
                                    participantService.clearQueue(UUID.fromString(meeting.getId()));
                                  }
                                }
                                return addedReceivers;
                              })
                          .get();
                  Meeting updatedMeeting = meetingRepository.update(meeting);
                  eventDispatcher.sendToUserExchange(
                      Stream.concat(
                              roomService
                                  .getRoomById(UUID.fromString(meeting.getRoomId()), user)
                                  .getMembers()
                                  .stream()
                                  .map(m -> m.getUserId().toString()),
                              queuedReceivers.stream())
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
    meeting.getRecordings().stream()
        .filter(r -> RecordingStatus.STARTED.equals(r.getStatus()))
        .findFirst()
        .ifPresent(
            recording -> {
              videoServerService.updateRecording(meeting.getId(), false);
              videoServerService.startRecordingPostProcessing(
                  meeting.getId(), meeting.getName(), null, null, recording.getToken());
            });
    videoServerService.stopMeeting(meeting.getId());
    meetingRepository.delete(meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingDeleted.create().meetingId(UUID.fromString(meeting.getId())));
  }

  @Override
  public void startMeetingRecording(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting =
        meetingRepository
            .getById(meetingId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));
    if (Boolean.FALSE.equals(meeting.getActive())) {
      throw new BadRequestException("Can't start recording on this meeting");
    }
    Subscription member = getSubscription(currentUser.getId(), meeting);
    if (Boolean.FALSE.equals(member.isOwner())) {
      throw new ForbiddenException(
          String.format(
              "User '%s' can't start recording on the meeting with id '%s'",
              currentUser.getId(), meetingId));
    }
    if (meeting.getRecordings().stream()
        .filter(r -> RecordingStatus.STARTED.equals(r.getStatus()))
        .findFirst()
        .isEmpty()) {
      videoServerService.updateRecording(meetingId.toString(), true);
      recordingRepository.insert(
          Recording.create()
              .id(UUID.randomUUID().toString())
              .meeting(meeting)
              .startedAt(OffsetDateTime.now(clock))
              .starterId(currentUser.getId())
              .status(RecordingStatus.STARTED)
              .token(currentUser.getAuthToken().orElse(null)));
      eventDispatcher.sendToUserExchange(
          meeting.getParticipants().stream().map(Participant::getUserId).toList(),
          MeetingRecordingStarted.create()
              .meetingId(UUID.fromString(meeting.getId()))
              .userId(currentUser.getUUID()));
    } else {
      ChatsLogger.debug("Recording is already started for the meeting " + meetingId);
    }
  }

  @Override
  public void stopMeetingRecording(
      UUID meetingId, String recordingName, String folderId, UserPrincipal currentUser) {
    Meeting meeting =
        meetingRepository
            .getById(meetingId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));
    if (Boolean.FALSE.equals(meeting.getActive())) {
      throw new BadRequestException("Can't stop recording on this meeting");
    }
    Subscription member = getSubscription(currentUser.getId(), meeting);
    if (Boolean.FALSE.equals(member.isOwner())) {
      throw new ForbiddenException(
          String.format(
              "User '%s' can't stop recording on the meeting with id '%s'",
              currentUser.getId(), meetingId));
    }
    meeting.getRecordings().stream()
        .filter(r -> RecordingStatus.STARTED.equals(r.getStatus()))
        .findFirst()
        .ifPresentOrElse(
            recording -> {
              videoServerService.updateRecording(meetingId.toString(), false);
              videoServerService.startRecordingPostProcessing(
                  meetingId.toString(),
                  meeting.getName(),
                  folderId,
                  recordingName,
                  currentUser.getAuthToken().orElse(null));
              recordingRepository.update(recording.status(RecordingStatus.STOPPED));
              eventDispatcher.sendToUserExchange(
                  meeting.getParticipants().stream().map(Participant::getUserId).toList(),
                  MeetingRecordingStopped.create()
                      .meetingId(UUID.fromString(meeting.getId()))
                      .userId(currentUser.getUUID()));
            },
            () -> ChatsLogger.debug("Recording is already stopped for the meeting " + meetingId));
  }

  private Subscription getSubscription(String userId, Meeting meeting) {
    if (meeting.getParticipants().stream().noneMatch(p -> userId.equals(p.getUserId()))) {
      throw new NotFoundException(
          String.format("User '%s' not found into meeting '%s'", userId, meeting.getId()));
    }
    return membersService
        .getSubscription(UUID.fromString(userId), UUID.fromString(meeting.getRoomId()))
        .orElseThrow(
            () -> new NotFoundException(String.format("User with id '%s' not found", userId)));
  }
}
