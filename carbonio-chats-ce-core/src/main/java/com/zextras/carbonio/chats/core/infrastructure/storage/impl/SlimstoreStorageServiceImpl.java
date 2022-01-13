package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.exception.GenericHttpException;
import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import com.zextras.filestore.api.Filestore;
import com.zextras.filestore.model.DriveIdentifier;
import com.zextras.slimstore.api.exception.SlimstoreHTTPException;
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
        filestore.download(DriveIdentifier.of(fileId, 1, currentUserId)),
        file);
      return file;
    } catch (SlimstoreHTTPException e) {
      throw new GenericHttpException(e.getCode(), e.getMessage(), e);
    } catch (Exception e) {
      throw new InternalErrorException(String.format("Cannot recover the file '%s'", fileId), e);
    }
  }

  @Override
  public void saveFile(File file, FileMetadata metadata, String currentUserId) {
    try {
      filestore.uploadPut(
        DriveIdentifier.of(metadata.getId(), 1, currentUserId),
        FileUtils.openInputStream(file));
    } catch (SlimstoreHTTPException e) {
      throw new GenericHttpException(e.getCode(), e.getMessage(), e);
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file inserting", e);
    }
  }

  @Override
  public void deleteFile(String fileId, String currentUserId) {
    try {
      filestore.delete(DriveIdentifier.of(fileId, 1, currentUserId));
    } catch (SlimstoreHTTPException e) {
      throw new GenericHttpException(e.getCode(), e.getMessage(), e);
    } catch (Exception e) {
      throw new InternalErrorException("An error occurred while file deleting", e);
    }
  }

  @Override
  public boolean isAlive() {
    // TODO: 13/01/22 waiting of health check in slimstore-sdk
    return true;
  }
}
