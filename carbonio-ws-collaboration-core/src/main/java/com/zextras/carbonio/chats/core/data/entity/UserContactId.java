// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserContactId implements Serializable {

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "CONTACT_ID", length = 64, nullable = false)
  private String contactId;

  public UserContactId() {}

  public UserContactId(String userId, String contactId) {
    this.userId = userId;
    this.contactId = contactId;
  }

  public String getUserId() {
    return userId;
  }

  public UserContactId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getContactId() {
    return contactId;
  }

  public UserContactId contactId(String contactId) {
    this.contactId = contactId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserContactId that = (UserContactId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(contactId, that.contactId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, contactId);
  }
}
