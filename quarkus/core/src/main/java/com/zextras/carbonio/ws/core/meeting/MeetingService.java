// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.ws.core.meeting;

import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingCreated;
import com.zextras.carbonio.async.model.MeetingDeleted;
import com.zextras.carbonio.async.model.MeetingParticipantJoined;
import com.zextras.carbonio.async.model.MeetingParticipantLeft;
import com.zextras.carbonio.async.model.MeetingStopped;
import com.zextras.carbonio.ws.core.meeting.dto.CreateMeetingRequest;
import com.zextras.carbonio.ws.core.meeting.dto.MeetingDto;
import com.zextras.carbonio.ws.core.persistence.MeetingEntity;
import com.zextras.carbonio.ws.core.persistence.MeetingRepository;
import com.zextras.carbonio.ws.core.persistence.ParticipantEntity;
import com.zextras.carbonio.ws.core.persistence.ParticipantRepository;
import com.zextras.carbonio.ws.core.persistence.RoomEntity;
import com.zextras.carbonio.ws.core.persistence.RoomRepository;
import com.zextras.carbonio.ws.core.persistence.SubscriptionRepository;
import com.zextras.carbonio.ws.core.videoserver.VideoServerService;
import com.zextras.carbonio.ws.plugin.api.EventPublisher;
import com.zextras.carbonio.ws.plugin.api.UserPrincipal;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStarted;
import com.zextras.carbonio.ws.plugin.api.event.MeetingStopping;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class MeetingService {

  static final String ACCEPTED = "ACCEPTED";
  private static final Set<String> MEETING_TYPES = Set.of("PERMANENT", "SCHEDULED");

  @Inject MeetingRepository meetings;
  @Inject RoomRepository rooms;
  @Inject SubscriptionRepository subscriptions;
  @Inject ParticipantRepository participants;
  @Inject VideoServerService videoServer;
  @Inject EventPublisher events;
  @Inject MeetingMapper mapper;
  @Inject Event<MeetingStarted> meetingStarted;
  @Inject Event<MeetingStopping> meetingStopping;

  @Transactional
  public MeetingDto create(UserPrincipal user, CreateMeetingRequest request) {
    if (request == null || request.roomId() == null) {
      throw new BadRequestException("roomId is required");
    }
    String type = Optional.ofNullable(request.meetingType()).map(String::toUpperCase).orElse("");
    if (!MEETING_TYPES.contains(type)) {
      throw new BadRequestException("Unknown meeting type: " + request.meetingType());
    }
    RoomEntity room = requireRoomMember(request.roomId().toString(), user);
    if (room.meetingId != null) {
      throw new ClientErrorException(
          String.format("Room %s has already an associated meeting", room.id), 409);
    }
    MeetingEntity meeting = new MeetingEntity();
    meeting.id = UUID.randomUUID().toString();
    meeting.roomId = room.id;
    meeting.name = request.name();
    meeting.meetingType = type;
    meeting.active = false;
    meetings.persist(meeting);
    room.meetingId = meeting.id;
    events.sendToUsers(
        memberIds(room.id),
        MeetingCreated.create()
            .meetingId(UUID.fromString(meeting.id))
            .roomId(UUID.fromString(room.id))
            .type(EventType.MEETING_CREATED)
            .sentDate(now()));
    meetings.flush();
    return mapper.toDto(meeting);
  }

  @Transactional
  public MeetingDto get(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    if (subscriptions.find(meeting.roomId, user.id().toString()).isEmpty()) {
      throw new ForbiddenException(
          String.format("User '%s' hasn't access to the meeting with id '%s'", user.id(), meetingId));
    }
    return mapper.toDto(meeting);
  }

  @Transactional
  public MeetingDto start(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    requireRoomMember(meeting.roomId, user);
    if (Boolean.TRUE.equals(meeting.active)) {
      return mapper.toDto(meeting);
    }
    videoServer.startMeeting(meeting.id);
    meeting.active = true;
    meeting.startedAt = now();
    events.sendToUsers(
        memberIds(meeting.roomId),
        com.zextras.carbonio.async.model.MeetingStarted.create()
            .meetingId(meetingId)
            .starterUser(user.id())
            .startedAt(meeting.startedAt)
            .type(EventType.MEETING_STARTED)
            .sentDate(now()));
    meetingStarted.fire(new MeetingStarted(meetingId, UUID.fromString(meeting.roomId), user.id()));
    return mapper.toDto(meeting);
  }

  @Transactional
  public MeetingDto stop(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    requireRoomMember(meeting.roomId, user);
    if (!Boolean.TRUE.equals(meeting.active)) {
      return mapper.toDto(meeting);
    }
    deactivate(user.id(), meeting);
    return mapper.toDto(meeting);
  }

  @Transactional
  public void delete(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    RoomEntity room = requireRoomMember(meeting.roomId, user);
    if (Boolean.TRUE.equals(meeting.active)) {
      meetingStopping.fire(new MeetingStopping(meetingId, user.id()));
      videoServer.stopMeeting(meeting.id);
    }
    participants.deleteByMeeting(meeting.id);
    room.meetingId = null;
    meetings.delete(meeting);
    events.sendToUsers(
        memberIds(room.id),
        MeetingDeleted.create().meetingId(meetingId).type(EventType.MEETING_DELETED).sentDate(now()));
  }

  @Transactional
  public String join(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    requireRoomMember(meeting.roomId, user);
    if (!Boolean.TRUE.equals(meeting.active)) {
      throw new BadRequestException(String.format("Meeting '%s' is not active", meetingId));
    }
    String userId = user.id().toString();
    String queueId = user.queueId() == null ? null : user.queueId().toString();
    Optional<ParticipantEntity> existing = participants.find(meeting.id, userId);
    if (existing.isPresent()) {
      existing.get().queueId = queueId;
      return ACCEPTED;
    }
    ParticipantEntity participant = new ParticipantEntity();
    participant.meetingId = meeting.id;
    participant.userId = userId;
    participant.queueId = queueId;
    participants.persist(participant);
    events.sendToUsers(
        memberIds(meeting.roomId),
        MeetingParticipantJoined.create()
            .meetingId(meetingId)
            .userId(user.id())
            .type(EventType.MEETING_PARTICIPANT_JOINED)
            .sentDate(now()));
    return ACCEPTED;
  }

  @Transactional
  public void leave(UserPrincipal user, UUID meetingId) {
    MeetingEntity meeting = require(meetingId);
    ParticipantEntity participant =
        participants.find(meeting.id, user.id().toString())
            .orElseThrow(() -> new NotFoundException(
                String.format("User '%s' not found into meeting '%s'", user.id(), meetingId)));
    participants.delete(participant);
    participants.flush();
    events.sendToUsers(
        memberIds(meeting.roomId),
        MeetingParticipantLeft.create()
            .meetingId(meetingId)
            .userId(user.id())
            .type(EventType.MEETING_PARTICIPANT_LEFT)
            .sentDate(now()));
    if (participants.listByMeeting(meeting.id).isEmpty()) {
      deactivate(user.id(), meeting);
    }
  }

  private void deactivate(UUID userId, MeetingEntity meeting) {
    // Plugins (e.g. recording) react while the videoserver rooms still exist.
    meetingStopping.fire(new MeetingStopping(UUID.fromString(meeting.id), userId));
    videoServer.stopMeeting(meeting.id);
    participants.deleteByMeeting(meeting.id);
    meeting.active = false;
    meeting.startedAt = null;
    events.sendToUsers(
        memberIds(meeting.roomId),
        MeetingStopped.create()
            .meetingId(UUID.fromString(meeting.id))
            .type(EventType.MEETING_STOPPED)
            .sentDate(now()));
  }

  private MeetingEntity require(UUID meetingId) {
    return meetings.findByIdOptional(meetingId.toString())
        .orElseThrow(() -> new NotFoundException(String.format("Meeting with id '%s' not found", meetingId)));
  }

  private RoomEntity requireRoomMember(String roomId, UserPrincipal user) {
    RoomEntity room =
        rooms.findByIdOptional(roomId)
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s' not found", roomId)));
    if (subscriptions.find(roomId, user.id().toString()).isEmpty()) {
      throw new ForbiddenException(
          String.format("User '%s' is not a member of room '%s'", user.id(), roomId));
    }
    return room;
  }

  private List<UUID> memberIds(String roomId) {
    return subscriptions.listByRoom(roomId).stream().map(s -> UUID.fromString(s.userId)).toList();
  }

  private static OffsetDateTime now() {
    return OffsetDateTime.now(ZoneOffset.UTC);
  }
}
