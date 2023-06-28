// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to kick a session from the audio room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeKickRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeKickRequest {

  public static final String KICK     = "kick";
  public static final String KICK_ALL = "kick_all";

  private String request;
  private String secret;
  private String room;
  private String id;

  public static AudioBridgeKickRequest create() {
    return new AudioBridgeKickRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeKickRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeKickRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeKickRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public AudioBridgeKickRequest id(String id) {
    this.id = id;
    return this;
  }
}
