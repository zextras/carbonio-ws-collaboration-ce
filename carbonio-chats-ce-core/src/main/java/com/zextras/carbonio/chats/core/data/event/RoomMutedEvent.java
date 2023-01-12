// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomMutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MUTED;

  private UUID roomId;

  public RoomMutedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomMutedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomMutedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMutedEvent roomId(UUID roomId) {
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
    return Objects.equals(getRoomId(), ((RoomMutedEvent) o).getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
