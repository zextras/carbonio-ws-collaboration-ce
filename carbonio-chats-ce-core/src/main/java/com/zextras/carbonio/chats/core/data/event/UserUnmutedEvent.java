package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserUnmutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_UNMUTED;

  private UUID memberId;

  public UserUnmutedEvent(UUID roomId) {
    super(roomId, EVENT_TYPE);
  }

  public static UserUnmutedEvent create(UUID roomId) {
    return new UserUnmutedEvent(roomId);
  }

  public UUID getMemberId() {
    return memberId;
  }

  public UserUnmutedEvent memberId(UUID memberId) {
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
    UserUnmutedEvent that = (UserUnmutedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(memberId, that.memberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRoomId(), memberId);
  }
}
