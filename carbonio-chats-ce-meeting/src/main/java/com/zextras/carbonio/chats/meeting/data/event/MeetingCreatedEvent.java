package com.zextras.carbonio.chats.meeting.data.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingCreatedEvent extends DomainEvent {

  private static final MeetingEventType EVENT_TYPE = MeetingEventType.MEETING_CREATED;

  private UUID meetingId;
  private UUID roomId;

  public MeetingCreatedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingCreatedEvent create(UUID from, @Nullable String sessionId) {
    return new MeetingCreatedEvent(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingCreatedEvent meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public MeetingCreatedEvent roomId(UUID roomId) {
    this.roomId = roomId;
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
    MeetingCreatedEvent that = (MeetingCreatedEvent) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getRoomId(), that.getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getRoomId());
  }
}