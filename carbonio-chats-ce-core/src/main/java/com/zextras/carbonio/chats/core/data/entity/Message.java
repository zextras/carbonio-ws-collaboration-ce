// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;


import com.zextras.carbonio.chats.core.data.type.MessageType;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "MESSAGE", schema = "CHATS")
public class Message {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "SENT_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime sentAt;

  @Column(name = "EDIT_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime editAt;

  @Column(name = "MESSAGE_TYPE", length = 64, nullable = false)
  @Enumerated(EnumType.STRING)
  private MessageType messageType = MessageType.MESSAGE;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "TEXT", length = 4096)
  private String text;

  @Column(name = "REACTION", length = 4096)
  private String reaction;

  @Column(name = "TYPE_EXTRAINFO", length = 4096)
  private String typeExtrainfo;

  @Column(name = "DELETED", nullable = false)
  private Boolean deleted = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "REPLIED_TO")
  private Message repliedTo;

  @Column(name = "FORWARDED_FROM", length = 64, nullable = false)
  private String forwardedFrom;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public OffsetDateTime getSentAt() {
    return sentAt;
  }

  public void setSentAt(OffsetDateTime sentAt) {
    this.sentAt = sentAt;
  }

  public OffsetDateTime getEditAt() {
    return editAt;
  }

  public void setEditAt(OffsetDateTime editAt) {
    this.editAt = editAt;
  }

  public MessageType getMessageType() {
    return messageType;
  }

  public void setMessageType(MessageType messageType) {
    this.messageType = messageType;
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

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getReaction() {
    return reaction;
  }

  public void setReaction(String reaction) {
    this.reaction = reaction;
  }

  public String getTypeExtrainfo() {
    return typeExtrainfo;
  }

  public void setTypeExtrainfo(String typeExtrainfo) {
    this.typeExtrainfo = typeExtrainfo;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public void setDeleted(Boolean deleted) {
    this.deleted = deleted;
  }

  public Message getRepliedTo() {
    return repliedTo;
  }

  public void setRepliedTo(Message repliedTo) {
    this.repliedTo = repliedTo;
  }

  public String getForwardedFrom() {
    return forwardedFrom;
  }

  public void setForwardedFrom(String forwardedFrom) {
    this.forwardedFrom = forwardedFrom;
  }
}
