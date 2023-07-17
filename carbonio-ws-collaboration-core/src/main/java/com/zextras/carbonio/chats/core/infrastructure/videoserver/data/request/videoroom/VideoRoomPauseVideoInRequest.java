// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to temporarily pause and resume later the whole media delivery of media
 * streams available in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomPauseVideoInRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomPauseVideoInRequest extends VideoRoomRequest {

  public static final String PAUSE = "pause";

  private String request;

  public static VideoRoomPauseVideoInRequest create() {
    return new VideoRoomPauseVideoInRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomPauseVideoInRequest request(String request) {
    this.request = request;
    return this;
  }
}
