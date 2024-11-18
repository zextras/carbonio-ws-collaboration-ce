// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.filestore.model.BulkDeleteRequestItem;
import com.zextras.filestore.model.BulkDeleteResponseItem;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.filestore.model.IdentifierType;
import com.zextras.filestore.powerstore.api.powerstore.PowerstoreClient;
import java.io.InputStream;
import java.util.List;

@Singleton
public class StoragesServicePowerstoreImpl implements StoragesService {

  private final PowerstoreClient powerstoreClient;

  @Inject
  public StoragesServicePowerstoreImpl(PowerstoreClient powerstoreClient) {
    this.powerstoreClient = powerstoreClient;
  }

  @Override
  public InputStream getFileStreamById(String fileId, String ownerId) {
    try {
      return powerstoreClient.download(ChatsIdentifier.of(fileId, ownerId));
    } catch (Exception e) {
      throw new StorageException(String.format("Cannot retrieve the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(InputStream file, String fileId, String ownerId, long originalSize) {
    try {
      powerstoreClient.uploadPut(ChatsIdentifier.of(fileId, ownerId), file, originalSize);
    } catch (Exception e) {
      throw new StorageException("An error occurred while uploading the file", e);
    }
  }

  @Override
  public void copyFile(
      String sourceId, String sourceOwnerId, String destinationId, String destinationOwnerId) {
    try {
      powerstoreClient.copy(
          ChatsIdentifier.of(sourceId, sourceOwnerId),
          ChatsIdentifier.of(destinationId, destinationOwnerId),
          false);
    } catch (Exception e) {
      throw new StorageException("An error occurred while coping the file", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String ownerId) {
    try {
      powerstoreClient.delete(ChatsIdentifier.of(fileId, ownerId));
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting the file", e);
    }
  }

  @Override
  public List<String> deleteFileList(List<String> fileIds, String ownerId) {
    try {
      return fileIds.isEmpty()
          ? List.of()
          : powerstoreClient
              .bulkDelete(
                  IdentifierType.chats,
                  ownerId,
                  fileIds.stream().map(BulkDeleteRequestItem::chatsItem).toList())
              .stream()
              .map(BulkDeleteResponseItem::getNode)
              .toList();
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting files list", e);
    }
  }

  @Override
  public boolean isAlive() {
    return true;
  }
}
