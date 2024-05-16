// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.filestore.api.Filestore.Liveness;
import com.zextras.filestore.model.BulkDeleteRequestItem;
import com.zextras.filestore.model.BulkDeleteResponseItem;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.filestore.model.FilesIdentifier;
import com.zextras.filestore.model.IdentifierType;
import com.zextras.storages.api.StoragesClient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class StoragesServiceImpl implements StoragesService {

  private final StoragesClient storagesClient;

  @Inject
  public StoragesServiceImpl(StoragesClient storagesClient) {
    this.storagesClient = storagesClient;
  }

  @Override
  public InputStream getFileById(String fileId, String ownerId) {
    try {
      return storagesClient.download(ChatsIdentifier.of(fileId, ownerId));
    } catch (Exception e) {
      throw new StorageException(String.format("Cannot retrieve the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(InputStream file, FileMetadata metadata, String currentUserId) {
    try {
      storagesClient.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        file, metadata.getOriginalSize());
    } catch (Exception e) {
      throw new StorageException("An error occurred while uploading the file", e);
    }
  }

  @Override
  public void copyFile(
      String sourceId, String sourceOwnerId, String destinationId, String destinationOwnerId) {
    try {
      storagesClient.copy(
          FilesIdentifier.of(sourceId, 0, sourceOwnerId),
          FilesIdentifier.of(destinationId, 0, destinationOwnerId),
          false);
    } catch (Exception e) {
      throw new StorageException("An error occurred while coping the file", e);
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
      return fileIds.size() == 0
          ? List.of()
          : storagesClient
              .bulkDelete(
                  IdentifierType.chats,
                  currentUserId,
                  fileIds.stream()
                      .map(BulkDeleteRequestItem::chatsItem)
                      .collect(Collectors.toList()))
              .stream()
              .map(BulkDeleteResponseItem::getNode)
              .collect(Collectors.toList());
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting files list", e);
    }
  }

  @Override
  public boolean isAlive() {
    return storagesClient.checkLiveness().equals(Liveness.OK);
  }
}
