// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "CHATS_USER", schema = "CHATS")
public class User {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "PICTURE_UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime pictureUpdatedAt;

  @Column(name = "STATUS_MESSAGE", length = 256, nullable = false)
  private String statusMessage = "";

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  public static User create() {
    return new User();
  }

  public String getId() {
    return id;
  }

  public User id(String id) {
    this.id = id;
    return this;
  }

  public String getStatusMessage() {
    return statusMessage;
  }

  public User statusMessage(String statusMessage) {
    this.statusMessage = statusMessage;
    return this;
  }

  public OffsetDateTime getPictureUpdatedAt() {
    return pictureUpdatedAt;
  }

  public User pictureUpdatedAt(OffsetDateTime pictureUpdatedAt) {
    this.pictureUpdatedAt = pictureUpdatedAt;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    User user = (User) o;
    return Objects.equals(id, user.id)
        && (Objects.equals(pictureUpdatedAt, user.pictureUpdatedAt)
            || Objects.equals(
                pictureUpdatedAt.toInstant().toEpochMilli(),
                user.pictureUpdatedAt.toInstant().toEpochMilli()))
        && Objects.equals(statusMessage, user.statusMessage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, pictureUpdatedAt, statusMessage);
  }
}
