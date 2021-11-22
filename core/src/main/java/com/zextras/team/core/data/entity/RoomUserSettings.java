package com.zextras.team.core.data.entity;

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
@Table(name = "ROOM_USER_SETTINGS", schema = "TEAM")
public class RoomUserSettings {

  @EmbeddedId
  private SubscriptionId id;

  @MapsId("userId")
  private String user;

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

  public SubscriptionId getId() {
    return id;
  }

  public void setId(SubscriptionId id) {
    this.id = id;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public Room getRoom() {
    return room;
  }

  public void setRoom(Room room) {
    this.room = room;
  }

  public LocalDateTime getMutedUntil() {
    return mutedUntil;
  }

  public void setMutedUntil(LocalDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
  }

  public LocalDateTime getClearedAt() {
    return clearedAt;
  }

  public void setClearedAt(LocalDateTime clearedAt) {
    this.clearedAt = clearedAt;
  }
}
