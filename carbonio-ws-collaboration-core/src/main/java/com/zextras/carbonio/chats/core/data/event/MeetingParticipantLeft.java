// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingParticipantLeft extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_LEFT;

  private UUID meetingId;
  private UUID userId;

  public MeetingParticipantLeft() {
    super(EVENT_TYPE);
  }

  public static MeetingParticipantLeft create() {
    return new MeetingParticipantLeft();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantLeft meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingParticipantLeft userId(UUID userId) {
    this.userId = userId;
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
    MeetingParticipantLeft that = (MeetingParticipantLeft) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}