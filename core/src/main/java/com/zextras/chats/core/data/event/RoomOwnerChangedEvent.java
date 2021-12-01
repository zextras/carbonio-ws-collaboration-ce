package com.zextras.chats.core.data.event;

import java.time.LocalDateTime;
import java.util.UUID;

public class RoomOwnerChangedEvent extends Event {

  private static final EventType EVENT_TYPE = EventType.ROOM_OWNER_CHANGED;

  private UUID memberModifiedId;
  private boolean isOwner;

  public RoomOwnerChangedEvent(UUID roomId, LocalDateTime sentDate) {
    super(roomId, EVENT_TYPE, sentDate);
  }

  public static RoomOwnerChangedEvent create(UUID roomId, LocalDateTime sentDate) {
    return new RoomOwnerChangedEvent(roomId, sentDate);
  }

  public UUID getMemberModifiedId() {
    return memberModifiedId;
  }

  public RoomOwnerChangedEvent memberModifiedId(UUID memberModifiedId) {
    this.memberModifiedId = memberModifiedId;
    return this;
  }

  public boolean isOwner() {
    return isOwner;
  }

  public RoomOwnerChangedEvent isOwner(boolean owner) {
    isOwner = owner;
    return this;
  }
}
