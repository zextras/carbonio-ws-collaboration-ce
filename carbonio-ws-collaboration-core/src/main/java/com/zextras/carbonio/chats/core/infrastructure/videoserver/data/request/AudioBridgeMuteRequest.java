// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to mute/unmute a session in a room or the entire room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeMuteRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeMuteRequest {

  public static final String MUTE        = "mute";
  public static final String UNMUTE      = "unmute";
  public static final String MUTE_ROOM   = "mute_room";
  public static final String UNMUTE_ROOM = "unmute_room";

  private String request;
  private String secret;
  private String room;
  private String id;

  public static AudioBridgeMuteRequest create() {
    return new AudioBridgeMuteRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeMuteRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeMuteRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeMuteRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public AudioBridgeMuteRequest id(String id) {
    this.id = id;
    return this;
  }
}
