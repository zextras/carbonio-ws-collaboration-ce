// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.AttachmentFilter;
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
   * Gets file metadata info from FILE_METADATA table by userId and/or roomId and type
   *
   * @param userId user identifier
   * @param roomId room identifier
   * @param type file metadata type {@link FileMetadataType}
   * @return The required file metadata
   */
  Optional<FileMetadata> find(String userId, String roomId, FileMetadataType type);

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
   * @param attachmentFilter {@link AttachmentFilter} optional field filters and sort options
   * @return The required file metadata list {@link FileMetadata}
   */
  List<FileMetadata> getByRoomIdAndType(
      String roomId,
      FileMetadataType type,
      int itemsNumber,
      @Nullable PaginationFilter paginationFilter,
      @Nullable AttachmentFilter attachmentFilter);

  /**
   * Counts file metadata rows filtered by roomId and type.
   *
   * @param roomId room identifier
   * @param type file metadata type {@link FileMetadataType}
   * @param attachmentFilter {@link AttachmentFilter} optional field filters
   * @return number of matching file metadata rows
   */
  long countByRoomIdAndType(
      String roomId, FileMetadataType type, @Nullable AttachmentFilter attachmentFilter);

  /**
   * Gets identifiers of files that match the given IDs, belong to the specified room, and are owned
   * by the specified user
   *
   * @param ids list of file identifiers to filter
   * @param roomId room identifier
   * @param userId owner user identifier
   * @return identifiers of matching files
   */
  List<String> getIdsByIdsAndRoomIdAndUserId(List<String> ids, String roomId, String userId);

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
   * Delete a metadata info list by their identifier
   *
   * @param ids identifier of metadata info to delete
   */
  void deleteByIds(List<String> ids);
}
