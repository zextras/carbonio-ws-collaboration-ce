// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.service.impl;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.config.ConfigName;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.FileResponse;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import com.zextras.carbonio.chats.core.service.PreviewService;
import com.zextras.carbonio.chats.core.service.RoomService;
import com.zextras.carbonio.chats.core.web.security.UserPrincipal;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.preview.queries.BlobResponse;
import com.zextras.carbonio.preview.queries.Query;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.Quality;
import com.zextras.carbonio.preview.queries.enums.ServiceType;
import com.zextras.carbonio.preview.queries.enums.Shape;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.apache.commons.io.FileUtils;


import java.io.File;
import java.time.Instant;
import java.util.UUID;

@Singleton
public class PreviewServiceImpl implements PreviewService {

  private final PreviewClient previewClient;

  private final RoomService roomService;

  private final FileMetadataRepository fileMetadataRepository;

  @Inject
  public PreviewServiceImpl(
    RoomService roomService,
    FileMetadataRepository fileMetadataRepository,
    PreviewClient previewClient

  ) {
    this.previewClient = previewClient;
    this.roomService = roomService;
    this.fileMetadataRepository = fileMetadataRepository;
  }

  private Try<FileResponse> remapBlobToDataFile(Try<BlobResponse> response) {
    return response.map(bResp ->
      Try.of(() -> {
        File file = File.createTempFile(Instant.now().toString(), ".tmp");
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
      .setVersion(1)
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
      .setVersion(1)
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
      .setVersion(1)
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
      .setVersion(1)
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
