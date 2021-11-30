package com.zextras.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ATTACHMENT", schema = "CHATS")
public class Attachment {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 256, nullable = false)
  private String name;

  @Column(name = "ORIGINAL_SIZE", length = 256, nullable = false)
  private Long originalSize;

  @Column(name = "MIME_TYPE", length = 64, nullable = false)
  private String mimeType;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private LocalDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private LocalDateTime updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Long getOriginalSize() {
    return originalSize;
  }

  public void setOriginalSize(Long originalSize) {
    this.originalSize = originalSize;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
