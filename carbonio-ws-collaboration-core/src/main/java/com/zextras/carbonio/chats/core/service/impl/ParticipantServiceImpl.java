// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.*;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingMediaStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantClashed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantJoined;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantLeft;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.AudioStreamSettingsDto;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ParticipantServiceImpl implements ParticipantService {

  private final MeetingService meetingService;
  private final RoomService roomService;
  private final ParticipantRepository participantRepository;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;
  private final Clock clock;

  @Inject
  public ParticipantServiceImpl(
      MeetingService meetingService,
      RoomService roomService,
      ParticipantRepository participantRepository,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      Clock clock) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.participantRepository = participantRepository;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
    this.clock = clock;
  }

  @Override
  public JoinStatus insertMeetingParticipant(
      UUID meetingId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {

    Meeting meeting = validateMeeting(meetingId);
    Room room = validateMeetingRoom(meeting);

    return meeting.getParticipants().stream()
        .filter(participant -> participant.getUserId().equals(currentUser.getId()))
        .findFirst()
        .map(
            participant ->
                handleExistingParticipant(meeting, currentUser, room, participant, joinSettingsDto))
        .orElseGet(() -> handleNewParticipant(meeting, joinSettingsDto, currentUser, room));
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

  // Handles logic when the user is already a participant
  private JoinStatus handleExistingParticipant(
      Meeting meeting,
      UserPrincipal currentUser,
      Room room,
      Participant participant,
      JoinSettingsDto joinSettingsDto) {
    if (participant.getQueueId().equals(currentUser.getQueueId().toString())) {
      throw new ConflictException("User is already inserted into the meeting");
    } else {
      destroyMeetingParticipantClashed(meeting, participant, currentUser, room);
      addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
      participantRepository.update(
          participant
              .audioStreamOn(false)
              .videoStreamOn(false)
              .screenStreamOn(false)
              .queueId(currentUser.getQueueId().toString()));
    }
    return JoinStatus.ACCEPTED;
  }

  // Handles logic for new participants based on meeting type
  private JoinStatus handleNewParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser, Room room) {

    return switch (meeting.getMeetingType()) {
      case SCHEDULED ->
          handleScheduledMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
      case PERMANENT ->
          handlePermanentMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
    };
  }

  // Handles joining for a permanent meeting
  private JoinStatus handleScheduledMeetingParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser, Room room) {

    boolean isRoomOwner =
        room.getSubscriptions().stream()
            .anyMatch(s -> s.getUserId().equals(currentUser.getId()) && s.isOwner());
    if (!isRoomOwner) {
      throw new ForbiddenException("User cannot join the meeting");
    }

    return joinMeetingParticipant(meeting, currentUser, room, joinSettingsDto);
  }

  @NotNull
  private JoinStatus joinMeetingParticipant(
      Meeting meeting, UserPrincipal currentUser, Room room, JoinSettingsDto joinSettingsDto) {
    if (participantRepository.getById(meeting.getId(), currentUser.getId()).isEmpty()) {
      addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
      Participant participant =
          Participant.create(meeting, currentUser.getId())
              .queueId(currentUser.getQueueId().toString())
              .createdAt(OffsetDateTime.now(clock));
      participantRepository.insert(participant);
    }
    return JoinStatus.ACCEPTED;
  }

  // Handles joining for a permanent meeting
  private JoinStatus handlePermanentMeetingParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser, Room room) {

    boolean isRoomMember =
        room.getSubscriptions().stream().anyMatch(s -> s.getUserId().equals(currentUser.getId()));
    if (!isRoomMember) {
      throw new ForbiddenException("User cannot join the meeting");
    }

    return joinMeetingParticipant(meeting, currentUser, room, joinSettingsDto);
  }

  private void addMeetingParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser, Room room) {
    videoServerService.addMeetingParticipant(
        currentUser.getId(),
        currentUser.getQueueId().toString(),
        meeting.getId(),
        joinSettingsDto.isVideoStreamEnabled(),
        joinSettingsDto.isAudioStreamEnabled());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingParticipantJoined.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID()));
  }

  private void destroyMeetingParticipantClashed(
      Meeting meeting, Participant participant, UserPrincipal currentUser, Room room) {
    videoServerService.destroyMeetingParticipant(currentUser.getId(), meeting.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingParticipantLeft.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID()));
    eventDispatcher.sendToUserQueue(
        currentUser.getId(),
        participant.getQueueId(),
        MeetingParticipantClashed.create().meetingId(UUID.fromString(meeting.getId())));
  }

  @Override
  public void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room =
        roomService.getRoomAndValidateUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    removeMeetingParticipant(meeting, room, currentUser.getUUID());
  }

  @Override
  public void removeMeetingParticipant(Meeting meeting, Room room, UUID userId) {
    meeting.getParticipants().stream()
        .filter(p -> userId.toString().equals(p.getUserId()))
        .toList()
        .forEach(participant -> removeMeetingParticipant(participant, meeting, room));
  }

  @Override
  public void removeMeetingParticipant(UUID queueId) {
    participantRepository
        .getByQueueId(queueId.toString())
        .ifPresent(
            participant ->
                roomService
                    .getRoom(UUID.fromString(participant.getMeeting().getRoomId()))
                    .ifPresent(
                        room ->
                            removeMeetingParticipant(
                                participant.getMeeting(),
                                room,
                                UUID.fromString(participant.getUserId()))));
  }

  private void removeMeetingParticipant(Participant participant, Meeting meeting, Room room) {
    videoServerService.destroyMeetingParticipant(participant.getUserId(), meeting.getId());
    participantRepository.remove(participant);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingParticipantLeft.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(UUID.fromString(participant.getUserId())));
    if (participantRepository.getByMeetingId(meeting.getId()).isEmpty()) {
      meetingService.stopMeeting(
          UserPrincipal.create(UUID.fromString(participant.getUserId())),
          UUID.fromString(meeting.getId()));
    }
  }

  @Override
  public void updateMediaStream(
      UUID meetingId, MediaStreamSettingsDto mediaStreamSettingsDto, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Participant participant =
        meeting.getParticipants().stream()
            .filter(p -> currentUser.getId().equals(p.getUserId()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format(
                            "User '%s' not found into meeting '%s'",
                            currentUser.getId(), meetingId)));
    boolean mediaStreamEnabled = mediaStreamSettingsDto.isEnabled();
    switch (mediaStreamSettingsDto.getType()) {
      case VIDEO:
        if (participant.hasVideoStreamOn() != mediaStreamEnabled) {
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
          participantRepository.update(participant.videoStreamOn(mediaStreamEnabled));
          if (!mediaStreamEnabled) {
            eventDispatcher.sendToUserExchange(
                meeting.getParticipants().stream().map(Participant::getUserId).distinct().toList(),
                MeetingMediaStreamChanged.create()
                    .meetingId(meetingId)
                    .userId(UUID.fromString(currentUser.getId()))
                    .mediaType(MediaType.VIDEO)
                    .active(false));
          }
        }
        break;
      case SCREEN:
        if (participant.hasScreenStreamOn() != mediaStreamEnabled) {
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
          participantRepository.update(participant.screenStreamOn(mediaStreamEnabled));
          if (!mediaStreamEnabled) {
            eventDispatcher.sendToUserExchange(
                meeting.getParticipants().stream().map(Participant::getUserId).distinct().toList(),
                MeetingMediaStreamChanged.create()
                    .meetingId(meetingId)
                    .userId(UUID.fromString(currentUser.getId()))
                    .mediaType(MediaType.SCREEN)
                    .active(false));
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  public void updateAudioStream(
      UUID meetingId, AudioStreamSettingsDto audioStreamSettingsDto, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    String userId =
        audioStreamSettingsDto.getUserToModerate() == null
            ? currentUser.getId()
            : audioStreamSettingsDto.getUserToModerate();
    Participant participant =
        meeting.getParticipants().stream()
            .filter(p -> userId.equals(p.getUserId()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("User '%s' not found into meeting '%s'", userId, meetingId)));
    boolean enabled = audioStreamSettingsDto.isEnabled();
    if (!userId.equals(currentUser.getId())) {
      if (enabled) {
        throw new BadRequestException(
            String.format(
                "User '%s' cannot enable the audio stream of the user '%s'",
                currentUser.getId(), userId));
      }
      roomService.getRoomAndValidateUser(UUID.fromString(meeting.getRoomId()), currentUser, true);
    }
    if (participant.hasAudioStreamOn() != enabled) {
      videoServerService.updateAudioStream(userId, meetingId.toString(), enabled);
      participantRepository.update(participant.audioStreamOn(enabled));
      Optional.ofNullable(audioStreamSettingsDto.getUserToModerate())
          .ifPresentOrElse(
              targetUserId ->
                  eventDispatcher.sendToUserExchange(
                      meeting.getParticipants().stream()
                          .map(Participant::getUserId)
                          .distinct()
                          .toList(),
                      MeetingAudioStreamChanged.create()
                          .meetingId(meetingId)
                          .userId(UUID.fromString(targetUserId))
                          .moderatorId(currentUser.getUUID())
                          .active(enabled)),
              () ->
                  eventDispatcher.sendToUserExchange(
                      meeting.getParticipants().stream()
                          .map(Participant::getUserId)
                          .distinct()
                          .toList(),
                      MeetingAudioStreamChanged.create()
                          .meetingId(meetingId)
                          .userId(currentUser.getUUID())
                          .active(enabled)));
    }
  }

  @Override
  public void answerRtcMediaStream(UUID meetingId, String sdp, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    roomService.getRoomAndValidateUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.answerRtcMediaStream(currentUser.getId(), meetingId.toString(), sdp);
  }

  @Override
  public void updateSubscriptionsMediaStream(
      UUID meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    roomService.getRoomAndValidateUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.updateSubscriptionsMediaStream(
        currentUser.getId(), meetingId.toString(), subscriptionUpdatesDto);
  }

  @Override
  public void offerRtcAudioStream(UUID meetingId, String sdp, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    roomService.getRoomAndValidateUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.offerRtcAudioStream(currentUser.getId(), meetingId.toString(), sdp);
  }
}
