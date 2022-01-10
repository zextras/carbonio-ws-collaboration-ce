package com.zextras.carbonio.chats.core.infrastructure.storage;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.io.File;

public interface StorageService {

  File getFileById(String fileId);

  /**
   * Save a file on the repository
   *
   * @param file     file to save
   * @param metadata file metadata
   */
  void saveFile(File file, FileMetadata metadata);

  void deleteFile(String fileId);
}
