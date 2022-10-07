package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class RoomHistoryClearedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HISTORY_CLEARED;

  private UUID roomId;
  private OffsetDateTime clearedAt;

  public RoomHistoryClearedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomHistoryClearedEvent create(UUID from) {
    return new RoomHistoryClearedEvent(from);
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
