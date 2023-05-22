// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.StorageException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.carbonio.chats.core.logging.ChatsLogger;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.filestore.powerstore.api.powerstore.PowerstoreClient;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class StoragesServicePowerstoreImpl implements StoragesService {

  private final PowerstoreClient powerstoreClient;

  @Inject
  public StoragesServicePowerstoreImpl(PowerstoreClient powerstoreClient) {
    this.powerstoreClient = powerstoreClient;
  }

  @Override
  public File getFileById(String fileId, String ownerId) {
    try {
      InputStream in = powerstoreClient.download(ChatsIdentifier.of(fileId, ownerId));
      File file = File.createTempFile(fileId, ".tmp");
      FileUtils.copyInputStreamToFile(in, file);
      return file;
    } catch (Exception e) {
      throw new StorageException(String.format("Cannot retrieve the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      powerstoreClient.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        FileUtils.openInputStream(file), metadata.getOriginalSize());
    } catch (Exception e) {
      throw new StorageException("An error occurred while uploading the file", e);
    }
  }

  @Override
  public void copyFile(String sourceId, String sourceOwnerId, String destinationId, String destinationOwnerId) {
    try {
      powerstoreClient.copy(
        ChatsIdentifier.of(sourceId, sourceOwnerId),
        ChatsIdentifier.of(destinationId, destinationOwnerId), false);
    } catch (Exception e) {
      throw new StorageException("An error occurred while coping the file", e);
    }
  }

  @Override
  public String deleteFile(String fileId, String ownerId) {
    try {
      powerstoreClient.delete(ChatsIdentifier.of(fileId, ownerId));
      return fileId;
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting the file", e);
    }
  }

  @Override
  public List<String> deleteFileList(List<String> fileIds, String currentUserId) {
    try {
      List<String> deletedFilesIds = new ArrayList<>();
      fileIds.forEach(fileId -> {
        try {
          deletedFilesIds.add(deleteFile(fileId, currentUserId));
        } catch (StorageException e) {
          // intentionally left blank
        }
      });
      return deletedFilesIds;
    } catch (Exception e) {
      throw new StorageException("An error occurred while deleting files list", e);
    }
  }

  @Override
  public boolean isAlive() {
    return true;
  }
}
