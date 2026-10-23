// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
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
@Table(name = "MESSAGE_REACTION", schema = "CHATS")
public class MessageReaction {

  @EmbeddedId private MessageReactionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("messageId")
  @JoinColumn(name = "MESSAGE_ID")
  private Message message;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @Column(name = "REACTION", length = 32, nullable = false)
  private String reaction;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  public MessageReaction() {
    this.id = new MessageReactionId();
  }

  public MessageReaction(Message message, String userId, String reaction) {
    this.id = new MessageReactionId(message.getId(), userId);
    this.message = message;
    this.userId = userId;
    this.reaction = reaction;
  }

  public static MessageReaction create() {
    return new MessageReaction();
  }

  public static MessageReaction create(Message message, String userId, String reaction) {
    return new MessageReaction(message, userId, reaction);
  }

  public MessageReactionId getId() {
    return id;
  }

  public MessageReaction id(MessageReactionId id) {
    this.id = id;
    return this;
  }

  public Message getMessage() {
    return message;
  }

  public MessageReaction message(Message message) {
    this.message = message;
    if (message != null) {
      this.id.messageId(message.getId());
    }
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MessageReaction userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public String getReaction() {
    return reaction;
  }

  public MessageReaction reaction(String reaction) {
    this.reaction = reaction;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MessageReaction that = (MessageReaction) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
