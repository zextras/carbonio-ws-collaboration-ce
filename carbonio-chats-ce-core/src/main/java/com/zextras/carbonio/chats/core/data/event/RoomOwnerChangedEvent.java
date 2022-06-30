// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomOwnerChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_OWNER_CHANGED;

  private UUID    roomId;
  private UUID    memberId;
  private boolean isOwner;

  public RoomOwnerChangedEvent() {
    super(EVENT_TYPE);
  }

  public static RoomOwnerChangedEvent create() {
    return new RoomOwnerChangedEvent();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomOwnerChangedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
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
