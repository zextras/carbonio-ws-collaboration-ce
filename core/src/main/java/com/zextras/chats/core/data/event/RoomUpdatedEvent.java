package com.zextras.chats.core.data.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class RoomUpdatedEvent extends Event<RoomUpdatedEvent> {

  private static final EventType EVENT_TYPE = EventType.ROOM_UPDATED;

  private UUID from;

  public RoomUpdatedEvent(UUID roomId, LocalDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomUpdatedEvent create(UUID roomId, LocalDateTime sentDate) {
    return new RoomUpdatedEvent(roomId, sentDate);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomUpdatedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
