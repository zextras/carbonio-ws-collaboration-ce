// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import com.zextras.carbonio.chats.core.data.builder.IdDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.event.AttachmentAddedEvent;
import com.zextras.carbonio.chats.core.data.event.AttachmentRemovedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.OrderDirection;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.IdDto;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AttachmentServiceImpl implements AttachmentService {

  private final FileMetadataRepository fileMetadataRepository;
  private final AttachmentMapper       attachmentMapper;
  private final StoragesService        storagesService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;

  @Inject
  public AttachmentServiceImpl(
    FileMetadataRepository fileMetadataRepository, AttachmentMapper attachmentMapper, StoragesService storagesService,
    RoomService roomService,
    EventDispatcher eventDispatcher
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.attachmentMapper = attachmentMapper;
    this.storagesService = storagesService;
    this.roomService = roomService;
    this.eventDispatcher = eventDispatcher;
  }

  @Override
  @Transactional
  public FileContentAndMetadata getAttachmentById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    File file = storagesService.getFileById(metadata.getId(), metadata.getUserId());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  public File getAttachmentPreviewById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata originMetadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomAndCheckUser(UUID.fromString(originMetadata.getRoomId()), currentUser, false);
    return storagesService.getPreview(originMetadata, originMetadata.getUserId());
  }

  @Override
  @Transactional
  public List<AttachmentDto> getAttachmentInfoByRoomId(UUID roomId, UserPrincipal currentUser) {
    roomService.getRoomAndCheckUser(roomId, currentUser, false);
    List<FileMetadata> metadataList = fileMetadataRepository.getByRoomIdAndType(roomId.toString(),
      FileMetadataType.ATTACHMENT, OrderDirection.DESC);
    return attachmentMapper.ent2dto(metadataList);
  }

  @Override
  @Transactional
  public AttachmentDto getAttachmentInfoById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    return attachmentMapper.ent2dto(metadata);
  }

  @Override
  @Transactional
  public IdDto addAttachment(UUID roomId, File file, String mimeType, String fileName, UserPrincipal currentUser) {
    roomService.getRoomAndCheckUser(roomId, currentUser, false);
    UUID id = UUID.randomUUID();
    FileMetadata metadata = FileMetadata.create()
      .id(id.toString())
      .name(fileName)
      .originalSize(file.length())
      .mimeType(mimeType)
      .type(FileMetadataType.ATTACHMENT)
      .userId(currentUser.getId())
      .roomId(roomId.toString());
    fileMetadataRepository.save(metadata);
    storagesService.saveFile(file, metadata, currentUser.getId());
    eventDispatcher.sendToTopic(currentUser.getUUID(), roomId.toString(), AttachmentAddedEvent
      .create(roomId)
      .from(currentUser.getUUID()));

    return IdDtoBuilder.create().id(id).build();
  }

  @Override
  @Transactional
  public void deleteAttachment(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    Room room = roomService.getRoomAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    room.getSubscriptions().stream()
      .filter(subscription -> subscription.getUserId().equals(currentUser.getId()) && (
        subscription.getUserId().equals(metadata.getUserId()) || subscription.isOwner()
      )).findAny().orElseThrow(() -> new ForbiddenException(
        String.format("User '%s' can not delete attachment '%s'", currentUser.getId(), fileId)));
    fileMetadataRepository.delete(metadata);
    storagesService.deleteFile(fileId.toString(), metadata.getUserId());
    eventDispatcher.sendToTopic(currentUser.getUUID(), room.getId(), AttachmentRemovedEvent
      .create(UUID.fromString(room.getId()))
      .from(currentUser.getUUID()));
  }
}
