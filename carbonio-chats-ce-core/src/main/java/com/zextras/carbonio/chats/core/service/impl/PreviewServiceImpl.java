// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;

import ch.qos.logback.core.util.TimeUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zextras.carbonio.chats.core.data.builder.IdDtoBuilder;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.entity.Room;
import com.zextras.carbonio.chats.core.data.entity.Subscription;
import com.zextras.carbonio.chats.core.data.event.AttachmentAddedEvent;
import com.zextras.carbonio.chats.core.data.event.AttachmentRemovedEvent;
import com.zextras.carbonio.chats.core.data.model.FileContentAndMetadata;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.ForbiddenException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.exception.NotFoundException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.event.EventDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.messaging.MessageDispatcher;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.mapper.AttachmentMapper;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.repository.SubscriptionRepository;
import com.zextras.carbonio.chats.core.service.AttachmentService;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.chats.model.AttachmentDto;
import com.zextras.carbonio.chats.model.AttachmentsPaginationDto;
import com.zextras.carbonio.chats.model.IdDto;
import com.zextras.carbonio.chats.model.ImageQualityEnumDto;
import com.zextras.carbonio.chats.model.ImageShapeEnumDto;
import com.zextras.carbonio.chats.model.ImageTypeEnumDto;
import com.zextras.carbonio.chats.model.RoomTypeDto;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.preview.queries.BlobResponse;
import com.zextras.carbonio.preview.queries.Query;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.Quality;
import com.zextras.carbonio.preview.queries.enums.ServiceType;
import com.zextras.carbonio.preview.queries.enums.Shape;
import io.ebean.annotation.Transactional;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.math.NumberUtils.min;

@Singleton
public class PreviewServiceImpl implements PreviewService {

  private final PreviewClient previewClient;

  private final RoomService roomService;

  private final FileMetadataRepository fileMetadataRepository;

  @Inject
  public PreviewServiceImpl(
    PreviewClient previewclient,
    RoomService roomService,
    FileMetadataRepository fileMetadataRepository
  ) {
    this.previewClient = previewclient;
    this.roomService = roomService;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  private Try<FileResponse> remapBlobToDataFile(Try<BlobResponse> response) {
    return response.map(bResp ->
      Try.of(() -> {
        File file = File.createTempFile("", ".tmp");
        FileUtils.copyInputStreamToFile(bResp.getContent(), file);
        return new FileResponse(file, bResp.getLength(),bResp.getMimeType());
      }).get());
  }

  @Override
  public FileResponse getImage(UserPrincipal user, UUID fileId, String area, Option<Quality> quality, Option<Format> outputFormat, Option<Boolean> crop) {
    validateUser(fileId,user);

    Query.QueryBuilder parameters = new Query.QueryBuilder()
      .setServiceType(ServiceType.CHATS)
      .setNodeId(fileId.toString())
      .setPreviewArea(area);
    quality.map(parameters::setQuality);
    outputFormat.map(parameters::setOutputFormat);
    crop.map(parameters::setCrop);

    return remapBlobToDataFile(previewClient.getPreviewOfImage(parameters.build())).get();
  }

  @Override
  public FileResponse getImageThumbnail(UserPrincipal user,
                                        UUID fileId,
                                        String area,
                                        Option<Quality> quality,
                                        Option<Format> outputFormat,
                                        Option<Shape> shape) {
    validateUser(fileId,user);

    Query.QueryBuilder parameters = new Query.QueryBuilder()
      .setServiceType(ServiceType.CHATS)
      .setNodeId(fileId.toString())
      .setPreviewArea(area);
    quality.map(parameters::setQuality);
    outputFormat.map(parameters::setOutputFormat);
    shape.map(parameters::setShape);

    return remapBlobToDataFile(previewClient.getThumbnailOfImage(parameters.build())).get();
  }

  @Override
  public FileResponse getPDF(UserPrincipal user, UUID fileId, Integer firstPage, Integer lastPage) {
    validateUser(fileId,user);

    Query parameters = new Query.QueryBuilder()
      .setServiceType(ServiceType.CHATS)
      .setNodeId(fileId.toString())
      .setFirstPage(firstPage)
      .setLastPage(lastPage)
      .build();
    return remapBlobToDataFile(previewClient.getPreviewOfPdf(parameters)).get();
  }

  @Override
  public FileResponse getPDFThumbnail(UserPrincipal user,
                                      UUID fileId,
                                      String area,
                                      Option<Quality> quality,
                                      Option<Format> outputFormat,
                                      Option<Shape> shape) {
    validateUser(fileId,user);

    Query.QueryBuilder parameters = new Query.QueryBuilder()
      .setServiceType(ServiceType.CHATS)
      .setNodeId(fileId.toString())
      .setPreviewArea(area);
    quality.map(parameters::setQuality);
    outputFormat.map(parameters::setOutputFormat);
    shape.map(parameters::setShape);

    return remapBlobToDataFile(previewClient.getThumbnailOfPdf(parameters.build())).get();
  }

  private void validateUser(UUID fileId, UserPrincipal user){
    FileMetadata originMetadata = fileMetadataRepository.getById(fileId.toString())
      .orElseThrow(() -> new com.zextras.carbonio.chats.core.exception.NotFoundException(String.format("File with id '%s' not found", fileId)));
    roomService.getRoomEntityAndCheckUser(UUID.fromString(originMetadata.getRoomId()), user, false);
  }

  @Override
  public boolean isAlive() {
    return previewClient.healthReady();
  }
}
