// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

public class RoomHistoryClearedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HISTORY_CLEARED;

  private UUID           roomId;
  private OffsetDateTime clearedAt;

  public RoomHistoryClearedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static RoomHistoryClearedEvent create(UUID from, @Nullable String sessionId) {
    return new RoomHistoryClearedEvent(from, sessionId);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomHistoryClearedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public OffsetDateTime getClearedAt() {
    return clearedAt;
  }

  public RoomHistoryClearedEvent clearedAt(OffsetDateTime clearedAt) {
    this.clearedAt = clearedAt;
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
    RoomHistoryClearedEvent that = (RoomHistoryClearedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getClearedAt(), that.getClearedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getClearedAt());
  }
}
