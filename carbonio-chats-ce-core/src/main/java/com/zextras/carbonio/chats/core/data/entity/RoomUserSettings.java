package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "ROOM_USER_SETTINGS", schema = "CHATS")
public class RoomUserSettings {

  @EmbeddedId
  private SubscriptionId id;

  @MapsId("userId")
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roomId")
  @JoinColumn(name = "ROOM_ID")
  private Room room;

  @Column(name = "MUTED_UNTIL")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime mutedUntil;

  @Column(name = "CLEARED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime clearedAt;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;


  public static RoomUserSettings create() {
    return new RoomUserSettings();
  }

  public SubscriptionId getId() {
    return id;
  }

  public void setId(SubscriptionId id) {
    this.id = id;
  }

  public RoomUserSettings id(SubscriptionId id) {
    this.id = id;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String user) {
    this.userId = user;
  }

  public RoomUserSettings userId(String user) {
    this.userId = user;
    return this;
  }

  public Room getRoom() {
    return room;
  }

  public void setRoom(Room room) {
    this.room = room;
  }

  public RoomUserSettings room(Room room) {
    this.room = room;
    return this;
  }

  public OffsetDateTime getMutedUntil() {
    return mutedUntil;
  }

  public void setMutedUntil(OffsetDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
  }

  public RoomUserSettings mutedUntil(OffsetDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
    return this;
  }

  public OffsetDateTime getClearedAt() {
    return clearedAt;
  }

  public void setClearedAt(OffsetDateTime clearedAt) {
    this.clearedAt = clearedAt;
  }

  public RoomUserSettings clearedAt(OffsetDateTime clearedAt) {
    this.clearedAt = clearedAt;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
