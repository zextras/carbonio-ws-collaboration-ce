// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.builder.HashDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.RoomCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomDeletedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomHashResetEvent;
import com.zextras.carbonio.chats.core.data.event.RoomPictureChangedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomUpdatedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
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
import com.zextras.carbonio.chats.core.utils.Messages;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.HashDto;
import com.zextras.carbonio.chats.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.model.RoomDto;
import com.zextras.carbonio.chats.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.model.RoomInfoDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RoomServiceImpl implements RoomService {

  private final RoomRepository             roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final RoomMapper                 roomMapper;
  private final EventDispatcher            eventDispatcher;
  private final MessageDispatcher          messageDispatcher;
  private final UserService                userService;
  private final MembersService             membersService;
  private final FileMetadataRepository     fileMetadataRepository;
  private final StoragesService            storagesService;

  @Inject
  public RoomServiceImpl(
    RoomRepository roomRepository, RoomUserSettingsRepository roomUserSettingsRepository, RoomMapper roomMapper,
    EventDispatcher eventDispatcher,
    MessageDispatcher messageDispatcher,
    UserService userService,
    MembersService membersService,
    FileMetadataRepository fileMetadataRepository,
    StoragesService storagesService
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
  }


  @Override
  public List<RoomDto> getRooms(UserPrincipal currentUser) {
    List<Room> rooms = roomRepository.getByUserId(currentUser.getId());
    return roomMapper.ent2roomDto(rooms);
  }

  @Override
  @Transactional
  public RoomInfoDto getRoomById(UUID roomId, UserPrincipal currentUser) {
    // get the room
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
    // get current user settings for the room
    roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), currentUser.getId())
      .ifPresent(settings -> room.userSettings(Collections.singletonList(settings)));
    return roomMapper.ent2roomInfoDto(room, currentUser.getId());
  }

  @Override
  @Transactional
  public RoomInfoDto createRoom(RoomCreationFieldsDto insertRoomRequestDto, UserPrincipal currentUser) {
    // check if invited members list has duplicates
    if (insertRoomRequestDto.getMembersIds().size() != new HashSet<>(insertRoomRequestDto.getMembersIds()).size()) {
      throw new BadRequestException("Members cannot be duplicated");
    }
    // check if invited members list has the current user
    if (insertRoomRequestDto.getMembersIds().stream()
      .anyMatch(memberId -> memberId.toString().equals(currentUser.getId()))) {
      throw new BadRequestException("Requester can't be invited to the room");
    }
    insertRoomRequestDto.getMembersIds().stream()
      .filter(memberId -> !userService.userExists(memberId, currentUser))
      .findFirst()
      .ifPresent((uuid) -> {
        throw new NotFoundException(String.format("User with identifier '%s' not found", uuid));
      });
    // entity building
    UUID id = UUID.randomUUID();
    Room room = Room.create()
      .id(id.toString())
      .name(insertRoomRequestDto.getName())
      .description(insertRoomRequestDto.getDescription())
      .hash(Utils.encodeUuidHash(id.toString()))
      .domain(null)
      .type(insertRoomRequestDto.getType())
      .password(generateRoomPassword());
    room.subscriptions(
      membersService.initRoomSubscriptions(insertRoomRequestDto.getMembersIds(), room, currentUser));
    // persist room
    room = roomRepository.insert(room);

    // room creation on server XMPP
    messageDispatcher.createRoom(room, currentUser.getId());

    // send event
    UUID finalId = UUID.fromString(room.getId());
    room.getSubscriptions().forEach(member ->
      eventDispatcher.sendToQueue(currentUser.getUUID(), member.getUserId(),
        RoomCreatedEvent.create(finalId).from(currentUser.getUUID())
      )
    );
    // get new room result
    return roomMapper.ent2roomInfoDto(room, currentUser.getId());
  }

  @Override
  @Transactional
  public RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, UserPrincipal currentUser) {
    // get room
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    // change name and description
    room
      .name(updateRoomRequestDto.getName())
      .description(updateRoomRequestDto.getDescription());
    // room update
    roomRepository.update(room);
    // send update event to room topic
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
      RoomUpdatedEvent.create(roomId).from(currentUser.getUUID()));
    return roomMapper.ent2roomDto(room);
  }

  @Override
  @Transactional
  public void deleteRoom(UUID roomId, UserPrincipal currentUser) {
    // check the room
    getRoomAndCheckUser(roomId, currentUser, true);
    // this cascades to other
    roomRepository.delete(roomId.toString());
    // room deleting on server XMPP
    messageDispatcher.deleteRoom(roomId.toString(), currentUser.getId());
    // send to room topic
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(), new RoomDeletedEvent(roomId));
  }

  @Override
  public HashDto resetRoomHash(UUID roomId, UserPrincipal currentUser) {
    // get room
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    // generate hash
    String hash = Utils.encodeUuidHash(roomId.toString());
    room.hash(hash);
    roomRepository.update(room);
    // send event
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(),
      RoomHashResetEvent.create(roomId).hash(hash));
    return HashDtoBuilder.create().hash(hash).build();
  }


  @Override
  public void muteRoom(UUID roomId, UserPrincipal currentUser) {

  }

  @Override
  public void unmuteRoom(UUID roomId, UserPrincipal currentUser) {

  }

  private String generateRoomPassword() {
    // TODO: 22/11/21
    return null;
  }

  @Override
  public Room getRoomAndCheckUser(UUID roomId, UserPrincipal currentUser, boolean mustBeOwner) {
    // get room
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
      new NotFoundException(String.format("Room '%s'", roomId)));

    if (!currentUser.isSystemUser()) {
      // check that the current user is a member of the room and that he is an owner
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
    // gets the room and check that the user is a member
    getRoomAndCheckUser(roomId, currentUser, false);
    FileMetadata metadata = fileMetadataRepository.getById(roomId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", roomId)));
    // gets file from repository
    File file = storagesService.getFileById(metadata.getId(), metadata.getUserId());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  @Transactional
  public void setRoomPicture(UUID roomId, File image, String mimeType, String fileName, UserPrincipal currentUser) {
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
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
    fileMetadataRepository.save(metadata);
    if (oldUser.isPresent()) {
      try {
        storagesService.deleteFile(metadata.getId(), oldUser.get());
      } catch (Exception e) {
        ChatsLogger.warn("Could not delete older group profile picture: " + e.getMessage());
      }
    }
    storagesService.saveFile(image, metadata, currentUser.getId());
    eventDispatcher.sendToTopic(currentUser.getUUID(), room.getId(),
      RoomPictureChangedEvent.create(UUID.fromString(room.getId())).from(currentUser.getUUID()));
    messageDispatcher.sendMessageToRoom(room.getId(), currentUser.getId(), Messages.SET_PICTURE_FOR_ROOM_MESSAGE);
  }
}
