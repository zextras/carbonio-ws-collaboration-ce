// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingWaitingParticipantClashed extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_WAITING_PARTICIPANT_CLASHED;

  private UUID meetingId;

  public MeetingWaitingParticipantClashed() {
    super(EVENT_TYPE);
  }

  public static MeetingWaitingParticipantClashed create() {
    return new MeetingWaitingParticipantClashed();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingWaitingParticipantClashed meetingId(UUID meetingId) {
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
    MeetingWaitingParticipantClashed that = (MeetingWaitingParticipantClashed) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
