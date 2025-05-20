// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ROOM_USER_SETTINGS", schema = "CHATS")
public class RoomUserSettings {

  @EmbeddedId private SubscriptionId id;

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

  public RoomUserSettings() {}

  public RoomUserSettings(Room room, String userId) {
    this.id = new SubscriptionId(room.getId(), userId);
    this.room = room;
    this.userId = userId;
  }

  public static RoomUserSettings create() {
    return new RoomUserSettings();
  }

  public static RoomUserSettings create(Room room, String userId) {
    return new RoomUserSettings(room, userId);
  }

  public SubscriptionId getId() {
    return id;
  }

  public RoomUserSettings id(SubscriptionId id) {
    this.id = id;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public RoomUserSettings userId(String userId) {
    this.userId = userId;
    return this;
  }

  public Room getRoom() {
    return room;
  }

  public RoomUserSettings room(Room room) {
    this.room = room;
    return this;
  }

  public OffsetDateTime getMutedUntil() {
    return mutedUntil;
  }

  public RoomUserSettings mutedUntil(OffsetDateTime mutedUntil) {
    this.mutedUntil = mutedUntil;
    return this;
  }

  public OffsetDateTime getClearedAt() {
    return clearedAt;
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
