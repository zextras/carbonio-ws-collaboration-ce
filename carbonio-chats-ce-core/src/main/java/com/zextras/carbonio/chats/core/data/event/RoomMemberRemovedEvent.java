// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomMemberRemovedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_REMOVED;

  private UUID roomId;
  private UUID userId;

  public RoomMemberRemovedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomMemberRemovedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomMemberRemovedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMemberRemovedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public RoomMemberRemovedEvent userId(UUID userId) {
    this.userId = userId;
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
    RoomMemberRemovedEvent that = (RoomMemberRemovedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getUserId());
  }
}
