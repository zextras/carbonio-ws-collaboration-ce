// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MeetingParticipantHandRaisedList extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_HAND_RAISED_LIST;

  private UUID meetingId;
  private List<UUID> participants;

  public MeetingParticipantHandRaisedList() {
    super(EVENT_TYPE);
  }

  public static MeetingParticipantHandRaisedList create() {
    return new MeetingParticipantHandRaisedList();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantHandRaisedList meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public List<UUID> getParticipants() {
    return participants;
  }

  public MeetingParticipantHandRaisedList participants(List<UUID> participants) {
    this.participants = participants;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MeetingParticipantHandRaisedList that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getParticipants(), that.getParticipants());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getParticipants());
  }
}
