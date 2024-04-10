// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;

@Entity
@Table(name = "FILE_METADATA", schema = "CHATS")
public class FileMetadata {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 256, nullable = false)
  private String name;

  @Column(name = "ORIGINAL_SIZE", nullable = false)
  private Long originalSize;

  @Column(name = "MIME_TYPE", length = 256, nullable = false)
  private String mimeType;

  @Column(name = "TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private FileMetadataType type;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64)
  private String roomId;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  public static FileMetadata create() {
    return new FileMetadata();
  }

  public String getId() {
    return id;
  }

  public FileMetadata id(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public FileMetadata name(String name) {
    this.name = name;
    return this;
  }

  public Long getOriginalSize() {
    return originalSize;
  }

  public FileMetadata originalSize(Long originalSize) {
    this.originalSize = originalSize;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public FileMetadata mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public FileMetadataType getType() {
    return type;
  }

  public FileMetadata type(FileMetadataType type) {
    this.type = type;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public FileMetadata userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public FileMetadata roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
