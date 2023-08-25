// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.it.entity;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import java.util.UUID;

public class ParticipantBuilder {

  private final UUID    userId;
  private final String  queueId;
  private       Boolean audioStreamOn  = false;
  private       Boolean videoStreamOn  = false;
  private       Boolean screenStreamOn = false;

  public ParticipantBuilder(UUID userId, String queueid) {
    this.userId = userId;
    this.queueId = queueid;
  }

  public static ParticipantBuilder create(UUID userId, String queueid) {
    return new ParticipantBuilder(userId, queueid);
  }

  public UUID getUserId() {
    return userId;
  }

  public String getQueueId() {
    return queueId;
  }

  public Boolean getAudioStreamOn() {
    return audioStreamOn;
  }

  public ParticipantBuilder audioStreamOn(Boolean audioStreamOn) {
    this.audioStreamOn = audioStreamOn;
    return this;
  }

  public Boolean getVideoStreamOn() {
    return videoStreamOn;
  }

  public ParticipantBuilder videoStreamOn(Boolean videoStreamOn) {
    this.videoStreamOn = videoStreamOn;
    return this;
  }

  public Boolean getScreenStreamOn() {
    return screenStreamOn;
  }

  public ParticipantBuilder screenStreamOn(Boolean screenStreamOn) {
    this.screenStreamOn = screenStreamOn;
    return this;
  }

  public Participant build(Meeting meeting) {
    return Participant.create(meeting, userId.toString())
      .queueId(this.queueId)
      .audioStreamOn(this.audioStreamOn)
      .videoStreamOn(this.videoStreamOn)
      .screenStreamOn(this.screenStreamOn);
  }
}

