// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RtcSessionDescription {

  private RtcType type;
  private String  sdp;

  public static RtcSessionDescription create() {
    return new RtcSessionDescription();
  }

  public RtcType getType() {
    return type;
  }

  public RtcSessionDescription type(RtcType type) {
    this.type = type;
    return this;
  }

  public String getSdp() {
    return sdp;
  }

  public RtcSessionDescription sdp(String sdp) {
    this.sdp = sdp;
    return this;
  }
}