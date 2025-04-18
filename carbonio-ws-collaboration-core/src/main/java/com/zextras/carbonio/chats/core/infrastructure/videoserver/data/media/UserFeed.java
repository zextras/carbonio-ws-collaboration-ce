// SPDX-FileCopyrightText: 2025 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import java.util.Objects;

public class UserFeed {

  private MediaTrackType mediaTrackType;
  private String userId;
  private String meetingId;

  public static UserFeed create() {
    return new UserFeed();
  }

  public MediaTrackType getMediaTrackType() {
    return mediaTrackType;
  }

  public UserFeed mediaTrackType(MediaTrackType mediaTrackType) {
    this.mediaTrackType = mediaTrackType;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public UserFeed userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getMeetingId() {
    return meetingId;
  }

  public UserFeed meetingId(String meetingId) {
    this.meetingId = meetingId;
    return this;
  }

  public static UserFeed fromString(String opaqueId) {
    String[] userFeedId = opaqueId.split("/");
    if (userFeedId.length != 3) {
      return UserFeed.create();
    }
    return UserFeed.create()
        .mediaTrackType(MediaTrackType.fromString(userFeedId[0]))
        .userId(userFeedId[1])
        .meetingId(userFeedId[2]);
  }

  @Override
  public String toString() {
    return mediaTrackType.getType() + "/" + userId + "/" + meetingId;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof UserFeed userFeed)) return false;
    return getMediaTrackType() == userFeed.getMediaTrackType()
        && Objects.equals(getUserId(), userFeed.getUserId())
        && Objects.equals(getMeetingId(), userFeed.getMeetingId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMediaTrackType(), getUserId(), getMeetingId());
  }
}
