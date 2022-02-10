// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StoragesService;
import com.zextras.filestore.api.Filestore.Liveness;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.storages.api.StoragesClient;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class StoragesServiceImpl implements StoragesService {

  private final StoragesClient storagesClient;

  @Inject
  public StoragesServiceImpl(StoragesClient storagesClient) {
    this.storagesClient = storagesClient;
  }

  @Override
  public File getFileById(String fileId, String currentUserId) {
    try {
      File file = File.createTempFile(fileId, ".tmp");
      FileUtils.copyInputStreamToFile(
        storagesClient.download(ChatsIdentifier.of(fileId, currentUserId)),
        file);
      return file;
    } catch (Exception e) {
      throw new InternalErrorException(String.format("Cannot recover the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      storagesClient.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        FileUtils.openInputStream(file));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file inserting", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String currentUserId) {
    try {
      storagesClient.delete(ChatsIdentifier.of(fileId, currentUserId));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file deleting", e);
    }
  }

  @Override
  public boolean isAlive() {
    return storagesClient.checkLiveness().equals(Liveness.OK);
  }
}
