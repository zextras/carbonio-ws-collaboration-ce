// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.model.MemberDto;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomMemberAdded extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_ADDED;

  private UUID      roomId;

  private UUID      userId;

  private boolean   isOwner;

  public RoomMemberAdded() {
    super(EVENT_TYPE);
  }

  public static RoomMemberAdded create() {
    return new RoomMemberAdded();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMemberAdded roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public RoomMemberAdded userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public boolean getIsOwner(){return isOwner;}

  public RoomMemberAdded isOwner(boolean isOwner){
    this.isOwner = isOwner;
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
    RoomMemberAdded that = (RoomMemberAdded) o;
    return Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(getUserId(), that.getUserId()) ;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getUserId());
  }
}
