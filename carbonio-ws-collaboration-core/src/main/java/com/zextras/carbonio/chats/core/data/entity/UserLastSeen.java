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
@Table(name = "USER_LAST_SEEN", schema = "CHATS")
public class UserLastSeen {

  @Id
  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "LAST_SEEN_AT", nullable = false)
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime lastSeenAt;

  public static UserLastSeen create() {
    return new UserLastSeen();
  }

  public String getUserId() {
    return userId;
  }

  public UserLastSeen userId(String userId) {
    this.userId = userId;
    return this;
  }

  public OffsetDateTime getLastSeenAt() {
    return lastSeenAt;
  }

  public UserLastSeen lastSeenAt(OffsetDateTime lastSeenAt) {
    this.lastSeenAt = lastSeenAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserLastSeen that = (UserLastSeen) o;
    return Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId);
  }
}
