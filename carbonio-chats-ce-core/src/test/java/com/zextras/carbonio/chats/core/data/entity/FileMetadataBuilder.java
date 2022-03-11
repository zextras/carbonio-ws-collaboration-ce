package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Objects;

public class FileMetadataBuilder extends FileMetadata {

  public static FileMetadataBuilder create() {
    return new FileMetadataBuilder();
  }

  @Override
  public FileMetadataBuilder id(String id) {
    super.id(id);
    return this;
  }

  @Override
  public FileMetadataBuilder name(String name) {
    super.name(name);
    return this;
  }

  @Override
  public FileMetadataBuilder originalSize(Long originalSize) {
    super.originalSize(originalSize);
    return this;
  }

  @Override
  public FileMetadataBuilder mimeType(String mimeType) {
    super.mimeType(mimeType);
    return this;
  }

  @Override
  public FileMetadataBuilder type(FileMetadataType type) {
    super.type(type);
    return this;
  }

  @Override
  public FileMetadataBuilder userId(String userId) {
    super.userId(userId);
    return this;
  }

  @Override
  public FileMetadataBuilder roomId(String roomId) {
    super.roomId(roomId);
    return this;
  }

  public FileMetadataBuilder createdAt(OffsetDateTime createdAt) {
    try {
      Field createdAtField = FileMetadata.class.getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(this, createdAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public FileMetadataBuilder updatedAt(OffsetDateTime updatedAt) {
    try {
      Field updatedAtField = FileMetadata.class.getDeclaredField("updatedAt");
      updatedAtField.setAccessible(true);
      updatedAtField.set(this, updatedAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || (o.getClass() != FileMetadata.class && o.getClass() != FileMetadataBuilder.class)) {
      return false;
    }
    FileMetadata that = (FileMetadata) o;
    return Objects.equals(getId(), that.getId()) && Objects.equals(getName(), that.getName()) && Objects.equals(
      getOriginalSize(), that.getOriginalSize()) && Objects.equals(getMimeType(), that.getMimeType())
      && getType() == that.getType()
      && Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getRoomId(), that.getRoomId())
      && Objects.equals(getCreatedAt(), that.getCreatedAt()) && Objects.equals(getUpdatedAt(), that.getUpdatedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getName(), getOriginalSize(), getMimeType(), getType(), getUserId(), getRoomId(),
      getCreatedAt(), getUpdatedAt());
  }
}
