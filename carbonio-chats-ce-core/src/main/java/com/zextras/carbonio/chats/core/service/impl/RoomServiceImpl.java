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
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.Messages;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.core.web.security.AccountService;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
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
  private final AccountService             accountService;
  private final MembersService             membersService;
  private final FileMetadataRepository     fileMetadataRepository;
  private final StorageService             storageService;

  @Inject
  public RoomServiceImpl(
    RoomRepository roomRepository, RoomUserSettingsRepository roomUserSettingsRepository, RoomMapper roomMapper,
    EventDispatcher eventDispatcher,
    MessageDispatcher messageDispatcher,
    AccountService accountService,
    MembersService membersService,
    FileMetadataRepository fileMetadataRepository,
    StorageService storageService
  ) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.roomMapper = roomMapper;
    this.eventDispatcher = eventDispatcher;
    this.messageDispatcher = messageDispatcher;
    this.accountService = accountService;
    this.membersService = membersService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.storageService = storageService;
  }


  @Override
  public List<RoomDto> getRooms(MockUserPrincipal currentUser) {
    List<Room> rooms = roomRepository.getByUserId(currentUser.getId().toString());
    return roomMapper.ent2roomDto(rooms);
  }

  @Override
  @Transactional
  public RoomInfoDto getRoomById(UUID roomId, MockUserPrincipal currentUser) {
    // get the room
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
    // get current user settings for the room
    roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), currentUser.getId().toString())
      .ifPresent(settings -> room.setUserSettings(Collections.singletonList(settings)));
    return roomMapper.ent2roomInfoDto(room, currentUser.getId().toString());
  }

  @Override
  public RoomInfoDto createRoom(RoomCreationFieldsDto insertRoomRequestDto, MockUserPrincipal currentUser) {
    // check for duplicates
    if (insertRoomRequestDto.getMembersIds().size() != new HashSet<>(insertRoomRequestDto.getMembersIds()).size()) {
      throw new BadRequestException("Members cannot be duplicated");
    }
    // check the users existence
    insertRoomRequestDto.getMembersIds()
      .forEach(userId ->
        accountService.getById(userId.toString())
          .orElseThrow(() -> new NotFoundException(String.format("User with identifier '%s' not found", userId))));
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
    room.setSubscriptions(membersService.initRoomSubscriptions(insertRoomRequestDto.getMembersIds(), room, currentUser));
    // persist room
    room = roomRepository.insert(room);
    // send event
    UUID finalId = UUID.fromString(room.getId());
    room.getSubscriptions().forEach(member ->
      eventDispatcher.sendToQueue(currentUser.getId(), member.getUserId(),
        RoomCreatedEvent.create(finalId).from(currentUser.getId())
      )
    );
    // room creation on server XMPP
    messageDispatcher.createRoom(room, currentUser.getId().toString());
    // get new room result
    return roomMapper.ent2roomInfoDto(room, currentUser.getId().toString());
  }

  @Override
  @Transactional
  public RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto updateRoomRequestDto, MockUserPrincipal currentUser) {
    // get room
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    // change name and description
    room
      .name(updateRoomRequestDto.getName())
      .description(updateRoomRequestDto.getDescription());
    // room update
    roomRepository.update(room);
    // send update event to room topic
    eventDispatcher.sendToTopic(currentUser.getId(), roomId.toString(),
      RoomUpdatedEvent.create(roomId).from(currentUser.getId()));
    return roomMapper.ent2roomDto(room);
  }

  @Override
  @Transactional
  public void deleteRoom(UUID roomId, MockUserPrincipal currentUser) {
    // check the room
    getRoomAndCheckUser(roomId, currentUser, true);
    // this cascades to other   public void deleteRoom(UUID roomId, MockUserPrincipal currentUser) {
    roomRepository.delete(roomId.toString());
    // send to room topic
    eventDispatcher.sendToTopic(currentUser.getId(), roomId.toString(), new RoomDeletedEvent(roomId));
    // room deleting on server XMPP
    messageDispatcher.deleteRoom(roomId.toString(), currentUser.getId().toString());
  }

  @Override
  public HashDto resetRoomHash(UUID roomId, MockUserPrincipal currentUser) {
    // get room
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    // generate hash
    String hash = Utils.encodeUuidHash(roomId.toString());
    room.hash(hash);
    roomRepository.update(room);
    // send event
    eventDispatcher.sendToTopic(currentUser.getId(), roomId.toString(),
      RoomHashResetEvent.create(roomId).hash(hash));
    return HashDtoBuilder.create().hash(hash).build();
  }


  @Override
  public void muteRoom(UUID roomId, MockUserPrincipal currentUser) {

  }

  @Override
  public void unmuteRoom(UUID roomId, MockUserPrincipal currentUser) {

  }

  private String generateRoomPassword() {
    // TODO: 22/11/21
    return null;
  }

  @Override
  public Room getRoomAndCheckUser(UUID roomId, MockUserPrincipal currentUser, boolean mustBeOwner) {
    // get room
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
      new NotFoundException(String.format("Room '%s'", roomId)));

    if (!currentUser.isSystemUser()) {
      // check that the current user is a member of the room and that he is an owner
      Subscription member = room.getSubscriptions().stream()
        .filter(subscription -> subscription.getUserId().equals(currentUser.getId().toString()))
        .findAny()
        .orElseThrow(() -> new ForbiddenException(
          String.format("User '%s' is not a member of room '%s'", currentUser.getId().toString(), roomId)));
      if (mustBeOwner && !member.isOwner()) {
        throw new ForbiddenException(
          String.format("User '%s' is not an owner of room '%s'", currentUser.getId().toString(), roomId));
      }
    }
    return room;
  }

  @Override
  @Transactional
  public FileContentAndMetadata getRoomPicture(UUID roomId, MockUserPrincipal currentUser) {
    // gets the room and check that the user is a member
    getRoomAndCheckUser(roomId, currentUser, false);
    FileMetadata metadata = fileMetadataRepository.getById(roomId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", roomId)));
    // gets file from repository
    File file = storageService.getFileById(metadata.getId(), currentUser.getId().toString());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  @Transactional
  public void setRoomPicture(UUID roomId, File image, String mimeType, String fileName, MockUserPrincipal currentUser) {
    // gets the room and check that the user is a member
    Room room = getRoomAndCheckUser(roomId, currentUser, false);
    // validate field
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set to group type rooms");
    }
    if ((ChatsConstant.MAX_ROOM_IMAGE_SIZE_IN_KB * 1024) < image.length()) {
      throw new BadRequestException(String.format("The room picture cannot be greater than %d KB", ChatsConstant.MAX_ROOM_IMAGE_SIZE_IN_KB));
    }
    if (!mimeType.startsWith("image/")) {
      throw new BadRequestException("The room picture must be an image");
    }
    // get or create the entity
    FileMetadata metadata = fileMetadataRepository.getById(roomId.toString())
      .orElseGet(() -> FileMetadata.create()
        .id(roomId.toString())
        .type(FileMetadataType.ROOM_AVATAR)
        .roomId(roomId.toString()));
    metadata
      .name(fileName)
      .originalSize(image.length())
      .mimeType(mimeType)
      .userId(currentUser.getId().toString());
    fileMetadataRepository.save(metadata);
    // save file in repository
    storageService.saveFile(image, metadata, currentUser.getId().toString());
    // send event to room topic
    eventDispatcher.sendToTopic(currentUser.getId(), room.getId(),
      RoomPictureChangedEvent.create(UUID.fromString(room.getId())).from(currentUser.getId()));
    // send message to XMPP room
    messageDispatcher.sendMessageToRoom(room.getId(), currentUser.getId().toString(), Messages.SET_PICTURE_FOR_ROOM_MESSAGE);
  }
}
