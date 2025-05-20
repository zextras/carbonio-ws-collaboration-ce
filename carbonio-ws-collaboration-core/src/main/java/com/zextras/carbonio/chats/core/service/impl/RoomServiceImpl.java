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
import com.zextras.carbonio.chats.model.MemberDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import jakarta.annotation.Nullable;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
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
  private final UserService userService;
  private final MembersService membersService;
  private final MeetingService meetingService;
  private final StoragesService storagesService;
  private final AttachmentService attachmentService;
  private final CapabilityService capabilityService;
  private final EventDispatcher eventDispatcher;
  private final MessageDispatcher messageDispatcher;
  private final RoomMapper roomMapper;
  private final Clock clock;
  private final AppConfig appConfig;

  @Inject
  public RoomServiceImpl(
      RoomRepository roomRepository,
      RoomUserSettingsRepository roomUserSettingsRepository,
      FileMetadataRepository fileMetadataRepository,
      UserService userService,
      MembersService membersService,
      MeetingService meetingService,
      StoragesService storagesService,
      AttachmentService attachmentService,
      CapabilityService capabilityService,
      EventDispatcher eventDispatcher,
      MessageDispatcher messageDispatcher,
      RoomMapper roomMapper,
      Clock clock,
      AppConfig appConfig) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.fileMetadataRepository = fileMetadataRepository;
    this.userService = userService;
    this.membersService = membersService;
    this.meetingService = meetingService;
    this.storagesService = storagesService;
    this.attachmentService = attachmentService;
    this.capabilityService = capabilityService;
    this.eventDispatcher = eventDispatcher;
    this.messageDispatcher = messageDispatcher;
    this.roomMapper = roomMapper;
    this.clock = clock;
    this.appConfig = appConfig;
  }

  @Override
  public List<RoomDto> getRooms(
      @Nullable List<RoomExtraFieldDto> extraFields, UserPrincipal currentUser) {
    boolean includeMembers = false;
    boolean includeSettings = false;
    if (extraFields != null) {
      includeMembers = extraFields.contains(RoomExtraFieldDto.MEMBERS);
      includeSettings = extraFields.contains(RoomExtraFieldDto.SETTINGS);
    }
    List<Room> rooms = roomRepository.getByUserId(currentUser.getId(), includeMembers);
    Map<String, RoomUserSettings> settingsMap =
        includeSettings
            ? roomUserSettingsRepository.getMapGroupedByUserId(currentUser.getId())
            : null;
    return roomMapper.ent2dto(rooms, settingsMap, includeMembers, includeSettings);
  }

  @Override
  public RoomDto getRoomById(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, false);
    return roomMapper.ent2dto(
        room,
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
    Room room = initializeRoom(roomCreationFields);

    createRoom(room, currentUser, members);

    configureSubscriptions(room, members);
    room = roomRepository.insert(room);

    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).toList(),
        RoomCreated.create().roomId(UUID.fromString(room.getId())));

    return mapRoomToDto(room, currentUser);
  }

  private List<MemberDto> prepareRoomMembers(
      RoomCreationFieldsDto roomCreationFields, UUID currentUserUUID) {
    List<MemberDto> members = new ArrayList<>(roomCreationFields.getMembers());
    members.add(0, MemberDto.create().userId(currentUserUUID).owner(true));
    return members;
  }

  private Room initializeRoom(RoomCreationFieldsDto roomCreationFields) {
    Room room = Room.create().id(UUID.randomUUID().toString()).type(roomCreationFields.getType());

    Optional.ofNullable(roomCreationFields.getName()).ifPresent(room::name);
    Optional.ofNullable(roomCreationFields.getDescription()).ifPresent(room::description);

    return room;
  }

  private void createRoom(Room room, UserPrincipal currentUser, List<MemberDto> members) {
    List<String> memberIds =
        members.stream().map(MemberDto::getUserId).map(UUID::toString).toList();
    messageDispatcher.createRoom(
        room.getId(),
        currentUser.getId(),
        memberIds.stream().filter(member -> !member.equals(currentUser.getId())).toList());

    if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
      messageDispatcher.addUsersToContacts(
          members.get(0).getUserId().toString(), members.get(1).getUserId().toString());
    }
  }

  private void configureSubscriptions(Room room, List<MemberDto> members) {
    room.subscriptions(membersService.initRoomSubscriptions(members, room));
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

    switch (roomCreationFields.getType()) {
      case ONE_TO_ONE -> validateOneToOneRoom(currentUser, memberIds);
      case GROUP -> validateGroupRoom(currentUser, memberIds);
      case TEMPORARY -> {
        // No validation required for TEMPORARY
      }
      default -> throw new BadRequestException("Unsupported room type");
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
    if (memberIds.size() != 1) {
      throw new BadRequestException("Only 2 users can participate in a one-to-one room");
    }
    if (roomRepository
        .getOneToOneByAllUserIds(currentUser.getId(), memberIds.get(0).toString())
        .isPresent()) {
      throw new ConflictException("The one-to-one room already exists for these users");
    }
  }

  private void validateGroupRoom(UserPrincipal currentUser, List<UUID> memberIds) {
    Integer maxGroupMembers = capabilityService.getCapabilities(currentUser).getMaxGroupMembers();
    if (memberIds.size() < 2) {
      throw new BadRequestException("Too few members (required at least 2)");
    } else if (memberIds.size() > maxGroupMembers) {
      throw new BadRequestException(
          "Too many members (required less than " + (maxGroupMembers - 1) + ")");
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
    boolean isRoomUpdated = updateRoomDetails(room, updateRoomRequestDto, currentUser.getId());

    if (isRoomUpdated) {
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

  private boolean updateRoomDetails(
      Room room, RoomEditableFieldsDto updateRoomRequestDto, String currentUserId) {
    boolean changed = false;

    if (isNameChanged(room, updateRoomRequestDto)) {
      changed = true;
      room.name(updateRoomRequestDto.getName());
      messageDispatcher.updateRoomName(room.getId(), currentUserId, updateRoomRequestDto.getName());
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
      messageDispatcher.updateRoomDescription(
          room.getId(), currentUserId, updateRoomRequestDto.getDescription());
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
        RoomDeleted.create().roomId(roomId));
  }

  private void deleteRoom(Room room) {
    room.getSubscriptions().stream()
        .map(Subscription::getUserId)
        .forEach(userId -> messageDispatcher.removeRoomMember(room.getId(), userId));
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
      eventDispatcher.sendToUserExchange(currentUser.getId(), RoomMuted.create().roomId(roomId));
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
                    currentUser.getId(), RoomUnmuted.create().roomId(roomId));
              }
            });
  }

  @Override
  public OffsetDateTime clearRoomHistory(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndValidateUser(roomId, currentUser, false);
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
  public Room getRoomAndValidateUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
    Room room =
        roomRepository
            .getById(roomId.toString())
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s'", roomId)));

    Subscription member =
        room.getSubscriptions().stream()
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
    Room room = getRoomAndValidateUser(roomId, currentUser, true);
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
    Room room = getRoomAndValidateUser(roomId, currentUser, false);
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
