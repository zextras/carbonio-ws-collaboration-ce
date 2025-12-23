// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

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

@Entity
@Table(name = "MONGOOSENT_MESSAGE_REACTION", schema = "CHATS")
public class MNTMessageReaction {

  @EmbeddedId
  private MNTMessageReactionId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("messageId")
  @JoinColumn(name = "MESSAGE_ID")
  private MNTMessage message;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @Column(name = "REACTION", insertable = false, updatable = false)
  private String reaction;

  @Column(name = "CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  public MNTMessageReaction() {
    this.id = new MNTMessageReactionId();
  }

  public MNTMessageReaction(MNTMessage message, String userId, String reaction) {
    this.id = new MNTMessageReactionId(message.getId(), userId, reaction);
    this.message = message;
    this.userId = userId;
    this.reaction = reaction;
  }

  public static MNTMessageReaction create() {
    return new MNTMessageReaction();
  }

  public static MNTMessageReaction create(MNTMessage message, String userId, String reaction) {
    return new MNTMessageReaction(message, userId, reaction);
  }

  public MNTMessageReactionId getId() {
    return id;
  }

  public MNTMessage getMessage() {
    return message;
  }

  public MNTMessageReaction message(MNTMessage message) {
    this.message = message;
    this.id.messageId(message.getId());
    return this;
  }

  public String getUserId() {
    return userId != null ? userId : id.getUserId();
  }

  public MNTMessageReaction userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public String getReaction() {
    return reaction != null ? reaction : id.getReaction();
  }

  public MNTMessageReaction reaction(String reaction) {
    this.reaction = reaction;
    this.id.reaction(reaction);
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
