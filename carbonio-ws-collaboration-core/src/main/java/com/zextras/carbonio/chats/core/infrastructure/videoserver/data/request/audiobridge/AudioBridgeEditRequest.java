// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.request.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Objects;

/**
 * This class represents the audio bridge request to edit a room.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeEditRequest</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeEditRequest extends AudioBridgeRequest {

  public static final String EDIT = "edit";

  private String request;
  private String room;
  private String newMjrsDir;

  public static AudioBridgeEditRequest create() {
    return new AudioBridgeEditRequest();
  }

  public String getRequest() {
    return request;
  }

  public AudioBridgeEditRequest request(String request) {
    this.request = request;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeEditRequest room(String room) {
    this.room = room;
    return this;
  }

  public String getNewMjrsDir() {
    return newMjrsDir;
  }

  public AudioBridgeEditRequest newMjrsDir(String newMjrsDir) {
    this.newMjrsDir = newMjrsDir;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AudioBridgeEditRequest that)) return false;
    return Objects.equals(getRequest(), that.getRequest())
        && Objects.equals(getRoom(), that.getRoom())
        && Objects.equals(getNewMjrsDir(), that.getNewMjrsDir());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRequest(), getRoom(), getNewMjrsDir());
  }
}
