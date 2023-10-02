// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;
import java.util.Objects;

/**
 * This class represents the video room request to subscribe to multiple streams available in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomSubscribeRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomSubscribeRequest extends VideoRoomRequest {

  public static final String SUBSCRIBE = "subscribe";

  private String       request;
  private List<Stream> streams;

  public static VideoRoomSubscribeRequest create() {
    return new VideoRoomSubscribeRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomSubscribeRequest request(String request) {
    this.request = request;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomSubscribeRequest streams(List<Stream> streams) {
    this.streams = streams;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof VideoRoomSubscribeRequest)) {
      return false;
    }
    VideoRoomSubscribeRequest that = (VideoRoomSubscribeRequest) o;
    return Objects.equals(getRequest(), that.getRequest()) && Objects.equals(getStreams(),
      that.getStreams());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getStreams());
  }
}
