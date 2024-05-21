// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the video room request to publish a stream in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomPublishRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomPublishRequest extends VideoRoomRequest {

  public static final String PUBLISH = "publish";

  private String request;
  private String filename;

  public static VideoRoomPublishRequest create() {
    return new VideoRoomPublishRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomPublishRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public VideoRoomPublishRequest filename(String fileName) {
    this.filename = fileName;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof VideoRoomPublishRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getFilename(), that.getFilename());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getFilename());
  }
}
