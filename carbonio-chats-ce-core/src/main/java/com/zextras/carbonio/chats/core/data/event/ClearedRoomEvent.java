package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class ClearedRoomEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.CLEARED_ROOM_EVENT;

  private UUID roomId;

  public ClearedRoomEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static ClearedRoomEvent create(UUID from) {
    return new ClearedRoomEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public ClearedRoomEvent roomId(UUID roomId) {
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
    return getRoomId().equals(((ClearedRoomEvent) o).getRoomId());
  }
}
