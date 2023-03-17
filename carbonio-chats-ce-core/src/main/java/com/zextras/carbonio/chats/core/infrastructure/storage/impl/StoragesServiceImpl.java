// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.config.ChatsConstant;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.BadRequestException;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.preview.PreviewClient;
import com.zextras.carbonio.preview.queries.BlobResponse;
import com.zextras.carbonio.preview.queries.Query;
import com.zextras.carbonio.preview.queries.Query.QueryBuilder;
import com.zextras.carbonio.preview.queries.enums.Format;
import com.zextras.carbonio.preview.queries.enums.ServiceType;
import com.zextras.filestore.api.Filestore.Liveness;
import com.zextras.filestore.model.BulkDeleteRequestItem;
import com.zextras.filestore.model.BulkDeleteResponseItem;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.filestore.model.IdentifierType;
import com.zextras.storages.api.StoragesClient;
import io.vavr.control.Try;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class StoragesServiceImpl implements StoragesService {

  private final StoragesClient storagesClient;

  @Inject
  public StoragesServiceImpl(StoragesClient storagesClient){
    this.storagesClient = storagesClient;
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
      throw new StorageException(String.format("Cannot retrieve the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      storagesClient.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        FileUtils.openInputStream(file));
    } catch (Exception e) {
      throw new StorageException("An error occurred while uploading the file", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String ownerId) {
    try {
      storagesClient.delete(ChatsIdentifier.of(fileId, ownerId));
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting the file", e);
    }
  }

  @Override
  public List<String> deleteFileList(List<String> fileIds, String currentUserId) {
    try {
      return fileIds.size() == 0 ? List.of() :
        storagesClient.bulkDelete(IdentifierType.chats, currentUserId, fileIds.stream()
            .map(BulkDeleteRequestItem::chatsItem).collect(Collectors.toList()))
          .stream().map(BulkDeleteResponseItem::getNode).collect(Collectors.toList());
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting files list", e);
    }
  }

  @Override
  public boolean isAlive() {
    return storagesClient.checkLiveness().equals(Liveness.OK);
  }
}
