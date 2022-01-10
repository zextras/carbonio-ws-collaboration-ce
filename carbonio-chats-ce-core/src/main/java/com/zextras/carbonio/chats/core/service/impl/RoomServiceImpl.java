package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.builder.HashDtoBuilder;
import com.zextras.carbonio.chats.core.data.event.RoomCreatedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomDeletedEvent;
import com.zextras.carbonio.chats.core.data.event.RoomHashResetEvent;
import com.zextras.carbonio.chats.core.data.event.RoomUpdatedEvent;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageService;
import com.zextras.carbonio.chats.core.mapper.RoomMapper;
import com.zextras.carbonio.chats.core.model.HashDto;
import com.zextras.carbonio.chats.core.model.RoomCreationFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomDto;
import com.zextras.carbonio.chats.core.model.RoomEditableFieldsDto;
import com.zextras.carbonio.chats.core.model.RoomInfoDto;
import com.zextras.carbonio.chats.core.repository.RoomRepository;
import com.zextras.carbonio.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.carbonio.chats.core.service.MembersService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.utils.Utils;
import com.zextras.carbonio.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.carbonio.chats.core.web.security.AccountService;
import com.zextras.carbonio.chats.core.web.security.MockUserPrincipal;
import io.ebean.annotation.Transactional;
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
  private final AccountService             accountService;
  private final MembersService             membersService;
  private final MessageService             messageService;

  @Inject
  public RoomServiceImpl(
    RoomRepository roomRepository,
    RoomUserSettingsRepository roomUserSettingsRepository,
    RoomMapper roomMapper,
    EventDispatcher eventDispatcher,
    AccountService accountService,
    MembersService membersService,
    MessageService messageService
  ) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.roomMapper = roomMapper;
    this.eventDispatcher = eventDispatcher;
    this.accountService = accountService;
    this.membersService = membersService;
    this.messageService = messageService;
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
  public RoomInfoDto createRoom(RoomCreationFieldsDto roomCreationFieldsDto, MockUserPrincipal currentUser) {
    // check for duplicates
    if (roomCreationFieldsDto.getMembersIds().size() != new HashSet<>(roomCreationFieldsDto.getMembersIds()).size()) {
      throw new BadRequestException("Members cannot be duplicated");
    }
    // check the users existence
    roomCreationFieldsDto.getMembersIds()
      .forEach(userId ->
        accountService.getById(userId)
          .orElseThrow(() -> new NotFoundException(String.format("User with identifier '%s' not found", userId))));
    // entity building
    UUID id = UUID.randomUUID();
    Room room = Room.create()
      .id(id.toString())
      .name(roomCreationFieldsDto.getName())
      .description(roomCreationFieldsDto.getDescription())
      .hash(Utils.encodeUuidHash(id.toString()))
      .domain(null)
      .type(roomCreationFieldsDto.getType())
      .password(generateRoomPassword());
    room.setSubscriptions(membersService.initRoomSubscriptions(roomCreationFieldsDto.getMembersIds(), room, currentUser));
    // persist room
    room = roomRepository.insert(room);
    // send event
    room.getSubscriptions().forEach(member ->
      eventDispatcher.sendToQueue(currentUser.getId(), member.getUserId(),
        RoomCreatedEvent.create(id).from(currentUser.getId())
      )
    );
    // room creation on server XMPP
    messageService.createRoom(room, currentUser.getId().toString());
    // get new room result
    return roomMapper.ent2roomInfoDto(room, currentUser.getId().toString());
  }

  @Override
  @Transactional
  public RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto roomEditableFieldsDto, MockUserPrincipal currentUser) {
    // get room
    Room room = getRoomAndCheckUser(roomId, currentUser, true);
    // change name and description
    room
      .name(roomEditableFieldsDto.getName())
      .description(roomEditableFieldsDto.getDescription());
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
    messageService.deleteRoom(roomId.toString(), currentUser.getId().toString());
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
}
