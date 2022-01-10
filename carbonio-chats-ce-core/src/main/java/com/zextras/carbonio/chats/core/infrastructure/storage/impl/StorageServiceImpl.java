package com.zextras.carbonio.chats.core.infrastructure.storage.impl;

import com.zextras.carbonio.chats.core.exception.InternalErrorException;
import com.zextras.carbonio.chats.core.config.AppConfig;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.storage.StorageService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.commons.io.FileUtils;

@Singleton
public class StorageServiceImpl implements StorageService {

  private final AppConfig appConfig;

  @Inject
  public StorageServiceImpl(AppConfig appConfig) {
    this.appConfig = appConfig;
  }

  @Override
  public File getFileById(String fileId) {
    return new File(getFilePath(fileId));
  }

  @Override
  public void saveFile(File file, FileMetadata metadata) {
    try {
      FileOutputStream outputStream = new FileOutputStream(getFilePath(metadata.getId()));
      byte[] fileToBytes = FileUtils.readFileToByteArray(file);
      outputStream.write(fileToBytes);
      outputStream.close();
    } catch (IOException e) {
      throw new InternalErrorException("An error occurred while file saving", e);
    }
  }

  @Override
  public void deleteFile(String fileId) {
    try {
      Files.delete(Path.of(getFilePath(fileId)));
    } catch (IOException e) {
      throw new InternalErrorException("An error occurred while file deleting", e);
    }
  }

  private String getFilePath(String fileId) {
    String folder = appConfig.get(String.class, "STORAGE_FOLDER")
      .orElseThrow(() -> new InternalErrorException("Storage folder property not found"));
    return String.join(folder.endsWith("/") ? "" : "/", folder, fileId);
  }
}
