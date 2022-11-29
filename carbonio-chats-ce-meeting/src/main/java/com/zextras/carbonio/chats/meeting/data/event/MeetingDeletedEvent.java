package com.zextras.carbonio.chats.meeting.data.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingDeletedEvent extends DomainEvent {

  private static final MeetingEventType EVENT_TYPE = MeetingEventType.MEETING_DELETED;

  private UUID meetingId;

  public MeetingDeletedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingDeletedEvent create(UUID from, @Nullable String sessionId) {
    return new MeetingDeletedEvent(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingDeletedEvent meetingId(UUID meetingId) {
    this.meetingId = meetingId;
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
    MeetingDeletedEvent that = (MeetingDeletedEvent) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
