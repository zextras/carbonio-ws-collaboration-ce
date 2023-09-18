// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the audio bridge request to destroy a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeDestroyRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeDestroyRequest extends AudioBridgeRequest {

  public static final String DESTROY = "destroy";

  private String  request;
  private String  room;
  private String  secret;
  private Boolean permanent;

  public static AudioBridgeDestroyRequest create() {
    return new AudioBridgeDestroyRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeDestroyRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeDestroyRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeDestroyRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public Boolean getPermanent() {
    return permanent;
  }

  public AudioBridgeDestroyRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AudioBridgeDestroyRequest)) {
      return false;
    }
    AudioBridgeDestroyRequest that = (AudioBridgeDestroyRequest) o;
    return Objects.equals(getRequest(), that.getRequest()) && Objects.equals(getRoom(),
      that.getRoom()) && Objects.equals(getSecret(), that.getSecret()) && Objects.equals(
      getPermanent(), that.getPermanent());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getSecret(), getPermanent());
  }
}
