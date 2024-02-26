// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingWaitingParticipantJoined extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_WAITING_PARTICIPANT_JOINED;

  private UUID meetingId;

  private UUID userId;

  public MeetingWaitingParticipantJoined() {
    super(EVENT_TYPE);
  }

  public static MeetingWaitingParticipantJoined create() {
    return new MeetingWaitingParticipantJoined();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingWaitingParticipantJoined meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingWaitingParticipantJoined userId(UUID userId) {
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
    MeetingWaitingParticipantJoined that = (MeetingWaitingParticipantJoined) o;
    return Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId());
  }
}
