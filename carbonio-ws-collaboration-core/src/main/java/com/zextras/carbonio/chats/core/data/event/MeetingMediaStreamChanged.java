// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public class MeetingMediaStreamChanged extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_MEDIA_STREAM_CHANGED;

  private UUID      meetingId;
  private UUID      userId;
  private MediaType mediaType;
  private Boolean   active;

  public MeetingMediaStreamChanged(){super(EVENT_TYPE);}

  public static MeetingMediaStreamChanged create() {
    return new MeetingMediaStreamChanged();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingMediaStreamChanged meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingMediaStreamChanged userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public MeetingMediaStreamChanged mediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public Boolean getActive() {
    return active;
  }

  public MeetingMediaStreamChanged active(Boolean active) {
    this.active = active;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingMediaStreamChanged)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingMediaStreamChanged that = (MeetingMediaStreamChanged) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId()) &&
      getMediaType() == that.getMediaType() &&
      getActive() == that.getActive();
  }

  @Override
  public int hashCode() {
    return
      Objects.hash(super.hashCode(), getMeetingId(), getUserId(), getMediaType(), getActive());
  }
}
