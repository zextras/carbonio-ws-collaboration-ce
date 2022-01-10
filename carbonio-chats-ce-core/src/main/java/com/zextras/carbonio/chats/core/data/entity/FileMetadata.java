package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.FileMetadataType;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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

  @Column(name = "MIME_TYPE", length = 64, nullable = false)
  private String mimeType;

  @Column(name = "TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private FileMetadataType type;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
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

  public void setId(String id) {
    this.id = id;
  }

  public FileMetadata id(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public FileMetadata name(String name) {
    this.name = name;
    return this;
  }

  public Long getOriginalSize() {
    return originalSize;
  }

  public void setOriginalSize(Long originalSize) {
    this.originalSize = originalSize;
  }

  public FileMetadata originalSize(Long originalSize) {
    this.originalSize = originalSize;
    return this;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public FileMetadata mimeType(String mimeType) {
    this.mimeType = mimeType;
    return this;
  }

  public FileMetadataType getType() {
    return type;
  }

  public void setType(FileMetadataType type) {
    this.type = type;
  }

  public FileMetadata type(FileMetadataType type) {
    this.type = type;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public FileMetadata userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
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
