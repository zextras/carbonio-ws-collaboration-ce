// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to list all rooms.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeListRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeListRequest extends AudioBridgeRequest {

  public static final String LIST = "list";

  private String request;

  public static AudioBridgeListRequest create() {
    return new AudioBridgeListRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeListRequest request(String request) {
    this.request = request;
    return this;
  }

}
