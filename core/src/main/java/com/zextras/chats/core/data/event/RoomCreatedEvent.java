package com.zextras.chats.core.data.event;


import java.time.OffsetDateTime;
import java.util.UUID;

public class RoomCreatedEvent extends Event<RoomCreatedEvent> {

  private static final EventType EVENT_TYPE = EventType.ROOM_CREATED;

  private UUID from;

  public RoomCreatedEvent(UUID roomId, OffsetDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomCreatedEvent create(UUID roomId, OffsetDateTime sentDate) {
    return new RoomCreatedEvent(roomId, sentDate);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomCreatedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
