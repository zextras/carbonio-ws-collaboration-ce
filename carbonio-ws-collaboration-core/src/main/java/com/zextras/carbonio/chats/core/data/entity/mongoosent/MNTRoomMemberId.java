// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity.mongoosent;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MNTRoomMemberId implements Serializable {

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  public MNTRoomMemberId() {}

  public MNTRoomMemberId(String roomId, String userId) {
    this.roomId = roomId;
    this.userId = userId;
  }

  public String getRoomId() {
    return roomId;
  }

  public MNTRoomMemberId roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public MNTRoomMemberId userId(String userId) {
    this.userId = userId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MNTRoomMemberId that = (MNTRoomMemberId) o;
    return Objects.equals(roomId, that.roomId) && Objects.equals(userId, that.userId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(roomId, userId);
  }
}
