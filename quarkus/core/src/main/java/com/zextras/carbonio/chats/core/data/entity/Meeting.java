// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.MeetingType;
import org.hibernate.annotations.CreationTimestamp;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "MEETING", schema = "CHATS")
public class Meeting {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @Column(name = "NAME", length = 128)
  private String name;

  @Column(name = "ROOM_ID", length = 64, nullable = false)
  private String roomId;

  @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  @Fetch(FetchMode.SELECT)
  private List<Participant> participants = new ArrayList<>();

  @Column(name = "MEETING_TYPE", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private MeetingType meetingType;

  @Column(name = "CREATED_AT")
  @CreationTimestamp
  private OffsetDateTime createdAt;

  @Column(name = "STARTED_AT")
  private OffsetDateTime startedAt;

  @Column(name = "EXPIRATION")
  private OffsetDateTime expiration;

  @Column(name = "ACTIVE", nullable = false)
  private Boolean active;

  public static Meeting create() {
    return new Meeting();
  }

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

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public Meeting startedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public Boolean getActive() {
    return active;
  }

  public Meeting active(Boolean active) {
    this.active = active;
    return this;
  }

  /** Id-based equality, as Ebean's enhancement provided for entities without equals. */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Meeting other)) {
      return false;
    }
    return id != null && id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : System.identityHashCode(this);
  }
}
