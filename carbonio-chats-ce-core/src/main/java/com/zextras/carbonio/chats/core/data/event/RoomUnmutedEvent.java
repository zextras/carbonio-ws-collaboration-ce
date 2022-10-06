package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomUnmutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_UNMUTED;

  private UUID roomId;

  public RoomUnmutedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomUnmutedEvent create(UUID from) {
    return new RoomUnmutedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomUnmutedEvent roomId(UUID roomId) {
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
    return Objects.equals(getRoomId(), ((RoomUnmutedEvent) o).getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
