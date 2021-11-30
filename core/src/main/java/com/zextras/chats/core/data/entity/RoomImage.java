package com.zextras.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ROOM_IMAGE", schema = "CHATS")
public class RoomImage {

  @Id
  @Column(name = "ROOM_ID")
  private String roomId;

  @Column(name = "IMAGE")
  @Lob
  private byte[] image;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private LocalDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private LocalDateTime updatedAt;

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public byte[] getImage() {
    return image;
  }

  public void setImage(byte[] image) {
    this.image = image;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
