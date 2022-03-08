// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.preview.queries.BlobResponse;
import com.zextras.carbonio.preview.queries.Query;
import com.zextras.carbonio.preview.queries.Query.QueryBuilder;
import com.zextras.carbonio.preview.queries.enums.ServiceType;
import com.zextras.filestore.api.Filestore.Liveness;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.storages.api.StoragesClient;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class StoragesServiceImpl implements StoragesService {

  private final StoragesClient storagesClient;
  private final PreviewClient  previewClient;

  @Inject
  public StoragesServiceImpl(StoragesClient storagesClient, PreviewClient previewClient) {
    this.storagesClient = storagesClient;
    this.previewClient = previewClient;
  }

  @Override
  public File getFileById(String fileId, String ownerId) {
    try {
      File file = File.createTempFile(fileId, ".tmp");
      FileUtils.copyInputStreamToFile(
        storagesClient.download(ChatsIdentifier.of(fileId, ownerId)),
        file);
      return file;
    } catch (Exception e) {
      throw new InternalErrorException(String.format("Cannot retrieve the file '%s'", fileId), e);
    }
  }

  @Override
  public File getPreview(FileMetadata metadata, String ownerId) {
    Query query = new QueryBuilder(metadata.getId(), 1, ServiceType.CHATS)
      .setPreviewArea(ChatsConstant.PREVIEW_AREA)
      .build();
    Try<BlobResponse> response;
    if (metadata.getMimeType().startsWith("image/")) {
      response = previewClient.getPreviewOfImage(query);
    } else if (metadata.getMimeType().startsWith("application/pdf")) {
      response = previewClient.getPreviewOfPdf(query);
    } else {
      throw new BadRequestException("MimeType not supported by previewer");
    }
    File file;
    try {
      file = File.createTempFile(metadata.getId(), ".tmp");
      FileUtils.copyInputStreamToFile(response.getOrElseThrow(
        (Supplier<IOException>) IOException::new).getContent(), file);
    } catch (IOException e) {
      throw new InternalErrorException("An error occurred getting preview", e);
    }
    return file;
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      storagesClient.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        FileUtils.openInputStream(file));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while uploading the file", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String currentUserId) {
    try {
      storagesClient.delete(ChatsIdentifier.of(fileId, currentUserId));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while deleting the file", e);
    }
  }

  @Override
  public boolean isAlive() {
    return storagesClient.checkLiveness().equals(Liveness.OK);
  }
}
