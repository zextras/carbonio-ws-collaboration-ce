package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import java.util.Optional;

public interface FileMetadataRepository {

  /**
   * Gets file metadata info from FILE_METADATA table by identifier
   *
   * @param fileId file identifier
   * @return The required file metadata
   */
  Optional<FileMetadata> getById(String fileId);

  /**
   * Saves a new file metadata entity
   *
   * @param metadata metadata entity to save
   * @return Saved metadata info
   */
  FileMetadata save(FileMetadata metadata);

  /**
   * Deletes metadata info
   *
   * @param metadata metadata info to delete
   */
  void delete(FileMetadata metadata);

}
