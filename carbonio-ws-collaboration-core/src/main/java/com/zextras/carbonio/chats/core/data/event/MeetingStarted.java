// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public class MeetingStarted extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_STARTED;

  private UUID meetingId;
  private UUID starterUser;
  private OffsetDateTime startedAt;

  public MeetingStarted() {
    super(EVENT_TYPE);
  }

  public static MeetingStarted create() {
    return new MeetingStarted();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingStarted meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getStarterUser() {
    return starterUser;
  }

  public MeetingStarted starterUser(UUID starterUser) {
    this.starterUser = starterUser;
    return this;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public MeetingStarted startedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MeetingStarted that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getStarterUser(), that.getStarterUser())
        && Objects.equals(getStartedAt(), that.getStartedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getStarterUser(), getStartedAt());
  }
}
