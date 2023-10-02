// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Feed {

  private MediaType type;
  private String    userId;

  public static Feed create() {
    return new Feed();
  }

  public MediaType getType() {
    return type;
  }

  public Feed type(MediaType type) {
    this.type = type;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public Feed userId(String userId) {
    this.userId = userId;
    return this;
  }

  public static Feed fromString(String feedId) {
    String[] typeUserId = feedId.split("/");
    if (typeUserId.length != 2) {
      return Feed.create();
    }
    return Feed.create().type(MediaType.valueOf(typeUserId[1].toUpperCase())).userId(typeUserId[0]);
  }

  @Override
  public String toString() {
    return userId + "/" + getType().toString().toLowerCase();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Feed)) {
      return false;
    }
    Feed feed = (Feed) o;
    return getType() == feed.getType() && Objects.equals(getUserId(), feed.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getUserId());
  }
}