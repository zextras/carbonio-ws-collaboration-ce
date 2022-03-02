// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "CHATS_USER", schema = "CHATS")
public class User {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "LAST_SEEN")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime lastSeen;

  @Column(name = "STATUS_MESSAGE", length = 256, nullable = false)
  private String statusMessage = "";

  @Column(name = "IMAGE")
  @Lob
  private byte[] image;

  @Column(name = "IMAGE_UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private Date imageUpdatedAt;

  @Column(name = "HASH", length = 256, unique = true, nullable = false)
  private String hash;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  public String getId() {
    return id;
  }

  public User id(String id) {
    this.id = id;
    return this;
  }

  public OffsetDateTime getLastSeen() {
    return lastSeen;
  }

  public User setLastSeen(OffsetDateTime lastSeen) {
    this.lastSeen = lastSeen;
    return this;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public User statusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
    return this;
  }

  public byte[] getImage() {
    return image;
  }

  public User image(byte[] image) {
    this.image = image;
    return this;
  }

  public Date getImageUpdatedAt() {
    return imageUpdatedAt;
  }

  public User imageUpdatedAt(Date imageUpdatedAt) {
    this.imageUpdatedAt = imageUpdatedAt;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public User hash(String hash) {
    this.hash = hash;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
