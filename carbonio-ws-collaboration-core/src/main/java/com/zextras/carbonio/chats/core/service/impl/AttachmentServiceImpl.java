// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import static org.apache.commons.lang3.math.NumberUtils.min;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.builder.IdDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.impl.graphql.StanzaResponse;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.BulkDeleteAttachmentsResponseDto;
import com.zextras.carbonio.chats.model.IdDto;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Singleton
public class AttachmentServiceImpl implements AttachmentService {

  private final FileMetadataRepository fileMetadataRepository;
  private final AttachmentMapper attachmentMapper;
  private final StoragesService storagesService;
  private final RoomService roomService;
  private final MessageDispatcher messageDispatcher;
  private final ObjectMapper objectMapper;

  @Inject
  public AttachmentServiceImpl(
      FileMetadataRepository fileMetadataRepository,
      AttachmentMapper attachmentMapper,
      StoragesService storagesService,
      RoomService roomService,
      MessageDispatcher messageDispatcher,
      ObjectMapper objectMapper) {
    this.fileMetadataRepository = fileMetadataRepository;
    this.attachmentMapper = attachmentMapper;
    this.storagesService = storagesService;
    this.roomService = roomService;
    this.messageDispatcher = messageDispatcher;
    this.objectMapper = objectMapper;
  }

