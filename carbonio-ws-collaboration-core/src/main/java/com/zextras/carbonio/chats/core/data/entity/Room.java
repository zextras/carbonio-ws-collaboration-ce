// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.model.RoomTypeDto;
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
import java.util.List;

@Entity
@Table(name = "ROOM", schema = "CHATS")
public class Room {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 128, nullable = false)
  private String name;

  @Column(name = "DESCRIPTION", length = 256)
  private String description;

  @Column(name = "TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private RoomTypeDto type;

  // This is a redundancy which simplifies querying, allowing us to retrieve this without a join on
  // the attachment table
  @Column(name = "PICTURE_UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime pictureUpdatedAt;

  @Column(name = "MEETING_ID", length = 64)
  private String meetingId;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Subscription> subscriptions;

  @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<RoomUserSettings> userSettings;

  public static Room create() {
    return new Room();
  }

  public String getId() {
    return id;
  }

  public Room id(String id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public Room name(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public Room description(String description) {
    this.description = description;
    return this;
  }

  public RoomTypeDto getType() {
    return type;
  }

  public Room type(RoomTypeDto type) {
    this.type = type;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public OffsetDateTime getPictureUpdatedAt() {
    return pictureUpdatedAt;
  }

  public Room pictureUpdatedAt(OffsetDateTime pictureUpdatedAt) {
    this.pictureUpdatedAt = pictureUpdatedAt;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public Room meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public List<Subscription> getSubscriptions() {
    return subscriptions;
  }

  public Room subscriptions(List<Subscription> subscriptions) {
    this.subscriptions = subscriptions;
    return this;
  }

  public List<RoomUserSettings> getUserSettings() {
    return userSettings;
  }

  public Room userSettings(List<RoomUserSettings> roomUserSettings) {
    this.userSettings = roomUserSettings;
    return this;
  }
}
