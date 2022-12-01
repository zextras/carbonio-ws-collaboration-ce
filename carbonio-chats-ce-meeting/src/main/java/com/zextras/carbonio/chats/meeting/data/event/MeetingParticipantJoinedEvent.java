package com.zextras.carbonio.chats.meeting.data.event;

import com.zextras.carbonio.chats.core.data.event.DomainEvent;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingParticipantJoinedEvent extends DomainEvent {

  private static final MeetingEventType EVENT_TYPE = MeetingEventType.MEETING_PARTICIPANT_JOINED;

  private UUID meetingId;

  public MeetingParticipantJoinedEvent(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingParticipantJoinedEvent create(UUID from, @Nullable String sessionId) {
    return new MeetingParticipantJoinedEvent(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantJoinedEvent meetingId(UUID meetingId) {
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
    MeetingParticipantJoinedEvent that = (MeetingParticipantJoinedEvent) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
