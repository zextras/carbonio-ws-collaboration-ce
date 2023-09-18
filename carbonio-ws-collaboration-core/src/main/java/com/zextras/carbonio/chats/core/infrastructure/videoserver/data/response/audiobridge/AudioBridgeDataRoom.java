// SPDX-FileCopyrightText: 2023 Zextras <https://www.zextras.com>
//
// SPDX-License-Identifier: AGPL-3.0-only

package com.zextras.carbonio.chats.core.infrastructure.videoserver.data.response.audiobridge;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * This class represents the data room contained in the audio bridge response provided by VideoServer.
 *
 * @see <a href="https://janus.conf.meetecho.com/docs/audiobridge.html">AudioBridgeResponse</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AudioBridgeDataRoom {

  private String  room;
  private String  description;
  private Boolean pinRequired;
  private Long    samplingRate;
  private Boolean spatialAudio;
  private Boolean record;
  private Boolean muted;
  private Integer numParticipants;

  public static AudioBridgeDataRoom create() {
    return new AudioBridgeDataRoom();
  }

  public String getRoom() {
    return room;
  }

  public AudioBridgeDataRoom room(String room) {
    this.room = room;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public AudioBridgeDataRoom description(String description) {
    this.description = description;
    return this;
  }

  public Boolean getPinRequired() {
    return pinRequired;
  }

  public AudioBridgeDataRoom pinRequired(boolean pinRequired) {
    this.pinRequired = pinRequired;
    return this;
  }

  public Long getSamplingRate() {
    return samplingRate;
  }

  public AudioBridgeDataRoom samplingRate(long samplingRate) {
    this.samplingRate = samplingRate;
    return this;
  }

  public Boolean getSpatialAudio() {
    return spatialAudio;
  }

  public AudioBridgeDataRoom spatialAudio(boolean spatialAudio) {
    this.spatialAudio = spatialAudio;
    return this;
  }

  public Boolean getRecord() {
    return record;
  }

  public AudioBridgeDataRoom record(boolean record) {
    this.record = record;
    return this;
  }

  public Boolean getMuted() {
    return muted;
  }

  public AudioBridgeDataRoom muted(boolean muted) {
    this.muted = muted;
    return this;
  }

  public Integer getNumParticipants() {
    return numParticipants;
  }

  public AudioBridgeDataRoom numParticipants(int numParticipants) {
    this.numParticipants = numParticipants;
    return this;
  }
}
