// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the audio bridge request to enable recording on a room by saving the
 * individual contributions of participants to separate MJR files.
 *
 * @see <a
 *     href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeEnableMjrsRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeEnableMjrsRequest extends AudioBridgeRequest {

  public static final String ENABLE_MJRS = "enable_mjrs";

  private String request;
  private String room;
  private Boolean mjrs;

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

  public Boolean getMjrs() {
    return mjrs;
  }

  public AudioBridgeEnableMjrsRequest mjrs(boolean mjrs) {
    this.mjrs = mjrs;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AudioBridgeEnableMjrsRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getMjrs(), that.getMjrs());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getMjrs());
  }
}
