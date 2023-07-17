// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to allow session to leave a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeLeaveRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeLeaveRequest extends AudioBridgeRequest {

  public static final String LEAVE = "leave";

  private String request;

  public static AudioBridgeLeaveRequest create() {
    return new AudioBridgeLeaveRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeLeaveRequest request(String request) {
    this.request = request;
    return this;
  }
}
