// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.event;

import java.util.Objects;
import java.util.UUID;

public class MeetingAudioAnswered extends DomainEvent {

  private static final EventType EVENT_TYPE = EventType.MEETING_AUDIO_ANSWERED;

  private UUID   meetingId;
  private UUID   userId;
  private String sdp;

  public MeetingAudioAnswered() {
    super(EVENT_TYPE);
  }

  public static MeetingAudioAnswered create() {
    return new MeetingAudioAnswered();
  }

  public UUID getMeetingId() {
    return meetingId;
  }

  public MeetingAudioAnswered meetingId(UUID meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public UUID getUserId() {
    return userId;
  }

  public MeetingAudioAnswered userId(UUID userId) {
    this.userId = userId;
    return this;
  }

  public String getSdp() {
    return sdp;
  }

  public MeetingAudioAnswered sdp(String sdp) {
    this.sdp = sdp;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MeetingAudioAnswered)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MeetingAudioAnswered that = (MeetingAudioAnswered) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId()) &&
      Objects.equals(getSdp(), that.getSdp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getMeetingId(), getUserId(), getSdp());
  }
}
