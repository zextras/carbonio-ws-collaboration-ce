// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingParticipantHandRaised extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_HAND_RAISED;

  private UUID meetingId;
  private UUID userId;
  private boolean raised;

  public MeetingParticipantHandRaised() {
    super(EVENT_TYPE);
  }

  public static MeetingParticipantHandRaised create() {
    return new MeetingParticipantHandRaised();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantHandRaised meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingParticipantHandRaised userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public boolean isRaised() {
    return raised;
  }

  public MeetingParticipantHandRaised raised(boolean raised) {
    this.raised = raised;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MeetingParticipantHandRaised that)) return false;
    if (!super.equals(o)) return false;
    return isRaised() == that.isRaised()
        && Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getUserId(), isRaised());
  }
}
