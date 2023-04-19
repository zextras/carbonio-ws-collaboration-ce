// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomUpdatedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_UPDATED;

  private UUID   roomId;
  private String name;
  private String description;

  public RoomUpdatedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomUpdatedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomUpdatedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomUpdatedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getName() {
    return name;
  }

  public RoomUpdatedEvent name(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RoomUpdatedEvent description(String description) {
    this.description = description;
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
    RoomUpdatedEvent that = (RoomUpdatedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getName(), that.getName())
      && Objects.equals(getDescription(), that.getDescription());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getName(), getDescription());
  }
}
