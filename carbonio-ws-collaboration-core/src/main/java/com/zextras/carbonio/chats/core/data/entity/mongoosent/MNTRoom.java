// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MONGOOSENT_ROOM", schema = "CHATS")
public class MNTRoom {

  public enum RoomType {
    ONE_TO_ONE,
    GROUP
  }

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 256)
  private String name;

  @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "TYPE", length = 32, nullable = false)
  private RoomType type;

  @Column(name = "CREATED_BY", length = 64, nullable = false)
  private String createdBy;

  @Column(name = "CREATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<MNTRoomMember> members = new ArrayList<>();

  public MNTRoom() {}

  public static MNTRoom create() {
    return new MNTRoom();
  }

  public String getId() {
    return id;
  }

  public MNTRoom id(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public MNTRoom name(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public MNTRoom description(String description) {
    this.description = description;
    return this;
  }

  public RoomType getType() {
    return type;
  }

  public MNTRoom type(RoomType type) {
    this.type = type;
    return this;
  }

  public String getCreatedBy() {
    return createdBy;
  }

  public MNTRoom createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public List<MNTRoomMember> getMembers() {
    return members;
  }

  public MNTRoom members(List<MNTRoomMember> members) {
    this.members = members;
    return this;
  }
}
