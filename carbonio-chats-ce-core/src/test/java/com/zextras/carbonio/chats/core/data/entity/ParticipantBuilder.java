package com.zextras.carbonio.chats.core.data.entity;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ParticipantBuilder {

  private final Participant participant;

  public ParticipantBuilder(UUID userId, Meeting meeting, String sessionId) {
    this.participant = Participant.create(userId.toString(), meeting, sessionId);
  }

  public static ParticipantBuilder create(UUID userId, Meeting meeting, String sessionId) {
    return new ParticipantBuilder(userId, meeting, sessionId);
  }

  public ParticipantBuilder microphoneOn(Boolean microphoneOn) {
    this.participant.microphoneOn(microphoneOn);
    return this;
  }


  public ParticipantBuilder cameraOn(Boolean cameraOn) {
    this.participant.cameraOn(cameraOn);
    return this;
  }


  public ParticipantBuilder createdAt(OffsetDateTime createdAt) {
    try {
      Field createdAtField = Participant.class.getDeclaredField("createdAt");
      createdAtField.setAccessible(true);
      createdAtField.set(participant, createdAt);
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public Participant build() {
    return participant;
  }

}
