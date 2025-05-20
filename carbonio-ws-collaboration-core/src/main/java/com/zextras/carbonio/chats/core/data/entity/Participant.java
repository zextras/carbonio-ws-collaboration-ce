// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "PARTICIPANT", schema = "CHATS")
public class Participant {

  @EmbeddedId private ParticipantId id;

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

  @Column(name = "HAND_RAISED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime handRaisedAt;

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

  public ParticipantId getId() {
    return id;
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

  public Participant updatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public OffsetDateTime getHandRaisedAt() {
    return handRaisedAt;
  }

  public Participant handRaisedAt(OffsetDateTime handRaisedAt) {
    this.handRaisedAt = handRaisedAt;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Participant that)) return false;
    return Objects.equals(getId(), that.getId())
        && Objects.equals(getUserId(), that.getUserId())
        && Objects.equals(getQueueId(), that.getQueueId())
        && Objects.equals(audioStreamOn, that.audioStreamOn)
        && Objects.equals(videoStreamOn, that.videoStreamOn)
        && Objects.equals(screenStreamOn, that.screenStreamOn)
        && Objects.equals(getCreatedAt(), that.getCreatedAt())
        && Objects.equals(getUpdatedAt(), that.getUpdatedAt())
        && Objects.equals(getHandRaisedAt(), that.getHandRaisedAt());
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getId(),
        getUserId(),
        getQueueId(),
        audioStreamOn,
        videoStreamOn,
        screenStreamOn,
        getCreatedAt(),
        getUpdatedAt(),
        getHandRaisedAt());
  }
}
