package com.zextras.carbonio.chats.core.repository.impl;

import com.zextras.carbonio.chats.core.data.entity.FileMetadata;
import com.zextras.carbonio.chats.core.repository.FileMetadataRepository;
import io.ebean.Database;
import io.ebean.annotation.Transactional;
import java.util.Optional;
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
  public Optional<FileMetadata> getById(String roomId) {
    return db.find(FileMetadata.class)
      .where()
      .eq("id", roomId)
      .findOneOrEmpty();
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
