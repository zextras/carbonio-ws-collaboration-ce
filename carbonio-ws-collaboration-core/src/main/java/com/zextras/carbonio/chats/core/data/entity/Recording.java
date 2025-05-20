// SPDX-FileCopyrightText: 2024 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.RecordingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.time.OffsetDateTime;

@Entity
@Table(name = "RECORDING", schema = "CHATS")
public class Recording {

  @Id
  @Column(name = "ID", length = 64, nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "MEETING_ID")
  private Meeting meeting;

  @Column(name = "STARTER_ID", length = 64, nullable = false)
  private String starterId;

  @Column(name = "STARTED_AT")
  @Temporal(TemporalType.TIMESTAMP)
  private OffsetDateTime startedAt;

  @Column(name = "STATUS", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private RecordingStatus status;

  public static Recording create() {
    return new Recording();
  }

  public String getId() {
    return id;
  }

  public Recording id(String id) {
    this.id = id;
    return this;
  }

  public Meeting getMeeting() {
    return meeting;
  }

  public Recording meeting(Meeting meeting) {
    this.meeting = meeting;
    return this;
  }

  public String getStarterId() {
    return starterId;
  }

  public Recording starterId(String starterId) {
    this.starterId = starterId;
    return this;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public Recording startedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
    return this;
  }

  public RecordingStatus getStatus() {
    return status;
  }

  public Recording status(RecordingStatus status) {
    this.status = status;
    return this;
  }
}
