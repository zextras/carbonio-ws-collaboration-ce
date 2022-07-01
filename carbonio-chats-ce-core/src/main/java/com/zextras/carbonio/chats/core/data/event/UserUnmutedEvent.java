package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserUnmutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_UNMUTED;

  private UUID roomId;
  private UUID memberId;

  public UserUnmutedEvent() {
    super(EVENT_TYPE);
  }

  public static UserUnmutedEvent create() {
    return new UserUnmutedEvent();
  }

  public UUID getRoomId() {
    return roomId;
  }

  public UserUnmutedEvent roomId(UUID roomId) {
    this.roomId = roomId;
    return this;
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
