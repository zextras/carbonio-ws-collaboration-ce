// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Feed;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MeetingParticipantStreams extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_PARTICIPANT_STREAMS;

  private UUID       meetingId;
  private UUID       userId;
  private List<Feed> streams;

  public MeetingParticipantStreams() {
    super(EVENT_TYPE);
  }

  public static MeetingParticipantStreams create() {
    return new MeetingParticipantStreams();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingParticipantStreams meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingParticipantStreams userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public List<Feed> getStreams() {
    return streams;
  }

  public MeetingParticipantStreams streams(List<Feed> streams) {
    this.streams = streams;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingParticipantStreams)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingParticipantStreams that = (MeetingParticipantStreams) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getUserId(),
      that.getUserId()) && Objects.equals(getStreams(), that.getStreams());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getUserId(), getStreams());
  }
}
