package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomDeletedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_DELETED;

  public RoomDeletedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomDeletedEvent create(UUID roomId) {
    return new RoomDeletedEvent(roomId);
  }

}
