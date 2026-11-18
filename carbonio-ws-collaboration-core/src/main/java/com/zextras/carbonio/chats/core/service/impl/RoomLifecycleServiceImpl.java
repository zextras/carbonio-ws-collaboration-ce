// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.EventType;
import com.zextras.carbonio.async.model.MeetingDeleted;
import com.zextras.carbonio.async.model.RoomDeleted;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MeetingRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.ParticipantService;
import com.zextras.carbonio.chats.core.service.RoomAccess;
import com.zextras.carbonio.chats.core.service.RoomLifecycleService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class RoomLifecycleServiceImpl implements RoomLifecycleService {

  private final RoomAccess roomAccess;
  private final RoomRepository roomRepository;
  private final MeetingRepository meetingRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final MeetingService meetingService;
  private final ParticipantService participantService;
  private final MembersService membersService;
  private final AttachmentService attachmentService;
  private final StoragesService storagesService;
  private final EventDispatcher eventDispatcher;

  @Inject
  public RoomLifecycleServiceImpl(
      RoomAccess roomAccess,
      RoomRepository roomRepository,
      MeetingRepository meetingRepository,
      FileMetadataRepository fileMetadataRepository,
      MeetingService meetingService,
      ParticipantService participantService,
      MembersService membersService,
      AttachmentService attachmentService,
      StoragesService storagesService,
      EventDispatcher eventDispatcher) {
    this.roomAccess = roomAccess;
    this.roomRepository = roomRepository;
    this.meetingRepository = meetingRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.meetingService = meetingService;
    this.participantService = participantService;
    this.membersService = membersService;
    this.attachmentService = attachmentService;
    this.storagesService = storagesService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  public void deleteRoom(UUID roomId, UserPrincipal currentUser) {
    deleteRoom(roomAccess.loadForOwner(roomId, currentUser), currentUser);
  }

  @Override
  public void deleteRoom(Room room, UserPrincipal currentUser) {
    UUID roomId = UUID.fromString(room.getId());
    List<String> receivers = room.getSubscriptions().stream().map(Subscription::getUserId).toList();
    Optional<Meeting> meeting =
        room.getMeetingId() == null
            ? Optional.empty()
            : meetingService.getMeetingEntity(UUID.fromString(room.getMeetingId()));

    meeting.ifPresent(m -> meetingService.releaseMeetingResources(currentUser.getId(), m));
    RemovedRows removed = deleteRows(room, meeting);

    meeting.ifPresent(
        m ->
            eventDispatcher.sendToUserExchange(
                receivers,
                MeetingDeleted.create()
                    .meetingId(UUID.fromString(m.getId()))
                    .type(EventType.MEETING_DELETED)));
    eventDispatcher.sendToUserExchange(
        receivers, RoomDeleted.create().roomId(roomId).type(EventType.ROOM_DELETED));
    deleteAttachments(removed.attachmentIds(), currentUser);
    removed.avatar().ifPresent(this::deleteAvatarFile);
  }

  private record RemovedRows(List<String> attachmentIds, Optional<FileMetadata> avatar) {}

  @Transactional
  private RemovedRows deleteRows(Room room, Optional<Meeting> meeting) {
    String roomId = room.getId();
    meeting.ifPresent(meetingRepository::delete);
    List<String> attachmentIds =
        fileMetadataRepository.getIdsByRoomIdAndType(roomId, FileMetadataType.ATTACHMENT);
    Optional<FileMetadata> avatar =
        room.getPictureUpdatedAt() == null
            ? Optional.empty()
            : fileMetadataRepository.find(null, roomId, FileMetadataType.ROOM_AVATAR);
    avatar.ifPresent(fileMetadataRepository::delete);
    roomRepository.delete(roomId);
    return new RemovedRows(attachmentIds, avatar);
  }

  private void deleteAttachments(List<String> attachmentIds, UserPrincipal currentUser) {
    try {
      attachmentService.deleteAttachmentsByIds(attachmentIds, currentUser);
    } catch (Exception e) {
      ChatsLogger.warn("Failed to delete the attachments of a deleted room: " + e.getMessage());
    }
  }

  private void deleteAvatarFile(FileMetadata avatar) {
    try {
      storagesService.deleteFile(avatar.getId(), avatar.getUserId());
    } catch (Exception e) {
      ChatsLogger.warn("Failed to delete the picture file of a deleted room: " + e.getMessage());
    }
  }

  @Override
  public void removeMember(UUID roomId, UUID userId, UserPrincipal currentUser) {
    Room room =
        currentUser.getUUID().equals(userId)
            ? roomAccess.loadForMember(roomId, currentUser)
            : roomAccess.loadForOwner(roomId, currentUser);
    validateUserMembership(userId, currentUser, room);
    if (room.getSubscriptions().size() == 1) {
      deleteRoom(room, currentUser);
      return;
    }
    if (room.getMeetingId() != null) {
      meetingService
          .getMeetingEntity(UUID.fromString(room.getMeetingId()))
          .ifPresent(meeting -> participantService.removeMeetingParticipant(meeting, room, userId));
    }
    validateLastRoomOwner(userId.toString(), room);
    membersService.deleteRoomMember(userId.toString(), room, currentUser.getId());
  }

  private static void validateUserMembership(UUID userId, UserPrincipal currentUser, Room room) {
    if (!currentUser.getUUID().equals(userId)
        && room.getSubscriptions().stream()
            .noneMatch(s -> s.getUserId().equals(userId.toString()))) {
      throw new NotFoundException("The user is not a room member");
    }
  }

  private static void validateLastRoomOwner(String userId, Room room) {
    List<String> owners =
        room.getSubscriptions().stream()
            .filter(Subscription::isOwner)
            .map(Subscription::getUserId)
            .toList();
    if (owners.size() == 1
        && owners.getFirst().equals(userId)
        && room.getSubscriptions().size() > 1) {
      throw new BadRequestException("Last owner can't leave the room");
    }
  }
}
