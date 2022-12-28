// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.model.MemberDto;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomMemberAddedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_ADDED;

  private UUID      roomId;
  private MemberDto member;

  public RoomMemberAddedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomMemberAddedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomMemberAddedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMemberAddedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public MemberDto getMember() {
    return member;
  }

  public RoomMemberAddedEvent member(MemberDto member) {
    this.member = member;
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
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getMember(), that.getMember());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getMember());
  }
}
