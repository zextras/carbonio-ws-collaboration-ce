package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserMutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_MUTED;

  private UUID memberId;

  public UserMutedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static UserMutedEvent create(UUID roomId) {
    return new UserMutedEvent(roomId);
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
    UserMutedEvent that = (UserMutedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(memberId, that.memberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRoomId(), memberId);
  }
}
