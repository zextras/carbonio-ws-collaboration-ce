// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.preview.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.exception.PreviewException;
import com.zextras.carbonio.chats.core.infrastructure.preview.PreviewService;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.preview.sdk.PreviewClient;
import com.zextras.carbonio.preview.sdk.PreviewResponse;
import com.zextras.carbonio.preview.sdk.QueryBuilder;
import io.vavr.control.Option;

import java.util.UUID;

@Singleton
public class PreviewServiceImpl implements PreviewService {

  private static final String SERVICE_TYPE_CHATS = "chats";

  private final PreviewClient previewClient;

  private final RoomService roomService;

  private final FileMetadataRepository fileMetadataRepository;

  @Inject
  public PreviewServiceImpl(
      RoomService roomService,
      FileMetadataRepository fileMetadataRepository,
      PreviewClient previewClient) {
    this.previewClient = previewClient;
    this.roomService = roomService;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  private FileResponse remapPreviewResponse(PreviewResponse response) {
    try {
      return new FileResponse(response.getContent(), response.getLength(), response.getMimeType());
    } catch (Exception t) {
      throw new PreviewException(t);
    }
  }

  @Override
  public FileResponse getImage(
      UserPrincipal user,
      UUID fileId,
      String area,
      Option<ImageQualityEnumDto> quality,
      Option<ImageTypeEnumDto> outputFormat,
      Option<Boolean> crop) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    QueryBuilder parameters =
        new QueryBuilder()
            .ownerId(metadata.getUserId())
            .serviceType(SERVICE_TYPE_CHATS)
            .fileId(fileId.toString())
            .version(0)
            .area(area);
    quality.forEach(q -> parameters.quality(q.toString().toLowerCase()));
    outputFormat.forEach(f -> parameters.outputFormat(f.toString().toLowerCase()));
    crop.forEach(parameters::crop);

    try {
      return remapPreviewResponse(previewClient.getPreviewOfImage(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw new PreviewException(e);
    }
  }

  @Override
  public FileResponse getImageThumbnail(
      UserPrincipal user,
      UUID fileId,
      String area,
      Option<ImageQualityEnumDto> quality,
      Option<ImageTypeEnumDto> outputFormat,
      Option<ImageShapeEnumDto> shape) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    QueryBuilder parameters =
        new QueryBuilder()
            .ownerId(metadata.getUserId())
            .serviceType(SERVICE_TYPE_CHATS)
            .fileId(fileId.toString())
            .version(0)
            .area(area);
    quality.forEach(q -> parameters.quality(q.toString().toLowerCase()));
    outputFormat.forEach(f -> parameters.outputFormat(f.toString().toLowerCase()));
    shape.forEach(s -> parameters.shape(s.toString().toLowerCase()));

    try {
      return remapPreviewResponse(previewClient.getThumbnailOfImage(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw new PreviewException(e);
    }
  }

  @Override
  public FileResponse getPDF(UserPrincipal user, UUID fileId, Integer firstPage, Integer lastPage) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    QueryBuilder parameters =
        new QueryBuilder()
            .ownerId(metadata.getUserId())
            .serviceType(SERVICE_TYPE_CHATS)
            .fileId(fileId.toString())
            .version(0);
    Option.of(firstPage).forEach(parameters::firstPage);
    Option.of(lastPage).forEach(parameters::lastPage);

    try {
      return remapPreviewResponse(previewClient.getPreviewOfPdf(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw new PreviewException(e);
    }
  }

  @Override
  public FileResponse getPDFThumbnail(
      UserPrincipal user,
      UUID fileId,
      String area,
      Option<ImageQualityEnumDto> quality,
      Option<ImageTypeEnumDto> outputFormat,
      Option<ImageShapeEnumDto> shape) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    QueryBuilder parameters =
        new QueryBuilder()
            .ownerId(metadata.getUserId())
            .serviceType(SERVICE_TYPE_CHATS)
            .fileId(fileId.toString())
            .version(0)
            .area(area);
    quality.forEach(q -> parameters.quality(q.toString().toLowerCase()));
    outputFormat.forEach(f -> parameters.outputFormat(f.toString().toLowerCase()));
    shape.forEach(s -> parameters.shape(s.toString().toLowerCase()));

    try {
      return remapPreviewResponse(previewClient.getThumbnailOfPdf(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw new PreviewException(e);
    }
  }

  private FileMetadata getValidMetadata(UUID fileId) {
    return fileMetadataRepository
        .getById(fileId.toString())
        .orElseThrow(
            () ->
                new com.zextras.carbonio.chats.core.exception.NotFoundException(
                    String.format("File with id '%s' not found", fileId)));
  }

  @Override
  public boolean isAlive() {
    return previewClient.healthReady();
  }
}
