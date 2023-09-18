// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.zextras.carbonio.chats.core.infrastructure.videoserver.data.media.Stream;
import java.util.List;

/**
 * This class represents the video room request to switch any of the subscription streams to a different publisher in a
 * room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomSwitchRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomSwitchRequest extends VideoRoomRequest {

  public static final String SWITCH = "switch";

  private String       request;
  private List<Stream> streams;

  public static VideoRoomSwitchRequest create() {
    return new VideoRoomSwitchRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomSwitchRequest request(String request) {
    this.request = request;
    return this;
  }

  public List<Stream> getStreams() {
    return streams;
  }

  public VideoRoomSwitchRequest streams(List<Stream> subscriptions) {
    this.streams = subscriptions;
    return this;
  }
}
