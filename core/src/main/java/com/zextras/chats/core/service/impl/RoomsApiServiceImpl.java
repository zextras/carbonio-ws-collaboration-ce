package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.api.RoomsApiService;
import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.event.RoomCreatedEvent;
import com.zextras.chats.core.data.event.RoomUpdatedEvent;
import com.zextras.chats.core.exception.BadRequestException;
import com.zextras.chats.core.exception.ForbiddenException;
import com.zextras.chats.core.exception.NotFoundException;
import com.zextras.chats.core.exception.UnauthorizedException;
import com.zextras.chats.core.mapper.RoomMapper;
import com.zextras.chats.core.model.HashDto;
import com.zextras.chats.core.model.IdDto;
import com.zextras.chats.core.model.MemberDto;
import com.zextras.chats.core.model.RoomCreationFieldsDto;
import com.zextras.chats.core.model.RoomDto;
import com.zextras.chats.core.model.RoomEditableFieldsDto;
import com.zextras.chats.core.model.RoomInfoDto;
import com.zextras.chats.core.repository.RoomRepository;
import com.zextras.chats.core.repository.RoomUserSettingsRepository;
import com.zextras.chats.core.utils.Utils;
import com.zextras.chats.core.web.security.AccountService;
import com.zextras.chats.core.web.security.MockSecurityContext;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import com.zextras.chats.core.data.entity.Subscription;
import com.zextras.chats.core.data.entity.SubscriptionId;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.lang3.StringUtils;

public class RoomsApiServiceImpl implements RoomsApiService {

  private final RoomRepository             roomRepository;
  private final RoomUserSettingsRepository roomUserSettingsRepository;
  private final RoomMapper                 roomMapper;
  private final EventDispatcher            eventDispatcher;
  private final AccountService             accountService;
  private final MockSecurityContext        mockSecurityContext;

  @Inject
  public RoomsApiServiceImpl(
    RoomRepository roomRepository, RoomUserSettingsRepository roomUserSettingsRepository, RoomMapper roomMapper,
    EventDispatcher eventDispatcher,
    AccountService accountService,
    MockSecurityContext mockSecurityContext
  ) {
    this.roomRepository = roomRepository;
    this.roomUserSettingsRepository = roomUserSettingsRepository;
    this.roomMapper = roomMapper;
    this.eventDispatcher = eventDispatcher;
    this.accountService = accountService;
    this.mockSecurityContext = mockSecurityContext;
  }

  @Override
  public List<RoomDto> getRooms(SecurityContext securityContext) {
    MockUserPrincipal user = (MockUserPrincipal) mockSecurityContext.getUserPrincipal().orElseThrow(
      UnauthorizedException::new);

    List<Room> rooms = roomRepository.getByUserId(user.getId().toString());
    return roomMapper.ent2roomDto(rooms);
  }

