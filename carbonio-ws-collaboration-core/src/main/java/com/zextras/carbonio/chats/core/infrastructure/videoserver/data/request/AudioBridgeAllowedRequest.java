// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;

/**
 * This class represents the audio bridge request to change ACL for a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeAllowedRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeAllowedRequest extends AudioBridgeRequest {

  public static final String ALLOWED = "allowed";
  public static final String ENABLE  = "enable";
  public static final String DISABLE = "disable";
  public static final String ADD     = "add";
  public static final String REMOVE  = "remove";

  private String       request;
  private String       secret;
  private String       action;
  private String       room;
  private List<String> allowed;

  public static AudioBridgeAllowedRequest create() {
    return new AudioBridgeAllowedRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeAllowedRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeAllowedRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public String getAction() {
    return action;
  }

  public AudioBridgeAllowedRequest action(String action) {
    this.action = action;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeAllowedRequest room(String room) {
    this.room = room;
    return this;
  }

  public List<String> getAllowed() {
    return allowed;
  }

  public AudioBridgeAllowedRequest allowed(List<String> allowed) {
    this.allowed = allowed;
    return this;
  }
}
