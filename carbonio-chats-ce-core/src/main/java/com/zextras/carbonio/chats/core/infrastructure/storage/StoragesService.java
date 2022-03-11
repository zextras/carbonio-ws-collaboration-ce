// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.storage;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.infrastructure.HealthIndicator;
import java.io.File;
import java.io.IOException;

public interface StoragesService extends HealthIndicator {

  /**
   * Retrieves the file associated to the identifier
   *
   * @param fileId  file identifier
   * @param ownerId identifier of the current user
   * @return Required file {@link File}
   */
  File getFileById(String fileId, String ownerId);

  File getPreview(FileMetadata file, String ownerId);

  /**
   * Saves a file on the repository
   *
   * @param file          file to save
   * @param metadata      file metadata
   * @param currentUserId identifier of the current user
   */
  void saveFile(File file, FileMetadata metadata, String currentUserId);

  /**
   * Deletes a file from the repository
   *
   * @param fileId        file identifier
   * @param ownerId identifier of the current user
   */
  void deleteFile(String fileId, String ownerId);
}
