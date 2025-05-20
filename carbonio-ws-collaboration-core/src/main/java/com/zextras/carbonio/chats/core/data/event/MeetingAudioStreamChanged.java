// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import jakarta.annotation.Nullable;
import java.util.Objects;
import java.util.UUID;

public class MeetingAudioStreamChanged extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_AUDIO_STREAM_CHANGED;

  private UUID meetingId;
  private UUID userId;
  @Nullable private UUID moderatorId;
  private boolean active;

  public MeetingAudioStreamChanged() {
    super(EVENT_TYPE);
  }

  public static MeetingAudioStreamChanged create() {
    return new MeetingAudioStreamChanged();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingAudioStreamChanged meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingAudioStreamChanged userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  @Nullable
  public UUID getModeratorId() {
    return moderatorId;
  }

  public MeetingAudioStreamChanged moderatorId(UUID moderatorId) {
    this.moderatorId = moderatorId;
    return this;
  }

  public boolean isActive() {
    return active;
  }

  public MeetingAudioStreamChanged active(boolean active) {
    this.active = active;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof MeetingAudioStreamChanged that)) return false;
    if (!super.equals(o)) return false;
    return isActive() == that.isActive()
        && Objects.equals(getMeetingId(), that.getMeetingId())
        && Objects.equals(getUserId(), that.getUserId())
        && Objects.equals(getModeratorId(), that.getModeratorId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), getMeetingId(), getUserId(), getModeratorId(), isActive());
  }
}
