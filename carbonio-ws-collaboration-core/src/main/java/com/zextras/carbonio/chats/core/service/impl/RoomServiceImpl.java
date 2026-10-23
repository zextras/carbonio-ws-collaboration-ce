// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.async.model.*;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.entity.SystemEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.repository.DatabaseClock;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.MessageRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.repository.SystemEventRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.ChatMessageService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.SystemEventContentFactory;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.Transactional;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class RoomServiceImpl implements RoomService {

  private static final OffsetDateTime MUTED_TO_INFINITY =
      OffsetDateTime.parse("0001-01-01T00:00:00Z");

  private final RoomRepository roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final FileMetadataRepository fileMetadataRepository;
  private final MessageRepository messageRepository;
  private final SystemEventRepository systemEventRepository;
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final StoragesService storagesService;
  private final AttachmentService attachmentService;
  private final EventDispatcher eventDispatcher;
  private final MessageDispatcher messageDispatcher;
  private final RoomMapper roomMapper;
  private final Clock clock;
  private final ChatMessageService chatMessageService;
  private final ObjectMapper objectMapper;
  private final DatabaseClock databaseClock;
  private final SystemEventContentFactory systemEventContentFactory;

  @Inject
  public RoomServiceImpl(
      RoomRepository roomRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      FileMetadataRepository fileMetadataRepository,
      MessageRepository messageRepository,
      SystemEventRepository systemEventRepository,
      UserService userService,
      MembersService membersService,
      MeetingService meetingService,
      StoragesService storagesService,
      AttachmentService attachmentService,
      EventDispatcher eventDispatcher,
      MessageDispatcher messageDispatcher,
      RoomMapper roomMapper,
      Clock clock,
      ChatMessageService chatMessageService,
      ObjectMapper objectMapper,
      DatabaseClock databaseClock,
      SystemEventContentFactory systemEventContentFactory) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.messageRepository = messageRepository;
    this.systemEventRepository = systemEventRepository;
    this.userService = userService;
    this.membersService = membersService;
    this.meetingService = meetingService;
    this.storagesService = storagesService;
    this.attachmentService = attachmentService;
    this.eventDispatcher = eventDispatcher;
    this.messageDispatcher = messageDispatcher;
    this.roomMapper = roomMapper;
    this.clock = clock;
    this.chatMessageService = chatMessageService;
    this.objectMapper = objectMapper;
    this.databaseClock = databaseClock;
    this.systemEventContentFactory = systemEventContentFactory;
  }

  @Override
  public List<RoomDto> getRooms(
      @Nullable List<RoomExtraFieldDto> extraFields, UserPrincipal currentUser) {
    boolean includeMembers = false, includeSettings = false;
    if (extraFields != null) {
      includeMembers = extraFields.contains(RoomExtraFieldDto.MEMBERS);
      includeSettings = extraFields.contains(RoomExtraFieldDto.SETTINGS);
    }
    List<Room> rooms = roomRepository.getByUserId(currentUser.getId(), includeMembers);
    Map<String, RoomUserSettings> settingsMap = null;
    if (includeSettings) {
      settingsMap = roomUserSettingsRepository.getMapGroupedByUserId(currentUser.getId());
    }
    return roomMapper.ent2dto(rooms, settingsMap, includeMembers, includeSettings);
  }

  @Override
  public RoomDto getRoomById(UUID roomId, UserPrincipal currentUser) {
    return roomMapper.ent2dto(
        getRoomAndValidateUser(roomId, currentUser, false),
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElse(null),
        true,
        true);
  }

  @Override
  public RoomDto createRoom(RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    validateCreateRoom(roomCreationFields, currentUser);

    List<MemberDto> members = prepareRoomMembers(roomCreationFields, currentUser.getUUID());
    int creationEvents =
        RoomTypeDto.ONE_TO_ONE.equals(roomCreationFields.getType()) ? 0 : members.size() - 1;
    OffsetDateTime creationInstant = databaseClock.now().minus(creationEvents, ChronoUnit.MILLIS);
    Room room = initializeRoom(roomCreationFields).createdAt(creationInstant);

    createRoom(room, currentUser, members, true);

    room.subscriptions(
        membersService.initRoomSubscriptions(
            members, room, creationInstant.plus(creationEvents, ChronoUnit.MILLIS)));
    room = roomRepository.insert(room);

    String roomCreatedContent;
    try {
      roomCreatedContent =
          objectMapper.writeValueAsString(Map.of("creatorId", currentUser.getId()));
    } catch (Exception e) {
      ChatsLogger.warn("Failed to serialize ROOM_CREATED system event content: " + e.getMessage());
      roomCreatedContent = "{\"creatorId\":\"" + currentUser.getId() + "\"}";
    }
    chatMessageService.insertSystemEvent(
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(room.getId())
            .type("ROOM_CREATED")
            .actorId(currentUser.getId())
            .content(roomCreatedContent)
            .createdAt(creationInstant));

    if (!RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      for (int i = 1; i < members.size(); i++) {
        chatMessageService.insertSystemEvent(
            SystemEvent.create()
                .id(UUID.randomUUID().toString())
                .roomId(room.getId())
                .type("MEMBER_ADDED")
                .actorId(currentUser.getId())
                .content(
                    systemEventContentFactory.memberAdded(
                        currentUser.getId(), List.of(members.get(i).getUserId().toString())))
                .createdAt(creationInstant.plus(i, ChronoUnit.MILLIS)));
      }
    }

    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomCreated.create()
            .roomId(UUID.fromString(room.getId()))
            .type(EventType.ROOM_CREATED)
            .sentDate(OffsetDateTime.now()));

    return mapRoomToDto(room, currentUser);
  }

  private List<MemberDto> prepareRoomMembers(
      RoomCreationFieldsDto roomCreationFields, UUID currentUserUUID) {
    List<MemberDto> members = new ArrayList<>(roomCreationFields.getMembers());
    members.addFirst(MemberDto.create().userId(currentUserUUID).owner(true));
    return members;
  }

  private Room initializeRoom(RoomCreationFieldsDto roomCreationFields) {
    Room room = Room.create().id(UUID.randomUUID().toString()).type(roomCreationFields.getType());

    Optional.ofNullable(roomCreationFields.getName()).ifPresent(room::name);
    Optional.ofNullable(roomCreationFields.getDescription()).ifPresent(room::description);

    return room;
  }

  private void createRoom(
      Room room,
      UserPrincipal currentUser,
      List<MemberDto> members,
      boolean sendAffiliationMessages) {
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      messageDispatcher.addUsersToContacts(
          members.get(0).getUserId().toString(), members.get(1).getUserId().toString());
    }
  }

  private RoomDto mapRoomToDto(Room room, UserPrincipal currentUser) {
    RoomUserSettings currentUserSettings =
        room.getUserSettings().stream()
            .filter(settings -> settings.getUserId().equals(currentUser.getId()))
            .findAny()
            .orElse(null);

    return roomMapper.ent2dto(room, currentUserSettings, true, true);
  }

  private void validateCreateRoom(
      RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    List<UUID> memberIds = extractUniqueMemberIds(roomCreationFields);

    validateNoDuplicateMembers(roomCreationFields, memberIds);
    validateRequesterNotIncluded(currentUser, memberIds);

    if (RoomTypeDto.ONE_TO_ONE.equals(roomCreationFields.getType())) {
      validateOneToOneRoom(currentUser, memberIds);
    }

    validateAllUsersExist(memberIds, currentUser);
  }

  private List<UUID> extractUniqueMemberIds(RoomCreationFieldsDto roomCreationFields) {
    return new ArrayList<>(
        new HashSet<>(roomCreationFields.getMembers().stream().map(MemberDto::getUserId).toList()));
  }

  private void validateNoDuplicateMembers(
      RoomCreationFieldsDto roomCreationFields, List<UUID> memberIds) {
    if (roomCreationFields.getMembers().size() != memberIds.size()) {
      throw new BadRequestException("Members cannot be duplicated");
    }
  }

  private void validateRequesterNotIncluded(UserPrincipal currentUser, List<UUID> memberIds) {
    if (memberIds.stream().anyMatch(id -> id.toString().equals(currentUser.getId()))) {
      throw new BadRequestException("Requester can't be invited to the room");
    }
  }

  private void validateOneToOneRoom(UserPrincipal currentUser, List<UUID> memberIds) {
    if (roomRepository
        .getOneToOneByAllUserIds(currentUser.getId(), memberIds.getFirst().toString())
        .isPresent()) {
      throw new ConflictException("The one-to-one room already exists for these users");
    }
  }

  private void validateAllUsersExist(List<UUID> memberIds, UserPrincipal currentUser) {
    memberIds.stream()
        .filter(memberId -> !userService.userExists(memberId, currentUser))
        .findFirst()
        .ifPresent(
            uuid -> {
              throw new NotFoundException(String.format("User with id '%s' not found", uuid));
            });
  }

  @Override
  public RoomDto updateRoom(
      UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, true);
    boolean nameChanged = isNameChanged(room, updateRoomRequestDto);
    boolean descriptionChanged = isDescriptionChanged(room, updateRoomRequestDto);
    boolean isRoomUpdated = updateRoomDetails(room, updateRoomRequestDto);

    if (isRoomUpdated) {
      roomRepository.update(room);
      OffsetDateTime eventInstant = databaseClock.now();
      Optional<SystemEvent> nameSystemEvent = Optional.empty();
      Optional<SystemEvent> descriptionSystemEvent = Optional.empty();
      if (nameChanged) {
        nameSystemEvent =
            Optional.of(
                insertRoomSystemEvent(
                    room,
                    "ROOM_NAME_CHANGED",
                    currentUser.getId(),
                    systemEventContentFactory.roomNameChanged(currentUser.getId(), room.getName()),
                    eventInstant));
      }
      if (descriptionChanged) {
        descriptionSystemEvent =
            Optional.of(
                insertRoomSystemEvent(
                    room,
                    "ROOM_DESCRIPTION_CHANGED",
                    currentUser.getId(),
                    systemEventContentFactory.roomDescriptionChanged(
                        currentUser.getId(), room.getDescription()),
                    nameChanged ? eventInstant.plus(1, ChronoUnit.MILLIS) : eventInstant));
      }
      eventDispatcher.sendToUserExchange(
          room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
          RoomUpdated.create()
              .roomId(roomId)
              .name(room.getName())
              .description(room.getDescription())
              .updatedBy(currentUser.getUUID())
              .nameSystemEventId(nameSystemEvent.map(SystemEvent::getId).orElse(null))
              .nameSystemEventCreatedAt(nameSystemEvent.map(SystemEvent::getCreatedAt).orElse(null))
              .descriptionSystemEventId(descriptionSystemEvent.map(SystemEvent::getId).orElse(null))
              .descriptionSystemEventCreatedAt(
                  descriptionSystemEvent.map(SystemEvent::getCreatedAt).orElse(null))
              .type(EventType.ROOM_UPDATED)
              .sentDate(OffsetDateTime.now()));
    }

    return roomMapper.ent2dto(
        room,
        room.getUserSettings().stream()
            .filter(userSettings -> userSettings.getUserId().equals(currentUser.getId()))
            .findAny()
            .orElse(null),
        false,
        false);
  }

  private SystemEvent insertRoomSystemEvent(
      Room room, String type, String actorId, String content, OffsetDateTime createdAt) {
    SystemEvent event =
        SystemEvent.create()
            .id(UUID.randomUUID().toString())
            .roomId(room.getId())
            .type(type)
            .actorId(actorId)
            .content(content)
            .createdAt(createdAt);
    chatMessageService.insertSystemEvent(event);
    return event;
  }

  private boolean updateRoomDetails(Room room, RoomEditableFieldsDto updateRoomRequestDto) {
    boolean changed = false;

    if (isNameChanged(room, updateRoomRequestDto)) {
      changed = true;
      room.name(updateRoomRequestDto.getName());
      if (room.getMeetingId() != null) {
        meetingService
            .getMeetingEntity(UUID.fromString(room.getMeetingId()))
            .ifPresent(
                meeting ->
                    meetingService.updateMeeting(meeting.name(updateRoomRequestDto.getName())));
      }
    }

    if (isDescriptionChanged(room, updateRoomRequestDto)) {
      changed = true;
      room.description(updateRoomRequestDto.getDescription());
    }

    return changed;
  }

  private boolean isNameChanged(Room room, RoomEditableFieldsDto updateRoomRequestDto) {
    return updateRoomRequestDto.getName() != null
        && !updateRoomRequestDto.getName().equals(room.getName());
  }

  private boolean isDescriptionChanged(Room room, RoomEditableFieldsDto updateRoomRequestDto) {
    return updateRoomRequestDto.getDescription() != null
        && !updateRoomRequestDto.getDescription().equals(room.getDescription());
  }

  @Override
  public void deleteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, true);
    if (room.getMeetingId() != null) {
      meetingService
          .getMeetingEntity(UUID.fromString(room.getMeetingId()))
          .ifPresent(meeting -> meetingService.deleteMeeting(currentUser.getId(), meeting, room));
    }
    attachmentService.deleteAttachmentsByRoomId(roomId, currentUser);
    if (room.getPictureUpdatedAt() != null) {
      fileMetadataRepository
          .find(null, roomId.toString(), FileMetadataType.ROOM_AVATAR)
          .ifPresent(
              metadata -> {
                storagesService.deleteFile(metadata.getId(), metadata.getUserId());
                fileMetadataRepository.delete(metadata);
              });
    }
    deleteRoom(room);
    roomRepository.delete(roomId.toString());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomDeleted.create()
            .roomId(roomId)
            .type(EventType.ROOM_DELETED)
            .sentDate(OffsetDateTime.now()));
  }

  private void deleteRoom(Room room) {
    // Member removal from XMPP is no longer needed; handled by DomainEvent system.
  }

  @Override
  public void muteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, false);
    RoomUserSettings settings =
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElseGet(() -> RoomUserSettings.create(room, currentUser.getId()));
    if (settings.getMutedUntil() == null) {
      roomUserSettingsRepository.save(settings.mutedUntil(MUTED_TO_INFINITY));
      eventDispatcher.sendToUserExchange(
          currentUser.getId(),
          RoomMuted.create()
              .roomId(roomId)
              .type(EventType.ROOM_MUTED)
              .sentDate(OffsetDateTime.now()));
    }
  }

  @Override
  public void unmuteRoom(UUID roomId, UserPrincipal currentUser) {
    getRoomAndValidateUser(roomId, currentUser, false);
    roomUserSettingsRepository
        .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
        .ifPresent(
            settings -> {
              if (settings.getMutedUntil() != null) {
                roomUserSettingsRepository.save(settings.mutedUntil(null));
                eventDispatcher.sendToUserExchange(
                    currentUser.getId(),
                    RoomUnmuted.create()
                        .roomId(roomId)
                        .type(EventType.ROOM_UNMUTED)
                        .sentDate(OffsetDateTime.now()));
              }
            });
  }

  @Override
  @Transactional
  public OffsetDateTime clearRoomHistory(UUID roomId, UserPrincipal currentUser) {
    Room room =
        getRoom(roomId)
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s'", roomId)));

    if (RoomTypeDto.TEMPORARY.equals(room.getType())) {
      return purgeTemporaryRoomHistory(room, currentUser);
    } else {
      return softClearHistoryForUser(room, currentUser);
    }
  }

  private OffsetDateTime purgeTemporaryRoomHistory(Room room, UserPrincipal currentUser) {
    boolean isOwner =
        room.getSubscriptions().stream()
            .filter(s -> s.getUserId().equals(currentUser.getId()))
            .findAny()
            .map(Subscription::isOwner)
            .orElse(false);

    if (!isOwner) {
      throw new ForbiddenException(
          String.format(
              "User '%s' is not an owner of room '%s'", currentUser.getId(), room.getId()));
    }

    attachmentService.deleteAttachmentsByRoomId(UUID.fromString(room.getId()), currentUser);
    // message delete cascades reactions/pin (FK ON DELETE CASCADE); system events don't
    messageRepository.deleteByRoomId(room.getId());
    systemEventRepository.deleteByRoomId(room.getId());
    SystemEvent systemEvent =
        insertRoomSystemEvent(
            room,
            "ROOM_HISTORY_CLEARED",
            currentUser.getId(),
            systemEventContentFactory.roomHistoryCleared(currentUser.getId()),
            null);

    List<String> subscribersIds =
        room.getSubscriptions().stream().map(Subscription::getUserId).toList();

    OffsetDateTime clearedAt = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
    eventDispatcher.sendToUserExchange(
        subscribersIds,
        RoomHistoryCleared.create()
            .roomId(UUID.fromString(room.getId()))
            .clearedAt(clearedAt)
            .clearedBy(currentUser.getUUID())
            .systemEventId(systemEvent.getId())
            .systemEventCreatedAt(systemEvent.getCreatedAt())
            .type(EventType.ROOM_HISTORY_CLEARED)
            .sentDate(OffsetDateTime.now()));

    return clearedAt;
  }

  private OffsetDateTime softClearHistoryForUser(Room room, UserPrincipal currentUser) {
    boolean isMember =
        room.getSubscriptions().stream().anyMatch(s -> s.getUserId().equals(currentUser.getId()));

    if (!isMember) {
      throw new ForbiddenException(
          String.format(
              "User '%s' is not a member of room '%s'", currentUser.getId(), room.getId()));
    }

    OffsetDateTime clearedAt =
        roomUserSettingsRepository.setClearedAtNow(room.getId(), currentUser.getId());

    eventDispatcher.sendToUserExchange(
        currentUser.getId(),
        RoomHistoryCleared.create()
            .roomId(UUID.fromString(room.getId()))
            .clearedAt(clearedAt)
            .type(EventType.ROOM_HISTORY_CLEARED)
            .sentDate(OffsetDateTime.now()));

    return clearedAt;
  }

  @Override
  public List<UUID> getRoomsIds(UserPrincipal currentUser) {
    return roomRepository.getIdsByUserId(currentUser.getId()).stream()
        .map(UUID::fromString)
        .toList();
  }

  @Override
  public Room getRoomAndValidateUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
    Room room =
        roomRepository
            .getById(roomId.toString())
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s'", roomId)));
    List<Subscription> subscriptions = room.getSubscriptions();

    Subscription member =
        subscriptions.stream()
            .filter(subscription -> subscription.getUserId().equals(currentUser.getId()))
            .findAny()
            .orElseThrow(
                () ->
                    new ForbiddenException(
                        String.format(
                            "User '%s' is not a member of room '%s'",
                            currentUser.getId(), roomId)));
    if (mustBeOwner && !member.isOwner()) {
      throw new ForbiddenException(
          String.format("User '%s' is not an owner of room '%s'", currentUser.getId(), roomId));
    }
    return room;
  }

  @Override
  public Optional<Room> getRoom(UUID roomId) {
    return roomRepository.getById(roomId.toString());
  }

  @Override
  public FileContentAndMetadata getRoomPicture(UUID roomId, UserPrincipal currentUser) {
    getRoomAndValidateUser(roomId, currentUser, false);
    FileMetadata metadata =
        fileMetadataRepository
            .find(null, roomId.toString(), FileMetadataType.ROOM_AVATAR)
            .orElseThrow(
                () -> new NotFoundException(String.format("Room picture '%s' not found", roomId)));
    return new FileContentAndMetadata(
        storagesService.getFileStreamById(metadata.getId(), metadata.getUserId()), metadata);
  }

  @Override
  public void setRoomPicture(
      UUID roomId,
      InputStream image,
      String mimeType,
      Long contentLength,
      String fileName,
      UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, true);
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set for groups");
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The room picture must be an image");
    }
    Optional<FileMetadata> oldMetadata =
        fileMetadataRepository.find(null, roomId.toString(), FileMetadataType.ROOM_AVATAR);
    if (oldMetadata.isPresent()) {
      storagesService.deleteFile(oldMetadata.get().getId(), oldMetadata.get().getUserId());
      fileMetadataRepository.delete(oldMetadata.get());
    }
    String fileId = UUID.randomUUID().toString();
    storagesService.saveFile(image, fileId, currentUser.getId(), contentLength);
    fileMetadataRepository.save(
        FileMetadata.create()
            .id(fileId)
            .type(FileMetadataType.ROOM_AVATAR)
            .roomId(roomId.toString())
            .name(fileName)
            .originalSize(contentLength)
            .mimeType(mimeType)
            .userId(currentUser.getId()));
    roomRepository.update(
        room.pictureUpdatedAt(OffsetDateTime.ofInstant(clock.instant(), clock.getZone())));
    SystemEvent systemEvent =
        insertRoomSystemEvent(
            room,
            "ROOM_PICTURE_UPDATED",
            currentUser.getId(),
            systemEventContentFactory.roomPictureUpdated(currentUser.getId()),
            null);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomPictureChanged.create()
            .roomId(UUID.fromString(room.getId()))
            .updatedAt(room.getPictureUpdatedAt())
            .updatedBy(currentUser.getUUID())
            .systemEventId(systemEvent.getId())
            .systemEventCreatedAt(systemEvent.getCreatedAt())
            .type(EventType.ROOM_PICTURE_CHANGED)
            .sentDate(OffsetDateTime.now()));
  }

  @Override
  public void deleteRoomPicture(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, true);
    FileMetadata metadata =
        fileMetadataRepository
            .find(null, roomId.toString(), FileMetadataType.ROOM_AVATAR)
            .orElseThrow(
                () -> new NotFoundException(String.format("Room picture '%s' not found", roomId)));
    storagesService.deleteFile(metadata.getId(), metadata.getUserId());
    fileMetadataRepository.delete(metadata);
    roomRepository.update(room.pictureUpdatedAt(null));
    SystemEvent systemEvent =
        insertRoomSystemEvent(
            room,
            "ROOM_PICTURE_DELETED",
            currentUser.getId(),
            systemEventContentFactory.roomPictureDeleted(currentUser.getId()),
            null);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomPictureDeleted.create()
            .roomId(UUID.fromString(room.getId()))
            .deletedBy(currentUser.getUUID())
            .systemEventId(systemEvent.getId())
            .systemEventCreatedAt(systemEvent.getCreatedAt())
            .type(EventType.ROOM_PICTURE_DELETED)
            .sentDate(OffsetDateTime.now()));
  }

  @Override
  public void setMeetingIntoRoom(Room room, Meeting meeting) {
    roomRepository.update(room.meetingId(meeting.getId()));
  }
}
