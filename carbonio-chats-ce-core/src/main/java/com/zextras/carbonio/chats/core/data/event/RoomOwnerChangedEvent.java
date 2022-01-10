package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomOwnerChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_OWNER_CHANGED;

  private UUID    memberId;
  private boolean isOwner;

  public RoomOwnerChangedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomOwnerChangedEvent create(UUID roomId) {
    return new RoomOwnerChangedEvent(roomId);
  }

  public UUID getMemberId() {
    return memberId;
  }

  public RoomOwnerChangedEvent memberId(UUID memberModifiedId) {
    this.memberId = memberModifiedId;
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
