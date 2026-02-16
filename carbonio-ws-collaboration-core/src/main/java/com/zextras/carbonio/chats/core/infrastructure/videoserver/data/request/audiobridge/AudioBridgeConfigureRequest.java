// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the audio bridge request to join a room.
 *
 * @see <a href= "https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeJoinRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeConfigureRequest extends AudioBridgeRequest {

  public static final String CONFIGURE = "configure";

  private String request;

  public static AudioBridgeConfigureRequest create() {
    return new AudioBridgeConfigureRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeConfigureRequest request(String request) {
    this.request = request;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) return false;
    AudioBridgeConfigureRequest that = (AudioBridgeConfigureRequest) o;
    return Objects.equals(request, that.request);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(request);
  }
}
