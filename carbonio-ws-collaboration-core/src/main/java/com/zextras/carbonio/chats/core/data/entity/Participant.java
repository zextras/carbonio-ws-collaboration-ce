// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
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

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "QUEUE_ID", length = 64, nullable = false)
  private String queueId;

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

  public Participant(Meeting meeting, String userId) {
    this.id = ParticipantId.create(meeting.getId(), userId);
    this.meeting = meeting;
    this.userId = userId;
  }

  public static Participant create() {
    return new Participant();
  }

  public static Participant create(Meeting meeting, String userId) {
    return new Participant(meeting, userId);
  }

  public String getUserId() {
    return userId;
  }

  public Participant userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getQueueId() {
    return queueId;
  }

  public Participant queueId(String queueId) {
    this.queueId = queueId;
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

  public Participant createdAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Participant)) {
      return false;
    }
    Participant that = (Participant) o;
    return Objects.equals(id, that.id) && Objects.equals(getMeeting(), that.getMeeting())
      && Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getQueueId(),
      that.getQueueId()) && Objects.equals(audioStreamOn, that.audioStreamOn) && Objects.equals(
      videoStreamOn, that.videoStreamOn) && Objects.equals(screenStreamOn, that.screenStreamOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, getMeeting(), getUserId(), getQueueId(), audioStreamOn, videoStreamOn, screenStreamOn);
  }
}
