// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import com.zextras.carbonio.chats.core.data.type.JoinStatus;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "WAITING_PARTICIPANT", schema = "CHATS")
public class WaitingParticipant {

  @Id
  @Column(name = "ID", length = 36, nullable = false)
  private String id;

  @Column(name = "USER_ID", length = 36, nullable = false)
  private String userId;

  @Column(name = "MEETING_ID", length = 36, nullable = false)
  private String meetingId;

  @Column(name = "QUEUE_ID", length = 36, nullable = false)
  private String queueId;

  @Column(name = "STATUS", length = 32, nullable = false)
  @Enumerated(EnumType.STRING)
  private JoinStatus status;

  public static WaitingParticipant create() {
    return new WaitingParticipant();
  }

  public String getId() {
    return id;
  }

  public WaitingParticipant id(String id) {
    this.id = id;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public WaitingParticipant userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public WaitingParticipant meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getQueueId() {
    return queueId;
  }

  public WaitingParticipant queueId(String queueId) {
    this.queueId = queueId;
    return this;
  }

  public JoinStatus getStatus() {
    return status;
  }

  public WaitingParticipant status(JoinStatus status) {
    this.status = status;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WaitingParticipant user = (WaitingParticipant) o;
    return Objects.equals(id, user.id)
        && Objects.equals(userId, user.userId)
        && Objects.equals(meetingId, user.meetingId)
        && Objects.equals(status, user.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, userId, meetingId, status);
  }
}
