// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ParticipantId implements Serializable {

  private static final long serialVersionUID = 6523005149894495425L;

  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  public ParticipantId() {
  }

  public ParticipantId(String meetingId, String userId) {
    this.meetingId = meetingId;
    this.userId = userId;
  }

  public static ParticipantId create() {
    return new ParticipantId();
  }

  public static ParticipantId create(String meetingId, String userId) {
    return new ParticipantId(meetingId, userId);
  }


  public String getMeetingId() {
    return meetingId;
  }

  public ParticipantId meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public ParticipantId userId(String userId) {
    this.userId = userId;
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
    ParticipantId that = (ParticipantId) o;
    return Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMeetingId(), getUserId());
  }
}
