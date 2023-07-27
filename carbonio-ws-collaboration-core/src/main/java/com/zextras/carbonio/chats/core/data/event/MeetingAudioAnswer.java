// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.MediaType;

import java.util.Objects;
import java.util.UUID;

public class MeetingAudioAnswer extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_AUDIO_ANSWERED;

  private UUID      meetingId;
  private UUID      userId;
  private String    sdp;

  public MeetingAudioAnswer(){super(EVENT_TYPE);}

  public static MeetingAudioAnswer create() {
    return new MeetingAudioAnswer();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingAudioAnswer meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingAudioAnswer userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public String getSdp() {
    return sdp;
  }

  public MeetingAudioAnswer sdp(String sdp) {
    this.sdp = sdp;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingAudioAnswer)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingAudioAnswer that = (MeetingAudioAnswer) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId()) &&
      Objects.equals(getSdp(), that.getSdp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getUserId(), getSdp());
  }
}
