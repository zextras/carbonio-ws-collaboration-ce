// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.io.File;
import java.util.List;

public interface StoragesService extends HealthIndicator {

  /**
   * Retrieves the file associated to the identifier
   *
   * @param fileId  file identifier
   * @param ownerId identifier of the owner of the file
   * @return Required file {@link File}
   */
  File getFileById(String fileId, String ownerId);

  /**
   * Retrieves a preview of the specified file
   *
   * @param file    file identifier
   * @param ownerId identifier of the owner of the file
   * @return A jpeg preview of the requested file {@link File}
   */
  File getPreview(FileMetadata file, String ownerId);

  /**
   * Saves a file on the repository
   *
   * @param file          file to save
   * @param metadata      file metadata {@link FileMetadata}
   * @param currentUserId identifier of the current user
   */
  void saveFile(File file, FileMetadata metadata, String currentUserId);

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
