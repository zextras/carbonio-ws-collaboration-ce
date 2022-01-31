// SPDX-FileCopyrightText: 2022 2021 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;


import java.time.OffsetDateTime;
import java.util.UUID;

public class DomainEvent {

  private UUID           id;
  private UUID           roomId;
  private EventType      type;
  private OffsetDateTime sentDate;

  public DomainEvent(UUID roomId, EventType type) {
    this.id = UUID.randomUUID();
    this.roomId = roomId;
    this.type = type;
    this.sentDate = OffsetDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public EventType getType() {
    return type;
  }

  public OffsetDateTime getSentDate() {
    return sentDate;
  }
}
