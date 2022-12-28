// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import java.time.OffsetDateTime;
import java.util.Objects;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("meetingId")
  @JoinColumn(name = "MEETING_ID")
  private Meeting meeting;

  @MapsId("sessionId")
  private String sessionId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "AUDIO_STREAM_ON")
  private Boolean audioStreamOn = false;

  @Column(name = "VIDEO_STREAM_ON")
  private Boolean videoStreamOn = false;

  @Column(name = "SCREEN_STREAM_ON")
  private Boolean screenStreamOn = false;

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

  public Participant(Meeting meeting, String sessionId) {
    this.id = ParticipantId.create(meeting.getId(), sessionId);
    this.meeting = meeting;
    this.sessionId = sessionId;
  }

  public static Participant create() {
    return new Participant();
  }

  public static Participant create(Meeting meeting, String sessionId) {
    return new Participant(meeting, sessionId);
  }

  public String getUserId() {
    return userId;
  }

  public Participant userId(String userId) {
    this.userId = userId;
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

  public Boolean hasAudioStreamOn() {
    return audioStreamOn;
  }

  public Participant audioStreamOn(Boolean audioStreamOn) {
    this.audioStreamOn = audioStreamOn;
    return this;
  }

  public Boolean hasVideoStreamOn() {
    return videoStreamOn;
  }

  public Participant videoStreamOn(Boolean videoStreamOn) {
    this.videoStreamOn = videoStreamOn;
    return this;
  }

  public Boolean hasScreenStreamOn() {
    return screenStreamOn;
  }

  public Participant screenStreamOn(Boolean screenStreamOn) {
    this.screenStreamOn = screenStreamOn;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Participant that = (Participant) o;
    return Objects.equals(id, that.id) && Objects.equals(meeting, that.meeting)
      && Objects.equals(sessionId, that.sessionId) && Objects.equals(userId, that.userId)
      && Objects.equals(audioStreamOn, that.audioStreamOn) && Objects.equals(videoStreamOn,
      that.videoStreamOn) && Objects.equals(screenStreamOn, that.screenStreamOn) && Objects.equals(
      createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, meeting, sessionId, userId, audioStreamOn, videoStreamOn, screenStreamOn, createdAt,
      updatedAt);
  }
}
