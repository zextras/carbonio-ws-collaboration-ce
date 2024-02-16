// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.*;
import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.WaitingParticipantRepository;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberToInsertDto;
import com.zextras.carbonio.meeting.model.*;
import io.ebean.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ParticipantServiceImpl implements ParticipantService {

  private final MeetingService meetingService;
  private final RoomService roomService;
  private final MembersService membersService;
  private final ParticipantRepository participantRepository;
  private final WaitingParticipantRepository waitingParticipantRepository;
  private final VideoServerService videoServerService;
  private final EventDispatcher eventDispatcher;

  @Inject
  public ParticipantServiceImpl(
      MeetingService meetingService,
      RoomService roomService,
      MembersService membersService,
      ParticipantRepository participantRepository,
      WaitingParticipantRepository waitingParticipantRepository,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher) {
    this.meetingService = meetingService;
    this.roomService = roomService;
    this.membersService = membersService;
    this.participantRepository = participantRepository;
    this.waitingParticipantRepository = waitingParticipantRepository;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  public JoinStatus insertMeetingParticipant(
      UUID meetingId, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting with id '%s' not found", meetingId)));

    return insertMeetingParticipant(meeting, joinSettingsDto, currentUser);
  }

  private JoinStatus insertMeetingParticipant(
      Meeting meeting, JoinSettingsDto joinSettingsDto, UserPrincipal currentUser) {
    Room room =
        roomService
            .getRoom(UUID.fromString(meeting.getRoomId()))
            .orElseThrow(
                () -> new NotFoundException(String.format("Room '%s'", meeting.getRoomId())));
    return meeting.getParticipants().stream()
        .filter(participant -> participant.getUserId().equals(currentUser.getId()))
        .findFirst()
        .map(
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
              return JoinStatus.ACCEPTED;
            })
        .orElseGet(
            () ->
                switch (meeting.getMeetingType()) {
                  case SCHEDULED:
                    yield room.getSubscriptions().stream()
                        .filter(s -> s.getUserId().equals(currentUser.getId()))
                        .map(
                            s -> {
                              if (s.isOwner()) {
                                participantRepository.insert(
                                    Participant.create(meeting, currentUser.getId())
                                        .queueId(currentUser.getQueueId().toString())
                                        .createdAt(OffsetDateTime.now()));
                                addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
                                return JoinStatus.ACCEPTED;
                              } else {
                                return waitingParticipantRepository
                                    .find(meeting.getId(), currentUser.getId(), null)
                                    .stream()
                                    .findFirst()
                                    .map(
                                        wp -> switch (wp.getStatus()) {
                                          case ACCEPTED:
                                            participantRepository.insert(
                                                Participant.create(meeting, currentUser.getId())
                                                    .queueId(currentUser.getQueueId().toString())
                                                    .createdAt(OffsetDateTime.now()));
                                            addMeetingParticipant(
                                                meeting, joinSettingsDto, currentUser, room);
                                            waitingParticipantRepository.remove(wp);
                                            yield JoinStatus.ACCEPTED;
                                          case WAITING:
                                            // This case should never happen
                                            // A user already inside the room should always have
                                            // been accepted or be an Owner, we'll treat it the same ways
                                            // as if the user was not inside the room and put it on queue on
                                            // this new tab/device
                                            if (!Objects.equals(
                                                wp.getQueueId(),
                                                currentUser.getQueueId().toString())) {
                                              eventDispatcher.sendToUserQueue(
                                                  currentUser.getId(),
                                                  wp.getQueueId(),
                                                  MeetingWaitingParticipantClashed.create()
                                                      .meetingId(
                                                          UUID.fromString(meeting.getId())));
                                              wp.queueId(currentUser.getQueueId().toString());
                                              waitingParticipantRepository.update(wp);
                                            }
                                            yield JoinStatus.WAITING;
                                        })
                                    .orElseGet(
                                        () -> {
                                          waitingParticipantRepository.insert(
                                              meeting.getId(),
                                              currentUser.getId(),
                                              currentUser.getQueueId().toString());
                                          eventDispatcher.sendToUserExchange(
                                              room.getSubscriptions().stream()
                                                  .filter(Subscription::isOwner)
                                                  .map(Subscription::getUserId)
                                                  .collect(Collectors.toList()),
                                              new MeetingWaitingParticipantJoined()
                                                  .meetingId(UUID.fromString(meeting.getId()))
                                                  .userId(UUID.fromString(currentUser.getId())));
                                          return JoinStatus.WAITING;
                                        });
                              }
                            })
                        .findFirst()
                        .orElseGet(
                            () ->
                                waitingParticipantRepository
                                    .find(meeting.getId(), currentUser.getId(), null)
                                    .stream()
                                    .findFirst()
                                    .map(
                                        wp -> {
                                          return switch (wp.getStatus()) {
                                            case ACCEPTED:
                                              // User is accepted but not inside the room, this should not happen
                                              // since when accepting the moderator also adds the user to the room
                                              // we'll put the user back on queue
                                              waitingParticipantRepository.update(
                                                  wp.status(JoinStatus.WAITING));
                                              eventDispatcher.sendToUserExchange(
                                                  room.getSubscriptions().stream()
                                                      .filter(Subscription::isOwner)
                                                      .map(Subscription::getUserId)
                                                      .collect(Collectors.toList()),
                                                  new MeetingWaitingParticipantJoined()
                                                      .meetingId(UUID.fromString(meeting.getId()))
                                                      .userId(
                                                          UUID.fromString(currentUser.getId())));
                                              yield JoinStatus.WAITING;
                                            case WAITING:
                                              // The user was already in queue, probably on another tab/device,
                                              // but not already accepted so we'll remove him from queue on the
                                              // previous tab/device and add it on queue on the new one
                                              if (!wp.getQueueId()
                                                  .equals(currentUser.getQueueId().toString())) {
                                                eventDispatcher.sendToUserQueue(
                                                    currentUser.getId(),
                                                    wp.getQueueId(),
                                                    MeetingWaitingParticipantClashed.create()
                                                        .meetingId(
                                                            UUID.fromString(meeting.getId())));
                                                wp.queueId(currentUser.getQueueId().toString());
                                                waitingParticipantRepository.update(wp);
                                              }
                                              yield JoinStatus.WAITING;
                                          };
                                        })
                                    .orElseGet(
                                        () -> {
                                          waitingParticipantRepository.insert(
                                              meeting.getId(),
                                              currentUser.getId(),
                                              currentUser.getQueueId().toString());
                                          eventDispatcher.sendToUserExchange(
                                              room.getSubscriptions().stream()
                                                  .filter(Subscription::isOwner)
                                                  .map(Subscription::getUserId)
                                                  .collect(Collectors.toList()),
                                              new MeetingWaitingParticipantJoined()
                                                  .meetingId(UUID.fromString(meeting.getId()))
                                                  .userId(UUID.fromString(currentUser.getId())));
                                          return JoinStatus.WAITING;
                                        }));
                  case PERMANENT:
                    room.getSubscriptions().stream()
                        .filter(s -> s.getUserId().equals(currentUser.getId()))
                        .findFirst()
                        .orElseThrow(ForbiddenException::new);
                    participantRepository.insert(
                        Participant.create(meeting, currentUser.getId())
                            .queueId(currentUser.getQueueId().toString())
                            .createdAt(OffsetDateTime.now()));
                    addMeetingParticipant(meeting, joinSettingsDto, currentUser, room);
                    yield JoinStatus.ACCEPTED;
                });
  }

  @Override
  public List<UUID> getQueue(UUID meetingId) {
    return waitingParticipantRepository
        .find(meetingId.toString(), null, JoinStatus.WAITING)
        .stream()
        .map(wp -> UUID.fromString(wp.getUserId()))
        .collect(Collectors.toList());
  }

  private boolean isOwner(Room room, String userId) {
    return room.getSubscriptions().stream()
        .filter(u -> u.getUserId().equals(userId))
        .findFirst()
        .map(Subscription::isOwner)
        .orElse(false);
  }

  @Override
  public void updateQueue(
      UUID meetingId, UUID userId, QueueUpdateStatusDto status, UserPrincipal currentUser) {
    Meeting meeting =
        meetingService
            .getMeetingEntity(meetingId)
            .orElseThrow(() -> new NotFoundException("Meeting not found"));
    Room room =
        roomService
            .getRoom(UUID.fromString(meeting.getRoomId()))
            .orElseThrow(
                () -> {
                  ChatsLogger.error(
                      "Not found room %s associated with meeting %s"
                          .formatted(meeting.getRoomId(), meetingId));
                  return new RuntimeException();
                });
    boolean moderator = isOwner(room, currentUser.getId());
    if (status.equals(QueueUpdateStatusDto.ACCEPTED) && !moderator
        || status.equals(QueueUpdateStatusDto.REJECTED)
            && (!moderator && !Objects.equals(userId.toString(), currentUser.getId()))) {
      throw new ForbiddenException();
    }
    waitingParticipantRepository.find(meetingId.toString(), userId.toString(), null).stream()
        .findFirst()
        .ifPresentOrElse(
            wp -> {
              DomainEvent event =
                  switch (status) {
                    case ACCEPTED:
                      wp.status(JoinStatus.ACCEPTED);
                      waitingParticipantRepository.update(wp);
                      try {
                        this.membersService.insertRoomMember(
                            UUID.fromString(meeting.getRoomId()),
                            new MemberToInsertDto()
                                .userId(userId)
                                .owner(false)
                                .historyCleared(false),
                            currentUser);
                      } catch (BadRequestException ignored) {
                      }
                      yield MeetingWaitingParticipantAccepted.create()
                          .meetingId(meetingId)
                          .userId(UUID.fromString(wp.getUserId()));
                    case REJECTED:
                      waitingParticipantRepository.remove(wp);
                      yield MeetingWaitingParticipantRejected.create()
                          .meetingId(meetingId)
                          .userId(UUID.fromString(wp.getUserId()));
                  };
              eventDispatcher.sendToUserExchange(
                  room.getSubscriptions().stream()
                      .filter(Subscription::isOwner)
                      .map(Subscription::getUserId)
                      .collect(Collectors.toList()),
                  event);
              eventDispatcher.sendToUserQueue(wp.getUserId(), wp.getQueueId(), event);
            },
            () -> {
              throw new NotFoundException("User not in queue");
            });
  }

  public void clearQueue(UUID meetingId) {
    waitingParticipantRepository.clear(meetingId.toString());
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
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        MeetingParticipantJoined.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID()));
  }

  private void destroyMeetingParticipant(Meeting meeting, UserPrincipal currentUser, Room room) {
    videoServerService.destroyMeetingParticipant(currentUser.getId(), meeting.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
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
        .collect(Collectors.toList())
        .forEach(participant -> removeMeetingParticipant(participant, meeting, room));
  }

  @Override
  @Transactional
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
        if (mediaStreamEnabled != participant.hasVideoStreamOn()) {
          participantRepository.update(participant.videoStreamOn(mediaStreamEnabled));
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
          if (!mediaStreamEnabled) {
            eventDispatcher.sendToUserExchange(
                meeting.getParticipants().stream()
                    .map(Participant::getUserId)
                    .distinct()
                    .collect(Collectors.toList()),
                MeetingMediaStreamChanged.create()
                    .meetingId(meetingId)
                    .userId(UUID.fromString(currentUser.getId()))
                    .mediaType(MediaType.VIDEO)
                    .active(mediaStreamEnabled));
          }
        }
        break;
      case SCREEN:
        if (mediaStreamEnabled != participant.hasScreenStreamOn()) {
          participantRepository.update(participant.screenStreamOn(mediaStreamEnabled));
          videoServerService.updateMediaStream(
              currentUser.getId(), meetingId.toString(), mediaStreamSettingsDto);
          if (!mediaStreamEnabled) {
            eventDispatcher.sendToUserExchange(
                meeting.getParticipants().stream()
                    .map(Participant::getUserId)
                    .distinct()
                    .collect(Collectors.toList()),
                MeetingMediaStreamChanged.create()
                    .meetingId(meetingId)
                    .userId(UUID.fromString(currentUser.getId()))
                    .mediaType(MediaType.SCREEN)
                    .active(mediaStreamEnabled));
          }
        }
        break;
      default:
        break;
    }
  }

  @Override
  @Transactional
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
    if (enabled != Boolean.TRUE.equals(participant.hasAudioStreamOn())) {
      participantRepository.update(participant.audioStreamOn(enabled));
      videoServerService.updateAudioStream(userId, meetingId.toString(), enabled);
      Optional.ofNullable(audioStreamSettingsDto.getUserToModerate())
          .ifPresentOrElse(
              targetUserId ->
                  eventDispatcher.sendToUserExchange(
                      meeting.getParticipants().stream()
                          .map(Participant::getUserId)
                          .distinct()
                          .collect(Collectors.toList()),
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
                          .collect(Collectors.toList()),
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
