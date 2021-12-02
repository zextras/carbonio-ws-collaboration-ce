package com.zextras.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RoomPictureChangedEvent extends Event {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_CHANGED;

  private UUID from;

  public RoomPictureChangedEvent(UUID roomId, OffsetDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomPictureChangedEvent create(UUID roomId, OffsetDateTime sentDate) {
    return new RoomPictureChangedEvent(roomId, sentDate);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomPictureChangedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
