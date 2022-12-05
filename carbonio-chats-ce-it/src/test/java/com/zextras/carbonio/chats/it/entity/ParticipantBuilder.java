package com.zextras.carbonio.chats.it.entity;

import com.zextras.carbonio.chats.core.data.entity.Meeting;
import com.zextras.carbonio.chats.core.data.entity.Participant;
import java.util.UUID;

public class ParticipantBuilder {

  private final UUID    userId;
  private final String  sessionId;
  private       Boolean microphoneOn = false;
  private       Boolean cameraOn     = false;

  public ParticipantBuilder(UUID userId, String sessionId) {
    this.userId = userId;
    this.sessionId = sessionId;
  }

  public static ParticipantBuilder create(UUID userId, String sessionId) {
    return new ParticipantBuilder(userId, sessionId);
  }

  public UUID getUserId() {
    return userId;
  }

  public String getSessionId() {
    return sessionId;
  }

  public Boolean getMicrophoneOn() {
    return microphoneOn;
  }

  public ParticipantBuilder microphoneOn(Boolean microphoneOn) {
    this.microphoneOn = microphoneOn;
    return this;
  }

  public Boolean getCameraOn() {
    return cameraOn;
  }

  public ParticipantBuilder cameraOn(Boolean cameraOn) {
    this.cameraOn = cameraOn;
    return this;
  }

  public Participant build(Meeting meeting) {
    return Participant.create(userId.toString(), meeting, sessionId)
      .microphoneOn(this.microphoneOn)
      .cameraOn(this.cameraOn);
  }
}
