// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * This class represents the description you can set in video room requests.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomDescription</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoRoomDescription {

  private String mid;
  private String description;

  public static VideoRoomDescription create() {
    return new VideoRoomDescription();
  }

  public String getMid() {
    return mid;
  }

  public VideoRoomDescription mid(String mid) {
    this.mid = mid;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public VideoRoomDescription description(String description) {
    this.description = description;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomDescription that)) return false;
    return Objects.equals(getMid(), that.getMid())
        && Objects.equals(getDescription(), that.getDescription());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getMid(), getDescription());
  }
}
