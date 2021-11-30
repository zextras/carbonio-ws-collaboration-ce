package com.zextras.chats.core.data.entity;

import java.time.LocalDateTime;
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
  private LocalDateTime mutedUntil;

  @Column(name = "CLEARED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private LocalDateTime clearedAt;

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

  public LocalDateTime getMutedUntil() {
    return mutedUntil;
  }

  public void setMutedUntil(LocalDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
  }

  public RoomUserSettings mutedUntil(LocalDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
    return this;
  }

  public LocalDateTime getClearedAt() {
    return clearedAt;
  }

  public void setClearedAt(LocalDateTime clearedAt) {
    this.clearedAt = clearedAt;
  }

  public RoomUserSettings clearedAt(LocalDateTime clearedAt) {
    this.clearedAt = clearedAt;
    return this;
  }
}
