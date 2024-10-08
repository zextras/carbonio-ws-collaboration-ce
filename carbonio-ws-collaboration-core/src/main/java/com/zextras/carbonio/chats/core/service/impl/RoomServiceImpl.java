// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomCreated;
import com.zextras.carbonio.chats.core.data.event.RoomDeleted;
import com.zextras.carbonio.chats.core.data.event.RoomHistoryCleared;
import com.zextras.carbonio.chats.core.data.event.RoomMuted;
import com.zextras.carbonio.chats.core.data.event.RoomPictureChanged;
import com.zextras.carbonio.chats.core.data.event.RoomPictureDeleted;
import com.zextras.carbonio.chats.core.data.event.RoomUnmuted;
import com.zextras.carbonio.chats.core.data.event.RoomUpdated;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ConflictException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.CapabilityService;
import com.zextras.carbonio.chats.core.service.MeetingService;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ForwardMessageDto;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Singleton
public class RoomServiceImpl implements RoomService {

  private static final OffsetDateTime MUTED_TO_INFINITY =
      OffsetDateTime.parse("0001-01-01T00:00:00Z");

  private final RoomRepository roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final RoomMapper roomMapper;
  private final EventDispatcher eventDispatcher;
  private final MessageDispatcher messageDispatcher;
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final FileMetadataRepository fileMetadataRepository;
  private final StoragesService storagesService;
  private final AttachmentService attachmentService;
  private final Clock clock;
  private final AppConfig appConfig;
  private final CapabilityService capabilityService;

