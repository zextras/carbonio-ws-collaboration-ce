// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomUpdatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_UPDATED;

  private UUID roomId;
  private UUID from;

  public RoomUpdatedEvent() {
    super(EVENT_TYPE);
  }

  public static RoomUpdatedEvent create() {
    return new RoomUpdatedEvent();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomUpdatedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getFrom() {
    return from;
  }

  public RoomUpdatedEvent from(UUID from) {
    this.from = from;
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
    RoomUpdatedEvent that = (RoomUpdatedEvent) o;
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(from, that.from);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getRoomId(), from);
  }
}
