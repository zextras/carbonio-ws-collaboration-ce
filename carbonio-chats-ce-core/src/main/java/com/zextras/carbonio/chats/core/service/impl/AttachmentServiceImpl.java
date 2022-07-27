// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.apache.commons.lang3.math.NumberUtils.min;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.builder.IdDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.AttachmentAddedEvent;
import com.zextras.carbonio.chats.core.data.event.AttachmentRemovedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.IdDto;
import io.ebean.annotation.Transactional;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AttachmentServiceImpl implements AttachmentService {

  private final FileMetadataRepository fileMetadataRepository;
  private final AttachmentMapper       attachmentMapper;
  private final StoragesService        storagesService;
  private final RoomService            roomService;
  private final EventDispatcher        eventDispatcher;
  private final ObjectMapper           objectMapper;

  @Inject
  public AttachmentServiceImpl(
    FileMetadataRepository fileMetadataRepository, AttachmentMapper attachmentMapper, StoragesService storagesService,
    RoomService roomService, EventDispatcher eventDispatcher, ObjectMapper objectMapper
  ) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.attachmentMapper = attachmentMapper;
    this.storagesService = storagesService;
    this.roomService = roomService;
    this.eventDispatcher = eventDispatcher;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public FileContentAndMetadata getAttachmentById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    File file = storagesService.getFileById(metadata.getId(), metadata.getUserId());
    return new FileContentAndMetadata(file, metadata);
  }

  @Override
  public File getAttachmentPreviewById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata originMetadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(originMetadata.getRoomId()), currentUser, false);
    return storagesService.getPreview(originMetadata, originMetadata.getUserId());
  }

  @Override
  @Transactional
  public AttachmentsPaginationDto getAttachmentInfoByRoomId(
    UUID roomId, Integer itemsNumber, @Nullable String filter, UserPrincipal currentUser
  ) {
    roomService.getRoomEntityAndCheckUser(roomId, currentUser, false);
    PaginationFilter paginationFilter = null;
    if (filter != null) {
      try {
        paginationFilter = objectMapper.readValue(Base64.getDecoder().decode(filter), PaginationFilter.class);
      } catch (IOException e) {
        throw new BadRequestException("Cannot parse pagination filter", e);
      }
    }
    List<FileMetadata> metadataList = fileMetadataRepository.getByRoomIdAndType(roomId.toString(),
      FileMetadataType.ATTACHMENT, itemsNumber + 1, paginationFilter);
    return AttachmentsPaginationDto.create()
      .attachments(attachmentMapper.ent2dto(metadataList.subList(0, min(itemsNumber, metadataList.size()))))
      .filter(createNextPaginationFilter(metadataList, itemsNumber).orElse(null));
  }

  private Optional<String> createNextPaginationFilter(List<FileMetadata> list, int itemsNumber) {
    if (list.size() > itemsNumber) {
      try {
        return Optional.of(Base64.getEncoder().encodeToString(
          objectMapper.writeValueAsBytes(
            PaginationFilter.create(list.get(itemsNumber - 1).getId(), list.get(itemsNumber - 1).getCreatedAt())
          )
        ));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Cannot generate next pagination filter");
      }
    }
    return Optional.empty();
  }

  @Override
  @Transactional
  public AttachmentDto getAttachmentInfoById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    return attachmentMapper.ent2dto(metadata);
  }

  @Override
  @Transactional
  public IdDto addAttachment(UUID roomId, File file, String mimeType, String fileName, UserPrincipal currentUser) {
    Room room = roomService.getRoomEntityAndCheckUser(roomId, currentUser, false);
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
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      AttachmentAddedEvent.create(currentUser.getUUID()).roomId(roomId));
    return IdDtoBuilder.create().id(id).build();
  }

  @Override
  @Transactional
  public void deleteAttachment(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
    Room room = roomService.getRoomEntityAndCheckUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    room.getSubscriptions().stream()
      .filter(subscription -> subscription.getUserId().equals(currentUser.getId()) && (
        subscription.getUserId().equals(metadata.getUserId()) || subscription.isOwner()
      )).findAny().orElseThrow(() -> new ForbiddenException(
        String.format("User '%s' can not delete attachment '%s'", currentUser.getId(), fileId)));
    fileMetadataRepository.delete(metadata);
    storagesService.deleteFile(fileId.toString(), metadata.getUserId());
    eventDispatcher.sendToUserQueue(
      room.getSubscriptions().stream().map(Subscription::getUserId).collect(Collectors.toList()),
      AttachmentRemovedEvent.create(currentUser.getUUID()).roomId(UUID.fromString(room.getId())));
  }
}
