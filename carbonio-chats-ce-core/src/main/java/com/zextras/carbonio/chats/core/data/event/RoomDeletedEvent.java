// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomDeletedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_DELETED;

  public RoomDeletedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomDeletedEvent create(UUID roomId) {
    return new RoomDeletedEvent(roomId);
  }

}
