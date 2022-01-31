// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MessageReadId implements Serializable {

  private static final long serialVersionUID = 886121975477750257L;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  public MessageReadId() {
  }

  public MessageReadId(String userId, String roomId, String messageId) {
    this.userId = userId;
    this.roomId = roomId;
    this.messageId = messageId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MessageReadId that = (MessageReadId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(roomId, that.roomId)
      && Objects.equals(messageId, that.messageId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roomId, messageId);
  }
}
