// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingParticipantAudioStreamOpened extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_AUDIO_STREAM_OPENED;

  private UUID   meetingId;
  private String sessionId;

  public MeetingParticipantAudioStreamOpened(UUID from, @Nullable String sessionId) {
    super(EVENT_TYPE, from, sessionId);
  }

  public static MeetingParticipantVideoStreamOpened create(UUID from, @Nullable String sessionId) {
    return new MeetingParticipantVideoStreamOpened(from, sessionId);
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantAudioStreamOpened meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public String getSessionId() {
    return sessionId;
  }

  public MeetingParticipantAudioStreamOpened sessionId(String sessionId) {
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
    MeetingParticipantAudioStreamOpened that = (MeetingParticipantAudioStreamOpened) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId());
  }
}
