// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the audio bridge request to enable recording on a room by saving the individual contributions
 * of participants to separate MJR files.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeEnableMjrsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeEnableMjrsRequest extends AudioBridgeRequest {

  public static final String ENABLE_MJRS = "enable_mjrs";

  private String  request;
  private String  room;
  private String  secret;
  private Boolean mjrs;
  private String  mjrsDir;

  public static AudioBridgeEnableMjrsRequest create() {
    return new AudioBridgeEnableMjrsRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeEnableMjrsRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeEnableMjrsRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getSecret() {
    return secret;
  }

  public AudioBridgeEnableMjrsRequest secret(String secret) {
    this.secret = secret;
    return this;
  }

  public Boolean isMjrs() {
    return mjrs;
  }

  public AudioBridgeEnableMjrsRequest mjrs(boolean mjrs) {
    this.mjrs = mjrs;
    return this;
  }

  public String getMjrsDir() {
    return mjrsDir;
  }

  public AudioBridgeEnableMjrsRequest mjrsDir(String mjrsDir) {
    this.mjrsDir = mjrsDir;
    return this;
  }
}
