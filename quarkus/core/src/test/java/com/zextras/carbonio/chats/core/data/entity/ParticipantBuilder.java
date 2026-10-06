// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ParticipantBuilder {

  private final Participant participant;

  public ParticipantBuilder(Meeting meeting, String userId) {
    this.participant = Participant.create(meeting, userId);
  }

  public static ParticipantBuilder create(Meeting meeting, String userId) {
    return new ParticipantBuilder(meeting, userId);
  }

  public ParticipantBuilder userId(UUID userId) {
    this.participant.userId(userId.toString());
    return this;
  }

  public ParticipantBuilder queueId(UUID queueId) {
    this.participant.queueId(queueId.toString());
    return this;
  }

  public ParticipantBuilder audioStreamOn(Boolean audioStreamOn) {
    this.participant.audioStreamOn(audioStreamOn);
    return this;
  }

  public ParticipantBuilder videoStreamOn(Boolean videoStreamOn) {
    this.participant.videoStreamOn(videoStreamOn);
    return this;
  }

  public ParticipantBuilder screenStreamOn(Boolean screenStreamOn) {
    this.participant.screenStreamOn(screenStreamOn);
    return this;
  }

  public ParticipantBuilder createdAt(OffsetDateTime createdAt) {
    try {
      Field createdAtField = Participant.class.getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(participant, createdAt);
      if (participant.getUpdatedAt() == null) {
        updatedAt(createdAt);
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public ParticipantBuilder updatedAt(OffsetDateTime updatedAt) {
    try {
      Field createdAtField = Participant.class.getDeclaredField("updatedAt");
      createdAtField.setAccessible(true);
      createdAtField.set(participant, updatedAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public ParticipantBuilder handRaisedAt(OffsetDateTime handRaisedAt) {
    try {
      Field createdAtField = Participant.class.getDeclaredField("handRaisedAt");
      createdAtField.setAccessible(true);
      createdAtField.set(participant, handRaisedAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public Participant build() {
    return participant;
  }
}
