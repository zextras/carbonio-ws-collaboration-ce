// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import org.antlr.v4.runtime.misc.ObjectEqualityComparator;

import java.util.Objects;
import java.util.UUID;

public class MeetingParticipantTalking extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_TALKING;

  private UUID meetingId;

  private UUID userId;

  private boolean isTalking;

  public MeetingParticipantTalking() {
    super(EVENT_TYPE);
  }

  public static MeetingParticipantTalking create() {
    return new MeetingParticipantTalking();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantTalking meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingParticipantTalking userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public boolean getIsTalking() {
    return isTalking;
  }

  public MeetingParticipantTalking isTalking(boolean isTalking) {
    this.isTalking = isTalking;
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
    MeetingParticipantTalking that = (MeetingParticipantTalking) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId()) &&
      Objects.equals(getIsTalking(), that.getIsTalking());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(),getUserId(), getIsTalking());
  }
}
