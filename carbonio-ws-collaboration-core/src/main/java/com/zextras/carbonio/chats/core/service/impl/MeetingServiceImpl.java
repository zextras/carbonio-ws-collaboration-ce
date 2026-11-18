// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingCreated;
import com.zextras.carbonio.async.model.MeetingDeclined;
import com.zextras.carbonio.async.model.MeetingDeleted;
import com.zextras.carbonio.async.model.MeetingStarted;
import com.zextras.carbonio.async.model.MeetingStopped;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.type.MeetingType;
import com.zextras.carbonio.chats.core.data.type.RoomType;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.ParticipantRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import io.ebean.annotation.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository meetingRepository;
  private final RoomAccess roomAccess;
  protected final RoomRepository roomRepository;
  protected final MembersService membersService;
  private final ParticipantRepository participantRepository;
  private final VideoServerService videoServerService;
  protected final EventDispatcher eventDispatcher;
  private final ChatMessageService chatMessageService;
  private final ObjectMapper objectMapper;
  protected final Clock clock;

  @Inject
  public MeetingServiceImpl(
      MeetingRepository meetingRepository,
      RoomAccess roomAccess,
      RoomRepository roomRepository,
      MembersService membersService,
      ParticipantRepository participantRepository,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      ChatMessageService chatMessageService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.meetingRepository = meetingRepository;
    this.roomAccess = roomAccess;
    this.roomRepository = roomRepository;
    this.membersService = membersService;
    this.participantRepository = participantRepository;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
    this.chatMessageService = chatMessageService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public Meeting createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingTypeDto,
      UUID roomId,
      OffsetDateTime expiration) {
    Room room = roomAccess.loadForMember(roomId, user);
    validateRoomMeeting(room.getId(), room.getMeetingId());

    Meeting meeting = createNewMeeting(name, meetingTypeDto, roomId.toString());
    roomRepository.update(room.meetingId(meeting.getId()));
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingCreated.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .roomId(UUID.fromString(room.getId()))
            .createdAt(meeting.getCreatedAt())
            .type(EventType.MEETING_CREATED));

    return meeting;
  }

  private void validateRoomMeeting(String roomId, String meetingId) {
    if (meetingId != null) {
      throw new ConflictException(
          String.format("Room %s has already an associated meeting", roomId));
    }
  }

  private Meeting createNewMeeting(String name, MeetingTypeDto meetingTypeDto, String roomId) {
    return meetingRepository.insert(
        Meeting.create()
            .id(UUID.randomUUID().toString())
            .name(name)
            .meetingType(MeetingType.valueOf(meetingTypeDto.toString().toUpperCase()))
            .active(false)
            .roomId(roomId));
  }

  @Override
  public Meeting startMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);
    Room room = roomAccess.loadForMember(UUID.fromString(meeting.getRoomId()), user);
    if (Boolean.TRUE.equals(meeting.getActive())) {
      return meeting;
    }

    Meeting updatedMeeting = activateMeeting(meeting);

    notifyMeetingStarted(user, room, updatedMeeting);

    return updatedMeeting;
  }

  @Override
  public Meeting stopMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);
    OffsetDateTime startedAt = meeting.getStartedAt();

    Meeting updatedMeeting = deactivateMeeting(user.getId(), meeting);
    List<String> extraReceivers = meetingStoppedExtraReceivers(meeting);

    roomRepository
        .getById(updatedMeeting.getRoomId())
        .ifPresent(
            room -> {
              Optional<EndedLine> endedLine = Optional.empty();
              if (RoomType.ONE_TO_ONE.equals(room.getType()) && startedAt != null) {
                endedLine =
                    insertMeetingEndedEvent(
                        room.getId(), user.getId(), updatedMeeting.getId(), startedAt);
              }
              notifyMeetingStopped(updatedMeeting, room, extraReceivers, user.getUUID(), endedLine);
              cleanUpRoomMembers(room);
            });

    return updatedMeeting;
  }

  @Override
  public void declineMeeting(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room = roomAccess.loadForMember(UUID.fromString(meeting.getRoomId()), currentUser);

    Optional<SystemEvent> systemEvent = Optional.empty();
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      systemEvent =
          insertMeetingSystemEvent(
              meeting.getRoomId(),
              "MEETING_DECLINED",
              currentUser.getId(),
              Map.of("declinedBy", currentUser.getId(), "meetingId", meeting.getId()));
    }

    eventDispatcher.sendToUserExchange(
        Stream.of(
                meeting.getParticipants().stream().map(Participant::getUserId),
                systemEvent.isPresent()
                    ? room.getSubscriptions().stream().map(Subscription::getUserId)
                    : Stream.<String>empty(),
                Stream.of(currentUser.getId()))
            .flatMap(Function.identity())
            .distinct()
            .toList(),
        MeetingDeclined.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID())
            .systemEventId(systemEvent.map(SystemEvent::getId).orElse(null))
            .systemEventCreatedAt(systemEvent.map(SystemEvent::getCreatedAt).orElse(null))
            .type(EventType.MEETING_DECLINED));
    afterMeetingDeclined(currentUser, room);

    if (RoomType.ONE_TO_ONE.equals(room.getType()) && meeting.getParticipants().size() == 1) {
      Meeting updatedMeeting = deactivateMeeting(currentUser.getId(), meeting);
      notifyMeetingStopped(
          updatedMeeting, room, List.of(), currentUser.getUUID(), Optional.empty());
    }
  }

  private void cleanUpRoomMembers(Room room) {
    List<Subscription> subscriptions = room.getSubscriptions();

    if (RoomType.TEMPORARY.equals(room.getType())) {
      subscriptions.stream()
          .filter(member -> !member.isOwner())
          .forEach(member -> membersService.deleteRoomMember(member.getUserId(), room));
    }

    afterRoomMembersCleanedUp(subscriptions);
  }

  protected Meeting validateMeeting(UUID meetingId) {
    return meetingRepository
        .getById(meetingId.toString())
        .orElseThrow(
            () ->
                new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
  }

  private Meeting activateMeeting(Meeting meeting) {
    videoServerService.startMeeting(meeting.getId());
    return meetingRepository.markStarted(meeting);
  }

  private Meeting deactivateMeeting(String userId, Meeting meeting) {
    beforeMeetingStopped(userId, meeting);
    videoServerService.stopMeeting(meeting.getId());
    participantRepository.clear(meeting.getId());
    meeting.active(false).participants(List.of()).startedAt(null);
    return meetingRepository.update(meeting);
  }

  private void notifyMeetingStarted(UserPrincipal user, Room room, Meeting updatedMeeting) {
    List<String> allReceivers =
        room.getSubscriptions().stream().map(Subscription::getUserId).toList();

    Optional<SystemEvent> systemEvent = Optional.empty();
    if (RoomType.ONE_TO_ONE.equals(room.getType())) {
      systemEvent =
          insertMeetingSystemEvent(
              updatedMeeting.getRoomId(),
              "MEETING_STARTED",
              user.getId(),
              Map.of("startedBy", user.getId(), "meetingId", updatedMeeting.getId()));
    }

    eventDispatcher.sendToUserExchange(
        allReceivers,
        MeetingStarted.create()
            .meetingId(UUID.fromString(updatedMeeting.getId()))
            .starterUser(user.getUUID())
            .startedAt(updatedMeeting.getStartedAt())
            .systemEventId(systemEvent.map(SystemEvent::getId).orElse(null))
            .systemEventCreatedAt(systemEvent.map(SystemEvent::getCreatedAt).orElse(null))
            .type(EventType.MEETING_STARTED));

    afterMeetingStarted(user, room, allReceivers);
  }

  private void notifyMeetingStopped(
      Meeting updatedMeeting,
      Room room,
      List<String> extraReceivers,
      UUID endedBy,
      Optional<EndedLine> endedLine) {
    List<String> receivers =
        extraReceivers.isEmpty()
            ? room.getSubscriptions().stream().map(Subscription::getUserId).toList()
            : Stream.concat(
                    room.getSubscriptions().stream().map(Subscription::getUserId),
                    extraReceivers.stream())
                .distinct()
                .toList();
    eventDispatcher.sendToUserExchange(
        receivers,
        MeetingStopped.create()
            .meetingId(UUID.fromString(updatedMeeting.getId()))
            .endedBy(endedBy)
            .systemEventId(endedLine.map(l -> l.event().getId()).orElse(null))
            .systemEventCreatedAt(endedLine.map(l -> l.event().getCreatedAt()).orElse(null))
            .durationSec(endedLine.map(EndedLine::durationSec).orElse(null))
            .type(EventType.MEETING_STOPPED));
  }

  private record EndedLine(SystemEvent event, Long durationSec) {}

  @Transactional
  private Optional<EndedLine> insertMeetingEndedEvent(
      String roomId, String endedBy, String meetingId, OffsetDateTime startedAt) {
    long startedAtMillis = startedAt.toInstant().toEpochMilli();
    Optional<SystemEvent> inserted =
        insertMeetingSystemEvent(
            roomId,
            "MEETING_ENDED",
            endedBy,
            Map.of("endedBy", endedBy, "meetingId", meetingId, "startedAt", startedAtMillis));
    return inserted.map(
        event -> {
          long durationSec =
              Math.max(0, Duration.between(startedAt, event.getCreatedAt()).toSeconds());
          try {
            event.content(
                objectMapper.writeValueAsString(
                    Map.of(
                        "endedBy", endedBy,
                        "meetingId", meetingId,
                        "startedAt", startedAtMillis,
                        "durationSec", durationSec)));
          } catch (Exception e) {
            ChatsLogger.warn(
                "Failed to serialize MEETING_ENDED system event content: " + e.getMessage());
            return new EndedLine(event, null);
          }
          chatMessageService.updateSystemEvent(event);
          return new EndedLine(event, durationSec);
        });
  }

  private Optional<SystemEvent> insertMeetingSystemEvent(
      String roomId, String type, String actorId, Map<String, ?> contentMap) {
    String content;
    try {
      content = objectMapper.writeValueAsString(contentMap);
    } catch (Exception e) {
      ChatsLogger.warn("Failed to serialize " + type + " system event content: " + e.getMessage());
      return Optional.empty();
    }
    SystemEvent event =
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(roomId)
            .type(type)
            .actorId(actorId)
            .content(content);
    chatMessageService.insertSystemEvent(event);
    return Optional.of(event);
  }

  @Override
  public List<Meeting> getMeetings(UserPrincipal currentUser) {
    List<String> roomsIds = roomRepository.getIdsByUserId(currentUser.getId());
    List<Meeting> meetings = meetingRepository.getByRoomsIds(roomsIds);
    return meetings;
  }

  @Override
  public Meeting getMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    if (membersService
        .getSubscription(currentUser.getUUID(), UUID.fromString(meeting.getRoomId()))
        .isEmpty()) {
      throw new ForbiddenException(
          String.format(
              "User '%s' hasn't access to the meeting with id '%s'",
              currentUser.getId(), meetingId));
    }
    return meeting;
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
  public Meeting getMeetingByRoomId(UUID roomId, UserPrincipal currentUser) {
    roomAccess.requireMember(roomId, currentUser);
    return getMeetingEntityByRoomId(roomId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("Meeting of the room with id '%s' doesn't exist", roomId)));
  }

  @Override
  public void deleteMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room = roomAccess.loadForMember(UUID.fromString(meeting.getRoomId()), currentUser);
    deleteMeeting(currentUser.getId(), meeting, room);
  }

  @Override
  public void releaseMeetingResources(String userId, Meeting meeting) {
    beforeMeetingStopped(userId, meeting);
    videoServerService.stopMeeting(meeting.getId());
  }

  @Override
  public void deleteMeeting(String userId, Meeting meeting, Room room) {
    releaseMeetingResources(userId, meeting);
    meetingRepository.delete(meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingDeleted.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .type(EventType.MEETING_DELETED));
  }

  @Override
  public void updateMeeting(Meeting updatedMeeting) {
    meetingRepository.update(updatedMeeting);
  }

  protected void beforeMeetingStopped(String userId, Meeting meeting) {}

  protected List<String> meetingStoppedExtraReceivers(Meeting meeting) {
    return List.of();
  }

  protected void afterMeetingStarted(UserPrincipal user, Room room, List<String> receivers) {}

  protected void afterMeetingDeclined(UserPrincipal currentUser, Room room) {}

  protected void afterRoomMembersCleanedUp(List<Subscription> subscriptions) {}
}
