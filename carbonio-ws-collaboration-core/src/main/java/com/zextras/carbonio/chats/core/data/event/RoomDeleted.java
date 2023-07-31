// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomDeleted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_DELETED;

  private UUID roomId;

  public RoomDeleted() {
    super(EVENT_TYPE);
  }

  public static RoomDeleted create() {
    return new RoomDeleted();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomDeleted roomId(UUID roomId) {
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
    if (!super.equals(o)) {
      return false;
    }
    return Objects.equals(roomId, ((RoomDeleted) o).getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
