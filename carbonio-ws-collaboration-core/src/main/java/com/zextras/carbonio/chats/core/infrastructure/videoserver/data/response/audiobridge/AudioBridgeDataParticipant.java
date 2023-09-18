// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the participant data contained in the audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeDataParticipant {

  private String  id;
  private String  display;
  private Boolean setup;
  private Boolean muted;
  private Boolean talking;
  private Integer spatialPosition;

  public static AudioBridgeDataParticipant create() {
    return new AudioBridgeDataParticipant();
  }

  public String getId() {
    return id;
  }

  public AudioBridgeDataParticipant id(String id) {
    this.id = id;
    return this;
  }

  public String getDisplay() {
    return display;
  }

  public AudioBridgeDataParticipant display(String display) {
    this.display = display;
    return this;
  }

  public Boolean getSetup() {
    return setup;
  }

  public AudioBridgeDataParticipant setup(boolean setup) {
    this.setup = setup;
    return this;
  }

  public Boolean getMuted() {
    return muted;
  }

  public AudioBridgeDataParticipant setMuted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public Boolean getTalking() {
    return talking;
  }

  public AudioBridgeDataParticipant talking(boolean talking) {
    this.talking = talking;
    return this;
  }

  public Integer getSpatialPosition() {
    return spatialPosition;
  }

  public AudioBridgeDataParticipant spatialPosition(int spatialPosition) {
    this.spatialPosition = spatialPosition;
    return this;
  }
}
