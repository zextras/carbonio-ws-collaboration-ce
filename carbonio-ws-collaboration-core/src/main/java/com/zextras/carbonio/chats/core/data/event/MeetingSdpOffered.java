// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;

import java.util.Objects;
import java.util.UUID;

public class MeetingSdpOffered extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_SDP_OFFERED;

  private UUID      meetingId;
  private UUID      userId;
  private MediaType mediaType;
  private String sdp;

  public MeetingSdpOffered(){super(EVENT_TYPE);}

  public static MeetingSdpOffered create() {
    return new MeetingSdpOffered();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingSdpOffered meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getSessionId() {
    return userId;
  }

  public MeetingSdpOffered userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public MediaType getMediaType() {
    return mediaType;
  }

  public MeetingSdpOffered mediaType(MediaType mediaType) {
    this.mediaType = mediaType;
    return this;
  }

  public String getSdp(){ return sdp;}

  public MeetingSdpOffered sdp(String sdp){
    this.sdp = sdp;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingSdpOffered)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingSdpOffered that = (MeetingSdpOffered) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) && Objects.equals(getSessionId(),
      that.getSessionId()) && getMediaType() == that.getMediaType();
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getSessionId(), getMediaType());
  }
}
