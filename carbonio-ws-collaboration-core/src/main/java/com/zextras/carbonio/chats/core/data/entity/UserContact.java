// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "USER_CONTACT", schema = "CHATS")
public class UserContact {

  @EmbeddedId private UserContactId id;

  @Column(name = "USER_ID", insertable = false, updatable = false)
  private String userId;

  @Column(name = "CONTACT_ID", insertable = false, updatable = false)
  private String contactId;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  public UserContact() {
    this.id = new UserContactId();
  }

  public UserContact(String userId, String contactId) {
    this.id = new UserContactId(userId, contactId);
    this.userId = userId;
    this.contactId = contactId;
  }

  public static UserContact create() {
    return new UserContact();
  }

  public static UserContact create(String userId, String contactId) {
    return new UserContact(userId, contactId);
  }

  public UserContactId getId() {
    return id;
  }

  public UserContact id(UserContactId id) {
    this.id = id;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public UserContact userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public String getContactId() {
    return contactId;
  }

  public UserContact contactId(String contactId) {
    this.contactId = contactId;
    this.id.contactId(contactId);
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserContact that = (UserContact) o;
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
