// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

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
import java.util.Objects;

@Entity
@Table(name = "READ_MARKER", schema = "CHATS")
public class ReadMarker {

  @EmbeddedId private ReadMarkerId id;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roomId")
  @JoinColumn(name = "ROOM_ID")
  private Room room;

  @Column(name = "ITEM_CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime itemCreatedAt;

  public ReadMarker() {
    this.id = new ReadMarkerId();
  }

  public static ReadMarker create() {
    return new ReadMarker();
  }

  public ReadMarkerId getId() {
    return id;
  }

  public ReadMarker id(ReadMarkerId id) {
    this.id = id;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public ReadMarker userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public Room getRoom() {
    return room;
  }

  public ReadMarker room(Room room) {
    this.room = room;
    if (room != null) {
      this.id.roomId(room.getId());
    }
    return this;
  }

  public OffsetDateTime getItemCreatedAt() {
    return itemCreatedAt;
  }

  public ReadMarker itemCreatedAt(OffsetDateTime itemCreatedAt) {
    this.itemCreatedAt = itemCreatedAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReadMarker that = (ReadMarker) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
