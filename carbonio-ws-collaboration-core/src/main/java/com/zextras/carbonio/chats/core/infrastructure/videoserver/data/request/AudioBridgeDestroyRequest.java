// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to destroy a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeDestroyRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeDestroyRequest {

  public static final String DESTROY = "destroy";

  private String  request;
  private String  room;
  private String  secret;
  private boolean permanent;

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

  public boolean isPermanent() {
    return permanent;
  }

  public AudioBridgeDestroyRequest permanent(boolean permanent) {
    this.permanent = permanent;
    return this;
  }
}
