// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Stream {

  private Feed   feed;
  private String mid;
  private String crossrefid;
  private String subMid;

  public static Stream create() {
    return new Stream();
  }

  public Feed getFeed() {
    return feed;
  }

  public Stream feed(Feed feed) {
    this.feed = feed;
    return this;
  }

  public String getMid() {
    return mid;
  }

  public Stream mid(String mid) {
    this.mid = mid;
    return this;
  }

  public String getCrossrefid() {
    return crossrefid;
  }

  public Stream crossrefid(String crossrefid) {
    this.crossrefid = crossrefid;
    return this;
  }

  public String getSubMid() {
    return subMid;
  }

  public Stream subMid(String subMid) {
    this.subMid = subMid;
    return this;
  }
}