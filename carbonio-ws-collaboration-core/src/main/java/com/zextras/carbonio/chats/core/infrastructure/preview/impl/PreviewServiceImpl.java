// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.preview.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.exception.*;
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
  private final AppConfig appConfig;

  @Inject
  public PreviewServiceImpl(
      RoomService roomService,
      FileMetadataRepository fileMetadataRepository,
      PreviewClient previewClient,
      AppConfig appConfig) {
    this.previewClient = previewClient;
    this.roomService = roomService;
    this.fileMetadataRepository = fileMetadataRepository;
    this.appConfig = appConfig;
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

  @Override
  public FileResponse getVideo(
      UserPrincipal user,
      UUID fileId,
      String area,
      Option<ImageQualityEnumDto> quality,
      Option<ImageTypeEnumDto> outputFormat,
      Option<Boolean> crop) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    // Q2 size gate: WSC rejects over-size videos before hitting preview.
    checkVideoSizeGate(metadata);

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
      return remapPreviewResponse(previewClient.getPreviewOfVideo(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw mapVideoPreviewException(e);
    }
  }

  @Override
  public FileResponse getVideoThumbnail(
      UserPrincipal user,
      UUID fileId,
      String area,
      Option<ImageQualityEnumDto> quality,
      Option<ImageTypeEnumDto> outputFormat,
      Option<ImageShapeEnumDto> shape) {
    FileMetadata metadata = getValidMetadata(fileId);
    roomService.getRoomAndValidateUser(UUID.fromString(metadata.getRoomId()), user, false);

    // Q2 size gate: WSC rejects over-size videos before hitting preview.
    checkVideoSizeGate(metadata);

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
      return remapPreviewResponse(previewClient.getThumbnailOfVideo(parameters.build()));
    } catch (com.zextras.carbonio.preview.sdk.PreviewException e) {
      throw mapVideoPreviewException(e);
    }
  }

  /**
   * Maps a {@link com.zextras.carbonio.preview.sdk.PreviewException} from the video proxy call to
   * the appropriate WSC exception so the global handler ({@link
   * com.zextras.carbonio.chats.core.web.exceptions.ChatsHttpExceptionHandler}) emits the correct
   * HTTP status code verbatim.
   *
   * <ul>
   *   <li>202 → {@link VideoPreviewGeneratingException} (preview is enqueuing/generating)
   *   <li>413 → {@link VideoPreviewTooLargeException} (defensive; preview won't normally emit this)
   *   <li>415 → {@link VideoPreviewUnsupportedException} (AV1 / corrupt — terminal)
   *   <li>422 → {@link VideoPreviewFailedException} (generation failed after max retries)
   *   <li>404 → {@link NotFoundException} (no preview and no source blob)
   *   <li>424 → {@link PreviewException} (Failed Dependency: the previewer's DB dependency is
   *       unavailable — video previews are temporarily disabled while other previews keep working)
   *   <li>else → re-wrap as generic {@link PreviewException}
   * </ul>
   */
  private static RuntimeException mapVideoPreviewException(
      com.zextras.carbonio.preview.sdk.PreviewException e) {
    return switch (e.getHttpStatus()) {
      case 202 -> new VideoPreviewGeneratingException();
      case 413 -> new VideoPreviewTooLargeException();
      case 415 -> new VideoPreviewUnsupportedException();
      case 422 -> new VideoPreviewFailedException();
      case 404 -> new NotFoundException("Video preview not found");
      case 424 -> new PreviewException(e);
      default -> new PreviewException(e);
    };
  }

  /**
   * Size gate (Q2). Throws {@link VideoPreviewTooLargeException} (HTTP 413) when the file exceeds
   * {@link ConfigName#MAX_VIDEO_SIZE_PREVIEW_IN_MB} (default 128 MB). The limit is literal: a value
   * of 0 means 0 MB (every video is rejected); for an effectively unlimited cap an admin sets a
   * very large KV value. A missing or null size is treated as exceeding the limit (fail-safe,
   * avoids NPE).
   */
  private void checkVideoSizeGate(FileMetadata metadata) {
    long maxMb =
        appConfig
            .get(Long.class, ConfigName.MAX_VIDEO_SIZE_PREVIEW_IN_MB)
            .orElse(
                (long) ChatsConstant.CONFIGURATIONS_DEFAULT_VALUES.MAX_VIDEO_SIZE_PREVIEW_IN_MB);
    Long originalSize = metadata.getOriginalSize();
    if (originalSize == null) {
      // Cannot determine size — fail-safe: reject.
      throw new VideoPreviewTooLargeException();
    }
    long maxBytes = maxMb * 1024L * 1024L;
    if (originalSize > maxBytes) {
      throw new VideoPreviewTooLargeException();
    }
  }

  private FileMetadata getValidMetadata(UUID fileId) {
    return fileMetadataRepository
        .getById(fileId.toString())
        .orElseThrow(
            () -> new NotFoundException(String.format("File with id '%s' not found", fileId)));
  }

  @Override
  public boolean isAlive() {
    return previewClient.healthReady();
  }
}
