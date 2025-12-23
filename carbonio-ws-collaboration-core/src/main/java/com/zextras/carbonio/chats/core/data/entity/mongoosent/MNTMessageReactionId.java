// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MNTMessageReactionId implements Serializable {

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "REACTION", length = 32, nullable = false)
  private String reaction;

  public MNTMessageReactionId() {}

  public MNTMessageReactionId(String messageId, String userId, String reaction) {
    this.messageId = messageId;
    this.userId = userId;
    this.reaction = reaction;
  }

  public String getMessageId() {
    return messageId;
  }

  public MNTMessageReactionId messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MNTMessageReactionId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getReaction() {
    return reaction;
  }

  public MNTMessageReactionId reaction(String reaction) {
    this.reaction = reaction;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MNTMessageReactionId that = (MNTMessageReactionId) o;
    return Objects.equals(messageId, that.messageId)
        && Objects.equals(userId, that.userId)
        && Objects.equals(reaction, that.reaction);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId, userId, reaction);
  }
}
