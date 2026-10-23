// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReadMarkerId implements Serializable {

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  public ReadMarkerId() {}

  public ReadMarkerId(String userId, String roomId) {
    this.userId = userId;
    this.roomId = roomId;
  }

  public String getUserId() {
    return userId;
  }

  public ReadMarkerId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public ReadMarkerId roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ReadMarkerId that = (ReadMarkerId) o;
    return Objects.equals(userId, that.userId) && Objects.equals(roomId, that.roomId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, roomId);
  }
}
