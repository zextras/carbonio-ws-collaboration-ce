// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingWaitingParticipantRejected extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_WAITING_PARTICIPANT_REJECTED;

  private UUID meetingId;

  private UUID userId;

  public MeetingWaitingParticipantRejected() {
    super(EVENT_TYPE);
  }

  public static MeetingWaitingParticipantRejected create() {
    return new MeetingWaitingParticipantRejected();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingWaitingParticipantRejected meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingWaitingParticipantRejected userId(UUID userId) {
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
    MeetingWaitingParticipantRejected that = (MeetingWaitingParticipantRejected) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
