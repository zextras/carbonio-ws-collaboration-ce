// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SubscribedStream {

  private String type;
  private String userId;
  private String mid;

  public static SubscribedStream create() {
    return new SubscribedStream();
  }

  public String getType() {
    return type;
  }

  public SubscribedStream type(String type) {
    this.type = type;
    return this;
  }

  public String getUserId() {
    return userId;
  }

  public SubscribedStream userId(String userId) {
    this.userId = userId;
    return this;
  }

  public String getMid() {
    return mid;
  }

  public SubscribedStream mid(String mid) {
    this.mid = mid;
    return this;
  }
}
