// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomUpdatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_UPDATED;

  private UUID roomId;

  public RoomUpdatedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomUpdatedEvent create(UUID from) {
    return new RoomUpdatedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomUpdatedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }
}
