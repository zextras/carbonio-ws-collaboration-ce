package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "PARTICIPANT", schema = "CHATS")
public class Participant {

  @EmbeddedId
  private ParticipantId id;

  @MapsId("userId")
  private String userId;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("meetingId")
  @JoinColumn(name = "MEETING_ID")
  private Meeting meeting;

  @MapsId("sessionId")
  private String sessionId;

  @Column(name = "MICROPHONE_ON")
  private Boolean microphoneOn;

  @Column(name = "CAMERA_ON")
  private Boolean cameraOn;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "UPDATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenModified
  private OffsetDateTime updatedAt;

  public Participant() {
    this.id = ParticipantId.create();
  }

  public Participant(String userId, Meeting meeting, String sessionId) {
    this.id = ParticipantId.create(userId, meeting.getId(), sessionId);
    this.userId = userId;
    this.meeting = meeting;
    this.sessionId = sessionId;
  }

  public static Participant create() {
    return new Participant();
  }

  public static Participant create(String userId, Meeting meeting, String sessionId) {
    return new Participant(userId, meeting, sessionId);
  }

  public String getUserId() {
    return userId;
  }

  public Participant userId(String userId) {
    this.userId = userId;
    this.id.userId(userId);
    return this;
  }

  public Meeting getMeeting() {
    return meeting;
  }

  public Participant meeting(Meeting meeting) {
    this.meeting = meeting;
    this.id.meetingId(meeting.getId());
    return this;
  }

  public String getSessionId() {
    return sessionId;
  }

  public Participant sessionId(String sessionId) {
    this.sessionId = sessionId;
    this.id.sessionId(sessionId);
    return this;
  }

  public Boolean getMicrophoneOn() {
    return microphoneOn;
  }

  public Participant microphoneOn(Boolean microphoneOn) {
    this.microphoneOn = microphoneOn;
    return this;
  }

  public Boolean getCameraOn() {
    return cameraOn;
  }

  public Participant cameraOn(Boolean cameraOn) {
    this.cameraOn = cameraOn;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }
}
