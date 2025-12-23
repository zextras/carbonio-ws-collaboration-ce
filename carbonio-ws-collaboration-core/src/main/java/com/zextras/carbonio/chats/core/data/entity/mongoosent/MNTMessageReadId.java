// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MNTMessageReadId implements Serializable {

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  public MNTMessageReadId() {}

  public MNTMessageReadId(String userId, String roomId) {
    this.userId = userId;
    this.roomId = roomId;
  }

  public String getUserId() {
    return userId;
  }

  public MNTMessageReadId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public MNTMessageReadId roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MNTMessageReadId that = (MNTMessageReadId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roomId);
  }
}
