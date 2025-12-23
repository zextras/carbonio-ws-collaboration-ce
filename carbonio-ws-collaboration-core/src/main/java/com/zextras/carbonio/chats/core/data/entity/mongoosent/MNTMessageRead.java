// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;

@Entity
@Table(name = "MONGOOSENT_MESSAGE_READ", schema = "CHATS")
public class MNTMessageRead {

  @EmbeddedId
  private MNTMessageReadId id;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @Column(name = "ROOM_ID", insertable = false, updatable = false)
  private String roomId;

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  @Column(name = "READ_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime readAt;

  public MNTMessageRead() {
    this.id = new MNTMessageReadId();
    this.readAt = OffsetDateTime.now();
  }

  public MNTMessageRead(String userId, String roomId, String messageId) {
    this.id = new MNTMessageReadId(userId, roomId);
    this.userId = userId;
    this.roomId = roomId;
    this.messageId = messageId;
    this.readAt = OffsetDateTime.now();
  }

  public static MNTMessageRead create() {
    return new MNTMessageRead();
  }

  public static MNTMessageRead create(String userId, String roomId, String messageId) {
    return new MNTMessageRead(userId, roomId, messageId);
  }

  public MNTMessageReadId getId() {
    return id;
  }

  public String getUserId() {
    return userId != null ? userId : id.getUserId();
  }

  public MNTMessageRead userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public String getRoomId() {
    return roomId != null ? roomId : id.getRoomId();
  }

  public MNTMessageRead roomId(String roomId) {
    this.roomId = roomId;
    this.id.roomId(roomId);
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MNTMessageRead messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public OffsetDateTime getReadAt() {
    return readAt;
  }

  public MNTMessageRead readAt(OffsetDateTime readAt) {
    this.readAt = readAt;
    return this;
  }
}
