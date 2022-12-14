package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingParticipantVideoStreamOpened extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_VIDEO_STREAM_OPENED;

  private UUID   meetingId;
  private String sessionId;

  public MeetingParticipantVideoStreamOpened(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingParticipantVideoStreamOpened create(UUID from, @Nullable String sessionId) {
    return new MeetingParticipantVideoStreamOpened(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantVideoStreamOpened meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public MeetingParticipantVideoStreamOpened sessionId(String sessionId) {
    this.sessionId = sessionId;
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
    MeetingParticipantVideoStreamOpened that = (MeetingParticipantVideoStreamOpened) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId());
  }
}
