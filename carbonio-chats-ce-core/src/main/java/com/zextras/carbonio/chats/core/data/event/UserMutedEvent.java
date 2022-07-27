package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserMutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_MUTED;

  private UUID roomId;
  private UUID memberId;

  public UserMutedEvent(UUID from) {
    super(EVENT_TYPE, from);
  }

  public static UserMutedEvent create(UUID from) {
    return new UserMutedEvent(from);
  }

  public UUID getRoomId() {
    return roomId;
  }

  public UserMutedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
  }

  public UUID getMemberId() {
    return memberId;
  }

  public UserMutedEvent memberId(UUID memberId) {
    this.memberId = memberId;
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
    UserMutedEvent that = (UserMutedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) &&
      Objects.equals(getMemberId(), that.getMemberId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getRoomId(), getMemberId());
  }
}
