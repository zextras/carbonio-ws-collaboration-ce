// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomMuted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MUTED;

  private UUID roomId;

  public RoomMuted() {
    super(EVENT_TYPE);
  }

  public static RoomMuted create() {
    return new RoomMuted();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMuted roomId(UUID roomId) {
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
    return Objects.equals(getRoomId(), ((RoomMuted) o).getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
