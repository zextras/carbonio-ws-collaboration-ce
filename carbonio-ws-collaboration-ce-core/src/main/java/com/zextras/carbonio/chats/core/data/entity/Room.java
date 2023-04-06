// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.model.RoomTypeDto;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

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

  @Column(name = "HASH", length = 256, unique = true, nullable = false)
  private String hash;

  @Column(name = "TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private RoomTypeDto type;

  //This is a redundancy which simplifies querying, allowing us to retrieve this without a join on the attachment table
  @Column(name = "PICTURE_UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime pictureUpdatedAt;

  @Column(name = "MEETING_ID", length = 64)
  private String meetingId;

  @Column(name = "PARENT_ID", length = 64)
  private String parentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "PARENT_ID", updatable = false, insertable = false)
  private Room parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Room> children;

  @Column(name = "RANK")
  private Integer rank;

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

  public String getHash() {
    return hash;
  }

  public Room hash(String hash) {
    this.hash = hash;
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

  public String getParentId() {
    return parentId;
  }

  public Room parentId(String parentId) {
    this.parentId = parentId;
    return this;
  }

  public Room getParent() {
    return parent;
  }

  public Room setParent(Room parent) {
    this.parent = parent;
    return this;
  }

  public List<Room> getChildren() {
    return children;
  }

  public Room children(List<Room> children) {
    this.children = children;
    return this;
  }

  public Integer getRank() {
    return rank;
  }

  public Room rank(Integer rank) {
    this.rank = rank;
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
