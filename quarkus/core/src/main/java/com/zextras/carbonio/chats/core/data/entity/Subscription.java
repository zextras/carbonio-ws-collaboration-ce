// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "SUBSCRIPTION", schema = "CHATS")
public class Subscription {

  @EmbeddedId private SubscriptionId id;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @ManyToOne(fetch = FetchType.EAGER)
  @MapsId("roomId")
  @JoinColumn(name = "ROOM_ID")
  private Room room;

  @Column(name = "JOINED_AT")
  private OffsetDateTime joinedAt;

  @Column(name = "OWNER")
  private Boolean isOwner = false;

  @Column(name = "EXTERNAL")
  private Boolean isExternal = false;

  @Column(name = "TEMPORARY")
  private Boolean isTemporary = false;

  @Column(name = "CREATED_AT")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  public Subscription() {
    this.id = new SubscriptionId();
  }

  public Subscription(Room room, String userId) {
    this.id = new SubscriptionId(room.getId(), userId);
    this.room = room;
    this.userId = userId;
  }

  public static Subscription create() {
    return new Subscription();
  }

  public static Subscription create(Room room, String userId) {
    return new Subscription(room, userId);
  }

  public SubscriptionId getId() {
    return id;
  }

  public String getUserId() {
    return userId;
  }

  public Room getRoom() {
    return room;
  }

  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }

  public Boolean isOwner() {
    return isOwner;
  }

  public Boolean isExternal() {
    return isExternal;
  }

  public Boolean isTemporary() {
    return isTemporary;
  }

  public Subscription id(SubscriptionId id) {
    this.id = id;
    return this;
  }

  public Subscription userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public Subscription room(Room room) {
    this.room = room;
    this.id.roomId(room.getId());
    return this;
  }

  public Subscription joinedAt(OffsetDateTime joinedAt) {
    this.joinedAt = joinedAt;
    return this;
  }

  public Subscription owner(Boolean owner) {
    this.isOwner = owner;
    return this;
  }

  public Subscription external(Boolean external) {
    this.isExternal = external;
    return this;
  }

  public Subscription temporary(Boolean temporary) {
    this.isTemporary = temporary;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
