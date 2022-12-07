package com.zextras.carbonio.chats.core.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class VideoServerSessionUserId implements Serializable {

  private static final long serialVersionUID = -164436656214153498L;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "SESSION_ID", length = 64, nullable = false)
  private String sessionId;

  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  public VideoServerSessionUserId() {
  }

  public VideoServerSessionUserId(String userId, String sessionId, String meetingId) {
    this.userId = userId;
    this.sessionId = sessionId;
    this.meetingId = meetingId;
  }

  public static VideoServerSessionUserId create() {
    return new VideoServerSessionUserId();
  }

  public static VideoServerSessionUserId create(String userId, String sessionId, String meetingId) {
    return new VideoServerSessionUserId(userId, sessionId, meetingId);
  }

  public String getUserId() {
    return userId;
  }

  public VideoServerSessionUserId userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getSessionId() {
    return sessionId;
  }

  public VideoServerSessionUserId sessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public VideoServerSessionUserId meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VideoServerSessionUserId)) {
      return false;
    }
    VideoServerSessionUserId that = (VideoServerSessionUserId) o;
    return Objects.equals(getUserId(), that.getUserId()) && Objects.equals(getSessionId(),
      that.getSessionId()) && Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUserId(), getSessionId(), getMeetingId());
  }
}
