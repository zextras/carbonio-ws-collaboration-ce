package com.zextras.chats.core.data.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class RoomHashResetEvent extends Event {

  private static final EventType EVENT_TYPE = EventType.ROOM_HASH_RESET_EVENT;

  public String hash;

  public RoomHashResetEvent(UUID roomId, LocalDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomHashResetEvent create(UUID roomId, LocalDateTime sentDate) {
    return new RoomHashResetEvent(roomId, sentDate);
  }

  public String getHash() {
    return hash;
  }

  public RoomHashResetEvent hash(String hash) {
    this.hash = hash;
    return this;
  }
}
