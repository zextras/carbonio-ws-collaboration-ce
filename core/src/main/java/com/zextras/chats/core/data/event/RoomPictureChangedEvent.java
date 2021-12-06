package com.zextras.chats.core.data.event;

import java.util.UUID;

public class RoomPictureChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_CHANGED;

  private UUID from;

  public RoomPictureChangedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomPictureChangedEvent create(UUID roomId) {
    return new RoomPictureChangedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomPictureChangedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
