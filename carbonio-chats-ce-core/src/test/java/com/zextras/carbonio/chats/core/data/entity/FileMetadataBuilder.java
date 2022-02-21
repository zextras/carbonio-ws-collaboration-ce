package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;

public class FileMetadataBuilder extends FileMetadata {

  public static FileMetadataBuilder create() {
    return new FileMetadataBuilder();
  }

  @Override
  public FileMetadataBuilder id(String id) {
    super.setId(id);
    return this;
  }

  @Override
  public FileMetadataBuilder name(String name) {
    super.setName(name);
    return this;
  }

  @Override
  public FileMetadataBuilder originalSize(Long originalSize) {
    super.setOriginalSize(originalSize);
    return this;
  }

  @Override
  public FileMetadataBuilder mimeType(String mimeType) {
    super.setMimeType(mimeType);
    return this;
  }

  @Override
  public FileMetadataBuilder type(FileMetadataType type) {
    super.setType(type);
    return this;
  }

  @Override
  public FileMetadataBuilder userId(String userId) {
    super.setUserId(userId);
    return this;
  }

  @Override
  public FileMetadataBuilder roomId(String roomId) {
    super.setRoomId(roomId);
    return this;
  }

  public FileMetadataBuilder createdAt(OffsetDateTime createdAt) throws NoSuchFieldException, IllegalAccessException {
    Field createdAtField = FileMetadata.class.getDeclaredField("createdAt");
    createdAtField.setAccessible(true);
    createdAtField.set(this, createdAt);
    return this;
  }

  public FileMetadataBuilder updatedAt(OffsetDateTime updatedAt) throws NoSuchFieldException, IllegalAccessException {
    Field updatedAtField = FileMetadata.class.getDeclaredField("updatedAt");
    updatedAtField.setAccessible(true);
    updatedAtField.set(this, updatedAt);
    return this;
  }
}
