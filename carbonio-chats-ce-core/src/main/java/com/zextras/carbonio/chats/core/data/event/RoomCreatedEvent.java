// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;


import java.util.Objects;
import java.util.UUID;

public class RoomCreatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_CREATED;

  private UUID from;

  public RoomCreatedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomCreatedEvent create(UUID roomId) {
    return new RoomCreatedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomCreatedEvent from(UUID from) {
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
    RoomCreatedEvent that = (RoomCreatedEvent) o;
    return Objects.equals(getType(), that.getType()) &&
      Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(from, that.from);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getRoomId(), from);
  }
}
