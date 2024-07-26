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
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
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
import io.ebean.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

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
                destroyMeetingParticipant(meeting, currentUser, room);
                eventDispatcher.sendToUserQueue(
                    currentUser.getId(),
                    participant.getQueueId(),
                    MeetingParticipantClashed.create().meetingId(UUID.fromString(meeting.getId())));
                participantRepository.update(
                    participant
                        .audioStreamOn(false)
                        .videoStreamOn(false)
                        .screenStreamOn(false)
                        .queueId(currentUser.getQueueId().toString()));
                addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
              }
            },
            () -> {
              participantRepository.insert(
                  Participant.create(meeting, currentUser.getId())
                      .queueId(currentUser.getQueueId().toString())
                      .createdAt(OffsetDateTime.now(clock)));
              addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
            });
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

  private void destroyMeetingParticipant(Meeting meeting, UserPrincipal currentUser, Room room) {
    videoServerService.destroyMeetingParticipant(currentUser.getId(), meeting.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingParticipantLeft.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID()));
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
  @Transactional
  public void removeMeetingParticipant(Meeting meeting, Room room, UUID userId) {
    meeting.getParticipants().stream()
        .filter(p -> userId.toString().equals(p.getUserId()))
        .toList()
        .forEach(participant -> removeMeetingParticipant(participant, meeting, room));
  }

  @Override
  @Transactional
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
    participantRepository.remove(participant);
    videoServerService.destroyMeetingParticipant(participant.getUserId(), meeting.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingParticipantLeft.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(UUID.fromString(participant.getUserId())));
    if (participantRepository.getByMeetingId(meeting.getId()).isEmpty()) {
      meetingService.updateMeeting(
          UserPrincipal.create(UUID.fromString(participant.getUserId())),
          UUID.fromString(meeting.getId()),
          false);
    }
  }

  @Override
  public void updateMediaStream(
      UUID meetingId, MediaStreamSettingsDto mediaStreamSettingsDto, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
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
        if (Boolean.TRUE.equals(participant.hasVideoStreamOn()) != mediaStreamEnabled) {
          participantRepository.update(participant.videoStreamOn(mediaStreamEnabled));
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
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
        if (Boolean.TRUE.equals(participant.hasScreenStreamOn()) != mediaStreamEnabled) {
          participantRepository.update(participant.screenStreamOn(mediaStreamEnabled));
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
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
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
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
      roomService.getRoomEntityAndCheckUser(
          UUID.fromString(meeting.getRoomId()), currentUser, true);
    }
    if (Boolean.TRUE.equals(participant.hasAudioStreamOn()) != enabled) {
      participantRepository.update(participant.audioStreamOn(enabled));
      videoServerService.updateAudioStream(userId, meetingId.toString(), enabled);
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
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.answerRtcMediaStream(currentUser.getId(), meetingId.toString(), sdp);
  }

  @Override
  public void updateSubscriptionsMediaStream(
      UUID meetingId, SubscriptionUpdatesDto subscriptionUpdatesDto, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () -> new NotFoundException(String.format("Meeting '%s' not found", meetingId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(meeting.getRoomId()), currentUser, false);
    videoServerService.updateSubscriptionsMediaStream(
        currentUser.getId(), meetingId.toString(), subscriptionUpdatesDto);
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
