// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.OrderDirection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.SortOrder;

public interface FileMetadataRepository {

  /**
   * Gets file metadata info from FILE_METADATA table by identifier
   *
   * @param fileId file identifier
   * @return The required file metadata
   */
  Optional<FileMetadata> getById(String fileId);

  /**
   * Gets a list of file metadata info filtered by roomId and type
   *
   * @param roomId room identifier
   * @param type   file metadata type {@link FileMetadataType}
   * @param orderDirection  if present, sets the result order by creation date {@link SortOrder}
   * @return The required file metadata list {@link FileMetadata}
   */
  List<FileMetadata> getByRoomIdAndType(String roomId, FileMetadataType type, @Nullable OrderDirection orderDirection);

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
