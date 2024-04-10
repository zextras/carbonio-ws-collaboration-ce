// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingRecordingStarted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_RECORDING_STARTED;

  private UUID meetingId;
  private UUID userId;

  public MeetingRecordingStarted() {
    super(EVENT_TYPE);
  }

  public static MeetingRecordingStarted create() {
    return new MeetingRecordingStarted();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingRecordingStarted meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingRecordingStarted userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MeetingRecordingStarted that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getUserId());
  }
}
