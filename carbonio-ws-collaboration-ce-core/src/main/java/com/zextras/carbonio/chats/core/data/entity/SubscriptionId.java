// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class SubscriptionId implements Serializable {

  private static final long serialVersionUID = -8264822214211111673L;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  public SubscriptionId() {
  }

  /**
   * Create a new instance of subscription identifier
   * @param roomId room identifier
   * @param userId user identifier
   */
  public SubscriptionId(String roomId, String userId) {
    this.userId = userId;
    this.roomId = roomId;
  }

  public String getUserId() {
    return userId;
  }

  public SubscriptionId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public SubscriptionId roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SubscriptionId that = (SubscriptionId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roomId);
  }
}