  @Override
  @Transactional
  public RoomInfoDto getRoomById(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal user = (MockUserPrincipal) mockSecurityContext.getUserPrincipal().orElseThrow(UnauthorizedException::new);
    // get the room
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
      new NotFoundException(String.format("Room '%s' not found", roomId)));
    // check that current user is member of the room
    room.getSubscriptions().stream()
      .filter(subscription -> user.getId().toString().equals(subscription.getUserId()))
      .findAny()
      .orElseThrow(() -> new ForbiddenException(String.format("User '%s' is not a member of room '%s'", user.getId().toString(), roomId)));
    // get current user settings for the room
    roomUserSettingsRepository.getByRoomIdAndUserId(roomId.toString(), user.getId().toString())
      .ifPresent( settings -> room.setUserSettings(Collections.singletonList(settings)));
    return roomMapper.ent2roomInfoDto(room, user.getId().toString());
  }

  @Override
  public RoomInfoDto createRoom(RoomCreationFieldsDto roomCreationFieldsDto, SecurityContext securityContext) {
    MockUserPrincipal user = (MockUserPrincipal) mockSecurityContext.getUserPrincipal().orElseThrow(UnauthorizedException::new);
    // Validation of input fields
    if (StringUtils.isEmpty(roomCreationFieldsDto.getName())) {
      throw new BadRequestException("Name cannot be null or empty");
    }
    if (roomCreationFieldsDto.getType() == null) {
      throw new BadRequestException("Type cannot be null");
    }
    if (roomCreationFieldsDto.getMembersIds() == null) {
      throw new BadRequestException("Members list cannot be null");
    } else {
      roomCreationFieldsDto.getMembersIds().forEach(userId -> {
        if (accountService.getById(userId) == null) {
          throw new NotFoundException(String.format("User not found with identifier '%s'", userId));
        }
      });
    }
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
    room.setSubscriptions(getSubscriptions(roomCreationFieldsDto, room, user));
    // persist room
    roomRepository.insert(room);
    // send event
    room.getSubscriptions().forEach(member ->
      eventDispatcher.sentToQueue(user.getId(), UUID.fromString(member.getUserId()),
        RoomCreatedEvent.create(id, LocalDateTime.now().minus(1, ChronoField.MILLI_OF_DAY.getBaseUnit()))
          .from(user.getId())
      )
    );
    // get new room result
    return roomMapper.ent2roomInfoDto(room, user.getId().toString());
  }

  @Override
  @Transactional
  public RoomDto updateRoom(UUID roomId, RoomEditableFieldsDto roomEditableFieldsDto, SecurityContext securityContext) {
    MockUserPrincipal user = (MockUserPrincipal) mockSecurityContext.getUserPrincipal().orElseThrow(UnauthorizedException::new);
    // Validation of input fields
    if (StringUtils.isEmpty(roomEditableFieldsDto.getName())) {
      throw new BadRequestException("Name cannot be null or empty");
    }
    // get room
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
        new NotFoundException(String.format("Room '%s'", roomId)));

    // check that the current user is a member of the room and that he is an owner
    if (!room.getSubscriptions().stream()
      .filter(subscription -> subscription.getUserId().equals(user.getId().toString()))
      .findAny()
      .orElseThrow(() -> new ForbiddenException(String.format("User '%s' is not a member of room '%s'", user.getId().toString(), roomId)))
      .getIsOwner()) {
      throw new ForbiddenException(String.format("User '%s' is not a member of room '%s'", user.getId().toString(), roomId));
    }
    // change name and description
    room
      .name(roomEditableFieldsDto.getName())
      .description(roomEditableFieldsDto.getDescription());
    // room update
    roomRepository.update(room);
    // send update event to room topic
    eventDispatcher.sentToTopic(user.getId(), roomId,
      RoomUpdatedEvent.create(roomId, LocalDateTime.now()).from(user.getId()));
    return roomMapper.ent2roomDto(room);
  }

  @Override
  @Transactional
  public void deleteRoom(UUID roomId, SecurityContext securityContext) {
    MockUserPrincipal user = (MockUserPrincipal) mockSecurityContext.getUserPrincipal().orElseThrow(UnauthorizedException::new);
    // get room
    Room room = roomRepository.getById(roomId.toString()).orElseThrow(() ->
      new NotFoundException(String.format("Room '%s'", roomId)));

    if (!user.isSystemUser()) {
      // check that the current user is a member of the room and that he is an owner
      if (!room.getSubscriptions().stream()
        .filter(subscription -> subscription.getUserId().equals(user.getId().toString()))
        .findAny()
        .orElseThrow(() -> new ForbiddenException(String.format("User '%s' is not a member of room '%s'", user.getId().toString(), roomId)))
        .getIsOwner()) {
        throw new ForbiddenException(String.format("User '%s' is not a member of room '%s'", user.getId().toString(), roomId));
      }
    }
    //this cascades to other tables
    roomRepository.delete(roomId.toString());
    // send to room topic
  }


  @Override
  public IdDto addAttachment(UUID roomId, File body, SecurityContext securityContext) {
    return null;
  }

  @Override
  public void addOwner(UUID roomId, UUID userId, SecurityContext securityContext) {

  }

  @Override
  public MemberDto addRoomMember(
    UUID roomId, UUID userid, MemberDto memberDto, SecurityContext securityContext
  ) {
    return null;
  }

  @Override
  public void deleteRoomMember(UUID roomId, UUID userid, SecurityContext securityContext) {

  }

  @Override
  public List<MemberDto> getRoomMembers(UUID roomId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public void muteRoom(UUID roomId, SecurityContext securityContext) {

  }

  @Override
  public void removeOwner(UUID roomId, UUID userId, SecurityContext securityContext) {

  }

  @Override
  public HashDto resetRoomHash(UUID roomId, SecurityContext securityContext) {
    return null;
  }

  @Override
  public void setRoomPicture(UUID roomId, File body, SecurityContext securityContext) {

  }

  @Override
  public void unmuteRoom(UUID roomId, SecurityContext securityContext) {

  }

  private String generateRoomPassword() {
    // TODO: 22/11/21
    return null;
  }

  private List<Subscription> getSubscriptions(RoomCreationFieldsDto roomCreationDto, Room room, MockUserPrincipal requester) {
    List<Subscription> result = roomCreationDto.getMembersIds().stream().map(userId ->
      Subscription.create()
        .id(new SubscriptionId(room.getId(), userId))
        .userId(userId)
        .room(room)
        .owner(false)
    ).collect(Collectors.toList());
    result.add(Subscription.create()
      .id(new SubscriptionId(room.getId(), requester.getId().toString()))
      .userId(requester.getId().toString())
      .room(room)
      .owner(true));

    return result;
  }
}
