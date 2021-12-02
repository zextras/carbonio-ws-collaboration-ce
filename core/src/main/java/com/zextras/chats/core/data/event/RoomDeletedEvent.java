package com.zextras.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RoomDeletedEvent extends Event {

  private static final EventType EVENT_TYPE = EventType.ROOM_DELETED;

  public RoomDeletedEvent(UUID roomId, OffsetDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomDeletedEvent create(UUID roomId, OffsetDateTime sentDate) {
    return new RoomDeletedEvent(roomId, sentDate);
  }

}
