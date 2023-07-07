// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the video room request to close a subscription and tear down the related PeerConnection.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/videoroom.html">VideoRoomLeaveSubscriberRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class VideoRoomLeaveSubscriberRequest extends VideoRoomRequest {

  public static final String LEAVE = "leave";

  private String request;

  public static VideoRoomLeaveSubscriberRequest create() {
    return new VideoRoomLeaveSubscriberRequest();
  }

  public String getRequest() {
    return request;
  }

  public VideoRoomLeaveSubscriberRequest request(String request) {
    this.request = request;
    return this;
  }
}
