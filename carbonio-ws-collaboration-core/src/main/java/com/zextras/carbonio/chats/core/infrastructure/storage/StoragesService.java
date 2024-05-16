// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.io.InputStream;
import java.util.List;

public interface StoragesService extends HealthIndicator {

  /**
   * Retrieves the file stream associated to the identifier
   *
   * @param fileId  file identifier
   * @param ownerId identifier of the owner of the file
   * @return Required file stream {@link InputStream}
   */
  InputStream getFileById(String fileId, String ownerId);

  /**
   * Saves a file on the repository
   *
   * @param file          file stream to save {@link InputStream}
   * @param metadata      file metadata {@link FileMetadata}
   * @param currentUserId identifier of the current user
   */
  void saveFile(InputStream file, FileMetadata metadata, String currentUserId);

  /**
   * Copies a file
   *
   * @param sourceId           identifier of the source file
   * @param sourceOwnerId      identifier of the source file owner
   * @param destinationId      identifier of the destination file
   * @param destinationOwnerId identifier of the destination file owner
   */
  void copyFile(String sourceId, String sourceOwnerId, String destinationId, String destinationOwnerId);

  /**
   * Deletes a file from the repository
   *
   * @param fileId  file identifier
   * @param ownerId identifier of the owner of the file
   */
  void deleteFile(String fileId, String ownerId);

  /**
   * Deletes file list by their identifiers
   *
   * @param fileIds       identifiers list of files to delete
   * @param currentUserId identifier of the current user
   * @return identifiers list of files deleted
   */
  List<String> deleteFileList(List<String> fileIds, String currentUserId);
}
