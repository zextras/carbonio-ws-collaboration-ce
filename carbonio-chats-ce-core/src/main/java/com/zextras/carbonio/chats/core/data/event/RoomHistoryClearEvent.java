package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class RoomHistoryClearEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.ROOM_HISTORY_CLEAR_EVENT;

  private UUID roomId;
  private OffsetDateTime clearedAt;

  public RoomHistoryClearEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static RoomHistoryClearEvent create(UUID from) {
    return new RoomHistoryClearEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public RoomHistoryClearEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public OffsetDateTime getClearedAt() {
    return clearedAt;
  }

  public RoomHistoryClearEvent clearedAt(OffsetDateTime clearedAt) {
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
    RoomHistoryClearEvent that = (RoomHistoryClearEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(getClearedAt(), that.getClearedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getClearedAt());
  }
}
