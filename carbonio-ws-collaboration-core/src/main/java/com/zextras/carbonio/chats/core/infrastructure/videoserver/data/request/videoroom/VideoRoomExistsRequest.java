// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to check if a room exists.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomExistsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomExistsRequest extends VideoRoomRequest {

  public static final String EXISTS = "exists";

  private String request;
  private String room;

  public static VideoRoomExistsRequest create() {
    return new VideoRoomExistsRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomExistsRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public VideoRoomExistsRequest room(String room) {
    this.room = room;
    return this;
  }
}
