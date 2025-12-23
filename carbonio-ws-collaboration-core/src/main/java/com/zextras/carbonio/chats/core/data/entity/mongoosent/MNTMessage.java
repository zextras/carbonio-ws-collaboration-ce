// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MONGOOSENT_MESSAGE", schema = "CHATS")
public class MNTMessage {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ROOM_ID", insertable = false, updatable = false)
  private MNTRoom room;

  @Column(name = "SENDER_ID", length = 64, nullable = false)
  private String senderId;

  @Column(name = "TEXT", nullable = false, columnDefinition = "TEXT")
  private String text;

  @Column(name = "REPLY_TO_ID", length = 64)
  private String replyToId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPLY_TO_ID", insertable = false, updatable = false)
  private MNTMessage replyTo;

  @Column(name = "FORWARDED_FROM_ID", length = 64)
  private String forwardedFromId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "FORWARDED_FROM_ID", insertable = false, updatable = false)
  private MNTMessage forwardedFrom;

  @Column(name = "FORWARDED_BY", length = 64)
  private String forwardedBy;

  @Column(name = "EDITED", nullable = false)
  private Boolean edited = false;

  @Column(name = "DELETED", nullable = false)
  private Boolean deleted = false;

  @Column(name = "CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "message", fetch = FetchType.LAZY)
  private List<MNTMessageReaction> reactions = new ArrayList<>();

  public MNTMessage() {}

  public static MNTMessage create() {
    return new MNTMessage();
  }

  public String getId() {
    return id;
  }

  public MNTMessage id(String id) {
    this.id = id;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public MNTMessage roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public MNTRoom getRoom() {
    return room;
  }

  public String getSenderId() {
    return senderId;
  }

  public MNTMessage senderId(String senderId) {
    this.senderId = senderId;
    return this;
  }

  public String getText() {
    return text;
  }

  public MNTMessage text(String text) {
    this.text = text;
    return this;
  }

  public String getReplyToId() {
    return replyToId;
  }

  public MNTMessage replyToId(String replyToId) {
    this.replyToId = replyToId;
    return this;
  }

  public MNTMessage getReplyTo() {
    return replyTo;
  }

  public String getForwardedFromId() {
    return forwardedFromId;
  }

  public MNTMessage forwardedFromId(String forwardedFromId) {
    this.forwardedFromId = forwardedFromId;
    return this;
  }

  public MNTMessage getForwardedFrom() {
    return forwardedFrom;
  }

  public String getForwardedBy() {
    return forwardedBy;
  }

  public MNTMessage forwardedBy(String forwardedBy) {
    this.forwardedBy = forwardedBy;
    return this;
  }

  public Boolean isEdited() {
    return edited;
  }

  public MNTMessage edited(Boolean edited) {
    this.edited = edited;
    return this;
  }

  public Boolean isDeleted() {
    return deleted;
  }

  public MNTMessage deleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<MNTMessageReaction> getReactions() {
    return reactions;
  }
}
