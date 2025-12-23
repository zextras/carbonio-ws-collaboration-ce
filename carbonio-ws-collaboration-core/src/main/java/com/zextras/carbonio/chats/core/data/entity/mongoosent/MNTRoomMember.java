// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

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
@Table(name = "MONGOOSENT_ROOM_MEMBER", schema = "CHATS")
public class MNTRoomMember {

  @EmbeddedId
  private MNTRoomMemberId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("roomId")
  @JoinColumn(name = "ROOM_ID")
  private MNTRoom room;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @Column(name = "IS_OWNER", nullable = false)
  private Boolean isOwner = false;

  @Column(name = "MUTED", nullable = false)
  private Boolean muted = false;

  @Column(name = "JOINED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime joinedAt;

  public MNTRoomMember() {
    this.id = new MNTRoomMemberId();
    this.joinedAt = OffsetDateTime.now();
  }

  public MNTRoomMember(MNTRoom room, String userId) {
    this.id = new MNTRoomMemberId(room.getId(), userId);
    this.room = room;
    this.userId = userId;
    this.joinedAt = OffsetDateTime.now();
  }

  public static MNTRoomMember create() {
    return new MNTRoomMember();
  }

  public static MNTRoomMember create(MNTRoom room, String userId) {
    return new MNTRoomMember(room, userId);
  }

  public MNTRoomMemberId getId() {
    return id;
  }

  public MNTRoom getRoom() {
    return room;
  }

  public MNTRoomMember room(MNTRoom room) {
    this.room = room;
    this.id.roomId(room.getId());
    return this;
  }

  public String getUserId() {
    return userId != null ? userId : id.getUserId();
  }

  public MNTRoomMember userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public Boolean isOwner() {
    return isOwner;
  }

  public MNTRoomMember owner(Boolean owner) {
    this.isOwner = owner;
    return this;
  }

  public Boolean isMuted() {
    return muted;
  }

  public MNTRoomMember muted(Boolean muted) {
    this.muted = muted;
    return this;
  }

  public OffsetDateTime getJoinedAt() {
    return joinedAt;
  }

  public MNTRoomMember joinedAt(OffsetDateTime joinedAt) {
    this.joinedAt = joinedAt;
    return this;
  }
}
