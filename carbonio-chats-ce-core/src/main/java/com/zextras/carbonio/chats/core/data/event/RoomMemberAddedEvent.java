// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomMemberAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_ADDED;

  private UUID    roomId;
  private UUID    memberId;
  private boolean isOwner     = false;
  private boolean isTemporary = false;
  private boolean isExternal  = false;

  public RoomMemberAddedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomMemberAddedEvent create(UUID from) {
    return new RoomMemberAddedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMemberAddedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
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
    RoomMemberAddedEvent that = (RoomMemberAddedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(getMemberId(), that.getMemberId()) &&
      isOwner() == that.isOwner() &&
      isTemporary() == that.isTemporary() &&
      isExternal() == that.isExternal();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getMemberId(), isOwner(), isTemporary(), isExternal());
  }
}
