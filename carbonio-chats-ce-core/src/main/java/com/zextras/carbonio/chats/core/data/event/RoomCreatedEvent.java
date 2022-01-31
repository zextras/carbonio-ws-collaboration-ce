// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;


import java.util.UUID;

public class RoomCreatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_CREATED;

  private UUID from;

  public RoomCreatedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomCreatedEvent create(UUID roomId) {
    return new RoomCreatedEvent(roomId);
  }

  public UUID getFrom() {
    return from;
  }

  public RoomCreatedEvent from(UUID from) {
    this.from = from;
    return this;
  }
}
