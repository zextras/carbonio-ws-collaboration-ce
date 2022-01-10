package com.zextras.chats.core.service.impl;

import com.zextras.chats.core.data.builder.IdDtoBuilder;
import com.zextras.chats.core.data.entity.FileMetadata;
import com.zextras.chats.core.data.entity.Room;
import com.zextras.chats.core.data.event.AttachmentAddedEvent;
import com.zextras.chats.core.data.event.AttachmentRemovedEvent;
import com.zextras.chats.core.data.model.AttachmentFile;
import com.zextras.chats.core.data.type.FileMetadataType;
import com.zextras.chats.core.exception.NotFoundException;
import com.zextras.chats.core.infrastructure.storage.StorageService;
import com.zextras.chats.core.mapper.AttachmentMapper;
import com.zextras.chats.core.model.AttachmentDto;
import com.zextras.chats.core.model.IdDto;
import com.zextras.chats.core.repository.FileMetadataRepository;
import com.zextras.chats.core.service.AttachmentService;
import com.zextras.chats.core.service.RoomService;
import com.zextras.chats.core.web.dispatcher.EventDispatcher;
import com.zextras.chats.core.web.security.MockUserPrincipal;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AttachmentServiceImpl implements AttachmentService {

  private final FileMetadataRepository fileMetadataRepository;
  private final AttachmentMapper       attachmentMapper;
  private final StorageService         storageService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;

  @Inject
  public AttachmentServiceImpl(
    FileMetadataRepository fileMetadataRepository, AttachmentMapper attachmentMapper, StorageService storageService,
    RoomService roomService,
    EventDispatcher eventDispatcher
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.attachmentMapper = attachmentMapper;
    this.storageService = storageService;
    this.roomService = roomService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  public AttachmentFile getAttachmentById(UUID fileId, MockUserPrincipal currentUser) {
    // gets file metadata from DB
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    // checks if current user is a member of the attachment room
    roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    // gets file from repository
    File file = storageService.getFileById(metadata.getId());
    return new AttachmentFile(file, metadata);
  }

  @Override
  public AttachmentFile getAttachmentPreviewById(UUID fileId, MockUserPrincipal currentUser) {
    // TODO: 07/01/22 momentarily returns the original file
    return getAttachmentById(fileId, currentUser);
  }

  @Override
  @Transactional
  public AttachmentDto getAttachmentInfoById(UUID fileId, MockUserPrincipal currentUser) {
    // gets file metadata from DB
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    // checks if current user is a member of the attachment room
    roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    return attachmentMapper.ent2dto(metadata);
  }

  @Override
  @Transactional
  public IdDto addAttachment(UUID roomId, File file, String mimeType, String fileName, MockUserPrincipal currentUser) {
    roomService.getRoomAndCheckUser(roomId, currentUser, false);
    // generates the file identifier
    UUID id = UUID.randomUUID();
    // creates the entity and saves it in DB
    FileMetadata metadata = FileMetadata.create()
      .id(id.toString())
      .name(fileName)
      .originalSize(file.length())
      .mimeType(mimeType)
      .type(FileMetadataType.ATTACHMENT)
      .userId(currentUser.getId().toString())
      .roomId(roomId.toString());
    fileMetadataRepository.save(metadata);
    // save the file in repository
    storageService.saveFile(file, metadata);
    // sends event
    eventDispatcher.sendToTopic(currentUser.getId(), roomId.toString(), AttachmentAddedEvent
      .create(roomId)
      .from(currentUser.getId()));

    return IdDtoBuilder.create().id(id).build();
  }

  @Override
  @Transactional
  public void deleteAttachment(UUID fileId, MockUserPrincipal currentUser) {
    // gets file metadata from DB
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    // checks if current user is a member of the attachment room
    Room room = roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    // delete file data from DB
    fileMetadataRepository.delete(metadata);
    // deletes file from repository
    storageService.deleteFile(fileId.toString());
    // sends the event
    eventDispatcher.sendToTopic(currentUser.getId(), room.getId(), AttachmentRemovedEvent
      .create(UUID.fromString(room.getId()))
      .from(currentUser.getId()));
  }
}
