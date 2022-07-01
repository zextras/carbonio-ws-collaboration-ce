package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class UserMutedEvent extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.USER_MUTED;

  private UUID roomId;
  private UUID memberId;

  public UserMutedEvent() {
    super(EVENT_TYPE);
  }

  public static UserMutedEvent create() {
    return new UserMutedEvent();
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
    UserMutedEvent that = (UserMutedEvent) o;
    return Objects.equals(getRoomId(), that.getRoomId()) && Objects.equals(memberId, that.memberId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRoomId(), memberId);
  }
}
