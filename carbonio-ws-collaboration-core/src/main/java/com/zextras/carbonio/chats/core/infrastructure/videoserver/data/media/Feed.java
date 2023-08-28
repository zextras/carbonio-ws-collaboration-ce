// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Feed {

  private MediaType type;
  private String    sessionId;

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

  public String getSessionId() {
    return sessionId;
  }

  public Feed sessionId(String sessionId) {
    this.sessionId = sessionId;
    return this;
  }

  @Override
  public String toString() {
    return sessionId + "/" + getType().toString().toLowerCase();
  }
}