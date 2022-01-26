package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.GenericHttpException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.filestore.api.Filestore;
import com.zextras.filestore.api.Filestore.Liveness;
import com.zextras.filestore.model.ChatsIdentifier;
import com.zextras.storages.api.exception.StoragesException;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class SlimstoreStorageServiceImpl implements StorageService {

  private final Filestore filestore;

  @Inject
  public SlimstoreStorageServiceImpl(Filestore filestore) {
    this.filestore = filestore;
  }

  @Override
  public File getFileById(String fileId, String currentUserId) {
    try {
      File file = File.createTempFile(fileId, ".tmp");
      FileUtils.copyInputStreamToFile(
        filestore.download(ChatsIdentifier.of(fileId, currentUserId)),
        file);
      return file;
    } catch (Exception e) {
      throw new InternalErrorException(String.format("Cannot recover the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      filestore.uploadPut(
        ChatsIdentifier.of(metadata.getId(), currentUserId),
        FileUtils.openInputStream(file));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file inserting", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String currentUserId) {
    try {
      filestore.delete(ChatsIdentifier.of(fileId, currentUserId));
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file deleting", e);
    }
  }

  @Override
  public boolean isAlive() {
    return filestore.checkLiveness().equals(Liveness.OK);
  }
}
