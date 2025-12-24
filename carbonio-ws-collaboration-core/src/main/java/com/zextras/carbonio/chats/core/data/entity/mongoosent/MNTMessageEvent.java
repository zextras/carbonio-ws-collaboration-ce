// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Append-only event log for message operations. This table is write-only and should never be
 * queried in normal application flow. Used for audit trail, compliance, and debugging.
 */
@Entity
@Table(name = "MONGOOSENT_MESSAGE_EVENT", schema = "CHATS")
public class MNTMessageEvent {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "MESSAGE_ID", length = 64, nullable = false)
  private String messageId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Enumerated(EnumType.STRING)
  @Column(name = "EVENT_TYPE", length = 32, nullable = false)
  private MNTMessageEventType eventType;

  @DbJson
  @Column(name = "PAYLOAD", columnDefinition = "JSONB")
  private Map<String, Object> payload;

  @Column(name = "CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  public MNTMessageEvent() {}

  public static MNTMessageEvent create() {
    return new MNTMessageEvent();
  }

  public String getId() {
    return id;
  }

  public MNTMessageEvent id(String id) {
    this.id = id;
    return this;
  }

  public String getMessageId() {
    return messageId;
  }

  public MNTMessageEvent messageId(String messageId) {
    this.messageId = messageId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public MNTMessageEvent roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MNTMessageEvent userId(String userId) {
    this.userId = userId;
    return this;
  }

  public MNTMessageEventType getEventType() {
    return eventType;
  }

  public MNTMessageEvent eventType(MNTMessageEventType eventType) {
    this.eventType = eventType;
    return this;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public MNTMessageEvent payload(Map<String, Object> payload) {
    this.payload = payload;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }
}
