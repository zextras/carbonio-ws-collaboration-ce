// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "MESSAGE_READ", schema = "CHATS")
public class MessageRead {

  @EmbeddedId
  private MessageReadId id;

  @MapsId("userId")
  private String userId;

  @MapsId("roomId")
  private String roomId;

  @MapsId("messageId")
  private String messageId;

  @Column(name = "READ_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date readAt;

  public MessageReadId getId() {
    return id;
  }

  public void setId(MessageReadId id) {
    this.id = id;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public Date getReadAt() {
    return readAt;
  }

  public void setReadAt(Date readAt) {
    this.readAt = readAt;
  }
}
