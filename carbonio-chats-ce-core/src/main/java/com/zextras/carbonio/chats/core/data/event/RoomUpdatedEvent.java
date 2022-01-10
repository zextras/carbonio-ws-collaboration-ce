package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomUpdatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_UPDATED;

  private UUID from;

  public RoomUpdatedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomUpdatedEvent create(UUID roomId)  {
    return new RoomUpdatedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomUpdatedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
