// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.CascadeType;
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
import java.util.Objects;

@Entity
@Table(name = "MESSAGE", schema = "CHATS")
public class Message {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ROOM_ID", insertable = false, updatable = false)
  private Room room;

  @Column(name = "SENDER_ID", length = 64, nullable = false)
  private String senderId;

  @Column(name = "TEXT", nullable = false)
  private String text = "";

  @Column(name = "REPLY_TO_ID", length = 64)
  private String replyToId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPLY_TO_ID", insertable = false, updatable = false)
  private Message replyTo;

  @Column(name = "FORWARDED", nullable = false)
  private Boolean forwarded = false;

  @Column(name = "FORWARDED_TEXT")
  private String forwardedText;

  @Column(name = "FORWARDED_SENDER_ID", length = 64)
  private String forwardedSenderId;

  @Column(name = "FORWARDED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime forwardedAt;

  @Column(name = "ATTACHMENT_ID", length = 64)
  private String attachmentId;

  @Column(name = "EDITED", nullable = false)
  private Boolean edited = false;

  @Column(name = "DELETED", nullable = false)
  private Boolean deleted = false;

  @Column(name = "DELETED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime deletedAt;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MessageReaction> reactions = new ArrayList<>();

  public static Message create() {
    return new Message();
  }

  public String getId() {
    return id;
  }

  public Message id(String id) {
    this.id = id;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public Message roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public Room getRoom() {
    return room;
  }

  public Message room(Room room) {
    this.room = room;
    this.roomId = room != null ? room.getId() : null;
    return this;
  }

  public String getSenderId() {
    return senderId;
  }

  public Message senderId(String senderId) {
    this.senderId = senderId;
    return this;
  }

  public String getText() {
    return text;
  }

  public Message text(String text) {
    this.text = text != null ? text : "";
    return this;
  }

  public String getReplyToId() {
    return replyToId;
  }

  public Message replyToId(String replyToId) {
    this.replyToId = replyToId;
    return this;
  }

  public Message getReplyTo() {
    return replyTo;
  }

  public Message replyTo(Message replyTo) {
    this.replyTo = replyTo;
    this.replyToId = replyTo != null ? replyTo.getId() : null;
    return this;
  }

  public Boolean isForwarded() {
    return forwarded;
  }

  public Message forwarded(Boolean forwarded) {
    this.forwarded = forwarded != null ? forwarded : false;
    return this;
  }

  public String getForwardedText() {
    return forwardedText;
  }

  public Message forwardedText(String forwardedText) {
    this.forwardedText = forwardedText;
    return this;
  }

  public String getForwardedSenderId() {
    return forwardedSenderId;
  }

  public Message forwardedSenderId(String forwardedSenderId) {
    this.forwardedSenderId = forwardedSenderId;
    return this;
  }

  public OffsetDateTime getForwardedAt() {
    return forwardedAt;
  }

  public Message forwardedAt(OffsetDateTime forwardedAt) {
    this.forwardedAt = forwardedAt;
    return this;
  }

  public String getAttachmentId() {
    return attachmentId;
  }

  public Message attachmentId(String attachmentId) {
    this.attachmentId = attachmentId;
    return this;
  }

  public Boolean isEdited() {
    return edited;
  }

  public Message edited(Boolean edited) {
    this.edited = edited != null ? edited : false;
    return this;
  }

  public Boolean isDeleted() {
    return deleted;
  }

  public Message deleted(Boolean deleted) {
    this.deleted = deleted != null ? deleted : false;
    return this;
  }

  public OffsetDateTime getDeletedAt() {
    return deletedAt;
  }

  public Message deletedAt(OffsetDateTime deletedAt) {
    this.deletedAt = deletedAt;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public Message createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<MessageReaction> getReactions() {
    return reactions;
  }

  public Message reactions(List<MessageReaction> reactions) {
    this.reactions = reactions != null ? reactions : new ArrayList<>();
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Message message = (Message) o;
    return Objects.equals(id, message.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
