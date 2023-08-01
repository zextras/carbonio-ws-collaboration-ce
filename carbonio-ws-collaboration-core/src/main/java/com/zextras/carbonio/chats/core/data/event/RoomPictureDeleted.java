// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomPictureDeleted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_PICTURE_DELETED;

  private UUID roomId;

  public RoomPictureDeleted() {
    super(EVENT_TYPE);
  }

  public static RoomPictureDeleted create() {
    return new RoomPictureDeleted();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomPictureDeleted roomId(UUID roomId) {
    this.roomId = roomId;
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
    return Objects.equals(getRoomId(), ((RoomPictureDeleted) o).getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId());
  }
}
