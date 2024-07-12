// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.model.PaginationFilter;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.Query;
import io.ebean.annotation.Transactional;
import io.vavr.control.Option;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Singleton
public class EbeanFileMetadataRepository implements FileMetadataRepository {

  private final Database db;

  @Inject
  public EbeanFileMetadataRepository(Database db) {
    this.db = db;
  }

  @Override
  public Optional<FileMetadata> getById(String fileId) {
    return db.find(FileMetadata.class).where().eq("id", fileId).findOneOrEmpty();
  }

  @Override
  public Optional<FileMetadata> find(String userId, String roomId, FileMetadataType type) {
    Query<FileMetadata> query = db.find(FileMetadata.class);
    Option.of(userId).map(p -> query.where().eq("userId", p));
    Option.of(roomId).map(p -> query.where().eq("roomId", p));
    Option.of(type).map(p -> query.where().eq("type", p));
    return query.findOneOrEmpty();
  }

  @Override
  public List<String> getIdsByRoomIdAndType(String roomId, FileMetadataType type) {
    return db.find(FileMetadata.class)
        .where()
        .eq("roomId", roomId)
        .and()
        .eq("type", type)
        .select("id")
        .findSingleAttributeList();
  }

  @Override
  @Transactional
  public List<FileMetadata> getByRoomIdAndType(
      String roomId,
      FileMetadataType type,
      int itemsNumber,
      @Nullable PaginationFilter paginationFilter) {
    ExpressionList<FileMetadata> query =
        db.find(FileMetadata.class).where().eq("roomId", roomId).and().eq("type", type);
    if (paginationFilter != null) {
      query
          .and()
          .lt("createdAt", paginationFilter.getCreatedAt())
          .or()
          .eq("createdAt", paginationFilter.getCreatedAt())
          .lt("id", paginationFilter.getId());
    }
    return query
        .orderBy()
        .desc("createdAt")
        .orderBy()
        .desc("id")
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
    if (!ids.isEmpty()) {
      db.deleteAll(FileMetadata.class, ids);
    }
  }
}
