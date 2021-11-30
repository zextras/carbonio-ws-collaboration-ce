package com.zextras.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.LocalDateTime;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "CHATS_USER", schema = "CHATS")
public class User {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "LAST_SEEN")
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime lastSeen;

  @Column(name = "STATUS_MESSAGE", length = 256, nullable = false)
  private String statusMessage = "";

  @Column(name = "IMAGE")
  @Lob
  private byte[] image;

  @Column(name = "IMAGE_UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date imageUpdatedAt;

  @Column(name = "HASH", length = 256, unique = true, nullable = false)
  private String hash;

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

  public LocalDateTime getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(LocalDateTime lastSeen) {
    this.lastSeen = lastSeen;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public void setStatusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }

  public Date getImageUpdatedAt() {
    return imageUpdatedAt;
  }

  public void setImageUpdatedAt(Date imageUpdatedAt) {
    this.imageUpdatedAt = imageUpdatedAt;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
