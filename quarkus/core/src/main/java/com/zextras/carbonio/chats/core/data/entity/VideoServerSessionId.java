// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VideoServerSessionId implements Serializable {

  private static final long serialVersionUID = -164436656214153498L;

  @Column(name = "USER_ID", length = 64, nullable = false)
  private String userId;

  @Column(name = "MEETING_ID", length = 64, nullable = false)
  private String meetingId;

  public VideoServerSessionId() {}

  public VideoServerSessionId(String userId, String meetingId) {
    this.userId = userId;
    this.meetingId = meetingId;
  }

  public static VideoServerSessionId create() {
    return new VideoServerSessionId();
  }

  public static VideoServerSessionId create(String sessionId, String meetingId) {
    return new VideoServerSessionId(sessionId, meetingId);
  }

  public String getUserId() {
    return userId;
  }

  public VideoServerSessionId userId(String userId) {
    this.userId = userId;
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
    return Objects.equals(getUserId(), that.getUserId())
        && Objects.equals(getMeetingId(), that.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getUserId(), getMeetingId());
  }
}
