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

  public AudioBridgeDataRoom() {
  }

  public String getRoom() {
    return room;
  }

  public String getDescription() {
    return description;
  }

  public boolean isPinRequired() {
    return pinRequired;
  }

  public long getSamplingRate() {
    return samplingRate;
  }

  public boolean isSpatialAudio() {
    return spatialAudio;
  }

  public boolean isRecord() {
    return record;
  }

  public boolean isMuted() {
    return muted;
  }

  public int getNumParticipants() {
    return numParticipants;
  }
}
