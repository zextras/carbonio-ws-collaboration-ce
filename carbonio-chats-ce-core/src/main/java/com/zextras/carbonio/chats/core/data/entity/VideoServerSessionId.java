// SPDX-FileCopyrightText: 2022 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class VideoServerSessionId implements Serializable {

  private static final long serialVersionUID = -164436656214153498L;

  @Column(name = "SESSION_ID", length = 64, nullable = false)
  private String sessionId;

  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  public VideoServerSessionId() {
  }

  public VideoServerSessionId(String sessionId, String meetingId) {
    this.sessionId = sessionId;
    this.meetingId = meetingId;
  }

  public static VideoServerSessionId create() {
    return new VideoServerSessionId();
  }

  public static VideoServerSessionId create(String sessionId, String meetingId) {
    return new VideoServerSessionId(sessionId, meetingId);
  }

  public String getSessionId() {
    return sessionId;
  }

  public VideoServerSessionId sessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public VideoServerSessionId meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VideoServerSessionId)) {
      return false;
    }
    VideoServerSessionId that = (VideoServerSessionId) o;
    return Objects.equals(getSessionId(), that.getSessionId()) && Objects.equals(getMeetingId(),
      that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getSessionId(), getMeetingId());
  }
}
