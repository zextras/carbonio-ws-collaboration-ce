// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomOwnerChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_OWNER_CHANGED;

  private UUID    roomId;
  private UUID    memberId;
  private boolean isOwner;

  public RoomOwnerChangedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomOwnerChangedEvent create(UUID from) {
    return new RoomOwnerChangedEvent(from);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    RoomOwnerChangedEvent that = (RoomOwnerChangedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(getMemberId(), that.getMemberId()) &&
      isOwner() == that.isOwner();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getMemberId(), isOwner());
  }
}
