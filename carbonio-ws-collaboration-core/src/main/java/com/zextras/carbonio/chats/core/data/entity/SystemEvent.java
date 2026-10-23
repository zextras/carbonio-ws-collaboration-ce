// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "SYSTEM_EVENT", schema = "CHATS")
public class SystemEvent {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "TYPE", length = 64, nullable = false)
  private String type;

  @Column(name = "ACTOR_ID", length = 64, nullable = false)
  private String actorId;

  @Column(name = "CONTENT")
  private String content;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime createdAt;

  public static SystemEvent create() {
    return new SystemEvent();
  }

  public String getId() {
    return id;
  }

  public SystemEvent id(String id) {
    this.id = id;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public SystemEvent roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getType() {
    return type;
  }

  public SystemEvent type(String type) {
    this.type = type;
    return this;
  }

  public String getActorId() {
    return actorId;
  }

  public SystemEvent actorId(String actorId) {
    this.actorId = actorId;
    return this;
  }

  public String getContent() {
    return content;
  }

  public SystemEvent content(String content) {
    this.content = content;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public SystemEvent createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SystemEvent that = (SystemEvent) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
