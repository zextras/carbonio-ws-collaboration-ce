// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomPictureChangedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_CHANGED;

  private UUID from;

  public RoomPictureChangedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomPictureChangedEvent create(UUID roomId) {
    return new RoomPictureChangedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomPictureChangedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
