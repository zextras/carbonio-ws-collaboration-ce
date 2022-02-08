// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomHashResetEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HASH_RESET_EVENT;

  public String hash;

  public RoomHashResetEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomHashResetEvent create(UUID roomId) {
    return new RoomHashResetEvent(roomId);
  }

  public String getHash() {
    return hash;
  }

  public RoomHashResetEvent hash(String hash) {
    this.hash = hash;
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
    RoomHashResetEvent that = (RoomHashResetEvent) o;
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(hash, that.hash);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getRoomId(), hash);
  }
}
