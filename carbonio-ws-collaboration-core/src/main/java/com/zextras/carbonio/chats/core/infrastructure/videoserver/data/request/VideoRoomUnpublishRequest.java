// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to unpublish a stream in a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomUnpublishRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomUnpublishRequest extends VideoRoomRequest {

  public static final String UNPUBLISH = "unpublish";

  private String request;

  public static VideoRoomUnpublishRequest create() {
    return new VideoRoomUnpublishRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomUnpublishRequest request(String request) {
    this.request = request;
    return this;
  }
}
