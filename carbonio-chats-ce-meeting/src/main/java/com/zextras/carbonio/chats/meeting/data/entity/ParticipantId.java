package com.zextras.carbonio.chats.meeting.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ParticipantId implements Serializable {

  private static final long serialVersionUID = 6523005149894495425L;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  @Column(name = "SESSION_ID", length = 64, nullable = false)
  private String sessionId;


  public ParticipantId() {
  }

  public ParticipantId(String userId, String meetingId, String sessionId) {
    this.userId = userId;
    this.meetingId = meetingId;
    this.sessionId = sessionId;
  }

  public String getUserId() {
    return userId;
  }

  public ParticipantId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public ParticipantId meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public String getSessionId() {
    return sessionId;
  }

  public ParticipantId sessionId(String sessionId) {
    this.sessionId = sessionId;
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
    return Objects.equals(getUserId(), that.getUserId()) &&
      Objects.equals(getMeetingId(), that.getMeetingId()) &&
      Objects.equals(getSessionId(), that.getSessionId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUserId(), getMeetingId(), getSessionId());
  }
}
