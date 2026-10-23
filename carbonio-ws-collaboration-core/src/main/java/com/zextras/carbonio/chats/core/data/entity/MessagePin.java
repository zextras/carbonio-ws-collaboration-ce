// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "MESSAGE_PIN", schema = "CHATS")
public class MessagePin {

  @Id
  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "MESSAGE_ID", insertable = false, updatable = false)
  private Message message;

  @Column(name = "PINNED_BY", length = 64, nullable = false)
  private String pinnedBy;

  @Column(name = "PINNED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime pinnedAt;

  public MessagePin() {}

  public MessagePin(String roomId, String messageId, String pinnedBy) {
    this.roomId = roomId;
    this.messageId = messageId;
    this.pinnedBy = pinnedBy;
  }

  public static MessagePin create() {
    return new MessagePin();
  }

  public static MessagePin create(String roomId, String messageId, String pinnedBy) {
    return new MessagePin(roomId, messageId, pinnedBy);
  }

  public String getRoomId() {
    return roomId;
  }

  public MessagePin roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MessagePin messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public Message getMessage() {
    return message;
  }

  public MessagePin message(Message message) {
    this.message = message;
    if (message != null) {
      this.messageId = message.getId();
    }
    return this;
  }

  public String getPinnedBy() {
    return pinnedBy;
  }

  public MessagePin pinnedBy(String pinnedBy) {
    this.pinnedBy = pinnedBy;
    return this;
  }

  public OffsetDateTime getPinnedAt() {
    return pinnedAt;
  }

  public MessagePin pinnedAt(OffsetDateTime pinnedAt) {
    this.pinnedAt = pinnedAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessagePin that = (MessagePin) o;
    return Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId);
  }
}