  @Inject
  public RoomServiceImpl(
      RoomRepository roomRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      RoomMapper roomMapper,
      EventDispatcher eventDispatcher,
      MessageDispatcher messageDispatcher,
      UserService userService,
      MembersService membersService,
      MeetingService meetingService,
      FileMetadataRepository fileMetadataRepository,
      StoragesService storagesService,
      AttachmentService attachmentService,
      Clock clock,
      AppConfig appConfig,
      CapabilityService capabilityService) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.roomMapper = roomMapper;
    this.eventDispatcher = eventDispatcher;
    this.messageDispatcher = messageDispatcher;
    this.userService = userService;
    this.membersService = membersService;
    this.meetingService = meetingService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagesService = storagesService;
    this.attachmentService = attachmentService;
    this.clock = clock;
    this.appConfig = appConfig;
    this.capabilityService = capabilityService;
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
        getRoomEntityAndCheckUser(roomId, currentUser, false),
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElse(null),
        true,
        true);
  }

  @Override
  public RoomDto createRoom(RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    createRoomValidation(roomCreationFields, currentUser);
    List<UUID> membersIds = new ArrayList<>(roomCreationFields.getMembersIds());
    membersIds.add(UUID.fromString(currentUser.getId()));

    UUID newRoomId = UUID.randomUUID();
    Room room = Room.create().id(newRoomId.toString()).type(roomCreationFields.getType());
    Optional.ofNullable(roomCreationFields.getName()).ifPresent(room::name);
    Optional.ofNullable(roomCreationFields.getDescription()).ifPresent(room::description);
    room = room.subscriptions(membersService.initRoomSubscriptions(membersIds, room, currentUser));
    room = roomRepository.insert(room);

    messageDispatcher.createRoom(room, currentUser.getId());
    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      messageDispatcher.addUsersToContacts(
          room.getSubscriptions().get(0).getUserId(), room.getSubscriptions().get(1).getUserId());
    }
    UUID finalId = UUID.fromString(room.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomCreated.create().roomId(finalId));
    return roomMapper.ent2dto(
        room,
        room.getUserSettings().stream()
            .filter(userSettings -> userSettings.getUserId().equals(currentUser.getId()))
            .findAny()
            .orElse(null),
        true,
        true);
  }

  private void createRoomValidation(
      RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    Set<UUID> membersSet = new HashSet<>(roomCreationFields.getMembersIds());

    if (roomCreationFields.getMembersIds().size() != membersSet.size()) {
      throw new BadRequestException("Members cannot be duplicated");
    }
    if (roomCreationFields.getMembersIds().stream()
        .anyMatch(memberId -> memberId.toString().equals(currentUser.getId()))) {
      throw new BadRequestException("Requester can't be invited to the room");
    }
    switch (roomCreationFields.getType()) {
      case ONE_TO_ONE:
        if (membersSet.size() != 1) {
          throw new BadRequestException("Only 2 users can participate to a one-to-one room");
        }
        if (roomRepository
            .getOneToOneByAllUserIds(
                currentUser.getId(), roomCreationFields.getMembersIds().get(0).toString())
            .isPresent()) {
          throw new ConflictException("The one to one room already exists for these users");
        }
        break;
      case GROUP:
        Integer maxGroupMembers =
            capabilityService.getCapabilities(currentUser).getMaxGroupMembers();
        if (membersSet.size() < 2) {
          throw new BadRequestException("Too few members (required at least 3)");
        } else if (membersSet.size() > maxGroupMembers) {
          throw new BadRequestException(
              "Too much members (required less than " + maxGroupMembers + ")");
        }
        break;
    }

    roomCreationFields.getMembersIds().stream()
        .filter(memberId -> !userService.userExists(memberId, currentUser))
        .findFirst()
        .ifPresent(
            uuid -> {
              throw new NotFoundException(
                  String.format("User with identifier '%s' not found", uuid));
            });
  }

  @Override
  public RoomDto updateRoom(
      UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    boolean changed = false;
    if (updateRoomRequestDto.getName() != null
        && !room.getName().equals(updateRoomRequestDto.getName())) {
      changed = true;
      room.name(updateRoomRequestDto.getName());
      messageDispatcher.updateRoomName(
          room.getId(), currentUser.getId(), updateRoomRequestDto.getName());
    }
    if (updateRoomRequestDto.getDescription() != null
        && !room.getDescription().equals(updateRoomRequestDto.getDescription())) {
      changed = true;
      room.description(updateRoomRequestDto.getDescription());
      messageDispatcher.updateRoomDescription(
          room.getId(), currentUser.getId(), updateRoomRequestDto.getDescription());
    }
    if (changed) {
      roomRepository.update(room);
      eventDispatcher.sendToUserExchange(
          room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
          RoomUpdated.create()
              .roomId(roomId)
              .name(room.getName())
              .description(room.getDescription()));
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

  @Override
  @Transactional
  public void deleteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    if (room.getMeetingId() != null) {
      meetingService
          .getMeetingEntity(UUID.fromString(room.getMeetingId()))
          .ifPresent(meeting -> meetingService.deleteMeeting(meeting, room, currentUser.getUUID()));
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
    roomRepository.delete(roomId.toString());
    messageDispatcher.deleteRoom(roomId.toString(), currentUser.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomDeleted.create().roomId(roomId));
  }

  @Override
  public void muteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    RoomUserSettings settings =
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElseGet(() -> RoomUserSettings.create(room, currentUser.getId()));
    if (settings.getMutedUntil() == null) {
      roomUserSettingsRepository.save(settings.mutedUntil(MUTED_TO_INFINITY));
      eventDispatcher.sendToUserExchange(currentUser.getId(), RoomMuted.create().roomId(roomId));
    }
  }

  @Override
  public void unmuteRoom(UUID roomId, UserPrincipal currentUser) {
    getRoomEntityAndCheckUser(roomId, currentUser, false);
    roomUserSettingsRepository
        .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
        .ifPresent(
            settings -> {
              if (settings.getMutedUntil() != null) {
                roomUserSettingsRepository.save(settings.mutedUntil(null));
                eventDispatcher.sendToUserExchange(
                    currentUser.getId(), RoomUnmuted.create().roomId(roomId));
              }
            });
  }

  @Override
  public OffsetDateTime clearRoomHistory(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    RoomUserSettings settings =
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElseGet(() -> RoomUserSettings.create(room, currentUser.getId()));
    settings =
        roomUserSettingsRepository.save(
            settings.clearedAt(OffsetDateTime.ofInstant(clock.instant(), clock.getZone())));
    eventDispatcher.sendToUserExchange(
        currentUser.getId(),
        RoomHistoryCleared.create().roomId(roomId).clearedAt(settings.getClearedAt()));
    return settings.getClearedAt();
  }

  @Override
  public List<UUID> getRoomsIds(UserPrincipal currentUser) {
    return roomRepository.getIdsByUserId(currentUser.getId()).stream()
        .map(UUID::fromString)
        .toList();
  }

  @Override
  public Room getRoomEntityAndCheckUser(
      UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
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
    getRoomEntityAndCheckUser(roomId, currentUser, false);
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
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set for groups");
    }
    Integer maxImageSizeKb =
        appConfig
            .get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB)
            .orElse(CONFIGURATIONS_DEFAULT_VALUES.MAX_ROOM_IMAGE_SIZE_IN_KB);
    if (contentLength > maxImageSizeKb * 1024) {
      throw new BadRequestException(
          String.format(
              "The size of the room picture exceeds the maximum value of %d kB", maxImageSizeKb));
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
    messageDispatcher.updateRoomPicture(room.getId(), currentUser.getId(), fileId, fileName);
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomPictureChanged.create()
            .roomId(UUID.fromString(room.getId()))
            .updatedAt(room.getPictureUpdatedAt()));
  }

  @Override
  public void deleteRoomPicture(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    FileMetadata metadata =
        fileMetadataRepository
            .find(null, roomId.toString(), FileMetadataType.ROOM_AVATAR)
            .orElseThrow(
                () -> new NotFoundException(String.format("Room picture '%s' not found", roomId)));
    storagesService.deleteFile(metadata.getId(), metadata.getUserId());
    fileMetadataRepository.delete(metadata);
    roomRepository.update(room.pictureUpdatedAt(null));
    messageDispatcher.deleteRoomPicture(room.getId(), currentUser.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomPictureDeleted.create().roomId(UUID.fromString(room.getId())));
  }

  @Override
  public void setMeetingIntoRoom(Room room, Meeting meeting) {
    roomRepository.update(room.meetingId(meeting.getId()));
  }

  @Override
  public void forwardMessages(
      UUID roomId, List<ForwardMessageDto> forwardMessageDto, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    forwardMessageDto.forEach(
        messageToForward ->
            messageDispatcher.forwardMessage(
                roomId.toString(),
                currentUser.getId(),
                messageToForward,
                messageDispatcher
                    .getAttachmentIdFromMessage(messageToForward.getOriginalMessage())
                    .map(
                        attachmentId ->
                            attachmentService.copyAttachment(
                                room, UUID.fromString(attachmentId), currentUser))
                    .orElse(null)));
  }
}
