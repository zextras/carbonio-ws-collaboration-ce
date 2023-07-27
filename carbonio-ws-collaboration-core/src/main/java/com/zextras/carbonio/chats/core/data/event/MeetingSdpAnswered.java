// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;

import java.util.Objects;
import java.util.UUID;

public class MeetingSdpAnswered extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_SDP_ANSWERED;

  private UUID      meetingId;
  private UUID      userId;
  private MediaType mediaType;
  private String sdp;

  public MeetingSdpAnswered(){super(EVENT_TYPE);}

  public static MeetingSdpAnswered create() {
    return new MeetingSdpAnswered();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingSdpAnswered meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getSessionId() {
    return userId;
  }

  public MeetingSdpAnswered userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public MeetingSdpAnswered mediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public String getSdp(){ return sdp;}

  public MeetingSdpAnswered sdp(String sdp){
    this.sdp = sdp;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingSdpAnswered)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingSdpAnswered that = (MeetingSdpAnswered) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(),
      that.getSessionId()) && getMediaType() == that.getMediaType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId(), getMediaType());
  }
}
