// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingParticipantScreenStreamClosed extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_SCREEN_STREAM_CLOSED;

  private UUID   meetingId;
  private String sessionId;

  public MeetingParticipantScreenStreamClosed(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingParticipantScreenStreamClosed create(UUID from, @Nullable String sessionId) {
    return new MeetingParticipantScreenStreamClosed(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantScreenStreamClosed meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public MeetingParticipantScreenStreamClosed sessionId(String sessionId) {
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
    MeetingParticipantScreenStreamClosed that = (MeetingParticipantScreenStreamClosed) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId());
  }
}
