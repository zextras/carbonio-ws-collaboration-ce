// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomDeletedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_DELETED;

  private UUID roomId;

  public RoomDeletedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomDeletedEvent create(UUID from) {
    return new RoomDeletedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomDeletedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }
}
