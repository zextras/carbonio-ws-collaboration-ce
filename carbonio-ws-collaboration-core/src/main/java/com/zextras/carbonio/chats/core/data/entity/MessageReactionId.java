// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MessageReactionId implements Serializable {

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  public MessageReactionId() {}

  public MessageReactionId(String messageId, String userId) {
    this.messageId = messageId;
    this.userId = userId;
  }

  public String getMessageId() {
    return messageId;
  }

  public MessageReactionId messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MessageReactionId userId(String userId) {
    this.userId = userId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessageReactionId that = (MessageReactionId) o;
    return Objects.equals(messageId, that.messageId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId, userId);
  }
}
