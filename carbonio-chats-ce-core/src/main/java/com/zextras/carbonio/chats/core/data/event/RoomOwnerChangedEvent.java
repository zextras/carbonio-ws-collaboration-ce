// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomOwnerChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_OWNER_CHANGED;

  private UUID    roomId;
  private UUID    userId;
  private boolean isOwner;

  public RoomOwnerChangedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomOwnerChangedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomOwnerChangedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomOwnerChangedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public RoomOwnerChangedEvent userId(UUID userId) {
    this.userId = userId;
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
      Objects.equals(getUserId(), that.getUserId()) &&
      isOwner() == that.isOwner();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getUserId(), isOwner());
  }
}
