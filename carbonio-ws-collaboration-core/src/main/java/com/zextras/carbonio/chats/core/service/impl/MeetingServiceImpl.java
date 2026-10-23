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
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.VideoServerService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.MeetingMapper;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MeetingDto;
import com.zextras.carbonio.chats.model.MeetingTypeDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

@Singleton
public class MeetingServiceImpl implements MeetingService {

  private final MeetingRepository meetingRepository;
  private final MeetingMapper meetingMapper;
  protected final RoomService roomService;
  protected final MembersService membersService;
  private final ParticipantService participantService;
  private final VideoServerService videoServerService;
  protected final EventDispatcher eventDispatcher;
  private final ChatMessageService chatMessageService;
  private final ObjectMapper objectMapper;
  protected final Clock clock;

  @Inject
  public MeetingServiceImpl(
      MeetingRepository meetingRepository,
      MeetingMapper meetingMapper,
      RoomService roomService,
      MembersService membersService,
      ParticipantService participantService,
      VideoServerService videoServerService,
      EventDispatcher eventDispatcher,
      ChatMessageService chatMessageService,
      ObjectMapper objectMapper,
      Clock clock) {
    this.meetingRepository = meetingRepository;
    this.meetingMapper = meetingMapper;
    this.roomService = roomService;
    this.membersService = membersService;
    this.participantService = participantService;
    this.videoServerService = videoServerService;
    this.eventDispatcher = eventDispatcher;
    this.chatMessageService = chatMessageService;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @Override
  public MeetingDto createMeeting(
      UserPrincipal user,
      String name,
      MeetingTypeDto meetingTypeDto,
      UUID roomId,
      OffsetDateTime expiration) {
    Room room = roomService.getRoomAndValidateUser(roomId, user, false);
    validateRoomMeeting(room.getId(), room.getMeetingId());

    Meeting meeting = createNewMeeting(name, meetingTypeDto, roomId.toString());
    roomService.setMeetingIntoRoom(room, meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingCreated.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .roomId(UUID.fromString(room.getId()))
            .type(EventType.MEETING_CREATED)
            .sentDate(OffsetDateTime.now(clock)));

    return meetingMapper.ent2dto(meeting);
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
  public MeetingDto startMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);

    Meeting updatedMeeting = activateMeeting(meeting);

    notifyMeetingStarted(user, updatedMeeting);

    return meetingMapper.ent2dto(updatedMeeting);
  }

  @Override
  public MeetingDto stopMeeting(UserPrincipal user, UUID meetingId) {
    Meeting meeting = validateMeeting(meetingId);
    OffsetDateTime startedAt = meeting.getStartedAt();

    Meeting updatedMeeting = deactivateMeeting(user.getId(), meeting);
    List<String> extraReceivers = meetingStoppedExtraReceivers(meeting);

    roomService
        .getRoom(UUID.fromString(updatedMeeting.getRoomId()))
        .ifPresent(
            room -> {
              Optional<SystemEvent> systemEvent = Optional.empty();
              if (RoomTypeDto.ONE_TO_ONE.equals(room.getType()) && startedAt != null) {
                OffsetDateTime now = OffsetDateTime.now(clock);
                long durationSec = Duration.between(startedAt, now).toSeconds();
                systemEvent =
                    insertMeetingSystemEvent(
                        room.getId(),
                        "MEETING_ENDED",
                        user.getId(),
                        Map.of(
                            "endedBy", user.getId(),
                            "meetingId", updatedMeeting.getId(),
                            "startedAt", startedAt.toInstant().toEpochMilli(),
                            "durationSec", durationSec));
              }
              notifyMeetingStopped(
                  updatedMeeting, room, extraReceivers, user.getUUID(), systemEvent);
              cleanUpRoomMembers(room);
            });

    return meetingMapper.ent2dto(updatedMeeting);
  }

  @Override
  public void declineMeeting(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room =
        roomService.getRoomAndValidateUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);

