// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.MeetingAudioStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingMediaStreamChanged;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantClashed;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantJoined;
import com.zextras.carbonio.chats.core.data.event.MeetingParticipantLeft;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.meeting.model.JoinSettingsDto;
import com.zextras.carbonio.meeting.model.MediaStreamSettingsDto;
import com.zextras.carbonio.meeting.model.MeetingDto;
import com.zextras.carbonio.meeting.model.SubscriptionUpdatesDto;
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipantServiceImpl implements ParticipantService {

  private final MeetingService meetingService;
  private final RoomService roomService;
  private final ParticipantRepository participantRepository;
  private final MeetingMapper meetingMapper;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;

  @Inject
  public ParticipantServiceImpl(
      MeetingService meetingService,
      RoomService roomService,
      ParticipantRepository participantRepository,
      MeetingMapper meetingMapper,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.participantRepository = participantRepository;
    this.meetingMapper = meetingMapper;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  @Deprecated(forRemoval = true)
  public Optional<MeetingDto> insertMeetingParticipantByRoomId(
      UUID roomId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {
    Meeting meeting = meetingService.getsOrCreatesMeetingEntityByRoomId(roomId, currentUser);
    insertMeetingParticipant(meeting, joinSettingsDto, currentUser);

    return Optional.ofNullable(
        meeting.getParticipants().size() > 1 ? null : meetingMapper.ent2dto(meeting));
  }

  @Override
  @Transactional
  public void insertMeetingParticipant(
      UUID meetingId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));

    insertMeetingParticipant(meeting, joinSettingsDto, currentUser);
  }

  private void insertMeetingParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {
    Room room =
        roomService.getRoomEntityAndCheckUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    meeting.getParticipants().stream()
        .filter(participant -> participant.getUserId().equals(currentUser.getId()))
        .findFirst()
        .ifPresentOrElse(
            participant -> {
              if (participant.getQueueId().equals(currentUser.getQueueId().toString())) {
                throw new ConflictException("User is already inserted into the meeting");
              } else {
                String participantUserId = participant.getUserId();
                videoServerService.destroyMeetingParticipant(participantUserId, meeting.getId());
                eventDispatcher.sendToUserExchange(
                    room.getSubscriptions().stream()
                        .map(Subscription::getUserId)
                        .collect(Collectors.toList()),
                    MeetingParticipantLeft.create()
                        .meetingId(UUID.fromString(meeting.getId()))
                        .userId(UUID.fromString(participantUserId)));
                eventDispatcher.sendToUserQueue(
                    participantUserId,
                    participant.getQueueId(),
                    MeetingParticipantClashed.create().meetingId(UUID.fromString(meeting.getId())));
                participantRepository.update(
                    participant.queueId(currentUser.getQueueId().toString()));
                videoServerService.addMeetingParticipant(
                    participantUserId,
                    currentUser.getQueueId().toString(),
                    meeting.getId(),
                    joinSettingsDto.isVideoStreamEnabled(),
                    joinSettingsDto.isAudioStreamEnabled());
                eventDispatcher.sendToUserExchange(
                    room.getSubscriptions().stream()
                        .map(Subscription::getUserId)
                        .collect(Collectors.toList()),
                    MeetingParticipantJoined.create()
                        .meetingId(UUID.fromString(meeting.getId()))
                        .userId(UUID.fromString(participantUserId)));
              }
            },
            () -> {
              participantRepository.insert(
                  Participant.create(meeting, currentUser.getId())
                      .queueId(currentUser.getQueueId().toString())
                      .createdAt(OffsetDateTime.now()));
              videoServerService.addMeetingParticipant(
                  currentUser.getId(),
                  currentUser.getQueueId().toString(),
                  meeting.getId(),
                  joinSettingsDto.isVideoStreamEnabled(),
                  joinSettingsDto.isAudioStreamEnabled());
              eventDispatcher.sendToUserExchange(
                  room.getSubscriptions().stream()
                      .map(Subscription::getUserId)
                      .collect(Collectors.toList()),
                  MeetingParticipantJoined.create()
                      .meetingId(UUID.fromString(meeting.getId()))
                      .userId(currentUser.getUUID()));
            });
  }

  @Override
  @Transactional
  public void removeMeetingParticipant(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));
    Room room =
        roomService.getRoomEntityAndCheckUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    removeMeetingParticipant(meeting, room, currentUser.getUUID());
  }

  @Override
  public void removeMeetingParticipant(Meeting meeting, Room room, UUID userId) {
    meeting.getParticipants().stream()
        .filter(p -> userId.toString().equals(p.getUserId()))
        .collect(Collectors.toList())
        .forEach(participant -> removeMeetingParticipant(participant, meeting, room));
  }

  @Override
  public void removeMeetingParticipant(Meeting meeting, Room room, UUID userId, UUID queueId) {
    meeting.getParticipants().stream()
        .filter(
            p ->
                userId.toString().equals(p.getUserId())
                    && queueId.toString().equals(p.getQueueId()))
        .findFirst()
        .ifPresent(participant -> removeMeetingParticipant(participant, meeting, room));
  }

  private void removeMeetingParticipant(Participant participant, Meeting meeting, Room room) {
    participantRepository.remove(participant);
    videoServerService.destroyMeetingParticipant(participant.getUserId(), meeting.getId());
    meeting.getParticipants().remove(participant);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        MeetingParticipantLeft.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(UUID.fromString(participant.getUserId())));
    if (meeting.getParticipants().isEmpty()) {
      meetingService.updateMeeting(
          UserPrincipal.create(UUID.fromString(participant.getUserId())),
          UUID.fromString(meeting.getId()),
          false);
    }
  }

  @Override
  @Transactional
  public void updateMediaStream(
      UUID meetingId, MediaStreamSettingsDto mediaStreamSettingsDto, UserPrincipal currentUser) {
    String userId = currentUser.getId();
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    Participant participant =
        meeting.getParticipants().stream()
            .filter(p -> userId.equals(p.getUserId()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("User '%s' not found into meeting '%s'", userId, meetingId)));
    boolean isVideoStream =
        MediaType.VIDEO.toString().equalsIgnoreCase(mediaStreamSettingsDto.getType().toString());
    boolean mediaStreamEnabled =
        isVideoStream ? participant.hasVideoStreamOn() : participant.hasScreenStreamOn();
    if (mediaStreamSettingsDto.isEnabled() != mediaStreamEnabled) {
      Participant participantToUpdate =
          isVideoStream
              ? participant.videoStreamOn(mediaStreamSettingsDto.isEnabled())
              : participant.screenStreamOn(mediaStreamSettingsDto.isEnabled());
      participantRepository.update(participantToUpdate);
      videoServerService.updateMediaStream(userId, meetingId.toString(), mediaStreamSettingsDto);
      if (!mediaStreamSettingsDto.isEnabled()) {
        eventDispatcher.sendToUserExchange(
            meeting.getParticipants().stream()
                .map(Participant::getUserId)
                .distinct()
                .collect(Collectors.toList()),
            MeetingMediaStreamChanged.create()
                .meetingId(meetingId)
                .userId(UUID.fromString(currentUser.getId()))
                .mediaType(
                    MediaType.valueOf(mediaStreamSettingsDto.getType().toString().toUpperCase()))
                .active(mediaStreamSettingsDto.isEnabled()));
      }
    }
  }

  @Override
  @Transactional
  public void updateAudioStream(
      UUID meetingId, String userId, boolean enabled, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    Participant participant =
        meeting.getParticipants().stream()
            .filter(p -> userId.equals(p.getUserId()))
            .findAny()
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("User '%s' not found into meeting '%s'", userId, meetingId)));
    if (!userId.equals(currentUser.getId())) {
      if (enabled) {
        throw new BadRequestException(
            String.format(
                "User '%s' cannot enable the audio stream of the user '%s'",
                currentUser.getId(), userId));
      }
      roomService.getRoomEntityAndCheckUser(
          UUID.fromString(meeting.getRoomId()), currentUser, true);
    }
    if (enabled != Boolean.TRUE.equals(participant.hasAudioStreamOn())) {
      participantRepository.update(participant.audioStreamOn(enabled));
      videoServerService.updateAudioStream(userId, meetingId.toString(), enabled);
      eventDispatcher.sendToUserExchange(
          meeting.getParticipants().stream()
              .map(Participant::getUserId)
              .distinct()
              .collect(Collectors.toList()),
          MeetingAudioStreamChanged.create()
              .userId(currentUser.getUUID())
              .meetingId(meetingId)
              .active(enabled));
    }
  }

  @Override
  public void answerRtcMediaStream(UUID meetingId, String sdp, UserPrincipal currentUser) {
    String userId = currentUser.getId();
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.answerRtcMediaStream(userId, meetingId.toString(), sdp);
  }

  @Override
  public void updateSubscriptionsMediaStream(
      UUID meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto, UserPrincipal currentUser) {
    String userId = currentUser.getId();
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.updateSubscriptionsMediaStream(
        userId, meetingId.toString(), subscriptionUpdatesDto);
  }

  @Override
  public void offerRtcAudioStream(UUID meetingId, String sdp, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.offerRtcAudioStream(currentUser.getId(), meetingId.toString(), sdp);
  }
}
