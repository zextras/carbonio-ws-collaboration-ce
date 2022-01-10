package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.entity.RoomImage;
import com.zextras.chats.core.data.event.RoomPictureChangedEvent;
import com.zextras.chats.core.exception.BadRequestException;
import com.zextras.chats.core.exception.InternalErrorException;
import com.zextras.chats.core.infrastructure.messaging.MessageService;
import com.zextras.chats.core.model.RoomTypeDto;
import com.zextras.chats.core.repository.RoomImageRepository;
import com.zextras.chats.core.service.RoomPictureService;
import com.zextras.chats.core.service.RoomService;
import com.zextras.chats.core.utils.Messages;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class RoomPictureServiceImpl implements RoomPictureService {

  private static final long MAX_FILE_SIZE_IN_KB = 256;

  private final RoomService         roomService;
  private final RoomImageRepository roomImageRepository;
  private final EventDispatcher     eventDispatcher;
  private final MessageService      messageService;

  @Inject
  public RoomPictureServiceImpl(
    RoomService roomService, RoomImageRepository roomImageRepository, EventDispatcher eventDispatcher,
    MessageService messageService
  ) {
    this.roomService = roomService;
    this.roomImageRepository = roomImageRepository;
    this.eventDispatcher = eventDispatcher;
    this.messageService = messageService;
  }

  @Override
  @Transactional
  public void setPictureForRoom(UUID roomId, File image, MockUserPrincipal currentUser) {
    // gets the room and check that the user is a member
    Room room = roomService.getRoomAndCheckUser(roomId, currentUser, false);
    // validate field
    if (!RoomTypeDto.GROUP.equals(room.getType())) {
      throw new BadRequestException("The room picture can only be set to group type rooms");
    }
    if ((MAX_FILE_SIZE_IN_KB * 1024) < image.length()) {
      throw new BadRequestException("The room picture cannot be greater than %d KB");
    }
    // get or create the entity
    RoomImage roomImage = roomImageRepository.getByRoomId(room.getId())
      .orElse(RoomImage.create().roomId(room.getId()));
    // insert or updated image
    roomImageRepository.save(roomImage.image(getByteArray(image)));
    // send event to room topic
    eventDispatcher.sendToTopic(currentUser.getId(), room.getId(),
      RoomPictureChangedEvent.create(UUID.fromString(room.getId())).from(currentUser.getId()));
    // send message to XMPP room
    messageService.sendMessageToRoom(room.getId(), currentUser.getId().toString(), Messages.SET_PICTURE_FOR_ROOM_MESSAGE);
  }

  private byte[] getByteArray(File file) {
    try {
      return FileUtils.readFileToByteArray(file);
    } catch (IOException e) {
      throw new InternalErrorException("Unable to read file stream", e);
    }
  }

}
