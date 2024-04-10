// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.MeetingType;
import io.ebean.annotation.WhenCreated;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "MEETING", schema = "CHATS")
public class Meeting {

  public static Meeting create() {
    return new Meeting();
  }

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 128)
  private String name;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Participant> participants;

  @Column(name = "MEETING_TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private MeetingType meetingType;

  @Column(name = "CREATED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  @WhenCreated
  private OffsetDateTime createdAt;

  @Column(name = "EXPIRATION")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime expiration;

  @Column(name = "ACTIVE", nullable = false)
  private Boolean active;

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Recording> recordings;

  public String getId() {
    return id;
  }

  public Meeting id(String id) {
    this.id = id;
    return this;
  }

  public String getRoomId() {
    return roomId;
  }

  public Meeting roomId(String roomId) {
    this.roomId = roomId;
    return this;
  }

  public String getName() {
    return name;
  }

  public Meeting name(String name) {
    this.name = name;
    return this;
  }

  public List<Participant> getParticipants() {
    return participants;
  }

  public Meeting participants(List<Participant> participants) {
    this.participants = participants;
    return this;
  }

  public MeetingType getMeetingType() {
    return meetingType;
  }

  public Meeting meetingType(MeetingType meetingType) {
    this.meetingType = meetingType;
    return this;
  }

  public OffsetDateTime getExpiration() {
    return expiration;
  }

  public Meeting expiration(OffsetDateTime expiration) {
    this.expiration = expiration;
    return this;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public Boolean getActive() {
    return active;
  }

  public Meeting active(Boolean active) {
    this.active = active;
    return this;
  }

  public List<Recording> getRecordings() {
    return recordings;
  }

  public Meeting recordings(List<Recording> recordings) {
    this.recordings = recordings;
    return this;
  }
}
