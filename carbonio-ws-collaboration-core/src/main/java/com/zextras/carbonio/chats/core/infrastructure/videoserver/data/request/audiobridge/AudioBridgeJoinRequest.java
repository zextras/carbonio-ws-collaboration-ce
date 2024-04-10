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
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeJoinRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeJoinRequest extends AudioBridgeRequest {

  public static final String JOIN = "join";

  public static final String FILENAME_DEFAULT = "audio";

  private String request;
  private String room;
  private String id;
  private Boolean muted;
  private String filename;

  public static AudioBridgeJoinRequest create() {
    return new AudioBridgeJoinRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeJoinRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeJoinRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getId() {
    return id;
  }

  public AudioBridgeJoinRequest id(String id) {
    this.id = id;
    return this;
  }

  public Boolean getMuted() {
    return muted;
  }

  public AudioBridgeJoinRequest muted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public String getFilename() {
    return filename;
  }

  public AudioBridgeJoinRequest filename(String filename) {
    this.filename = filename;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AudioBridgeJoinRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getId(), that.getId())
        && Objects.equals(getMuted(), that.getMuted())
        && Objects.equals(getFilename(), that.getFilename());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getId(), getMuted(), getFilename());
  }
}
