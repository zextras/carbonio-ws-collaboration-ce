// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class RoomHashResetEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HASH_RESET_EVENT;

  private UUID   roomId;
  private String hash;

  public RoomHashResetEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomHashResetEvent create(UUID from) {
    return new RoomHashResetEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomHashResetEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getHash() {
    return hash;
  }

  public RoomHashResetEvent hash(String hash) {
    this.hash = hash;
    return this;
  }
}
