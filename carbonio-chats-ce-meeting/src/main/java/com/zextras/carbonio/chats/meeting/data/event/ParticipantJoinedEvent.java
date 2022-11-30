package com.zextras.carbonio.chats.meeting.data.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class ParticipantJoinedEvent extends DomainEvent {

  private static final MeetingEventType EVENT_TYPE = MeetingEventType.PARTICIPANT_JOINED;

  private UUID meetingId;

  public ParticipantJoinedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static ParticipantJoinedEvent create(UUID from, @Nullable String sessionId) {
    return new ParticipantJoinedEvent(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public ParticipantJoinedEvent meetingId(UUID meetingId) {
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
    ParticipantJoinedEvent that = (ParticipantJoinedEvent) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
