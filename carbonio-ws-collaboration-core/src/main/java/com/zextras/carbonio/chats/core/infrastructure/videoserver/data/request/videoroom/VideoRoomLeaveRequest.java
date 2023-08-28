// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to leave a room. This will also implicitly unpublish you if you were an
 * active publisher in the room or close a subscription and tear down the related PeerConnection if you were a
 * subscriber in the room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomLeaveRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomLeaveRequest extends VideoRoomRequest {

  public static final String LEAVE = "leave";

  private String request;

  public static VideoRoomLeaveRequest create() {
    return new VideoRoomLeaveRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomLeaveRequest request(String request) {
    this.request = request;
    return this;
  }
}
