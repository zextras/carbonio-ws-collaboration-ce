// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.UUID;

public class RoomHashResetEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HASH_RESET_EVENT;

  public String hash;

  public RoomHashResetEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static RoomHashResetEvent create(UUID roomId) {
    return new RoomHashResetEvent(roomId);
  }

  public String getHash() {
    return hash;
  }

  public RoomHashResetEvent hash(String hash) {
    this.hash = hash;
    return this;
  }
}
