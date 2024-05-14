// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

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
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
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
import com.zextras.carbonio.chats.model.RoomRankDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.Transactional;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    Map<String, RoomUserSettings> settingsMap;
    if (includeSettings) {
      settingsMap = roomUserSettingsRepository.getMapGroupedByUserId(currentUser.getId());
    } else {
      List<String> ids =
          rooms.stream()
              .filter(room -> RoomTypeDto.WORKSPACE.equals(room.getType()))
              .map(Room::getId)
              .collect(Collectors.toList());
      settingsMap =
          roomUserSettingsRepository.getMapByRoomsIdsAndUserIdGroupedByRoomsIds(
              ids, currentUser.getId());
    }
    return roomMapper.ent2dto(rooms, settingsMap, includeMembers, includeSettings);
  }

  @Override
  @Transactional
  public RoomDto getRoomById(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      List<String> ids = room.getChildren().stream().map(Room::getId).collect(Collectors.toList());
      ids.add(roomId.toString());
      return roomMapper.ent2dto(
          room,
          roomUserSettingsRepository.getMapByRoomsIdsAndUserIdGroupedByRoomsIds(
              ids, currentUser.getId()),
          true,
          true);
    } else if (RoomTypeDto.CHANNEL.equals(room.getType())) {
      room.subscriptions(
          roomRepository.getById(room.getParentId()).orElseThrow().getSubscriptions());
    }
    return roomMapper.ent2dto(
        room,
        roomUserSettingsRepository
            .getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
            .orElse(null),
        true,
        true);
  }

  @Override
  @Transactional
  public RoomDto createRoom(RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    createRoomValidation(roomCreationFields, currentUser);
    List<UUID> membersIds = new ArrayList<>(roomCreationFields.getMembersIds());
    if (!RoomTypeDto.CHANNEL.equals(roomCreationFields.getType())) {
      membersIds.add(UUID.fromString(currentUser.getId()));
    }

    UUID newRoomId = UUID.randomUUID();
    Room room =
        Room.create()
            .id(newRoomId.toString())
            .type(roomCreationFields.getType())
            .parentId(
                Optional.ofNullable(roomCreationFields.getParentId())
                    .map(UUID::toString)
                    .orElse(null));
    Optional.ofNullable(roomCreationFields.getName()).ifPresent(room::name);
    Optional.ofNullable(roomCreationFields.getDescription()).ifPresent(room::description);
    room = room.subscriptions(membersService.initRoomSubscriptions(membersIds, room, currentUser));

    if (RoomTypeDto.WORKSPACE.equals(roomCreationFields.getType())) {
      List<RoomUserSettings> roomUserSettings = new ArrayList<>();
      Map<String, RoomUserSettings> maxRanksMapByUsers =
          roomUserSettingsRepository.getWorkspaceMaxRanksMapGroupedByUsers(
              membersIds.stream().map(UUID::toString).collect(Collectors.toList()));
      for (UUID memberId : membersIds) {
        RoomUserSettings maxRank = maxRanksMapByUsers.get(memberId.toString());
        roomUserSettings.add(
            RoomUserSettings.create(room, memberId.toString())
                .rank((maxRank != null && maxRank.getRank() != null ? maxRank.getRank() : 0) + 1));
      }
      room.userSettings(roomUserSettings);
    } else if (RoomTypeDto.CHANNEL.equals(roomCreationFields.getType())) {
      room.rank(
          roomRepository
                  .getChannelMaxRanksByWorkspace(roomCreationFields.getParentId().toString())
                  .orElse(0)
              + 1);
    }

    room = roomRepository.insert(room);

    if (RoomTypeDto.CHANNEL.equals(room.getType())) {
      room.subscriptions(
          roomRepository.getById(room.getParentId()).orElseThrow().getSubscriptions());
    }
    if (!RoomTypeDto.WORKSPACE.equals(room.getType())) {
      messageDispatcher.createRoom(room, currentUser.getId());
      if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
        messageDispatcher.addUsersToContacts(
            room.getSubscriptions().get(0).getUserId(), room.getSubscriptions().get(1).getUserId());
      }
    }
    UUID finalId = UUID.fromString(room.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
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
    if (!RoomTypeDto.CHANNEL.equals(roomCreationFields.getType())
        && roomCreationFields.getParentId() != null) {
      throw new BadRequestException("Parent is allowed only for channel room");
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
      case WORKSPACE:
        Integer maxGroupMembers =
            capabilityService.getCapabilities(currentUser).getMaxGroupMembers();
        if (membersSet.size() < 2) {
          throw new BadRequestException("Too few members (required at least 3)");
        } else if (membersSet.size() > maxGroupMembers) {
          throw new BadRequestException(
              "Too much members (required less than " + maxGroupMembers + ")");
        }
        break;
      case CHANNEL:
        if (membersSet.size() > 0) {
          throw new BadRequestException("Channels don't admit members");
        }
        if (roomCreationFields.getParentId() == null) {
          throw new BadRequestException("Channel must have an assigned workspace");
        }
        Room room = getRoomEntityAndCheckUser(roomCreationFields.getParentId(), currentUser, true);
        if (!RoomTypeDto.WORKSPACE.equals(room.getType())) {
          throw new BadRequestException("Channel parent must be a workspace");
        }
        break;
    }

    roomCreationFields.getMembersIds().stream()
        .filter(memberId -> !userService.userExists(memberId, currentUser))
        .findFirst()
        .ifPresent(
            (uuid) -> {
              throw new NotFoundException(
                  String.format("User with identifier '%s' not found", uuid));
            });
  }

  @Override
  @Transactional
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
          room.getSubscriptions().stream()
              .map(Subscription::getUserId)
              .collect(Collectors.toList()),
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
      fileMetadataRepository.deleteById(room.getId());
      storagesService.deleteFile(room.getId(), currentUser.getId());
    }
    roomRepository.delete(roomId.toString());
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      room.getChildren()
          .forEach(
              child -> {
                try {
                  messageDispatcher.deleteRoom(child.getId(), currentUser.getId());
                } catch (Exception e) {
                  ChatsLogger.warn(
                      String.format(
                          "An error occurred while sending room deletion to the message dispatcher."
                              + " Room identifier: '%s'",
                          child.getId()));
                }
              });
    } else {
      messageDispatcher.deleteRoom(roomId.toString(), currentUser.getId());
    }
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        RoomDeleted.create().roomId(roomId));
  }

  @Override
  @Transactional
  public void muteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      throw new BadRequestException("Cannot mute a workspace");
    }
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
  @Transactional
  public void unmuteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, false);
    if (RoomTypeDto.WORKSPACE.equals(room.getType())) {
      throw new BadRequestException("Cannot unmute a workspace");
    }
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
  public List<UUID> getRoomsIds(UserPrincipal currentUser) {
    return roomRepository.getIdsByUserId(currentUser.getId()).stream()
        .map(UUID::fromString)
        .collect(Collectors.toList());
  }

  @Override
  public Room getRoomEntityAndCheckUser(
      UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
    Room room =
        roomRepository
            .getById(roomId.toString())
            .orElseThrow(() -> new NotFoundException(String.format("Room '%s'", roomId)));
    List<Subscription> subscriptions;
    if (RoomTypeDto.CHANNEL.equals(room.getType())) {
      subscriptions =
          roomRepository
              .getById(room.getParentId())
              .orElseThrow(() -> new InternalErrorException(String.format("Room '%s'", roomId)))
              .getSubscriptions();
    } else {
      subscriptions = room.getSubscriptions();
    }

    if (!currentUser.isSystemUser()) {
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
    }
    return room;
  }

  @Override
  public Optional<Room> getRoom(UUID roomId) {
    return roomRepository.getById(roomId.toString());
  }

  @Override
  @Transactional
  public FileContentAndMetadata getRoomPicture(UUID roomId, UserPrincipal currentUser) {
    getRoomEntityAndCheckUser(roomId, currentUser, false);
    FileMetadata metadata =
        fileMetadataRepository
            .getById(roomId.toString())
            .orElseThrow(
                () -> new NotFoundException(String.format("File with id '%s' not found", roomId)));
    return new FileContentAndMetadata(
      storagesService.getFileStreamById(metadata.getId(), metadata.getUserId()),
      metadata);
  }

  @Override
  @Transactional
  public void setRoomPicture(
      UUID roomId, InputStream image, String mimeType, Long contentLength, String fileName, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set to group type rooms");
    }
    Integer maxImageSizeKb =
        appConfig
            .get(Integer.class, ConfigName.MAX_ROOM_IMAGE_SIZE_IN_KB)
            .orElse(CONFIGURATIONS_DEFAULT_VALUES.MAX_ROOM_IMAGE_SIZE_IN_KB);
    if (contentLength > maxImageSizeKb * 1024) {
      throw new BadRequestException(
          String.format(
              "The size of room picture exceeds the maximum value of %d kB", maxImageSizeKb));
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The room picture must be an image");
    }
    Optional<FileMetadata> oldMetadata = fileMetadataRepository.getById(roomId.toString());
    Optional<String> oldUser = oldMetadata.map(FileMetadata::getUserId);
    FileMetadata metadata =
        oldMetadata
            .orElseGet(
                () ->
                    FileMetadata.create()
                        .id(roomId.toString())
                        .type(FileMetadataType.ROOM_AVATAR)
                        .roomId(roomId.toString()))
            .name(fileName)
            .originalSize(contentLength)
            .mimeType(mimeType)
            .userId(currentUser.getId());
    room.pictureUpdatedAt(OffsetDateTime.ofInstant(clock.instant(), clock.getZone()));
    fileMetadataRepository.save(metadata);
    roomRepository.update(room);
    if (oldUser.isPresent()) {
      try {
        storagesService.deleteFile(metadata.getId(), oldUser.get());
      } catch (Exception e) {
        ChatsLogger.warn("Could not delete older group profile picture: " + e.getMessage());
      }
    }
    storagesService.saveFile(image, metadata, currentUser.getId());
    messageDispatcher.updateRoomPicture(
        room.getId(), currentUser.getId(), metadata.getId(), metadata.getName());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        RoomPictureChanged.create()
            .roomId(UUID.fromString(room.getId()))
            .updatedAt(room.getPictureUpdatedAt()));
  }

  @Override
  @Transactional
  public void deleteRoomPicture(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomEntityAndCheckUser(roomId, currentUser, true);
    FileMetadata metadata =
        fileMetadataRepository
            .getById(roomId.toString())
            .orElseThrow(
                () -> new NotFoundException(String.format("File with id '%s' not found", roomId)));
    fileMetadataRepository.delete(metadata);
    roomRepository.update(room.pictureUpdatedAt(null));
    storagesService.deleteFile(metadata.getId(), metadata.getUserId());
    messageDispatcher.deleteRoomPicture(room.getId(), currentUser.getId());
    eventDispatcher.sendToUserExchange(
        room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
        RoomPictureDeleted.create().roomId(UUID.fromString(room.getId())));
  }

  @Override
  @Transactional
  public void updateWorkspacesRank(List<RoomRankDto> roomRankDto, UserPrincipal currentUser) {
    List<RoomRankDto> roomRankList = new ArrayList<>(roomRankDto);
    if (roomRankList.size()
        > roomRankList.stream().map(RoomRankDto::getRoomId).collect(Collectors.toSet()).size()) {
      throw new BadRequestException("Rooms cannot be duplicated");
    }
    roomRankList.sort(Comparator.comparing(RoomRankDto::getRank));
    for (int i = 0; i < roomRankList.size(); i++) {
      if (roomRankList.get(i).getRank() != i + 1) {
        throw new BadRequestException("Ranks must be progressive number that starts with 1");
      }
    }
    Map<String, RoomUserSettings> userWorkspaces =
        roomUserSettingsRepository.getWorkspaceMapGroupedByRoomId(currentUser.getId());
    if (roomRankList.size() != userWorkspaces.size()) {
      throw new BadRequestException(
          String.format(
              "Too %s elements compared to user workspaces",
              roomRankList.size() < userWorkspaces.size() ? "few" : "many"));
    }
    roomRankList.forEach(
        roomRank ->
            Optional.ofNullable(userWorkspaces.get(roomRank.getRoomId().toString()))
                .ifPresentOrElse(
                    userSettings -> {
                      if (!userSettings.getRank().equals(roomRank.getRank())) {
                        userSettings.rank(roomRank.getRank());
                      }
                    },
                    () -> {
                      throw new BadRequestException(
                          String.format(
                              "There isn't a workspace with id '%s' for the user id '%s'",
                              roomRank.getRoomId(), currentUser.getId()));
                    }));
    roomUserSettingsRepository.save(userWorkspaces.values());
  }

  @Override
  public void updateChannelsRank(
      UUID workspaceId, List<RoomRankDto> roomRankDto, UserPrincipal currentUser) {
    List<RoomRankDto> roomRankList = new ArrayList<>(roomRankDto);
    roomRankList.sort(Comparator.comparing(RoomRankDto::getRank));
    for (int i = 0; i < roomRankList.size(); i++) {
      if (roomRankList.get(i).getRank() != i + 1) {
        throw new BadRequestException("Ranks must be progressive number that starts with 1");
      }
    }

    Map<String, Integer> roomRankMap =
        roomRankDto.stream()
            .collect(
                Collectors.toMap(
                    roomRank -> roomRank.getRoomId().toString(),
                    RoomRankDto::getRank,
                    (existing, replacement) -> existing));
    if (roomRankMap.size() != roomRankDto.size()) {
      throw new BadRequestException("Channels cannot be duplicated");
    }
    Room workspace = getRoomEntityAndCheckUser(workspaceId, currentUser, true);
    if (roomRankDto.size() != workspace.getChildren().size()) {
      throw new BadRequestException(
          String.format(
              "Too %s elements compared to workspace channels",
              roomRankDto.size() < workspace.getChildren().size() ? "few" : "many"));
    }

    workspace
        .getChildren()
        .forEach(
            child ->
                Optional.ofNullable(roomRankMap.get(child.getId()))
                    .ifPresentOrElse(
                        child::rank,
                        () -> {
                          throw new BadRequestException(
                              String.format(
                                  "Channel '%s' is not a child of workspace '%s'",
                                  child.getId(), workspaceId));
                        }));

    roomRepository.update(workspace);
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
