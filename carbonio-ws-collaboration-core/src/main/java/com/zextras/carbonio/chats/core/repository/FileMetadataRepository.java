// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import jakarta.annotation.Nullable;
import java.util.List;
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
   * Gets all identifiers of file metadata by the room identifier and type
   *
   * @param roomId room identifier
   * @param type file metadata type {@link FileMetadataType}
   * @return The required file metadata identifiers list
   */
  List<String> getIdsByRoomIdAndType(String roomId, FileMetadataType type);

  /**
   * Gets a paginated list of file metadata info filtered by roomId and type
   *
   * @param roomId room identifier
   * @param type file metadata type {@link FileMetadataType}
   * @param itemsNumber items number of metadata to return
   * @param paginationFilter {@link PaginationFilter} to apply to the query for keyset pagination
   * @return The required file metadata list {@link FileMetadata}
   */
  List<FileMetadata> getByRoomIdAndType(
      String roomId,
      FileMetadataType type,
      int itemsNumber,
      @Nullable PaginationFilter paginationFilter);

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

  /**
   * Deletes metadata info by its identifier
   *
   * @param id identifier of metadata to delete
   */
  void deleteById(String id);

  /**
   * Delete a metadata info list by their identifier
   *
   * @param ids identifier of metadata info to delete
   */
  void deleteByIds(List<String> ids);
}
