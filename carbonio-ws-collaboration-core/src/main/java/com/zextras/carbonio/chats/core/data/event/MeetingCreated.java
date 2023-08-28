// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingCreated extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_CREATED;

  private UUID meetingId;
  private UUID roomId;

  public MeetingCreated() {
    super(EVENT_TYPE);
  }

  public static MeetingCreated create() {
    return new MeetingCreated();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingCreated meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public MeetingCreated roomId(UUID roomId) {
    this.roomId = roomId;
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
    MeetingCreated that = (MeetingCreated) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getRoomId(), that.getRoomId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getRoomId());
  }
}
