// SPDX-FileCopyrightText: 2026 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ConfigureStream {

  private String mid;
  // Janus simulcast layer indices 0-2: substream = spatial/resolution layer, temporal = framerate
  // layer; higher = better.
  private Integer substream;
  private Integer temporal;

  public static ConfigureStream create() {
    return new ConfigureStream();
  }

  public String getMid() {
    return mid;
  }

  public ConfigureStream mid(String mid) {
    this.mid = mid;
    return this;
  }

  public Integer getSubstream() {
    return substream;
  }

  public ConfigureStream substream(Integer substream) {
    this.substream = substream;
    return this;
  }

  public Integer getTemporal() {
    return temporal;
  }

  public ConfigureStream temporal(Integer temporal) {
    this.temporal = temporal;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConfigureStream that)) return false;
    return Objects.equals(mid, that.mid)
        && Objects.equals(substream, that.substream)
        && Objects.equals(temporal, that.temporal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mid, substream, temporal);
  }
}
