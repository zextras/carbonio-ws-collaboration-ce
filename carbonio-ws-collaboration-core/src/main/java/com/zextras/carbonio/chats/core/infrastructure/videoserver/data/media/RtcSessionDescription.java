// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RtcSessionDescription {

  private RtcType type;
  private String  sdp;

  public RtcSessionDescription() {
  }

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RtcSessionDescription)) {
      return false;
    }
    RtcSessionDescription that = (RtcSessionDescription) o;
    return getType() == that.getType() && Objects.equals(getSdp(), that.getSdp());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), getSdp());
  }
}