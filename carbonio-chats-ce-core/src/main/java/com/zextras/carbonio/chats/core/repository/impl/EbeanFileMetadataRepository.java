// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import com.zextras.carbonio.chats.core.data.type.OrderDirection;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import io.ebean.Database;
import io.ebean.ExpressionList;
import io.ebean.OrderBy.Property;
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
  public List<FileMetadata> getByRoomIdAndType(
    String roomId, FileMetadataType type, @Nullable OrderDirection orderDirection
  ) {
    ExpressionList<FileMetadata> query = db.find(FileMetadata.class)
      .where()
      .eq("roomId", roomId).and()
      .eq("type", type);
    Optional.ofNullable(orderDirection).ifPresent(o ->
      query.order().add(new Property("createdAt", OrderDirection.ASC.equals(o))));

    return query.findList();
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
}