  @Override
  public FileContentAndMetadata getAttachmentById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata =
        fileMetadataRepository
            .getById(fileId.toString())
            .orElseThrow(
                () -> new NotFoundException(String.format("Attachment '%s' not found", fileId)));
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    return new FileContentAndMetadata(
        storagesService.getFileStreamById(metadata.getId(), metadata.getUserId()), metadata);
  }

  @Override
  public AttachmentsPaginationDto getAttachmentInfoByRoomId(
      UUID roomId,
      Integer limit,
      @Nullable String cursor,
      @Nullable AttachmentFilter attachmentFilter,
      UserPrincipal currentUser) {
    roomService.getRoomAndValidateUser(roomId, currentUser, false);
    PaginationFilter paginationFilter = null;
    if (cursor != null) {
      try {
        paginationFilter =
            objectMapper.readValue(Base64.getDecoder().decode(cursor), PaginationFilter.class);
      } catch (IOException e) {
        throw new BadRequestException("Cannot parse pagination cursor", e);
      }
      if (paginationFilter.getSortBy() != null && paginationFilter.getOrder() != null) {
        String currentSortBy =
            attachmentFilter != null ? attachmentFilter.getSortBy() : "created_at";
        String currentOrder = attachmentFilter != null ? attachmentFilter.getOrder() : "desc";
        if (!paginationFilter.getSortBy().equals(currentSortBy)
            || !paginationFilter.getOrder().equals(currentOrder)) {
          throw new BadRequestException(
              "Pagination cursor sort parameters do not match current query sort parameters");
        }
      }
    }
    List<FileMetadata> metadataList =
        fileMetadataRepository.getByRoomIdAndType(
            roomId.toString(),
            FileMetadataType.ATTACHMENT,
            limit + 1,
            paginationFilter,
            attachmentFilter);
    return AttachmentsPaginationDto.create()
        .attachments(
            attachmentMapper.ent2dto(metadataList.subList(0, min(limit, metadataList.size()))))
        .cursor(createNextCursor(metadataList, limit, attachmentFilter).orElse(null));
  }

  private Optional<String> createNextCursor(
      List<FileMetadata> list, int limit, @Nullable AttachmentFilter attachmentFilter) {
    if (list.size() > limit) {
      try {
        FileMetadata last = list.get(limit - 1);
        boolean sortByCreatedAt = attachmentFilter == null || attachmentFilter.isSortByCreatedAt();
        String sortBy = attachmentFilter != null ? attachmentFilter.getSortBy() : "created_at";
        String order = attachmentFilter != null ? attachmentFilter.getOrder() : "desc";
        PaginationFilter nextFilter =
            sortByCreatedAt
                ? PaginationFilter.create(last.getId(), last.getCreatedAt(), null, sortBy, order)
                : PaginationFilter.create(
                    last.getId(), null, last.getOriginalSize(), sortBy, order);
        return Optional.of(
            Base64.getEncoder().encodeToString(objectMapper.writeValueAsBytes(nextFilter)));
      } catch (JsonProcessingException e) {
        throw new InternalErrorException("Cannot generate next pagination cursor");
      }
    }
    return Optional.empty();
  }

  @Override
  public AttachmentDto getAttachmentInfoById(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata =
        fileMetadataRepository
            .getById(fileId.toString())
            .orElseThrow(
                () -> new NotFoundException(String.format("Attachment '%s' not found", fileId)));
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    return attachmentMapper.ent2dto(metadata);
  }

  @Override
  public IdDto addAttachment(
      UUID roomId,
      InputStream file,
      String mimeType,
      Long contentLength,
      String fileName,
      String description,
      @Nullable String messageId,
      @Nullable String replyId,
      @Nullable String area,
      UserPrincipal currentUser) {
    roomService.getRoomAndValidateUser(roomId, currentUser, false);

    UUID fileId = UUID.randomUUID();
    FileMetadata metadata =
        FileMetadata.create()
            .id(fileId.toString())
            .name(fileName)
            .originalSize(contentLength)
            .mimeType(mimeType)
            .type(FileMetadataType.ATTACHMENT)
            .userId(currentUser.getId())
            .roomId(roomId.toString())
            .messageId(messageId);
    storagesService.saveFile(file, fileId.toString(), currentUser.getId(), contentLength);
    StanzaResponse stanzaResponse =
        messageDispatcher.sendAttachment(
            roomId.toString(),
            currentUser.getId(),
            fileId.toString(),
            fileName,
            mimeType,
            contentLength,
            description,
            messageId,
            replyId,
            area);
    fileMetadataRepository.save(metadata.stanzaId(stanzaResponse.stanzaId()));
    return IdDtoBuilder.create().id(fileId).build();
  }

  @Override
  public FileMetadata copyAttachment(
      Room destinationRoom, UUID originalAttachmentId, UserPrincipal currentUser) {
    FileMetadata sourceMetadata =
        fileMetadataRepository
            .getById(originalAttachmentId.toString())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Attachment '%s' not found", originalAttachmentId)));
    roomService.getRoomAndValidateUser(
        UUID.fromString(sourceMetadata.getRoomId()), currentUser, false);
    FileMetadata metadata =
        FileMetadata.create()
            .id(UUID.randomUUID().toString())
            .name(sourceMetadata.getName())
            .originalSize(sourceMetadata.getOriginalSize())
            .mimeType(sourceMetadata.getMimeType())
            .type(FileMetadataType.ATTACHMENT)
            .userId(currentUser.getId())
            .roomId(destinationRoom.getId());
    storagesService.copyFile(
        sourceMetadata.getId(), sourceMetadata.getUserId(), metadata.getId(), currentUser.getId());
    fileMetadataRepository.save(metadata);
    return metadata;
  }

  @Override
  public void deleteAttachment(UUID fileId, UserPrincipal currentUser) {
    FileMetadata metadata =
        fileMetadataRepository
            .getById(fileId.toString())
            .orElseThrow(
                () -> new NotFoundException(String.format("Attachment '%s' not found", fileId)));
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), currentUser, false);
    if (!currentUser.getId().equals(metadata.getUserId())) {
      throw new ForbiddenException(
          String.format("User '%s' can not delete attachment '%s'", currentUser.getId(), fileId));
    }
    storagesService.deleteFile(fileId.toString(), metadata.getUserId());
    fileMetadataRepository.delete(metadata);
  }

  @Override
  public void deleteAttachmentsByRoomId(UUID roomId, UserPrincipal currentUser) {
    try {
      List<String> allIds =
          fileMetadataRepository.getIdsByRoomIdAndType(
              roomId.toString(), FileMetadataType.ATTACHMENT);
      List<String> failedIds = storagesService.deleteFileList(allIds, currentUser.getId());
      if (!failedIds.isEmpty()) {
        ChatsLogger.warn(
            "Failed to delete "
                + failedIds.size()
                + " attachment(s) from storage for room "
                + roomId
                + ": "
                + failedIds);
      }
      List<String> successIds = allIds.stream().filter(id -> !failedIds.contains(id)).toList();
      fileMetadataRepository.deleteByIds(successIds);
    } catch (StorageException e) {
      ChatsLogger.warn("Error while deleting attachments of room " + roomId, e);
    }
  }

  @Override
  public BulkDeleteAttachmentsResponseDto bulkDeleteRoomAttachments(
      UUID roomId, List<UUID> fileIds, UserPrincipal currentUser) {
    roomService.getRoomAndValidateUser(roomId, currentUser, false);
    List<String> fileIdStrings = fileIds.stream().map(UUID::toString).collect(Collectors.toList());
    List<String> accessibleIds =
        fileMetadataRepository.getIdsByIdsAndRoomIdAndUserId(
            fileIdStrings, roomId.toString(), currentUser.getId());
    if (accessibleIds.size() != fileIdStrings.size()) {
      throw new ForbiddenException("One or more attachments were not found or are not accessible");
    }
    List<String> failedIds = storagesService.deleteFileList(fileIdStrings, currentUser.getId());
    if (!failedIds.isEmpty()) {
      ChatsLogger.warn(
          "Failed to delete "
              + failedIds.size()
              + " attachment(s) from storage in room "
              + roomId
              + ": "
              + failedIds);
    }
    List<String> successIds = fileIdStrings.stream().filter(id -> !failedIds.contains(id)).toList();
    fileMetadataRepository.deleteByIds(successIds);
    return BulkDeleteAttachmentsResponseDto.create()
        .successIds(successIds.stream().map(UUID::fromString).toList())
        .failedIds(failedIds.stream().map(UUID::fromString).toList());
  }
}
