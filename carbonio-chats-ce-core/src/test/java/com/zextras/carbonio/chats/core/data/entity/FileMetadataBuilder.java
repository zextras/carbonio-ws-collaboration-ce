// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;

public class FileMetadataBuilder {

  private final FileMetadata fileMetadata;

  public FileMetadataBuilder() {
    this.fileMetadata = FileMetadata.create();
  }

  public static FileMetadataBuilder create() {
    return new FileMetadataBuilder();
  }

  @Override
  public FileMetadataBuilder clone() {
    return FileMetadataBuilder.create()
      .id(fileMetadata.getId())
      .name(fileMetadata.getName())
      .originalSize(fileMetadata.getOriginalSize())
      .mimeType(fileMetadata.getMimeType())
      .type(fileMetadata.getType())
      .userId(fileMetadata.getUserId())
      .roomId(fileMetadata.getRoomId())
      .createdAt(fileMetadata.getCreatedAt())
      .updatedAt(fileMetadata.getUpdatedAt());
  }

  public FileMetadataBuilder id(String id) {
    fileMetadata.id(id);
    return this;
  }

  public FileMetadataBuilder name(String name) {
    fileMetadata.name(name);
    return this;
  }

  public FileMetadataBuilder originalSize(Long originalSize) {
    fileMetadata.originalSize(originalSize);
    return this;
  }

  public FileMetadataBuilder mimeType(String mimeType) {
    fileMetadata.mimeType(mimeType);
    return this;
  }

  public FileMetadataBuilder type(FileMetadataType type) {
    fileMetadata.type(type);
    return this;
  }

  public FileMetadataBuilder userId(String userId) {
    fileMetadata.userId(userId);
    return this;
  }

  public FileMetadataBuilder roomId(String roomId) {
    fileMetadata.roomId(roomId);
    return this;
  }

  public FileMetadataBuilder createdAt(OffsetDateTime createdAt) {
    try {
      Field createdAtField = FileMetadata.class.getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(fileMetadata, createdAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public FileMetadataBuilder updatedAt(OffsetDateTime updatedAt) {
    try {
      Field updatedAtField = FileMetadata.class.getDeclaredField("updatedAt");
      updatedAtField.setAccessible(true);
      updatedAtField.set(fileMetadata, updatedAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public FileMetadata build() {
    return fileMetadata;
  }
}
