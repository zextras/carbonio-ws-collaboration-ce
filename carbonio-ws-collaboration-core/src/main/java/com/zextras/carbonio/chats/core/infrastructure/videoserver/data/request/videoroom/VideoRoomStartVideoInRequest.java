// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.videoroom;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to complete the setup a PeerConnection sending a JSEP SDP answer back to
 * the plugin and start receiving media streams from the other participants.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomUnpublishRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomStartVideoInRequest extends VideoRoomRequest {

  public static final String START = "start";

  private String request;

  public static VideoRoomStartVideoInRequest create() {
    return new VideoRoomStartVideoInRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomStartVideoInRequest request(String request) {
    this.request = request;
    return this;
  }
}
