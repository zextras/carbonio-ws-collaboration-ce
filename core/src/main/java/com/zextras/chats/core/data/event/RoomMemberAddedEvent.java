package com.zextras.chats.core.data.event;

import java.util.UUID;

public class RoomMemberAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_ADDED;

  private UUID    memberId;
  private boolean isOwner;
  private boolean isTemporary;
  private boolean isExternal;

  public RoomMemberAddedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomMemberAddedEvent create(UUID roomId) {
    return new RoomMemberAddedEvent(roomId);
  }

  public UUID getMemberId() {
    return memberId;
  }

  public RoomMemberAddedEvent memberId(UUID memberModifiedId) {
    this.memberId = memberModifiedId;
    return this;
  }

  public boolean isOwner() {
    return isOwner;
  }

  public RoomMemberAddedEvent isOwner(boolean owner) {
    isOwner = owner;
    return this;
  }

  public boolean isTemporary() {
    return isTemporary;
  }

  public RoomMemberAddedEvent temporary(boolean temporary) {
    isTemporary = temporary;
    return this;
  }

  public boolean isExternal() {
    return isExternal;
  }

  public RoomMemberAddedEvent external(boolean external) {
    isExternal = external;
    return this;
  }
}
