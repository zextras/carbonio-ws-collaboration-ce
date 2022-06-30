// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomMemberRemovedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_MEMBER_REMOVED;

  private UUID roomId;
  private UUID memberId;

  public RoomMemberRemovedEvent() {
    super(EVENT_TYPE);
  }

  public static RoomMemberRemovedEvent create() {
    return new RoomMemberRemovedEvent();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomMemberRemovedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public RoomMemberRemovedEvent memberId(UUID memberModifiedId) {
    this.memberId = memberModifiedId;
    return this;
  }
}
