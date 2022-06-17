// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.builder.HashDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.RoomUserSettings;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomDeletedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomHashResetEvent;
import com.zextras.carbonio.chats.core.data.event.RoomPictureChangedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomUpdatedEvent;
import com.zextras.carbonio.chats.core.data.event.UserMutedEvent;
import com.zextras.carbonio.chats.core.data.event.UserUnmutedEvent;
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
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.service.UserService;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomExtraFieldDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomRankDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RoomServiceImpl implements RoomService {

  private static final OffsetDateTime MUTED_TO_INFINITY = OffsetDateTime.parse("0001-01-01T00:00:00Z");

  private final RoomRepository             roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final RoomMapper                 roomMapper;
  private final EventDispatcher            eventDispatcher;
  private final MessageDispatcher          messageDispatcher;
  private final UserService                userService;
  private final MembersService             membersService;
  private final FileMetadataRepository     fileMetadataRepository;
  private final StoragesService            storagesService;
  private final Clock                      clock;

  @Inject
  public RoomServiceImpl(
    RoomRepository roomRepository, RoomUserSettingsRepository roomUserSettingsRepository, RoomMapper roomMapper,
    EventDispatcher eventDispatcher,
    MessageDispatcher messageDispatcher,
    UserService userService,
    MembersService membersService,
    FileMetadataRepository fileMetadataRepository,
    StoragesService storagesService,
    Clock clock
  ) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.roomMapper = roomMapper;
    this.eventDispatcher = eventDispatcher;
    this.messageDispatcher = messageDispatcher;
    this.userService = userService;
    this.membersService = membersService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.storagesService = storagesService;
    this.clock = clock;
  }

  @Override
  public List<RoomDto> getRooms(
    @Nullable List<RoomExtraFieldDto> extraFields, UserPrincipal currentUser
  ) {
    boolean includeMembers = false, includeSettings = false;
    if (extraFields != null) {
      includeMembers = extraFields.contains(RoomExtraFieldDto.MEMBERS);
      includeSettings = extraFields.contains(RoomExtraFieldDto.SETTINGS);
    }
    List<Room> rooms = roomRepository.getByUserId(currentUser.getId(), includeMembers);
    Map<String, RoomUserSettings> settingsMap = null;
    if (includeSettings) {
      settingsMap = roomUserSettingsRepository.getByUserId(currentUser.getId()).stream()
        .collect(Collectors.toMap(s -> s.getId().getRoomId(), Function.identity()));
    }
    return roomMapper.ent2roomDto(rooms, includeMembers, settingsMap);
  }

  @Override
  @Transactional
  public RoomInfoDto getRoomById(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
    roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
      .ifPresent(settings -> room.userSettings(Collections.singletonList(settings)));
    return roomMapper.ent2roomInfoDto(room);
  }

  @Override
  @Transactional
  public RoomInfoDto createRoom(RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
    createRoomValidation(roomCreationFields, currentUser);

    List<UUID> membersIds = new ArrayList<>(roomCreationFields.getMembersIds());
    membersIds.add(UUID.fromString(currentUser.getId()));

    UUID newRoomId = UUID.randomUUID();
    Room room = Room.create()
      .id(newRoomId.toString())
      .name(roomCreationFields.getName())
      .description(roomCreationFields.getDescription())
      .hash(Utils.encodeUuidHash(newRoomId.toString()))
      .type(roomCreationFields.getType());
    room = room.subscriptions(
      membersService.initRoomSubscriptions(membersIds, room, currentUser));

    if (RoomTypeDto.WORKSPACE.equals(roomCreationFields.getType())) {
      List<RoomUserSettings> roomUserSettings = new ArrayList<>();
      Map<String, RoomUserSettings> maxRanksMapByUsers = roomUserSettingsRepository.getWorkspaceMaxRanksMapByUsers(
        membersIds.stream().map(UUID::toString).collect(Collectors.toList())
      );
      for (UUID memberId : membersIds) {
        RoomUserSettings maxRank = maxRanksMapByUsers.get(memberId.toString());
        roomUserSettings.add(
          RoomUserSettings.create(room, memberId.toString())
            .rank((maxRank != null && maxRank.getRank() != null ? maxRank.getRank() : 0) + 1));
      }
      room.userSettings(roomUserSettings);
    }

    room = roomRepository.insert(room);

    if (!RoomTypeDto.WORKSPACE.equals(room.getType())) {
      messageDispatcher.createRoom(room, currentUser.getId());
      if (RoomTypeDto.ONE_TO_ONE.equals(room.getType())) {
        messageDispatcher.addUsersToContacts(
          room.getSubscriptions().get(0).getUserId(),
          room.getSubscriptions().get(1).getUserId());
      }
    }
    UUID finalId = UUID.fromString(room.getId());
    room.getSubscriptions().forEach(member ->
      eventDispatcher.sendToQueue(currentUser.getUUID(), member.getUserId(),
        RoomCreatedEvent.create(finalId).from(currentUser.getUUID())
      )
    );

    return roomMapper.ent2roomInfoDto(room, currentUser.getUUID());

  }

  private void createRoomValidation(RoomCreationFieldsDto roomCreationFields, UserPrincipal currentUser) {
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
        if (roomRepository.getOneToOneByAllUserIds(currentUser.getId(),
          roomCreationFields.getMembersIds().get(0).toString()).isPresent()) {
          throw new ConflictException("The one to one room already exists for these users");
        }
        break;
      case GROUP:
      case WORKSPACE:
        if (membersSet.size() < 2) {
          throw new BadRequestException("Too few members (required at least 3)");
        }
    }

    roomCreationFields.getMembersIds().stream()
      .filter(memberId -> !userService.userExists(memberId, currentUser))
      .findFirst()
      .ifPresent((uuid) -> {
        throw new NotFoundException(String.format("User with identifier '%s' not found", uuid));
      });
  }

  @Override
  @Transactional
  public RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    boolean changed = false;
    if (!room.getName().equals(updateRoomRequestDto.getName())) {
      changed = true;
      room.name(updateRoomRequestDto.getName());
      messageDispatcher.updateRoomName(room.getId(), currentUser.getId(), updateRoomRequestDto.getName());
    }
    if (!room.getDescription().equals(updateRoomRequestDto.getDescription())) {
      changed = true;
      room.description(updateRoomRequestDto.getDescription());
      messageDispatcher.updateRoomDescription(room.getId(), currentUser.getId(), updateRoomRequestDto.getDescription());
    }
    if (changed) {
      roomRepository.update(room);
      eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
        RoomUpdatedEvent.create(roomId).from(currentUser.getUUID()));
    }
    return roomMapper.ent2roomDto(room, false);
  }

  @Override
  @Transactional
  public void deleteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    roomRepository.delete(roomId.toString());
    if (!RoomTypeDto.WORKSPACE.equals(room.getType())) {
      messageDispatcher.deleteRoom(roomId.toString(), currentUser.getId());
    }
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(), new RoomDeletedEvent(roomId));
  }

  @Override
  public HashDto resetRoomHash(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    String hash = Utils.encodeUuidHash(roomId.toString());
    room.hash(hash);
    roomRepository.update(room);
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
      RoomHashResetEvent.create(roomId).hash(hash));
    return HashDtoBuilder.create().hash(hash).build();
  }

  @Override
  @Transactional
  public void muteRoom(UUID roomId, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
    RoomUserSettings settings = roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
      .orElseGet(() -> RoomUserSettings.create(room, currentUser.getId()));
    if (settings.getMutedUntil() == null) {
      roomUserSettingsRepository.save(settings.mutedUntil(MUTED_TO_INFINITY));
      eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
        UserMutedEvent.create(roomId).memberId(currentUser.getUUID()));
    }
  }

  @Override
  @Transactional
  public void unmuteRoom(UUID roomId, UserPrincipal currentUser) {
    getRoomAndCheckUser(roomId, currentUser, false);
    roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), currentUser.getId()).ifPresent(
      settings -> {
        if (settings.getMutedUntil() != null) {
          roomUserSettingsRepository.save(settings.mutedUntil(null));
          eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
            UserUnmutedEvent.create(roomId).memberId(currentUser.getUUID()));
        }
      });
  }

  private String generateRoomPassword() {
    // TODO: 22/11/21
    return null;
  }

  @Override
  public Room getRoomAndCheckUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
      new NotFoundException(String.format("Room '%s'", roomId)));
    if (!currentUser.isSystemUser()) {
      Subscription member = room.getSubscriptions().stream()
        .filter(subscription -> subscription.getUserId().equals(currentUser.getId()))
        .findAny()
        .orElseThrow(() -> new ForbiddenException(
          String.format("User '%s' is not a member of room '%s'", currentUser.getId(), roomId)));
      if (mustBeOwner && !member.isOwner()) {
        throw new ForbiddenException(
          String.format("User '%s' is not an owner of room '%s'", currentUser.getId(), roomId));
      }
    }
    return room;
  }

  @Override
  @Transactional
  public FileContentAndMetadata getRoomPicture(UUID roomId, UserPrincipal currentUser) {
    getRoomAndCheckUser(roomId, currentUser, false);
    FileMetadata metadata = fileMetadataRepository.getById(roomId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", roomId)));
    File file = storagesService.getFileById(metadata.getId(), metadata.getUserId());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  @Transactional
  public void setRoomPicture(UUID roomId, File image, String mimeType, String fileName, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set to group type rooms");
    }
    if (image.length() > ChatsConstant.MAX_ROOM_IMAGE_SIZE_IN_KB * 1024) {
      throw new BadRequestException(
        String.format("The room picture cannot be greater than %d KB", ChatsConstant.MAX_ROOM_IMAGE_SIZE_IN_KB));
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The room picture must be an image");
    }
    Optional<FileMetadata> oldMetadata = fileMetadataRepository.getById(roomId.toString());
    Optional<String> oldUser = oldMetadata.map(FileMetadata::getUserId);
    FileMetadata metadata = oldMetadata.orElseGet(() -> FileMetadata.create()
        .id(roomId.toString())
        .type(FileMetadataType.ROOM_AVATAR)
        .roomId(roomId.toString())
      )
      .name(fileName)
      .originalSize(image.length())
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
    messageDispatcher.updateRoomPictures(room.getId(), currentUser.getId(), metadata.getId(), metadata.getName());
    eventDispatcher.sendToTopic(currentUser.getUUID(), room.getId(),
      RoomPictureChangedEvent.create(UUID.fromString(room.getId())).from(currentUser.getUUID()));
  }

  @Override
  @Transactional
  public void updateWorkspacesRank(List<RoomRankDto> roomRankDto, UserPrincipal currentUser) {
    List<RoomRankDto> roomRankList = new ArrayList<>(roomRankDto);
    if (roomRankList.size() > roomRankList.stream().map(RoomRankDto::getRoomId).collect(Collectors.toSet()).size()) {
      throw new BadRequestException("Rooms cannot be duplicated");
    }
    roomRankList.sort(Comparator.comparing(RoomRankDto::getRank));
    for (int i = 0; i < roomRankList.size(); i++) {
      if (roomRankList.get(i).getRank() != i + 1) {
        throw new BadRequestException("Ranks must be progressive number that starts with 1");
      }
    }
    Map<String, RoomUserSettings> userWorkspaces = roomUserSettingsRepository.getWorkspaceMapByRoomId(
      currentUser.getId());
    if (roomRankList.size() != userWorkspaces.size()) {
      throw new ForbiddenException(String.format("Too %s elements compared to user workspaces",
        roomRankList.size() < userWorkspaces.size() ? "few" : "many"));
    }
    roomRankList.forEach(roomRank ->
      Optional.ofNullable(userWorkspaces.get(roomRank.getRoomId().toString()))
        .ifPresentOrElse(userSettings -> {
            if (!userSettings.getRank().equals(roomRank.getRank())) {
              userSettings.rank(roomRank.getRank());
              roomUserSettingsRepository.save(userSettings);
            }
          },
          () -> {
            throw new ForbiddenException(String.format(
              "There isn't a workspace with id '%s' for the user id '%s'", roomRank.getRoomId(), currentUser.getId()));
          }));
  }
}
