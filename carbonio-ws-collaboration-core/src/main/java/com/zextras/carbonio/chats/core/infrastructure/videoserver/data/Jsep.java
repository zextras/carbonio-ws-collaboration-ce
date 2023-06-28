// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Jsep {

  private String type;
  private String sdp;

  public static Jsep create() {
    return new Jsep();
  }

  public String getType() {
    return type;
  }

  public Jsep type(String type) {
    this.type = type;
    return this;
  }

  public String getSdp() {
    return sdp;
  }

  public Jsep sdp(String sdp) {
    this.sdp = sdp;
    return this;
  }
}