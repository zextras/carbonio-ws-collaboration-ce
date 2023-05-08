// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

@Transactional
@Singleton
public class EbeanFileMetadataRepository implements FileMetadataRepository {

  private final Database db;

  @Inject
  public EbeanFileMetadataRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<FileMetadata> getById(String fileId) {
    return db.find(FileMetadata.class)
      .where()
      .eq("id", fileId)
      .findOneOrEmpty();
  }

  @Override
  public List<String> getIdsByRoomIdAndType(String roomId, FileMetadataType type) {
    return db.find(FileMetadata.class)
      .where()
      .eq("roomId", roomId).and().eq("type", type)
      .select("id")
      .findSingleAttributeList();
  }


  @Override
  public List<FileMetadata> getByRoomIdAndType(
    String roomId, FileMetadataType type, int itemsNumber, @Nullable PaginationFilter paginationFilter
  ) {
    ExpressionList<FileMetadata> query = db.find(FileMetadata.class)
      .where()
      .eq("roomId", roomId).and()
      .eq("type", type);
    if (paginationFilter != null) {
      query.and()
        .lt("createdAt", paginationFilter.getCreatedAt()).or()
        .eq("createdAt", paginationFilter.getCreatedAt())
        .lt("id", paginationFilter.getId());
    }
    return query.order().desc("createdAt")
      .order().desc("id")
      .setMaxRows(itemsNumber)
      .findList();

  }

  @Override
  public FileMetadata save(FileMetadata metadata) {
    db.save(metadata);
    return metadata;
  }

  @Override
  public void delete(FileMetadata metadata) {
    db.delete(metadata);
  }

  @Override
  public void deleteById(String id) {
    db.delete(FileMetadata.class, id);
  }

  @Override
  public void deleteByIds(List<String> ids) {
    if (ids.size() > 0) {
      db.deleteAll(FileMetadata.class, ids);
    }
  }
}