// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to list all rooms.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomListRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomListRequest {

  public static final String LIST = "list";

  private String request;

  public static VideoRoomListRequest create() {
    return new VideoRoomListRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomListRequest request(String request) {
    this.request = request;
    return this;
  }

}
