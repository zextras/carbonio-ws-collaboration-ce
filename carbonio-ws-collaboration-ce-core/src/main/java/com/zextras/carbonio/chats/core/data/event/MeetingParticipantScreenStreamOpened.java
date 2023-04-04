// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingParticipantScreenStreamOpened extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_SCREEN_STREAM_OPENED;

  private UUID   meetingId;
  private String sessionId;

  public MeetingParticipantScreenStreamOpened(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingParticipantScreenStreamOpened create(UUID from, @Nullable String sessionId) {
    return new MeetingParticipantScreenStreamOpened(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantScreenStreamOpened meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public MeetingParticipantScreenStreamOpened sessionId(String sessionId) {
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
    MeetingParticipantScreenStreamOpened that = (MeetingParticipantScreenStreamOpened) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId());
  }
}