    Optional<SystemEvent> systemEvent = Optional.empty();
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      systemEvent =
          insertMeetingSystemEvent(
              meeting.getRoomId(),
              "MEETING_DECLINED",
              currentUser.getId(),
              Map.of("declinedBy", currentUser.getId(), "meetingId", meeting.getId()));
    }

    eventDispatcher.sendToUserExchange(
        meeting.getParticipants().stream().map(Participant::getUserId).distinct().toList(),
        MeetingDeclined.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .userId(currentUser.getUUID())
            .systemEventId(systemEvent.map(SystemEvent::getId).orElse(null))
            .systemEventCreatedAt(systemEvent.map(SystemEvent::getCreatedAt).orElse(null))
            .type(EventType.MEETING_DECLINED)
            .sentDate(OffsetDateTime.now(clock)));
    afterMeetingDeclined(currentUser, room);

    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType()) && meeting.getParticipants().size() == 1) {
      Meeting updatedMeeting = deactivateMeeting(currentUser.getId(), meeting);
      notifyMeetingStopped(
          updatedMeeting, room, List.of(), currentUser.getUUID(), Optional.empty());
    }
  }

  private void cleanUpRoomMembers(Room room) {
    List<Subscription> subscriptions = room.getSubscriptions();

    if (RoomTypeDto.TEMPORARY.equals(room.getType())) {
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
    meeting.active(true).startedAt(OffsetDateTime.now(clock));
    return meetingRepository.update(meeting);
  }

  private Meeting deactivateMeeting(String userId, Meeting meeting) {
    beforeMeetingStopped(userId, meeting);
    videoServerService.stopMeeting(meeting.getId());
    participantService.clear(UUID.fromString(meeting.getId()));
    meeting.active(false).participants(List.of()).startedAt(null);
    return meetingRepository.update(meeting);
  }

  private void notifyMeetingStarted(UserPrincipal user, Meeting updatedMeeting) {
    RoomDto room = roomService.getRoomById(UUID.fromString(updatedMeeting.getRoomId()), user);
    List<String> allReceivers =
        room.getMembers().stream().map(m -> m.getUserId().toString()).toList();

    Optional<SystemEvent> systemEvent = Optional.empty();
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
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
            .type(EventType.MEETING_STARTED)
            .sentDate(OffsetDateTime.now(clock)));

    afterMeetingStarted(user, room, allReceivers);
  }

  private void notifyMeetingStopped(
      Meeting updatedMeeting,
      Room room,
      List<String> extraReceivers,
      UUID endedBy,
      Optional<SystemEvent> systemEvent) {
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
            .systemEventId(systemEvent.map(SystemEvent::getId).orElse(null))
            .systemEventCreatedAt(systemEvent.map(SystemEvent::getCreatedAt).orElse(null))
            .type(EventType.MEETING_STOPPED)
            .sentDate(OffsetDateTime.now(clock)));
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
  public List<MeetingDto> getMeetings(UserPrincipal currentUser) {
    List<String> roomsIds =
        roomService.getRoomsIds(currentUser).stream().map(UUID::toString).toList();
    List<Meeting> meetings = meetingRepository.getByRoomsIds(roomsIds);
    return meetingMapper.ent2dto(meetings);
  }

  @Override
  public MeetingDto getMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
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
    roomService.getRoomAndValidateUser(roomId, currentUser, false);
    return meetingMapper.ent2dto(
        getMeetingEntityByRoomId(roomId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Meeting of the room with id '%s' doesn't exist", roomId))));
  }

  @Override
  public void deleteMeetingById(UUID meetingId, UserPrincipal currentUser) {
    Meeting meeting = validateMeeting(meetingId);
    Room room =
        roomService.getRoomAndValidateUser(
            UUID.fromString(meeting.getRoomId()), currentUser, false);
    deleteMeeting(currentUser.getId(), meeting, room);
  }

  @Override
  public void deleteMeeting(String userId, Meeting meeting, Room room) {
    beforeMeetingStopped(userId, meeting);
    videoServerService.stopMeeting(meeting.getId());
    meetingRepository.delete(meeting);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        MeetingDeleted.create()
            .meetingId(UUID.fromString(meeting.getId()))
            .type(EventType.MEETING_DELETED)
            .sentDate(OffsetDateTime.now(clock)));
  }

  @Override
  public void updateMeeting(Meeting updatedMeeting) {
    meetingRepository.update(updatedMeeting);
  }

  protected void beforeMeetingStopped(String userId, Meeting meeting) {}

  protected List<String> meetingStoppedExtraReceivers(Meeting meeting) {
    return List.of();
  }

  protected void afterMeetingStarted(UserPrincipal user, RoomDto room, List<String> receivers) {}

  protected void afterMeetingDeclined(UserPrincipal currentUser, Room room) {}

  protected void afterRoomMembersCleanedUp(List<Subscription> subscriptions) {}
}
